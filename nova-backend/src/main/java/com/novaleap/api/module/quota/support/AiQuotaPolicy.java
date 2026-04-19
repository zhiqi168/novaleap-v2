package com.novaleap.api.module.quota.support;

import com.novaleap.api.module.quota.config.AiQuotaProperties;
import com.novaleap.api.service.AiLimitService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiQuotaPolicy {
    private final AiQuotaProperties quotaProperties;

    private static final Map<AiLimitService.AiModule, Integer> GUEST_LIMITS = Map.of(
            AiLimitService.AiModule.COACH, 3,
            AiLimitService.AiModule.SOLVER, 7,
            AiLimitService.AiModule.CHAT, 5,
            AiLimitService.AiModule.RESUME, 0
    );

    private static final Map<AiLimitService.AiModule, Integer> USER_LIMITS = Map.of(
            AiLimitService.AiModule.COACH, 12,
            AiLimitService.AiModule.SOLVER, 30,
            AiLimitService.AiModule.CHAT, 16,
            AiLimitService.AiModule.RESUME, 5
    );

    public AiQuotaPolicy(AiQuotaProperties quotaProperties) {
        this.quotaProperties = quotaProperties;
        validateThresholdOrder();
    }

    public int moduleLimit(String role, AiLimitService.AiModule module) {
        boolean guest = "GUEST".equalsIgnoreCase(role);
        return guest ? GUEST_LIMITS.getOrDefault(module, 0) : USER_LIMITS.getOrDefault(module, 0);
    }

    public int totalLimit(String role, int degradeLevel) {
        boolean guest = "GUEST".equalsIgnoreCase(role);
        if (!guest) {
            return 45;
        }
        return degradeLevel >= 1 ? 6 : 10;
    }

    public int cooldownSeconds(String role, AiLimitService.AiModule module) {
        boolean guest = "GUEST".equalsIgnoreCase(role);
        if (module == AiLimitService.AiModule.RESUME) {
            return 10;
        }
        return guest ? 5 : 3;
    }

    public int degradeLevel(long usage) {
        long warningThreshold = thresholdFor(quotaProperties.getWarningRatio());
        long protectThreshold = thresholdFor(quotaProperties.getProtectRatio());
        long limitThreshold = thresholdFor(quotaProperties.getLimitRatio());

        if (usage >= limitThreshold) {
            return 3;
        }
        if (usage >= protectThreshold) {
            return 2;
        }
        if (usage >= warningThreshold) {
            return 1;
        }
        return 0;
    }

    private long thresholdFor(double ratio) {
        return Math.max(1L, Math.round(quotaProperties.getDailyTokenLimit() * ratio));
    }

    private void validateThresholdOrder() {
        double warning = quotaProperties.getWarningRatio();
        double protect = quotaProperties.getProtectRatio();
        double limit = quotaProperties.getLimitRatio();
        if (!(warning < protect && protect < limit)) {
            throw new IllegalStateException("Invalid quota ratios: warning < protect < limit is required.");
        }
    }
}
