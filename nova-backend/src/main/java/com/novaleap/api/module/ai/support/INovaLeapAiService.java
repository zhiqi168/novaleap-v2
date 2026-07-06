package com.novaleap.api.module.ai.support;

import com.novaleap.api.module.ai.dto.AiChatRequest;
import com.novaleap.api.service.AiLimitService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.function.Consumer;

/**
 * NovaLeap AI 服务顶层抽象接口。
 * 所有底层模型调用均通过此接口进行，业务层完全感知不到底层使用的是哪个模型服务商。
 * 切换模型只需修改配置文件 {@code novaleap.ai.*}，无需改动任何 Java 代码。
 */
public interface INovaLeapAiService {

    /**
     * 流式对话，返回 SSE Emitter
     *
     * @param request         通用对话请求
     * @param module          AI 模块类型（用于配额和熔断）
     * @param onCompleted     流结束回调，参数为完整响应文本
     * @param fallbackMessage 异常时的降级文案
     * @return SSE Emitter，异常时返回 null
     */
    SseEmitter streamChat(AiChatRequest request,
                          AiLimitService.AiModule module,
                          Consumer<String> onCompleted,
                          String fallbackMessage);

    /**
     * 非流式对话，异常时返回 fallback 文案
     *
     * @param request 通用对话请求
     * @param module  AI 模块类型
     * @param fallback 异常时的降级文案
     * @return 响应文本，或 fallback
     */
    String callChat(AiChatRequest request,
                    AiLimitService.AiModule module,
                    String fallback);

    /**
     * 非流式对话，异常向上抛出
     *
     * @param request 通用对话请求
     * @param module  AI 模块类型
     * @return 响应文本
     * @throws Exception 调用异常
     */
    String callChatRaw(AiChatRequest request,
                       AiLimitService.AiModule module) throws Exception;
}