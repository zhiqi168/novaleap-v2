package com.novaleap.api.module.system.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaleap.api.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
public class CurrentUserCacheSupport {

    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(2);
    private static final String USER_BY_USERNAME_PREFIX = "nova:user:username:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CurrentUserCacheSupport(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public User readByUsername(String username) {
        String key = key(username);
        if (!StringUtils.hasText(key)) {
            return null;
        }
        try {
            String payload = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(payload)) {
                return null;
            }
            return objectMapper.readValue(payload, User.class);
        } catch (Exception ignore) {
            redisTemplate.delete(key);
            return null;
        }
    }

    public void writeByUsername(String username, User user) {
        String key = key(username);
        if (!StringUtils.hasText(key) || user == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(user), USER_CACHE_TTL);
        } catch (Exception ignore) {
        }
    }

    public void evictByUsername(String username) {
        String key = key(username);
        if (!StringUtils.hasText(key)) {
            return;
        }
        redisTemplate.delete(key);
    }

    private String key(String username) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        return USER_BY_USERNAME_PREFIX + username.trim();
    }
}
