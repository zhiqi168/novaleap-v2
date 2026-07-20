package com.novaleap.api.module.ai.agent.tool;

/**
 * Agent 工具接口 —— 所有可供 Agent 调用的工具均需实现此接口。
 * 工具会在 Spring 启动时自动注册到 {@link AgentToolRegistry}。
 */
public interface AgentTool {

    /** 工具唯一名称，LLM 通过此名称指定要调用的工具。 */
    String getName();

    /** 工具描述，LLM 靠此理解工具的用途及何时调用。 */
    String getDescription();

    /** 参数 JSON Schema 字符串，描述工具接受的参数。 */
    String getParameterSchema();

    /**
     * 执行工具逻辑。
     * @param argsJson 工具参数的 JSON 字符串，由 LLM 生成
     * @param username 当前用户的标识
     * @return 工具执行结果文本，将作为 Observation 返回给 LLM
     */
    String execute(String argsJson, String username);
}