package com.novaleap.api.module.ai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 通用 AI 对话请求 DTO，与具体模型服务商无关。
 * 业务层通过此对象发起对话，底层实现将其转换为对应 API 的请求格式。
 */
@Data
@Builder
public class AiChatRequest {

    /** System prompt（可选） */
    private String systemPrompt;

    /** 用户消息 */
    private String userPrompt;

    /** 模型名称，为空则使用配置默认值 */
    private String model;

    /** 最大 token 数，为空则使用配置默认值 */
    private Integer maxTokens;

    /** 温度参数，为空则使用模型默认值 */
    private Double temperature;
}