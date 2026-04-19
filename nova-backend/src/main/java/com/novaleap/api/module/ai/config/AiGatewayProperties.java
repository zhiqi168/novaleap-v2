package com.novaleap.api.module.ai.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "nova.ai.gateway")
public class AiGatewayProperties {

    @Min(1_000)
    @Max(120_000)
    private int requestTimeoutMs = 15_000;

    @Min(1)
    @Max(4)
    private int maxRetryAttempts = 2;

    @Min(0)
    @Max(10_000)
    private int retryBackoffMs = 350;

    @Min(10)
    @Max(600)
    private int circuitWindowSeconds = 120;

    @Min(1)
    @Max(20)
    private int circuitFailureThreshold = 5;

    @Min(10)
    @Max(600)
    private int circuitOpenSeconds = 90;

    @Min(32)
    @Max(2_048)
    private int minResponseTokens = 128;

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public int getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(int retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public int getCircuitWindowSeconds() {
        return circuitWindowSeconds;
    }

    public void setCircuitWindowSeconds(int circuitWindowSeconds) {
        this.circuitWindowSeconds = circuitWindowSeconds;
    }

    public int getCircuitFailureThreshold() {
        return circuitFailureThreshold;
    }

    public void setCircuitFailureThreshold(int circuitFailureThreshold) {
        this.circuitFailureThreshold = circuitFailureThreshold;
    }

    public int getCircuitOpenSeconds() {
        return circuitOpenSeconds;
    }

    public void setCircuitOpenSeconds(int circuitOpenSeconds) {
        this.circuitOpenSeconds = circuitOpenSeconds;
    }

    public int getMinResponseTokens() {
        return minResponseTokens;
    }

    public void setMinResponseTokens(int minResponseTokens) {
        this.minResponseTokens = minResponseTokens;
    }
}
