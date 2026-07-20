package com.novaleap.api.module.quota.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "nova.ai.quota")
public class AiQuotaProperties {

    @Min(1)
    private long dailyTokenLimit = 1000L;

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax(value = "1.0", inclusive = true)
    private double warningRatio = 0.70D;

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax(value = "1.0", inclusive = true)
    private double protectRatio = 0.85D;

    @DecimalMin(value = "0.0", inclusive = false)
    @DecimalMax(value = "1.0", inclusive = true)
    private double limitRatio = 0.95D;

    @Min(1)
    private long perModelTokenLimit = 500L;

    @Min(1)
    private int modelUsageTtlDays = 2;

    public long getDailyTokenLimit() {
        return dailyTokenLimit;
    }

    public void setDailyTokenLimit(long dailyTokenLimit) {
        this.dailyTokenLimit = dailyTokenLimit;
    }

    public double getWarningRatio() {
        return warningRatio;
    }

    public void setWarningRatio(double warningRatio) {
        this.warningRatio = warningRatio;
    }

    public double getProtectRatio() {
        return protectRatio;
    }

    public void setProtectRatio(double protectRatio) {
        this.protectRatio = protectRatio;
    }

    public double getLimitRatio() {
        return limitRatio;
    }

    public void setLimitRatio(double limitRatio) {
        this.limitRatio = limitRatio;
    }

    public long getPerModelTokenLimit() {
        return perModelTokenLimit;
    }

    public void setPerModelTokenLimit(long perModelTokenLimit) {
        this.perModelTokenLimit = perModelTokenLimit;
    }

    public int getModelUsageTtlDays() {
        return modelUsageTtlDays;
    }

    public void setModelUsageTtlDays(int modelUsageTtlDays) {
        this.modelUsageTtlDays = modelUsageTtlDays;
    }
}
