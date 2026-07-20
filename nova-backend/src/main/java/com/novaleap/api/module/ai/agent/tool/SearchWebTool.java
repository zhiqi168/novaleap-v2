package com.novaleap.api.module.ai.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.module.ai.support.AiExternalContextService;
import org.springframework.stereotype.Component;

/**
 * 联网搜索最新信息。
 * 使用 DuckDuckGo 即时检索或 Wikipedia OpenSearch 获取最新技术资料。
 */
@Component
public class SearchWebTool implements AgentTool {

    private final AiExternalContextService externalContextService;
    private final ObjectMapper objectMapper;

    public SearchWebTool(AiExternalContextService externalContextService, ObjectMapper objectMapper) {
        this.externalContextService = externalContextService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "search_web";
    }

    @Override
    public String getDescription() {
        return "联网搜索最新信息。当需要查询最新技术动态、框架版本、解决方案或用户未指定具体题库/笔记搜索时使用。";
    }

    @Override
    public String getParameterSchema() {
        return "{\"query\": \"搜索内容（必填）\"}";
    }

    @Override
    public String execute(String argsJson, String username) {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            String query = args.path("query").asText("").trim();

            if (query.isBlank()) {
                return "错误：缺少必要参数 query（搜索内容）。";
            }

            String result = externalContextService.searchWeb(query);
            if (result.isBlank()) {
                return "联网搜索未找到与 \"" + query + "\" 相关的信息，请尝试更换关键词。";
            }
            return result;

        } catch (Exception e) {
            return "联网搜索时出错：" + e.getMessage();
        }
    }
}