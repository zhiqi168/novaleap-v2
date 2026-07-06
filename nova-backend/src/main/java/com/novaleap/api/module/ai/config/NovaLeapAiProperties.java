package com.novaleap.api.module.ai.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "novaleap.ai")
public class NovaLeapAiProperties {

    @NotBlank
    private String provider = "deepseek";

    @NotBlank
    private String apiKey = "";

    @NotBlank
    private String baseUrl = "https://api.deepseek.com";

    @NotBlank
    private String modelName = "deepseek-v4-flash";

    private String fallbackModel = "deepseek-v4-flash";

    @Min(1)
    @Max(128_000)
    private int maxTokens = 4096;

    @Min(1000)
    @Max(120_000)
    private int requestTimeout = 30_000;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getFallbackModel() { return fallbackModel; }
    public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public int getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(int requestTimeout) { this.requestTimeout = requestTimeout; }
}