package com.novaleap.api.module.auth.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class AuthLoginAuditSupport {

    private static final String RECENT_KEY = "nova:auth:audit:recent";
    private static final long RECENT_LIMIT = 200;
    private static final Duration RECENT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthLoginAuditSupport(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void recordSuccess(AuthPortal portal, String username, String role, String ip, String loginMethod) {
        record("SUCCESS", portal, username, role, ip, loginMethod, "OK");
    }

    public void recordFailure(AuthPortal portal, String username, String ip, String loginMethod, String reason) {
        record("FAILURE", portal, username, "", ip, loginMethod, reason);
    }

    public void recordDenied(AuthPortal portal, String username, String role, String ip, String loginMethod, String reason) {
        record("DENIED", portal, username, role, ip, loginMethod, reason);
    }

    private void record(
            String status,
            AuthPortal portal,
            String username,
            String role,
            String ip,
            String loginMethod,
            String reason
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("portal", portal.claimValue());
        payload.put("username", safe(username));
        payload.put("role", safe(role));
        payload.put("ip", safe(ip));
        payload.put("loginMethod", safe(loginMethod));
        payload.put("reason", safe(reason));
        payload.put("timestamp", LocalDateTime.now().toString());

        if ("SUCCESS".equals(status)) {
            log.info("auth login success portal={} username={} role={} ip={} method={}", portal, safe(username), safe(role), safe(ip), safe(loginMethod));
        } else {
            log.warn("auth login {} portal={} username={} role={} ip={} method={} reason={}",
                    status.toLowerCase(), portal, safe(username), safe(role), safe(ip), safe(loginMethod), safe(reason));
        }

        try {
            redisTemplate.opsForList().leftPush(RECENT_KEY, objectMapper.writeValueAsString(payload));
            redisTemplate.opsForList().trim(RECENT_KEY, 0, RECENT_LIMIT - 1);
            redisTemplate.expire(RECENT_KEY, RECENT_TTL);
        } catch (Exception e) {
            log.debug("auth login audit cache skipped: {}", e.getMessage());
        }
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
