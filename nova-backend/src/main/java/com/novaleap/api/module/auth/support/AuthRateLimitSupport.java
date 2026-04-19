package com.novaleap.api.module.auth.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
public class AuthRateLimitSupport {

    private static final Duration LOGIN_REQ_WINDOW = Duration.ofMinutes(1);
    private static final int CLIENT_LOGIN_REQ_LIMIT_PER_MIN = 30;
    private static final int ADMIN_LOGIN_REQ_LIMIT_PER_MIN = 12;

    private static final int CLIENT_LOGIN_FAIL_LOCK_AFTER = 5;
    private static final int ADMIN_LOGIN_FAIL_LOCK_AFTER = 5;
    private static final Duration CLIENT_LOGIN_FAIL_LOCK_TTL = Duration.ofMinutes(10);
    private static final Duration ADMIN_LOGIN_FAIL_LOCK_TTL = Duration.ofMinutes(15);

    private static final int REGISTER_REQ_LIMIT_PER_HOUR = 20;
    private static final Duration REGISTER_REQ_WINDOW = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public AuthRateLimitSupport(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String normalizeClientIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "unknown";
        }
        return ip.trim().replace(":", "_").replace("/", "_");
    }

    public void checkLoginRateLimit(String ipKey, AuthPortal portal) {
        long bucket = System.currentTimeMillis() / 60000L;
        String key = "nova:auth:" + portal.redisNamespace() + ":login:req:" + ipKey + ":" + bucket;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, LOGIN_REQ_WINDOW);
        }
        if (count != null && count > resolveLoginReqLimit(portal)) {
            throw new IllegalArgumentException("操作过于频繁，请稍后再试");
        }
    }

    public void checkRegisterRateLimit(String ipKey) {
        long bucket = System.currentTimeMillis() / 3600000L;
        String key = "nova:auth:register:req:" + ipKey + ":" + bucket;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, REGISTER_REQ_WINDOW);
        }
        if (count != null && count > REGISTER_REQ_LIMIT_PER_HOUR) {
            throw new IllegalArgumentException("注册请求过于频繁，请稍后再试");
        }
    }

    public boolean isLoginLocked(String ipKey, String usernameKey, AuthPortal portal) {
        String key = loginFailKey(ipKey, usernameKey, portal);
        String val = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(val)) {
            return false;
        }
        try {
            return Long.parseLong(val) >= resolveLoginFailThreshold(portal);
        } catch (Exception ignore) {
            return false;
        }
    }

    public void recordLoginFailure(String ipKey, String usernameKey, AuthPortal portal) {
        String key = loginFailKey(ipKey, usernameKey, portal);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, resolveLoginFailTtl(portal));
        }
    }

    public void clearLoginFailures(String ipKey, String usernameKey, AuthPortal portal) {
        redisTemplate.delete(loginFailKey(ipKey, usernameKey, portal));
    }

    private int resolveLoginReqLimit(AuthPortal portal) {
        return portal == AuthPortal.ADMIN ? ADMIN_LOGIN_REQ_LIMIT_PER_MIN : CLIENT_LOGIN_REQ_LIMIT_PER_MIN;
    }

    private int resolveLoginFailThreshold(AuthPortal portal) {
        return portal == AuthPortal.ADMIN ? ADMIN_LOGIN_FAIL_LOCK_AFTER : CLIENT_LOGIN_FAIL_LOCK_AFTER;
    }

    private Duration resolveLoginFailTtl(AuthPortal portal) {
        return portal == AuthPortal.ADMIN ? ADMIN_LOGIN_FAIL_LOCK_TTL : CLIENT_LOGIN_FAIL_LOCK_TTL;
    }

    private String loginFailKey(String ipKey, String usernameKey, AuthPortal portal) {
        return "nova:auth:" + portal.redisNamespace() + ":login:fail:" + ipKey + ":" + usernameKey;
    }
}
