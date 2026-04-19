package com.novaleap.api.module.auth.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String VERIFY_CODE_PREFIX = "nova:auth:verify:code:";
    private static final String VERIFY_COOLDOWN_PREFIX = "nova:auth:verify:cooldown:";
    private static final String VERIFY_FAIL_PREFIX = "nova:auth:verify:fail:";
    private static final String VERIFY_FREEZE_PREFIX = "nova:auth:verify:freeze:";

    private static final String SEND_EMAIL_WINDOW_PREFIX = "nova:auth:verify:send:email:";
    private static final String SEND_IP_WINDOW_PREFIX = "nova:auth:verify:send:ip:";
    private static final String SEND_PAIR_WINDOW_PREFIX = "nova:auth:verify:send:pair:";
    private static final String SEND_GLOBAL_WINDOW_PREFIX = "nova:auth:verify:send:global:";

    private static final Duration VERIFY_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFY_FREEZE_TTL = Duration.ofMinutes(10);
    private static final int VERIFY_MAX_FAILURES = 5;

    private static final Duration EMAIL_COOLDOWN_WINDOW = Duration.ofSeconds(60);
    private static final Duration EMAIL_LIMIT_HOUR_WINDOW = Duration.ofHours(1);
    private static final int EMAIL_LIMIT_PER_HOUR = 5;
    private static final Duration EMAIL_LIMIT_DAY_WINDOW = Duration.ofHours(24);
    private static final int EMAIL_LIMIT_PER_DAY = 10;

    private static final Duration IP_LIMIT_MINUTE_WINDOW = Duration.ofMinutes(1);
    private static final int IP_LIMIT_PER_MINUTE = 3;
    private static final Duration IP_LIMIT_HOUR_WINDOW = Duration.ofHours(1);
    private static final int IP_LIMIT_PER_HOUR = 20;
    private static final Duration IP_LIMIT_DAY_WINDOW = Duration.ofHours(24);
    private static final int IP_LIMIT_PER_DAY = 100;

    private static final Duration PAIR_LIMIT_WINDOW = Duration.ofMinutes(10);
    private static final int PAIR_LIMIT_MAX = 3;

    private static final Duration GLOBAL_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final int GLOBAL_LIMIT_MAX = 200;

    private static final int RISK_IP_MINUTE_THRESHOLD = 2;
    private static final int RISK_PAIR_THRESHOLD = 2;
    private static final int RISK_EMAIL_HOUR_THRESHOLD = 3;

    private static final DefaultRedisScript<List> WINDOW_COUNTER_SCRIPT = new DefaultRedisScript<>();

    static {
        WINDOW_COUNTER_SCRIPT.setScriptText(
                "local current = redis.call('INCR', KEYS[1]) "
                        + "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                        + "local ttl = redis.call('TTL', KEYS[1]) "
                        + "if ttl < 0 then ttl = tonumber(ARGV[1]) end "
                        + "if current > tonumber(ARGV[2]) then return {0, current, ttl} end "
                        + "return {1, current, ttl}"
        );
        WINDOW_COUNTER_SCRIPT.setResultType(List.class);
    }

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

    public boolean shouldRequireHumanCheck(String email, String type, String ip) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedType = normalizeType(type);
        String ipKey = normalizeIp(ip);
        if (!StringUtils.hasText(normalizedEmail)) {
            return false;
        }

        long ipMinuteCount = readCount(buildIpWindowKey(ipKey, "1m"));
        long pairCount = readCount(buildPairWindowKey(normalizedType, ipKey, normalizedEmail, "10m"));
        long emailHourCount = readCount(buildEmailWindowKey(normalizedType, normalizedEmail, "1h"));

        return ipMinuteCount >= RISK_IP_MINUTE_THRESHOLD
                || pairCount >= RISK_PAIR_THRESHOLD
                || emailHourCount >= RISK_EMAIL_HOUR_THRESHOLD;
    }

    public void sendVerificationCode(String email, String type, String ip, String userAgent, boolean allowDelivery) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedType = normalizeType(type);
        assertValidEmail(normalizedEmail);

        if (resend == null || !StringUtils.hasText(resendFrom)) {
            throw new IllegalStateException("邮件服务未配置，请联系管理员");
        }

        String normalizedIp = normalizeIp(ip);
        enforceSendRateLimits(normalizedType, normalizedEmail, normalizedIp);

        String cooldownKey = buildCooldownKey(normalizedType, normalizedEmail);
        long cooldownSeconds = resolveRemainingSeconds(cooldownKey, EMAIL_COOLDOWN_WINDOW);
        if (cooldownSeconds > 0) {
            throw new IllegalArgumentException("验证码发送过于频繁，请 " + cooldownSeconds + " 秒后再试");
        }

        if (!allowDelivery) {
            redisTemplate.opsForValue().set(cooldownKey, "1", EMAIL_COOLDOWN_WINDOW.getSeconds(), TimeUnit.SECONDS);
            log.info(
                    "verify.send skipped type={} email={} ip={} ua={} reason=account_mismatch",
                    normalizedType,
                    maskEmail(normalizedEmail),
                    normalizedIp,
                    sanitizeUserAgent(userAgent)
            );
            return;
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String verifyCodeKey = buildVerifyCodeKey(normalizedType, normalizedEmail);
        redisTemplate.opsForValue().set(verifyCodeKey, code, VERIFY_CODE_TTL.toMinutes(), TimeUnit.MINUTES);

        try {
            sendEmail(normalizedEmail, normalizedType, code);
            redisTemplate.opsForValue().set(cooldownKey, "1", EMAIL_COOLDOWN_WINDOW.getSeconds(), TimeUnit.SECONDS);
            clearVerifyFailures(normalizedEmail, normalizedType);
            log.info(
                    "verify.send success type={} email={} ip={} ua={}",
                    normalizedType,
                    maskEmail(normalizedEmail),
                    normalizedIp,
                    sanitizeUserAgent(userAgent)
            );
        } catch (RuntimeException ex) {
            redisTemplate.delete(verifyCodeKey);
            log.warn(
                    "verify.send fail type={} email={} ip={} ua={} reason={}",
                    normalizedType,
                    maskEmail(normalizedEmail),
                    normalizedIp,
                    sanitizeUserAgent(userAgent),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    public VerificationCheckResult verifyCodeWithPolicy(String email, String type, String code) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedType = normalizeType(type);
        String normalizedCode = safeTrim(code);

        if (!StringUtils.hasText(normalizedEmail) || !StringUtils.hasText(normalizedCode)) {
            return VerificationCheckResult.invalid();
        }

        long freezeSeconds = resolveRemainingSeconds(buildVerifyFreezeKey(normalizedType, normalizedEmail), VERIFY_FREEZE_TTL);
        if (freezeSeconds > 0) {
            log.warn(
                    "verify.check locked type={} email={} retryAfter={}s",
                    normalizedType,
                    maskEmail(normalizedEmail),
                    freezeSeconds
            );
            return VerificationCheckResult.locked(freezeSeconds);
        }

        String verifyCodeKey = buildVerifyCodeKey(normalizedType, normalizedEmail);
        String savedCode = redisTemplate.opsForValue().get(verifyCodeKey);
        if (!StringUtils.hasText(savedCode)) {
            log.info("verify.check miss type={} email={} reason=code_absent", normalizedType, maskEmail(normalizedEmail));
            return VerificationCheckResult.invalid();
        }

        if (!normalizedCode.equals(savedCode)) {
            long failures = incrementVerifyFailure(normalizedEmail, normalizedType);
            if (failures >= VERIFY_MAX_FAILURES) {
                redisTemplate.delete(verifyCodeKey);
                redisTemplate.opsForValue().set(
                        buildVerifyFreezeKey(normalizedType, normalizedEmail),
                        "1",
                        VERIFY_FREEZE_TTL.toSeconds(),
                        TimeUnit.SECONDS
                );
                clearVerifyFailures(normalizedEmail, normalizedType);
                log.warn(
                        "verify.check lock type={} email={} failures={} window={}s",
                        normalizedType,
                        maskEmail(normalizedEmail),
                        failures,
                        VERIFY_FREEZE_TTL.toSeconds()
                );
                return VerificationCheckResult.locked(VERIFY_FREEZE_TTL.toSeconds());
            }
            log.warn(
                    "verify.check fail type={} email={} failures={}",
                    normalizedType,
                    maskEmail(normalizedEmail),
                    failures
            );
            return VerificationCheckResult.invalid();
        }

        clearVerifyFailures(normalizedEmail, normalizedType);
        log.info("verify.check success type={} email={}", normalizedType, maskEmail(normalizedEmail));
        return VerificationCheckResult.success();
    }

    public boolean verifyCode(String email, String type, String code) {
        return verifyCodeWithPolicy(email, type, code).passed();
    }

    public void consumeCode(String email, String type) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedType = normalizeType(type);
        redisTemplate.delete(buildVerifyCodeKey(normalizedType, normalizedEmail));
        clearVerifyState(normalizedEmail, normalizedType);
    }

    private void sendEmail(String email, String type, String code) {
        String sceneText = switch (type) {
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
                .to(email)
                .subject(subject)
                .html(html)
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException | RuntimeException ex) {
            throw new IllegalStateException("邮件发送失败，请稍后重试", ex);
        }
    }

    private void enforceSendRateLimits(String type, String email, String ip) {
        enforceLimit(
                buildGlobalWindowKey("1m"),
                GLOBAL_LIMIT_WINDOW,
                GLOBAL_LIMIT_MAX,
                "请求过于频繁，请稍后重试"
        );

        enforceLimit(
                buildEmailWindowKey(type, email, "1h"),
                EMAIL_LIMIT_HOUR_WINDOW,
                EMAIL_LIMIT_PER_HOUR,
                "该邮箱发送次数过多"
        );
        enforceLimit(
                buildEmailWindowKey(type, email, "24h"),
                EMAIL_LIMIT_DAY_WINDOW,
                EMAIL_LIMIT_PER_DAY,
                "该邮箱今日发送次数已达上限"
        );

        enforceLimit(
                buildIpWindowKey(ip, "1m"),
                IP_LIMIT_MINUTE_WINDOW,
                IP_LIMIT_PER_MINUTE,
                "请求过于频繁"
        );
        enforceLimit(
                buildIpWindowKey(ip, "1h"),
                IP_LIMIT_HOUR_WINDOW,
                IP_LIMIT_PER_HOUR,
                "当前网络请求过于频繁"
        );
        enforceLimit(
                buildIpWindowKey(ip, "24h"),
                IP_LIMIT_DAY_WINDOW,
                IP_LIMIT_PER_DAY,
                "当前网络今日请求次数已达上限"
        );

        enforceLimit(
                buildPairWindowKey(type, ip, email, "10m"),
                PAIR_LIMIT_WINDOW,
                PAIR_LIMIT_MAX,
                "当前操作过于频繁"
        );
    }

    private void enforceLimit(String key, Duration window, int max, String message) {
        CounterResult result = incrementWindowCounter(key, window, max);
        if (!result.allowed()) {
            throw new IllegalArgumentException(message + "，请 " + result.retryAfterSeconds() + " 秒后再试");
        }
    }

    private CounterResult incrementWindowCounter(String key, Duration window, int max) {
        List<?> scriptResult = redisTemplate.execute(
                WINDOW_COUNTER_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(window.getSeconds()),
                String.valueOf(max)
        );
        if (scriptResult == null || scriptResult.size() < 3) {
            throw new IllegalStateException("限流服务异常，请稍后再试");
        }

        long allowFlag = parseLong(scriptResult.get(0));
        long ttlSeconds = parseLong(scriptResult.get(2));
        return new CounterResult(allowFlag == 1L, Math.max(1L, ttlSeconds));
    }

    private long incrementVerifyFailure(String email, String type) {
        String failKey = buildVerifyFailKey(type, email);
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(failKey, VERIFY_CODE_TTL.toSeconds(), TimeUnit.SECONDS);
        }
        return count == null ? 1L : count;
    }

    private void clearVerifyFailures(String email, String type) {
        redisTemplate.delete(buildVerifyFailKey(type, email));
    }

    private void clearVerifyState(String email, String type) {
        redisTemplate.delete(buildVerifyFailKey(type, email));
        redisTemplate.delete(buildVerifyFreezeKey(type, email));
    }

    private long resolveRemainingSeconds(String key, Duration fallback) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return 0L;
        }
        return Math.max(1L, Math.min(ttl, fallback.getSeconds()));
    }

    private long readCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignore) {
            return 0L;
        }
    }

    private long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignore) {
            return 0L;
        }
    }

    private String normalizeType(String type) {
        String safeType = safeTrim(type).toLowerCase(Locale.ROOT);
        if ("login".equals(safeType) || "reset".equals(safeType)) {
            return safeType;
        }
        return "register";
    }

    private String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "unknown";
        }
        return ip.trim().replace(":", "_").replace("/", "_");
    }

    private String buildVerifyCodeKey(String type, String email) {
        return VERIFY_CODE_PREFIX + type + ":" + email;
    }

    private String buildCooldownKey(String type, String email) {
        return VERIFY_COOLDOWN_PREFIX + type + ":" + email;
    }

    private String buildVerifyFailKey(String type, String email) {
        return VERIFY_FAIL_PREFIX + type + ":" + email;
    }

    private String buildVerifyFreezeKey(String type, String email) {
        return VERIFY_FREEZE_PREFIX + type + ":" + email;
    }

    private String buildEmailWindowKey(String type, String email, String windowTag) {
        return SEND_EMAIL_WINDOW_PREFIX + type + ":" + windowTag + ":" + email;
    }

    private String buildIpWindowKey(String ip, String windowTag) {
        return SEND_IP_WINDOW_PREFIX + windowTag + ":" + ip;
    }

    private String buildPairWindowKey(String type, String ip, String email, String windowTag) {
        return SEND_PAIR_WINDOW_PREFIX + type + ":" + windowTag + ":" + ip + ":" + email;
    }

    private String buildGlobalWindowKey(String windowTag) {
        return SEND_GLOBAL_WINDOW_PREFIX + windowTag;
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return "**" + domain;
        }
        return local.substring(0, 2) + "***" + domain;
    }

    private String sanitizeUserAgent(String userAgent) {
        String ua = safeTrim(userAgent);
        if (!StringUtils.hasText(ua)) {
            return "unknown";
        }
        return ua.length() > 120 ? ua.substring(0, 120) : ua;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private record CounterResult(boolean allowed, long retryAfterSeconds) {
    }

    public static final class VerificationCheckResult {
        private final boolean passed;
        private final String message;

        private VerificationCheckResult(boolean passed, String message) {
            this.passed = passed;
            this.message = message;
        }

        public static VerificationCheckResult success() {
            return new VerificationCheckResult(true, "");
        }

        public static VerificationCheckResult invalid() {
            return new VerificationCheckResult(false, "验证码错误或已过期");
        }

        public static VerificationCheckResult locked(long lockSeconds) {
            long minutes = Math.max(1L, (lockSeconds + 59) / 60);
            return new VerificationCheckResult(false, "验证码错误次数过多，请 " + minutes + " 分钟后再试");
        }

        public boolean passed() {
            return passed;
        }

        public String message() {
            return message;
        }
    }
}
