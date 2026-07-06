package com.novaleap.api.module.ai.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.module.ai.audit.AiCallAuditService;
import com.novaleap.api.module.ai.config.AiGatewayProperties;
import com.novaleap.api.module.ai.config.NovaLeapAiProperties;
import com.novaleap.api.module.ai.dto.AiChatRequest;
import com.novaleap.api.module.quota.config.AiQuotaProperties;
import com.novaleap.api.module.quota.support.AiQuotaUsageSupport;
import com.novaleap.api.service.AiLimitService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * AI 模型网关 —— 所有调用大模型的请求统一由此类处理。
 * <p>
 * 实现 {@link INovaLeapAiService} 接口，支持流式和非流式对话。
 * 内部使用 Spring AI {@link ChatClient}（程序化构建，配置来源于 {@code novaleap.ai.*}）。
 * </p>
 *
 * <h3>核心功能</h3>
 * <ul>
 *   <li>熔断器（Circuit Breaker）：基于 Redis 的失败计数和自动熔断</li>
 *   <li>自动重试：网络抖动时自动重试 + 指数退避</li>
 *   <li>Token 配额管理：模型级 + 全局级配额检查，动态降级</li>
 *   <li>模型降级：主模型配额耗尽或异常时自动切换至备用模型</li>
 *   <li>流式 SSE 封装：字符级增量推送 + 完成/超时信号</li>
 * </ul>
 *
 * <h3>切换模型</h3>
 * 只需修改 {@code application.yml} 中的 {@code novaleap.ai.base-url} 和 {@code novaleap.ai.model-name}，
 * 无需改动此文件或任何业务代码。
 */
@Slf4j
@Service
public class AiModelGateway implements INovaLeapAiService {

    private static final String KEY_MODEL_USAGE_PREFIX = "nova:ai:usage:";
    private static final String KEY_CIRCUIT_FAILURE_PREFIX = "nova:ai:circuit:fail:";
    private static final String KEY_CIRCUIT_OPEN_PREFIX = "nova:ai:circuit:open:";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ChatClient chatClient;
    private final StringRedisTemplate redisTemplate;
    private final AiLimitService aiLimitService;
    private final AiQuotaProperties quotaProperties;
    private final AiGatewayProperties gatewayProperties;
    private final AiQuotaUsageSupport aiQuotaUsageSupport;
    private final ObjectMapper objectMapper;
    private final AiCallAuditService aiCallAuditService;
    private final NovaLeapAiProperties novaleapAiProperties;
    private final ExecutorService aiExecutor;

    public AiModelGateway(
            ChatClient chatClient,
            StringRedisTemplate redisTemplate,
            AiLimitService aiLimitService,
            AiQuotaProperties quotaProperties,
            AiGatewayProperties gatewayProperties,
            AiQuotaUsageSupport aiQuotaUsageSupport,
            ObjectMapper objectMapper,
            AiCallAuditService aiCallAuditService,
            NovaLeapAiProperties novaleapAiProperties
    ) {
        this.chatClient = chatClient;
        this.redisTemplate = redisTemplate;
        this.aiLimitService = aiLimitService;
        this.quotaProperties = quotaProperties;
        this.gatewayProperties = gatewayProperties;
        this.aiQuotaUsageSupport = aiQuotaUsageSupport;
        this.objectMapper = objectMapper;
        this.aiCallAuditService = aiCallAuditService;
        this.novaleapAiProperties = novaleapAiProperties;
        this.aiExecutor = Executors.newFixedThreadPool(4, new AiExecutorThreadFactory());
    }

    // ========== INovaLeapAiService 接口实现 ==========

    @Override
    public SseEmitter streamChat(AiChatRequest request,
                                 AiLimitService.AiModule module,
                                 Consumer<String> onCompleted,
                                 String fallbackMessage) {
        String targetModel = resolveModel(request.getModel());
        return streamModelWithRetry(
                request.getSystemPrompt(),
                request.getUserPrompt(),
                fallbackMessage,
                onCompleted,
                targetModel,
                gatewayProperties.getMaxRetryAttempts(),
                true,
                module
        );
    }

    @Override
    public String callChat(AiChatRequest request,
                           AiLimitService.AiModule module,
                           String fallback) {
        try {
            String raw = callChatRaw(request, module);
            return StringUtils.hasText(raw) ? raw.trim() : fallback;
        } catch (Throwable e) {
            String model = "";
            try {
                model = resolveModel(request.getModel());
            } catch (Throwable ignore) {
                // ignore secondary errors
            }
            aiCallAuditService.recordFailure(model, module, e.getMessage());
            log.error("call model failed", e);
            return fallback;
        }
    }

    @Override
    public String callChatRaw(AiChatRequest request,
                              AiLimitService.AiModule module) throws Exception {
        String targetModel = resolveModel(request.getModel());
        try {
            return callModelWithPolicy(
                    request.getSystemPrompt(),
                    request.getUserPrompt(),
                    targetModel,
                    true,
                    module
            );
        } catch (RuntimeException runtimeException) {
            throw runtimeException;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========== 向下兼容的便捷方法（供 AiServiceImpl / AiNoteWorkflowSupport 调用） ==========

    /**
     * 流式问答（便捷重载）。
     *
     * @deprecated 请直接使用 {@link #streamChat(AiChatRequest, AiLimitService.AiModule, Consumer, String)}
     */
    @Deprecated
    public SseEmitter streamModelAnswer(
            String systemPrompt,
            String prompt,
            String fallback,
            Consumer<String> onCompleted,
            AiLimitService.AiModule module
    ) {
        AiChatRequest request = AiChatRequest.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(prompt)
                .build();
        return streamChat(request, module, onCompleted, fallback);
    }

    /**
     * 非流式问答（便捷重载）。
     *
     * @deprecated 请直接使用 {@link #callChat(AiChatRequest, AiLimitService.AiModule, String)}
     */
    @Deprecated
    public String callModel(String systemPrompt, String prompt, String fallback, AiLimitService.AiModule module) {
        AiChatRequest request = AiChatRequest.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(prompt)
                .build();
        return callChat(request, module, fallback);
    }

    /**
     * 非流式问答，异常向上抛（便捷重载）。
     *
     * @deprecated 请直接使用 {@link #callChatRaw(AiChatRequest, AiLimitService.AiModule)}
     */
    @Deprecated
    public String callModelRaw(String systemPrompt, String prompt, AiLimitService.AiModule module) {
        AiChatRequest request = AiChatRequest.builder()
                .systemPrompt(systemPrompt)
                .userPrompt(prompt)
                .build();
        try {
            return callChatRaw(request, module);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 以 SSE 流式格式返回一段静态文本（不调用模型）。
     */
    public SseEmitter streamStaticAnswer(String content) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> {
            try {
                String text = safe(content);
                for (String chunk : splitForSse(text, 48)) {
                    Map<String, String> payload = new LinkedHashMap<>();
                    payload.put("type", "delta");
                    payload.put("content", chunk);
                    emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(payload)));
                    Thread.sleep(22L);
                }

                Map<String, String> done = new LinkedHashMap<>();
                done.put("type", "done");
                emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(done)));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }, aiExecutor);
        return emitter;
    }

    @PreDestroy
    public void shutdownExecutor() {
        aiExecutor.shutdownNow();
    }

    // ========== 私有方法：模型选择与配额检查 ==========

    /**
     * 解析最终使用的模型名称：
     * 优先使用请求中指定的 model；若为空则使用配置文件中的默认模型；
     * 若默认模型配额已耗尽，则降级到备用模型。
     */
    private String resolveModel(String requestModel) {
        String configuredDefault = StringUtils.hasText(requestModel)
                ? requestModel
                : novaleapAiProperties.getModelName();
        if (!isModelQuotaExceeded(configuredDefault)) {
            return configuredDefault;
        }
        String fb = novaleapAiProperties.getFallbackModel();
        return StringUtils.hasText(fb) ? fb : configuredDefault;
    }

    private String resolveCurrentModel() {
        return resolveModel(null);
    }

    // ========== 私有方法：同步调用 ==========

    private String callModelWithPolicy(
            String systemPrompt,
            String prompt,
            String model,
            boolean allowFallback,
            AiLimitService.AiModule module
    ) throws Exception {
        String candidate = StringUtils.hasText(model) ? model : novaleapAiProperties.getModelName();
        if (isCircuitOpen(candidate, module)) {
            if (shouldFallback(candidate, allowFallback)) {
                log.warn("AI circuit is open for model={}, module={}, switching to fallback={}",
                        candidate, module, novaleapAiProperties.getFallbackModel());
                return callModelWithPolicy(systemPrompt, prompt, novaleapAiProperties.getFallbackModel(), false, module);
            }
            throw new IllegalStateException("AI circuit is open");
        }

        try {
            return executeWithRetry(() -> callModelExecution(systemPrompt, prompt, candidate, module), candidate, module);
        } catch (Throwable throwable) {
            Throwable actual = unwrap(throwable);
            recordModelFailure(candidate, module, actual);
            if (isQuotaExceededException(actual) && shouldFallback(candidate, allowFallback)) {
                markModelQuotaExceeded(candidate);
                return callModelWithPolicy(systemPrompt, prompt, novaleapAiProperties.getFallbackModel(), false, module);
            }
            if (isRetryableException(actual) && shouldFallback(candidate, allowFallback)) {
                return callModelWithPolicy(systemPrompt, prompt, novaleapAiProperties.getFallbackModel(), false, module);
            }
            if (actual instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(actual);
        }
    }

    private String callModelExecution(
            String systemPrompt,
            String prompt,
            String model,
            AiLimitService.AiModule module
    ) throws Exception {
        int maxTokens = calculateMaxTokens(model, aiLimitService.getCurrentDegradeLevel(), module);
        ChatResponse response = runWithTimeout(() -> {
            var request = chatClient.prompt();
            if (StringUtils.hasText(systemPrompt)) {
                request = request.system(systemPrompt);
            }
            return request.user(u -> u.text(prompt))
                    .options(OpenAiChatOptions.builder()
                            .withModel(model)
                            .withMaxTokens(maxTokens)
                            .build())
                    .call()
                    .chatResponse();
        });

        clearCircuit(model, module);
        String content = response.getResult().getOutput().getContent();
        recordModelUsage(model, module, response.getMetadata().getUsage());
        return content == null ? "" : content.trim();
    }

    // ========== 私有方法：流式调用 ==========

    private SseEmitter streamModelWithRetry(
            String systemPrompt,
            String prompt,
            String fallback,
            Consumer<String> onCompleted,
            String model,
            int remainingAttempts,
            boolean allowFallback,
            AiLimitService.AiModule module
    ) {
        if (!StringUtils.hasText(model)) {
            return null;
        }

        SseEmitter emitter = new SseEmitter(120_000L);
        StringBuilder answerBuilder = new StringBuilder();
        AtomicBoolean finished = new AtomicBoolean(false);
        emitter.onTimeout(() -> finishModelStream(emitter, answerBuilder, fallback, onCompleted, finished));

        boolean started = subscribeModelStream(
                emitter, answerBuilder, finished,
                systemPrompt, prompt, fallback, onCompleted,
                model, remainingAttempts, allowFallback, module
        );
        return started ? emitter : null;
    }

    private boolean subscribeModelStream(
            SseEmitter emitter,
            StringBuilder answerBuilder,
            AtomicBoolean finished,
            String systemPrompt,
            String prompt,
            String fallback,
            Consumer<String> onCompleted,
            String model,
            int remainingAttempts,
            boolean allowFallback,
            AiLimitService.AiModule module
    ) {
        if (isCircuitOpen(model, module)) {
            if (shouldFallback(model, allowFallback)) {
                return subscribeModelStream(
                        emitter, answerBuilder, finished,
                        systemPrompt, prompt, fallback, onCompleted,
                        novaleapAiProperties.getFallbackModel(),
                        1, false, module
                );
            }
            return false;
        }

        int maxTokens;
        try {
            maxTokens = calculateMaxTokens(model, aiLimitService.getCurrentDegradeLevel(), module);
        } catch (Throwable throwable) {
            Throwable actual = unwrap(throwable);
            recordModelFailure(model, module, actual);
            if (shouldFallback(model, allowFallback)) {
                return subscribeModelStream(
                        emitter, answerBuilder, finished,
                        systemPrompt, prompt, fallback, onCompleted,
                        novaleapAiProperties.getFallbackModel(),
                        1, false, module
                );
            }
            return false;
        }

        try {
            var request = chatClient.prompt();
            if (StringUtils.hasText(systemPrompt)) {
                request = request.system(systemPrompt);
            }
            request.user(u -> u.text(prompt))
                    .options(OpenAiChatOptions.builder()
                            .withModel(model)
                            .withMaxTokens(maxTokens)
                            .build())
                    .stream()
                    .chatResponse()
                    .subscribe(
                            response -> handleStreamChunk(emitter, answerBuilder, model, module, finished, response),
                            error -> handleStreamError(emitter, answerBuilder, finished, systemPrompt, prompt, fallback, onCompleted, model, remainingAttempts, allowFallback, module, error),
                            () -> finishModelStream(emitter, answerBuilder, fallback, onCompleted, finished)
                    );
            return true;
        } catch (Throwable throwable) {
            Throwable actual = unwrap(throwable);
            recordModelFailure(model, module, actual);
            log.warn("stream is unavailable for {}, fallback to static: {}", model, actual.getMessage());
            if (shouldFallback(model, allowFallback)) {
                return subscribeModelStream(
                        emitter, answerBuilder, finished,
                        systemPrompt, prompt, fallback, onCompleted,
                        novaleapAiProperties.getFallbackModel(),
                        1, false, module
                );
            }
            return false;
        }
    }

    private void handleStreamError(
            SseEmitter emitter,
            StringBuilder answerBuilder,
            AtomicBoolean finished,
            String systemPrompt,
            String prompt,
            String fallback,
            Consumer<String> onCompleted,
            String model,
            int remainingAttempts,
            boolean allowFallback,
            AiLimitService.AiModule module,
            Throwable error
    ) {
        Throwable actual = unwrap(error);
        log.warn("stream model {} failed: {}", model, actual.getMessage());
        if (answerBuilder.length() == 0 && remainingAttempts > 1 && isRetryableException(actual)) {
            sleepBackoff(gatewayProperties.getMaxRetryAttempts() - remainingAttempts + 1);
            boolean restarted = subscribeModelStream(
                    emitter, answerBuilder, finished,
                    systemPrompt, prompt, fallback, onCompleted,
                    model, remainingAttempts - 1, allowFallback, module
            );
            if (!restarted) {
                finishModelStream(emitter, answerBuilder, fallback, onCompleted, finished);
            }
            return;
        }
        recordModelFailure(model, module, actual);
        if (answerBuilder.length() == 0
                && shouldFallback(model, allowFallback)
                && (isQuotaExceededException(actual) || isRetryableException(actual))) {
            if (isQuotaExceededException(actual)) {
                markModelQuotaExceeded(model);
            }
            boolean switched = subscribeModelStream(
                    emitter, answerBuilder, finished,
                    systemPrompt, prompt, fallback, onCompleted,
                    novaleapAiProperties.getFallbackModel(),
                    1, false, module
            );
            if (!switched) {
                finishModelStream(emitter, answerBuilder, fallback, onCompleted, finished);
            }
            return;
        }
        finishModelStream(emitter, answerBuilder, fallback, onCompleted, finished);
    }

    // ========== 私有方法：SSE 处理 ==========

    private void handleStreamChunk(
            SseEmitter emitter,
            StringBuilder answerBuilder,
            String model,
            AiLimitService.AiModule module,
            AtomicBoolean finished,
            ChatResponse response
    ) {
        if (finished.get()) {
            return;
        }
        clearCircuit(model, module);
        String chunk = response.getResult().getOutput().getContent();
        if (StringUtils.hasText(chunk)) {
            answerBuilder.append(chunk);
            sendDeltaSafely(emitter, chunk);
        }
        Usage usage = response.getMetadata().getUsage();
        if (usage != null) {
            recordModelUsage(model, module, usage);
        }
    }

    private void finishModelStream(
            SseEmitter emitter,
            StringBuilder answerBuilder,
            String fallback,
            Consumer<String> onCompleted,
            AtomicBoolean finished
    ) {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        String finalText = answerBuilder.length() == 0 ? safe(fallback) : answerBuilder.toString().trim();
        if (answerBuilder.length() == 0 && StringUtils.hasText(finalText)) {
            sendDeltaSafely(emitter, finalText);
        }
        sendDoneSafely(emitter);
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
        if (onCompleted != null) {
            try {
                onCompleted.accept(finalText);
            } catch (Exception e) {
                log.debug("post stream callback failed: {}", e.getMessage());
            }
        }
    }

    private void sendDeltaSafely(SseEmitter emitter, String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        int offset = 0;
        while (offset < chunk.length()) {
            int codePoint = chunk.codePointAt(offset);
            String part = new String(Character.toChars(codePoint));
            try {
                Map<String, String> payload = new LinkedHashMap<>();
                payload.put("type", "delta");
                payload.put("content", part);
                emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(payload)));
            } catch (Exception e) {
                log.debug("send delta failed: {}", e.getMessage());
                return;
            }
            offset += Character.charCount(codePoint);
        }
    }

    private void sendDoneSafely(SseEmitter emitter) {
        try {
            Map<String, String> done = new LinkedHashMap<>();
            done.put("type", "done");
            emitter.send(SseEmitter.event().name("message").data(objectMapper.writeValueAsString(done)));
        } catch (Exception e) {
            log.debug("send done failed: {}", e.getMessage());
        }
    }

    private List<String> splitForSse(String text, int chunkSize) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        int size = Math.max(1, chunkSize);
        List<String> chunks = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int end = Math.min(text.length(), index + size);
            chunks.add(text.substring(index, end));
            index = end;
        }
        return chunks;
    }

    // ========== 私有方法：超时控制 ==========

    private <T> T runWithTimeout(CheckedSupplier<T> supplier) throws Exception {
        Future<T> future = aiExecutor.submit(() -> supplier.get());
        try {
            return future.get(gatewayProperties.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            future.cancel(true);
            throw timeoutException;
        } catch (ExecutionException executionException) {
            Throwable cause = executionException.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new RuntimeException(cause);
        }
    }

    // ========== 私有方法：Token 配额管理 ==========

    private int calculateMaxTokens(String model, int degradeLevel, AiLimitService.AiModule module) {
        int base = switch (module) {
            case RESUME -> 5000;
            case SOLVER -> 1200;
            case COACH -> 1000;
            case CHAT -> 800;
        };

        if (module == AiLimitService.AiModule.RESUME) {
            return applyQuotaBudget(model, base);
        }
        if (degradeLevel > 0) {
            double factor = switch (degradeLevel) {
                case 1 -> 0.85;
                case 2 -> 0.7;
                case 3 -> 0.5;
                default -> 1.0;
            };
            base = (int) (base * factor);
        }
        return applyQuotaBudget(model, base);
    }

    private int applyQuotaBudget(String model, int requestedTokens) {
        long modelRemaining = quotaProperties.getPerModelTokenLimit() - currentModelUsage(model);
        long dailyRemaining = quotaProperties.getDailyTokenLimit() - aiQuotaUsageSupport.currentTokenUsage();
        long allowed = Math.min(requestedTokens, Math.min(modelRemaining, dailyRemaining));
        if (allowed < gatewayProperties.getMinResponseTokens()) {
            throw new IllegalStateException("AI token budget exhausted");
        }
        return (int) allowed;
    }

    private long currentModelUsage(String model) {
        if (!StringUtils.hasText(model)) {
            return 0L;
        }
        String val = redisTemplate.opsForValue().get(modelUsageKey(model));
        if (!StringUtils.hasText(val)) {
            return 0L;
        }
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            return 0L;
        }
    }

    private boolean isModelQuotaExceeded(String model) {
        if (!StringUtils.hasText(model)) {
            return true;
        }
        String val = redisTemplate.opsForValue().get(modelUsageKey(model));
        if (val == null) {
            return false;
        }
        try {
            return Long.parseLong(val) >= quotaProperties.getPerModelTokenLimit();
        } catch (Exception e) {
            return false;
        }
    }

    private void markModelQuotaExceeded(String model) {
        if (!StringUtils.hasText(model)) {
            return;
        }
        String key = modelUsageKey(model);
        redisTemplate.opsForValue().set(key, String.valueOf(quotaProperties.getPerModelTokenLimit()));
        redisTemplate.expire(key, Duration.ofDays(quotaProperties.getModelUsageTtlDays()));
    }

    private void recordModelUsage(String model, AiLimitService.AiModule module, Usage usage) {
        if (usage == null || !StringUtils.hasText(model)) {
            return;
        }
        long totalTokens = usage.getTotalTokens();
        if (totalTokens <= 0) {
            return;
        }
        String usageKey = modelUsageKey(model);
        redisTemplate.opsForValue().increment(usageKey, totalTokens);
        redisTemplate.expire(usageKey, Duration.ofDays(quotaProperties.getModelUsageTtlDays()));
        aiLimitService.recordTokenUsage(totalTokens);
        aiCallAuditService.recordSuccess(model, module, totalTokens);
        log.info("Model {} consumed {} tokens.", model, totalTokens);
    }

    // ========== 私有方法：重试与异常判定 ==========

    private <T> T executeWithRetry(CheckedSupplier<T> supplier, String model, AiLimitService.AiModule module) throws Exception {
        int attempt = 1;
        while (true) {
            try {
                return supplier.get();
            } catch (Throwable throwable) {
                Throwable actual = unwrap(throwable);
                if (attempt >= gatewayProperties.getMaxRetryAttempts()
                        || !isRetryableException(actual)
                        || isQuotaExceededException(actual)) {
                    if (actual instanceof Exception exception) {
                        throw exception;
                    }
                    throw new RuntimeException(actual);
                }
                log.warn("retrying ai call, model={}, module={}, attempt={}, reason={}", model, module, attempt, actual.getMessage());
                sleepBackoff(attempt);
                attempt++;
            }
        }
    }

    private void sleepBackoff(int attempt) {
        long sleepMs = (long) gatewayProperties.getRetryBackoffMs() * Math.max(1, attempt);
        if (sleepMs <= 0) {
            return;
        }
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isQuotaExceededException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        String msg = safe(throwable.getMessage()).toLowerCase();
        return msg.contains("quota")
                || msg.contains("balance")
                || msg.contains("insufficient")
                || msg.contains("429")
                || msg.contains("402")
                || msg.contains("rate limit");
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof TimeoutException
                || throwable instanceof HttpTimeoutException
                || throwable instanceof ConnectException) {
            return true;
        }
        String msg = safe(throwable.getMessage()).toLowerCase();
        return msg.contains("timeout")
                || msg.contains("timed out")
                || msg.contains("connection reset")
                || msg.contains("connection refused")
                || msg.contains("temporarily unavailable")
                || msg.contains("service unavailable")
                || msg.contains("502")
                || msg.contains("503")
                || msg.contains("504");
    }

    private boolean shouldFallback(String model, boolean allowFallback) {
        String fb = novaleapAiProperties.getFallbackModel();
        return allowFallback && StringUtils.hasText(fb) && !fb.equals(model);
    }

    // ========== 私有方法：熔断器 ==========

    private void recordModelFailure(String model, AiLimitService.AiModule module, Throwable throwable) {
        aiCallAuditService.recordFailure(model, module, throwable == null ? "" : throwable.getMessage());
        if (!isRetryableException(throwable)) {
            return;
        }

        String failureKey = circuitFailureKey(model, module);
        Long failures = redisTemplate.opsForValue().increment(failureKey);
        redisTemplate.expire(failureKey, Duration.ofSeconds(gatewayProperties.getCircuitWindowSeconds()));
        if (failures != null && failures >= gatewayProperties.getCircuitFailureThreshold()) {
            redisTemplate.opsForValue().set(
                    circuitOpenKey(model, module),
                    String.valueOf(System.currentTimeMillis()),
                    Duration.ofSeconds(gatewayProperties.getCircuitOpenSeconds())
            );
            log.warn("opened ai circuit for model={}, module={}, failures={}", model, module, failures);
        }
    }

    private void clearCircuit(String model, AiLimitService.AiModule module) {
        redisTemplate.delete(circuitFailureKey(model, module));
        redisTemplate.delete(circuitOpenKey(model, module));
    }

    private boolean isCircuitOpen(String model, AiLimitService.AiModule module) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(circuitOpenKey(model, module)));
    }

    private String circuitFailureKey(String model, AiLimitService.AiModule module) {
        return KEY_CIRCUIT_FAILURE_PREFIX + module.name().toLowerCase() + ":" + model;
    }

    private String circuitOpenKey(String model, AiLimitService.AiModule module) {
        return KEY_CIRCUIT_OPEN_PREFIX + module.name().toLowerCase() + ":" + model;
    }

    private String modelUsageKey(String model) {
        return KEY_MODEL_USAGE_PREFIX + model + ":" + today();
    }

    private String today() {
        return LocalDate.now().format(DATE_FMT);
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof ExecutionException executionException && executionException.getCause() != null) {
            return executionException.getCause();
        }
        return throwable;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    private static final class AiExecutorThreadFactory implements ThreadFactory {
        private int index = 0;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ai-gateway-" + (++index));
            thread.setDaemon(true);
            return thread;
        }
    }
}