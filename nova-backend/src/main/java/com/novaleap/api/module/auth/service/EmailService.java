package com.novaleap.api.module.auth.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String VERIFY_CODE_PREFIX = "verify_code:";
    private static final String VERIFY_COOLDOWN_PREFIX = "verify_code_cooldown:";
    private static final String VERIFY_IP_WINDOW_PREFIX = "verify_code_ip_window:";
    private static final long VERIFY_CODE_TTL_MINUTES = 5;
    private static final long VERIFY_WINDOW_SECONDS = 60;
    private static final long VERIFY_WINDOW_MAX_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Resend resend;
    private final String resendFrom;

    public EmailService(
            StringRedisTemplate redisTemplate,
            @Value("${nova.mail.resend.api-key:}") String resendApiKey,
            @Value("${nova.mail.resend.from:}") String resendFrom
    ) {
        this.redisTemplate = redisTemplate;
        this.resend = StringUtils.hasText(resendApiKey) ? new Resend(resendApiKey.trim()) : null;
        this.resendFrom = safeTrim(resendFrom);
    }

    public String normalizeEmail(String email) {
        return safeTrim(email).toLowerCase(Locale.ROOT);
    }

    public void assertValidEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedEmail) || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("请输入正确的邮箱格式");
        }
    }

    public void sendVerificationCode(String email, String type, String ip) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedType = normalizeType(type);
        assertValidEmail(normalizedEmail);
        if (resend == null || !StringUtils.hasText(resendFrom)) {
            throw new IllegalStateException("邮件服务未配置，请联系管理员");
        }

        String emailCooldownKey = buildCooldownKey(normalizedType, normalizedEmail);
        Long cooldownSeconds = redisTemplate.getExpire(emailCooldownKey, TimeUnit.SECONDS);
        if (cooldownSeconds != null && cooldownSeconds > 0) {
            throw new IllegalArgumentException("验证码发送频繁，请 " + cooldownSeconds + " 秒后再试");
        }

        String ipWindowKey = null;
        if (StringUtils.hasText(ip)) {
            ipWindowKey = buildIpWindowKey(ip);
            try {
                incrementIpWindowOrThrow(ipWindowKey);
            } catch (RuntimeException ex) {
                throw ex;
            }
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        redisTemplate.opsForValue().set(
                buildVerifyCodeKey(normalizedType, normalizedEmail),
                code,
                VERIFY_CODE_TTL_MINUTES,
                TimeUnit.MINUTES
        );
        redisTemplate.opsForValue().set(
                emailCooldownKey,
                "1",
                VERIFY_WINDOW_SECONDS,
                TimeUnit.SECONDS
        );

        String sceneText = switch (normalizedType) {
            case "reset" -> "重置密码";
            case "login" -> "登录";
            default -> "注册";
        };
        String subject = "NovaLeap 验证码";
        String html = "<div style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; color:#111827;\">"
                + "<p>您好，您正在进行 <strong>" + sceneText + "</strong> 操作。</p>"
                + "<p>验证码：<strong style=\"font-size:22px; letter-spacing:2px;\">" + code + "</strong></p>"
                + "<p>验证码 5 分钟内有效，请勿泄露给他人。</p>"
                + "</div>";

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(resendFrom)
                .to(normalizedEmail)
                .subject(subject)
                .html(html)
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException | RuntimeException ex) {
            consumeCode(normalizedEmail, normalizedType);
            redisTemplate.delete(emailCooldownKey);
            rollbackWindow(ipWindowKey);
            throw new IllegalStateException("邮件发送失败，请稍后重试", ex);
        }
    }

    public boolean verifyCode(String email, String type, String code) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedType = normalizeType(type);
        String normalizedCode = safeTrim(code);
        if (!StringUtils.hasText(normalizedEmail) || !StringUtils.hasText(normalizedCode)) {
            return false;
        }

        String savedCode = redisTemplate.opsForValue().get(buildVerifyCodeKey(normalizedType, normalizedEmail));
        return normalizedCode.equals(savedCode);
    }

    public void consumeCode(String email, String type) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedType = normalizeType(type);
        redisTemplate.delete(buildVerifyCodeKey(normalizedType, normalizedEmail));
    }

    private String normalizeType(String type) {
        String safeType = safeTrim(type).toLowerCase(Locale.ROOT);
        if ("reset".equals(safeType) || "login".equals(safeType)) {
            return safeType;
        }
        return "register";
    }

    private String buildVerifyCodeKey(String type, String email) {
        return VERIFY_CODE_PREFIX + type + ":" + email;
    }

    private String buildCooldownKey(String type, String email) {
        return VERIFY_COOLDOWN_PREFIX + type + ":" + email;
    }

    private String buildIpWindowKey(String ip) {
        return VERIFY_IP_WINDOW_PREFIX + ip;
    }

    private void incrementIpWindowOrThrow(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, VERIFY_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (count != null && count > VERIFY_WINDOW_MAX_ATTEMPTS) {
            long ttlSeconds = resolveRemainingWindowSeconds(key);
            throw new IllegalArgumentException("请求过于频繁，请 " + ttlSeconds + " 秒后再试");
        }
    }

    private long resolveRemainingWindowSeconds(String key) {
        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttlSeconds == null || ttlSeconds <= 0 ? VERIFY_WINDOW_SECONDS : ttlSeconds;
    }

    private void rollbackWindow(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        Long count = redisTemplate.opsForValue().decrement(key);
        if (count != null && count <= 0) {
            redisTemplate.delete(key);
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
