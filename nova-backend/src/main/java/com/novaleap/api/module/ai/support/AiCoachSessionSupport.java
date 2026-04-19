package com.novaleap.api.module.ai.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AiCoachSessionSupport {

    private static final String KEY_COACH_HISTORY_PREFIX = "nova:ai:coach:history:";
    private static final String KEY_COACH_ACTIVE_SESSION_PREFIX = "nova:ai:coach:active_session:";
    private static final String LEGACY_SESSION_ID = "legacy";
    private static final int MAX_HISTORY_LENGTH = 120;
    private static final Duration SESSION_TTL = Duration.ofDays(14);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AiCoachSessionSupport(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> getCoachHistory(String username, int limit) {
        if (!StringUtils.hasText(username)) {
            return List.of();
        }
        String activeSessionId = ensureActiveCoachSession(username);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<String> records = redisTemplate.opsForList().range(historyKey(username), 0, safeLimit - 1);
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String record : records) {
            if (!StringUtils.hasText(record)) {
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> row = objectMapper.readValue(record, Map.class);
                String rowSessionId = safe(String.valueOf(row.getOrDefault("sessionId", "")));
                if (!belongsToSession(rowSessionId, activeSessionId)) {
                    continue;
                }
                result.add(row);
            } catch (Exception e) {
                log.debug("Invalid coach history json, skip: {}", record, e);
            }
        }
        return result;
    }

    public String createCoachSession(String username) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        String sessionId = newCoachSessionId();
        redisTemplate.opsForValue().set(activeSessionKey(username), sessionId, SESSION_TTL);
        return sessionId;
    }

    public void clearCoachHistory(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        redisTemplate.delete(historyKey(username));
        redisTemplate.delete(activeSessionKey(username));
    }

    public void saveCoachMessage(String username, String role, String content, String mode, String topic) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(content)) {
            return;
        }
        try {
            String sessionId = ensureActiveCoachSession(username);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("role", role);
            row.put("content", content);
            row.put("mode", mode);
            row.put("topic", topic);
            row.put("sessionId", sessionId);
            row.put("timestamp", LocalDateTime.now().toString());

            String key = historyKey(username);
            redisTemplate.opsForList().leftPush(key, objectMapper.writeValueAsString(row));
            redisTemplate.opsForList().trim(key, 0, MAX_HISTORY_LENGTH - 1);
            redisTemplate.expire(key, SESSION_TTL);
            redisTemplate.expire(activeSessionKey(username), SESSION_TTL);
        } catch (Exception e) {
            log.debug("save coach history failed: {}", e.getMessage());
        }
    }

    public String buildHistoryPrompt(List<Map<String, Object>> history, int maxItems) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("最近对话上下文:\n");
        int count = 0;
        for (Map<String, Object> item : history) {
            if (item == null) {
                continue;
            }
            String role = safe(String.valueOf(item.getOrDefault("role", "")));
            String content = safe(String.valueOf(item.getOrDefault("content", "")));
            if (content.isBlank()) {
                continue;
            }
            sb.append("- ").append(role).append(": ").append(content).append("\n");
            count++;
            if (count >= maxItems) {
                break;
            }
        }
        return sb.toString();
    }

    private String ensureActiveCoachSession(String username) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        String sessionKey = activeSessionKey(username);
        String existing = safe(redisTemplate.opsForValue().get(sessionKey));
        if (StringUtils.hasText(existing)) {
            redisTemplate.expire(sessionKey, SESSION_TTL);
            redisTemplate.expire(historyKey(username), SESSION_TTL);
            return existing;
        }

        String resolved;
        List<String> latest = redisTemplate.opsForList().range(historyKey(username), 0, 0);
        if (latest != null && !latest.isEmpty() && StringUtils.hasText(latest.get(0))) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> row = objectMapper.readValue(latest.get(0), Map.class);
                String rowSessionId = safe(String.valueOf(row.getOrDefault("sessionId", "")));
                resolved = StringUtils.hasText(rowSessionId) ? rowSessionId : LEGACY_SESSION_ID;
            } catch (Exception e) {
                resolved = LEGACY_SESSION_ID;
            }
        } else {
            resolved = newCoachSessionId();
        }

        redisTemplate.opsForValue().set(sessionKey, resolved, SESSION_TTL);
        redisTemplate.expire(historyKey(username), SESSION_TTL);
        return resolved;
    }

    private boolean belongsToSession(String rowSessionId, String activeSessionId) {
        if (!StringUtils.hasText(activeSessionId)) {
            return true;
        }
        if (StringUtils.hasText(rowSessionId)) {
            return activeSessionId.equals(rowSessionId);
        }
        return LEGACY_SESSION_ID.equals(activeSessionId);
    }

    private String historyKey(String username) {
        return KEY_COACH_HISTORY_PREFIX + username.trim();
    }

    private String activeSessionKey(String username) {
        return KEY_COACH_ACTIVE_SESSION_PREFIX + username.trim();
    }

    private String newCoachSessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
