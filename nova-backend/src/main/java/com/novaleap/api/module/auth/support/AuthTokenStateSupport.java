package com.novaleap.api.module.auth.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Locale;

@Service
public class AuthTokenStateSupport {

    private final StringRedisTemplate redisTemplate;
    private final Duration tokenStateTtl;

    public AuthTokenStateSupport(
            StringRedisTemplate redisTemplate,
            @Value("${nova.jwt.expiration}") long jwtExpirationMillis
    ) {
        this.redisTemplate = redisTemplate;
        long safeMillis = Math.max(jwtExpirationMillis, Duration.ofHours(1).toMillis());
        this.tokenStateTtl = Duration.ofMillis(safeMillis);
    }

    public void invalidateUserTokens(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        redisTemplate.opsForValue().set(tokenInvalidAfterKey(username), String.valueOf(System.currentTimeMillis()), tokenStateTtl);
    }

    public boolean isTokenActive(String username, Date issuedAt) {
        return isTokenActive(username, issuedAt, null);
    }

    public boolean isTokenActive(String username, Date issuedAt, Long issuedAtMillis) {
        if (!StringUtils.hasText(username) || issuedAt == null) {
            return true;
        }
        String value = redisTemplate.opsForValue().get(tokenInvalidAfterKey(username));
        if (!StringUtils.hasText(value)) {
            return true;
        }
        try {
            long tokenIssuedAtMillis = issuedAtMillis != null ? issuedAtMillis : issuedAt.getTime();
            return tokenIssuedAtMillis >= Long.parseLong(value);
        } catch (Exception ignore) {
            return true;
        }
    }

    private String tokenInvalidAfterKey(String username) {
        return "nova:auth:token:invalid-after:" + username.trim().toLowerCase(Locale.ROOT);
    }
}
