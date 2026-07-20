package com.novaleap.api.module.ai.agent.tool;

import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 工具注册中心 —— 自动收集所有 {@link AgentTool} Bean，提供按名称查找及生成描述的能力。
 */
@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> toolMap = new LinkedHashMap<>();

    public AgentToolRegistry(List<AgentTool> tools) {
        for (AgentTool tool : tools) {
            toolMap.put(tool.getName(), tool);
        }
    }

    /** 按名称获取工具。 */
    public AgentTool getTool(String name) {
        return toolMap.get(name);
    }

    /** 生成给 LLM 看的工具描述文本。 */
    public String buildToolDescriptions() {
        StringBuilder sb = new StringBuilder();
        for (AgentTool tool : toolMap.values()) {
            sb.append("### ").append(tool.getName()).append("\n");
            sb.append(tool.getDescription()).append("\n");
            sb.append("参数: ").append(tool.getParameterSchema()).append("\n\n");
        }
        return sb.toString();
    }

    /** 获取所有已注册的工具名称列表。 */
    public List<String> getAllToolNames() {
        return List.copyOf(toolMap.keySet());
    }
}