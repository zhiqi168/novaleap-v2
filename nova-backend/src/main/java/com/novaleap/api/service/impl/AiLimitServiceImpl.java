package com.novaleap.api.service.impl;

import com.novaleap.api.module.quota.support.AiQuotaPolicy;
import com.novaleap.api.module.quota.support.AiQuotaUsageSupport;
import com.novaleap.api.service.AiLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiLimitServiceImpl implements AiLimitService {

    private static final Logger log = LoggerFactory.getLogger(AiLimitServiceImpl.class);

    private final AiQuotaPolicy aiQuotaPolicy;
    private final AiQuotaUsageSupport aiQuotaUsageSupport;

    public AiLimitServiceImpl(AiQuotaPolicy aiQuotaPolicy, AiQuotaUsageSupport aiQuotaUsageSupport) {
        this.aiQuotaPolicy = aiQuotaPolicy;
        this.aiQuotaUsageSupport = aiQuotaUsageSupport;
    }

    @Override
    public LimitCheckResult checkLimit(String identifier, String role, AiModule module) {
        if (!StringUtils.hasText(identifier)) {
            return new LimitCheckResult(false, "未识别身份信息", 0);
        }

        int degradeLevel = getCurrentDegradeLevel();
        boolean isGuest = "GUEST".equalsIgnoreCase(role);

        if (degradeLevel >= 3 && (isGuest || (module != AiModule.SOLVER && module != AiModule.COACH))) {
            return new LimitCheckResult(false, "系统因 AI 资源紧张已进入保护模式，请稍后再试", degradeLevel);
        }
        if (degradeLevel >= 2 && isGuest) {
            return new LimitCheckResult(false, "当前系统负载过高，暂时仅限登录用户使用", degradeLevel);
        }
        if (aiQuotaUsageSupport.inCooldown(identifier, module)) {
            return new LimitCheckResult(false, "操作过快，请稍后刷新重试", degradeLevel);
        }

        int totalLimit = aiQuotaPolicy.totalLimit(role, degradeLevel);
        if (aiQuotaUsageSupport.currentTotal(identifier) >= totalLimit) {
            String msg = isGuest
                    ? "今日游客体验次数已用完，登录后可继续使用"
                    : "今日 AI 使用额度已达上限，明日会自动恢复";
            return new LimitCheckResult(false, msg, degradeLevel);
        }

        int moduleLimit = aiQuotaPolicy.moduleLimit(role, module);
        if (moduleLimit <= 0) {
            return new LimitCheckResult(false, "该功能需要登录后使用", degradeLevel);
        }
        if (aiQuotaUsageSupport.currentModule(identifier, module) >= moduleLimit) {
            return new LimitCheckResult(false, "该模块今日次数已达上限", degradeLevel);
        }

        aiQuotaUsageSupport.incrementUsage(identifier, module, aiQuotaPolicy.cooldownSeconds(role, module));
        return new LimitCheckResult(true, "可继续使用", degradeLevel);
    }

    @Override
    public void recordTokenUsage(long tokens) {
        aiQuotaUsageSupport.recordTokenUsage(tokens);
    }

    @Override
    public int getCurrentDegradeLevel() {
        try {
            return aiQuotaPolicy.degradeLevel(aiQuotaUsageSupport.currentTokenUsage());
        } catch (Exception e) {
            log.warn("Failed to read token usage from Redis, entering protection mode: {}", e.getMessage());
            return 2;
        }
    }
}
