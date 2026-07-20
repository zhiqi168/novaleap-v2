package com.novaleap.api.module.ai.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.module.ai.support.AiExternalContextService;
import org.springframework.stereotype.Component;

/**
 * 查询天气信息。
 * 使用 Open-Meteo API 获取指定城市的实时天气数据。
 */
@Component
public class GetWeatherTool implements AgentTool {

    private final AiExternalContextService externalContextService;
    private final ObjectMapper objectMapper;

    public GetWeatherTool(AiExternalContextService externalContextService, ObjectMapper objectMapper) {
        this.externalContextService = externalContextService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_weather";
    }

    @Override
    public String getDescription() {
        return "查询指定城市的实时天气信息，包括气温、体感温度、湿度、风速和天气状况。当用户询问天气、气温时使用。";
    }

    @Override
    public String getParameterSchema() {
        return "{\"location\": \"城市名称（必填），如：北京、上海、深圳\"}";
    }

    @Override
    public String execute(String argsJson, String username) {
        try {
            JsonNode args = objectMapper.readTree(argsJson);
            String location = args.path("location").asText("").trim();

            if (location.isBlank()) {
                return "错误：缺少必要参数 location（城市名称）。";
            }

            String result = externalContextService.getWeather(location);
            if (result.isBlank()) {
                return "未查到 \"" + location + "\" 的天气信息，请确认城市名称是否正确。";
            }
            return result;

        } catch (Exception e) {
            return "查询天气时出错：" + e.getMessage();
        }
    }
}