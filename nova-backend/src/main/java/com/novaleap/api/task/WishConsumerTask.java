package com.novaleap.api.task;

import com.novaleap.api.entity.Wish;
import com.novaleap.api.module.ai.support.AiModelGateway;
import com.novaleap.api.module.ai.support.AiPromptFactory;
import com.novaleap.api.module.wish.support.WishQueueConstants;
import com.novaleap.api.service.AiLimitService;
import com.novaleap.api.service.WishService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableScheduling
public class WishConsumerTask {

    private static final int MAX_BATCH_SIZE = 5;
    private static final int MAX_SCAN_SIZE = 50;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long PROCESSING_LOCK_TTL_SECONDS = 180L;
    private static final long PROCESSING_LOCK_RENEW_SECONDS = 45L;
    private static final Duration PROCESSING_STALE_AFTER = Duration.ofMinutes(4);
    private static final Duration RETRY_TTL = Duration.ofDays(1);
    private static final Duration RECOVERY_LOCK_TTL = Duration.ofSeconds(20);

    private static final DefaultRedisScript<Long> COMPARE_AND_DELETE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class
            );

    private static final DefaultRedisScript<Long> COMPARE_AND_EXPIRE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('expire', KEYS[1], ARGV[2]) else return 0 end",
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;
    private final WishService wishService;
    private final AiModelGateway aiModelGateway;
    private final AiPromptFactory aiPromptFactory;
    private final ScheduledExecutorService leaseExecutor;

    public WishConsumerTask(
            StringRedisTemplate redisTemplate,
            WishService wishService,
            AiModelGateway aiModelGateway,
            AiPromptFactory aiPromptFactory
    ) {
        this.redisTemplate = redisTemplate;
        this.wishService = wishService;
        this.aiModelGateway = aiModelGateway;
        this.aiPromptFactory = aiPromptFactory;
        this.leaseExecutor = Executors.newSingleThreadScheduledExecutor(new LeaseThreadFactory());
    }

    @Scheduled(fixedDelay = 3000)
    public void processPendingWishes() {
        for (int i = 0; i < MAX_BATCH_SIZE; i++) {
            if (!processNextWish()) {
                return;
            }
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    public void recoverStaleProcessingWishes() {
        String recoveryToken = acquireRecoveryLock();
        if (!StringUtils.hasText(recoveryToken)) {
            return;
        }
        try {
            long staleBefore = System.currentTimeMillis() - PROCESSING_STALE_AFTER.toMillis();
            Set<String> staleWishIds = redisTemplate.opsForZSet().rangeByScore(
                    WishQueueConstants.PROCESSING_ZSET,
                    0,
                    staleBefore,
                    0,
                    MAX_SCAN_SIZE
            );
            if (staleWishIds == null || staleWishIds.isEmpty()) {
                return;
            }
            for (String wishIdStr : staleWishIds) {
                if (!StringUtils.hasText(wishIdStr)) {
                    ackProcessingWish(wishIdStr);
                    continue;
                }
                if (hasProcessingLock(wishIdStr)) {
                    continue;
                }
                ackProcessingWish(wishIdStr);
                Long wishId = parseWishId(wishIdStr);
                if (wishId == null || isDeadLetter(wishIdStr)) {
                    continue;
                }
                if (wishService.getPendingWish(wishId) != null) {
                    wishService.enqueuePendingWish(wishId);
                }
            }
        } finally {
            releaseRecoveryLock(recoveryToken);
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 20000)
    public void requeuePendingWishes() {
        Set<String> pendingWishIds = redisTemplate.opsForSet().members(WishQueueConstants.PENDING_INDEX_SET);
        Set<String> processingWishIds = redisTemplate.opsForZSet().range(WishQueueConstants.PROCESSING_ZSET, 0, MAX_SCAN_SIZE - 1);

        for (Long wishId : wishService.listPendingWishIds(MAX_SCAN_SIZE)) {
            String wishIdStr = String.valueOf(wishId);
            if ((pendingWishIds != null && pendingWishIds.contains(wishIdStr))
                    || (processingWishIds != null && processingWishIds.contains(wishIdStr))
                    || hasProcessingLock(wishIdStr)
                    || isDeadLetter(wishIdStr)) {
                continue;
            }
            wishService.enqueuePendingWish(wishId);
        }
    }

    @PreDestroy
    public void shutdownLeaseExecutor() {
        leaseExecutor.shutdownNow();
    }

    private boolean processNextWish() {
        String wishIdStr = redisTemplate.opsForList().rightPopAndLeftPush(
                WishQueueConstants.PENDING_QUEUE,
                WishQueueConstants.PROCESSING_QUEUE
        );
        if (!StringUtils.hasText(wishIdStr)) {
            return false;
        }

        removeFromPendingIndex(wishIdStr);
        markProcessingStart(wishIdStr);

        Long wishId = parseWishId(wishIdStr);
        if (wishId == null || isDeadLetter(wishIdStr)) {
            ackProcessingWish(wishIdStr);
            return true;
        }

        String lockToken = acquireProcessingLock(wishIdStr);
        if (!StringUtils.hasText(lockToken)) {
            ackProcessingWish(wishIdStr);
            return true;
        }

        ScheduledFuture<?> leaseFuture = startLeaseRenewal(wishIdStr, lockToken);
        try {
            Wish wish = wishService.getPendingWish(wishId);
            if (wish == null) {
                ackProcessingWish(wishIdStr);
                clearRetryState(wishIdStr);
                return true;
            }

            EmotionProfile profile = analyzeWish(wish.getContent());
            int posX = ThreadLocalRandom.current().nextInt(10, 91);
            int posY = ThreadLocalRandom.current().nextInt(10, 91);

            boolean updated = wishService.completeAutoReview(
                    wishId,
                    profile.emotion(),
                    profile.color(),
                    profile.speed(),
                    posX,
                    posY
            );
            ackProcessingWish(wishIdStr);
            clearRetryState(wishIdStr);
            log.info("wish auto review completed, wishId={}, updated={}, emotion={}", wishId, updated, profile.emotion());
            return true;
        } catch (Exception e) {
            handleProcessingFailure(wishIdStr, wishId, e);
            return true;
        } finally {
            stopLeaseRenewal(leaseFuture);
            releaseProcessingLock(wishIdStr, lockToken);
        }
    }

    private EmotionProfile analyzeWish(String content) {
        String prompt = aiPromptFactory.buildWishEmotionPrompt(content);
        String raw = aiModelGateway.callModel(
                "",
                prompt,
                "hopeful",
                AiLimitService.AiModule.CHAT
        );
        String emotion = normalizeEmotion(raw);
        return switch (emotion) {
            case "happy" -> new EmotionProfile("happy", "#D4E0D0", 1.6D);
            case "confused" -> new EmotionProfile("confused", "#B8C4D4", 0.8D);
            case "anxious" -> new EmotionProfile("anxious", "#E0D3C1", 1.8D);
            default -> new EmotionProfile("hopeful", "#DDBFD1", 1.2D);
        };
    }

    private String normalizeEmotion(String raw) {
        String value = safe(raw).toLowerCase(Locale.ROOT);
        if (value.contains("happy")) {
            return "happy";
        }
        if (value.contains("confused")) {
            return "confused";
        }
        if (value.contains("anxious")) {
            return "anxious";
        }
        return "hopeful";
    }

    private void handleProcessingFailure(String wishIdStr, Long wishId, Exception e) {
        ackProcessingWish(wishIdStr);
        long retryCount = incrementRetryCount(wishIdStr);
        if (retryCount >= MAX_RETRY_COUNT) {
            redisTemplate.opsForSet().add(WishQueueConstants.DEAD_LETTER_SET, wishIdStr);
            log.error("wish auto review moved to dead letter, wishId={}, retryCount={}", wishIdStr, retryCount, e);
            return;
        }
        if (wishId != null) {
            wishService.enqueuePendingWish(wishId);
        }
        log.warn("wish auto review failed, requeued wishId={}, retryCount={}", wishIdStr, retryCount, e);
    }

    private long incrementRetryCount(String wishIdStr) {
        String retryKey = WishQueueConstants.RETRY_PREFIX + wishIdStr;
        Long retryCount = redisTemplate.opsForValue().increment(retryKey);
        redisTemplate.expire(retryKey, RETRY_TTL);
        return retryCount == null ? 0L : retryCount;
    }

    private void clearRetryState(String wishIdStr) {
        redisTemplate.delete(WishQueueConstants.RETRY_PREFIX + wishIdStr);
        redisTemplate.opsForSet().remove(WishQueueConstants.DEAD_LETTER_SET, wishIdStr);
    }

    private String acquireProcessingLock(String wishIdStr) {
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                WishQueueConstants.PROCESSING_LOCK_PREFIX + wishIdStr,
                lockToken,
                PROCESSING_LOCK_TTL_SECONDS,
                TimeUnit.SECONDS
        );
        return Boolean.TRUE.equals(locked) ? lockToken : null;
    }

    private boolean hasProcessingLock(String wishIdStr) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(WishQueueConstants.PROCESSING_LOCK_PREFIX + wishIdStr));
    }

    private void releaseProcessingLock(String wishIdStr, String lockToken) {
        if (!StringUtils.hasText(lockToken)) {
            return;
        }
        redisTemplate.execute(
                COMPARE_AND_DELETE_SCRIPT,
                Collections.singletonList(WishQueueConstants.PROCESSING_LOCK_PREFIX + wishIdStr),
                lockToken
        );
    }

    private void renewProcessingLock(String wishIdStr, String lockToken) {
        if (!StringUtils.hasText(lockToken)) {
            return;
        }
        redisTemplate.execute(
                COMPARE_AND_EXPIRE_SCRIPT,
                Collections.singletonList(WishQueueConstants.PROCESSING_LOCK_PREFIX + wishIdStr),
                lockToken,
                String.valueOf(PROCESSING_LOCK_TTL_SECONDS)
        );
        redisTemplate.opsForZSet().add(WishQueueConstants.PROCESSING_ZSET, wishIdStr, System.currentTimeMillis());
    }

    private ScheduledFuture<?> startLeaseRenewal(String wishIdStr, String lockToken) {
        return leaseExecutor.scheduleAtFixedRate(
                () -> renewProcessingLock(wishIdStr, lockToken),
                PROCESSING_LOCK_RENEW_SECONDS,
                PROCESSING_LOCK_RENEW_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void stopLeaseRenewal(ScheduledFuture<?> leaseFuture) {
        if (leaseFuture != null) {
            leaseFuture.cancel(true);
        }
    }

    private void markProcessingStart(String wishIdStr) {
        redisTemplate.opsForZSet().add(WishQueueConstants.PROCESSING_ZSET, wishIdStr, System.currentTimeMillis());
    }

    private void ackProcessingWish(String wishIdStr) {
        if (!StringUtils.hasText(wishIdStr)) {
            return;
        }
        redisTemplate.opsForList().remove(WishQueueConstants.PROCESSING_QUEUE, 0, wishIdStr);
        redisTemplate.opsForZSet().remove(WishQueueConstants.PROCESSING_ZSET, wishIdStr);
    }

    private void removeFromPendingIndex(String wishIdStr) {
        if (!StringUtils.hasText(wishIdStr)) {
            return;
        }
        redisTemplate.opsForSet().remove(WishQueueConstants.PENDING_INDEX_SET, wishIdStr);
    }

    private boolean isDeadLetter(String wishIdStr) {
        Boolean member = redisTemplate.opsForSet().isMember(WishQueueConstants.DEAD_LETTER_SET, wishIdStr);
        return Boolean.TRUE.equals(member);
    }

    private String acquireRecoveryLock() {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
                WishQueueConstants.PROCESSING_RECOVERY_LOCK,
                token,
                RECOVERY_LOCK_TTL
        );
        return Boolean.TRUE.equals(locked) ? token : null;
    }

    private void releaseRecoveryLock(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        redisTemplate.execute(
                COMPARE_AND_DELETE_SCRIPT,
                Collections.singletonList(WishQueueConstants.PROCESSING_RECOVERY_LOCK),
                token
        );
    }

    private Long parseWishId(String wishIdStr) {
        try {
            return Long.parseLong(wishIdStr);
        } catch (Exception e) {
            log.warn("invalid wish id in queue: {}", wishIdStr);
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record EmotionProfile(String emotion, String color, double speed) {
    }

    private static final class LeaseThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "wish-processing-lease");
            thread.setDaemon(true);
            return thread;
        }
    }
}
