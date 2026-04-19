package com.novaleap.api.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novaleap.api.entity.User;
import com.novaleap.api.mapper.UserMapper;
import com.novaleap.api.module.auth.support.AuthCheckinSupport;
import com.novaleap.api.module.auth.support.AuthLoginAuditSupport;
import com.novaleap.api.module.auth.support.AuthPasswordSupport;
import com.novaleap.api.module.auth.support.AuthPortal;
import com.novaleap.api.module.auth.support.AuthRateLimitSupport;
import com.novaleap.api.module.auth.support.AuthRoleSupport;
import com.novaleap.api.module.auth.support.AvatarSupport;
import com.novaleap.api.module.auth.support.TurnstileVerifier;
import com.novaleap.api.security.JwtUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class SharedAuthLoginService {

    private static final String EMAIL_NOT_REGISTERED_ERROR = "邮箱未注册";

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final AuthRateLimitSupport authRateLimitSupport;
    private final AuthPasswordSupport authPasswordSupport;
    private final AuthCheckinSupport authCheckinSupport;
    private final AvatarSupport avatarSupport;
    private final TurnstileVerifier turnstileVerifier;
    private final AuthRoleSupport authRoleSupport;
    private final AuthLoginAuditSupport authLoginAuditSupport;

    public SharedAuthLoginService(
            UserMapper userMapper,
            JwtUtils jwtUtils,
            AuthRateLimitSupport authRateLimitSupport,
            AuthPasswordSupport authPasswordSupport,
            AuthCheckinSupport authCheckinSupport,
            AvatarSupport avatarSupport,
            TurnstileVerifier turnstileVerifier,
            AuthRoleSupport authRoleSupport,
            AuthLoginAuditSupport authLoginAuditSupport
    ) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
        this.authRateLimitSupport = authRateLimitSupport;
        this.authPasswordSupport = authPasswordSupport;
        this.authCheckinSupport = authCheckinSupport;
        this.avatarSupport = avatarSupport;
        this.turnstileVerifier = turnstileVerifier;
        this.authRoleSupport = authRoleSupport;
        this.authLoginAuditSupport = authLoginAuditSupport;
    }

    public User authenticateByPassword(
            String username,
            String password,
            String ip,
            String turnstileToken,
            AuthPortal portal,
            Consumer<User> passwordMigrationAction
    ) {
        String input = normalizeUsernameInput(username);
        String passwordValue = authPasswordSupport.safeTrim(password);
        if (!StringUtils.hasText(input) || !StringUtils.hasText(passwordValue)) {
            throw new IllegalArgumentException("账号和密码不能为空");
        }

        String usernameLookup = normalizeUsernameLookup(input);
        String ipKey = authRateLimitSupport.normalizeClientIp(ip);
        authRateLimitSupport.checkLoginRateLimit(ipKey, portal);
        if (authRateLimitSupport.isLoginLocked(ipKey, usernameLookup, portal)) {
            authLoginAuditSupport.recordFailure(portal, input, ipKey, "password", "LOGIN_LOCKED");
            throw new IllegalArgumentException("登录失败次数过多，请稍后再试");
        }

        turnstileVerifier.verifyIfEnabled(turnstileToken, ip);

        User user = findUserByLookupValue(usernameLookup);
        if (user == null) {
            authRateLimitSupport.recordLoginFailure(ipKey, usernameLookup, portal);
            authLoginAuditSupport.recordFailure(portal, input, ipKey, "password", "USER_NOT_FOUND");
            if (isEmailIdentifier(input)) {
                throw new IllegalArgumentException(EMAIL_NOT_REGISTERED_ERROR);
            }
            throw new IllegalArgumentException("账号或密码错误");
        }

        boolean passwordOk = authPasswordSupport.verifyPasswordAndMigrateIfLegacy(passwordValue, user, passwordMigrationAction);
        if (!passwordOk) {
            authRateLimitSupport.recordLoginFailure(ipKey, usernameLookup, portal);
            authLoginAuditSupport.recordFailure(portal, user.getUsername(), ipKey, "password", "BAD_PASSWORD");
            throw new IllegalArgumentException("账号或密码错误");
        }

        authRateLimitSupport.clearLoginFailures(ipKey, usernameLookup, portal);
        return user;
    }

    public User authenticateByVerifiedIdentity(String username, String ip, String turnstileToken, AuthPortal portal) {
        String input = normalizeUsernameInput(username);
        if (!StringUtils.hasText(input)) {
            throw new IllegalArgumentException("邮箱不能为空");
        }

        String usernameLookup = normalizeUsernameLookup(input);
        String ipKey = authRateLimitSupport.normalizeClientIp(ip);
        authRateLimitSupport.checkLoginRateLimit(ipKey, portal);
        turnstileVerifier.verifyIfEnabled(turnstileToken, ip);

        User user = findUserByLookupValue(usernameLookup);
        if (user == null) {
            authLoginAuditSupport.recordFailure(portal, input, ipKey, "code", "USER_NOT_FOUND");
            throw new IllegalArgumentException(EMAIL_NOT_REGISTERED_ERROR);
        }
        return user;
    }

    public Map<String, Object> buildAuthResult(User user, boolean markLoginCheckin, AuthPortal portal) {
        String role = authRoleSupport.normalizeRole(user.getRole());
        String token = jwtUtils.generateToken(user.getUsername(), role, portal);
        User safeUser = buildSafeUser(user, role);
        Map<String, Object> checkin = authCheckinSupport.resolveCheckinPayload(
                user.getId(),
                markLoginCheckin && !"GUEST".equalsIgnoreCase(role)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", safeUser);
        result.put("avatar", avatarSupport.resolveAvatar(user.getUsername()));
        result.put("checkin", checkin);
        result.put("loginPortal", portal.claimValue());
        result.put("loginAt", LocalDateTime.now());
        return result;
    }

    public void recordSuccess(User user, String ip, AuthPortal portal, String loginMethod) {
        authLoginAuditSupport.recordSuccess(
                portal,
                user == null ? "" : user.getUsername(),
                user == null ? "" : authRoleSupport.normalizeRole(user.getRole()),
                authRateLimitSupport.normalizeClientIp(ip),
                loginMethod
        );
    }

    public void recordDenied(User user, String ip, AuthPortal portal, String loginMethod, String reason) {
        authLoginAuditSupport.recordDenied(
                portal,
                user == null ? "" : user.getUsername(),
                user == null ? "" : authRoleSupport.normalizeRole(user.getRole()),
                authRateLimitSupport.normalizeClientIp(ip),
                loginMethod,
                reason
        );
    }

    public User findUserByLookupValue(String lookupValue) {
        if (!StringUtils.hasText(lookupValue)) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .apply("LOWER(username) = {0}", lookupValue)
                .last("LIMIT 1"));
    }

    public String normalizeUsernameInput(String username) {
        return authPasswordSupport.normalizeIdentifier(authPasswordSupport.safeTrim(username));
    }

    public String normalizeUsernameLookup(String username) {
        return normalizeUsernameInput(username).toLowerCase(Locale.ROOT);
    }

    public boolean isEmailIdentifier(String value) {
        return StringUtils.hasText(value) && value.contains("@");
    }

    private User buildSafeUser(User user, String role) {
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setNickname(user.getNickname());
        safeUser.setRole(role);
        safeUser.setCreatedAt(user.getCreatedAt());
        safeUser.setPassword(null);
        return safeUser;
    }
}
