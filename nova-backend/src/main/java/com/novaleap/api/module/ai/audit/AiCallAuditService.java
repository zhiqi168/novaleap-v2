package com.novaleap.api.module.ai.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.service.AiLimitService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiCallAuditService {

    private static final String RECENT_KEY = "nova:ai:audit:recent";
    private static final long MAX_RECENT = 200;
    private static final Duration RECENT_TTL = Duration.ofDays(14);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AiCallAuditService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void recordSuccess(String model, AiLimitService.AiModule module, long totalTokens) {
        record(model, module, totalTokens, "SUCCESS", "");
    }

    public void recordFailure(String model, AiLimitService.AiModule module, String reason) {
        record(model, module, 0L, "FAILED", reason == null ? "" : reason);
    }

    private void record(String model, AiLimitService.AiModule module, long totalTokens, String status, String reason) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            payload.put("model", model);
            payload.put("module", module == null ? "UNKNOWN" : module.name());
            payload.put("tokens", totalTokens);
            payload.put("status", status);
            payload.put("reason", reason);
            redisTemplate.opsForList().leftPush(RECENT_KEY, objectMapper.writeValueAsString(payload));
            redisTemplate.opsForList().trim(RECENT_KEY, 0, MAX_RECENT - 1);
            redisTemplate.expire(RECENT_KEY, RECENT_TTL);
        } catch (Exception ignore) {
        }
    }
}
