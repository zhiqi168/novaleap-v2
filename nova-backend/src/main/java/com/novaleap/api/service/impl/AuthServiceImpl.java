package com.novaleap.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.novaleap.api.common.exception.ForbiddenException;
import com.novaleap.api.common.exception.NotFoundException;
import com.novaleap.api.common.exception.UnauthorizedException;
import com.novaleap.api.entity.User;
import com.novaleap.api.mapper.UserMapper;
import com.novaleap.api.module.auth.service.SharedAuthLoginService;
import com.novaleap.api.module.auth.support.AuthCheckinSupport;
import com.novaleap.api.module.auth.support.AuthPortal;
import com.novaleap.api.module.auth.support.AuthPasswordSupport;
import com.novaleap.api.module.auth.support.AuthRateLimitSupport;
import com.novaleap.api.module.auth.support.AuthRoleSupport;
import com.novaleap.api.module.auth.support.AuthTokenStateSupport;
import com.novaleap.api.module.auth.support.AvatarSupport;
import com.novaleap.api.module.auth.support.TurnstileVerifier;
import com.novaleap.api.module.system.security.CurrentUserCacheSupport;
import com.novaleap.api.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    private static final String RESET_GENERIC_ERROR = "重置密码失败";

    private final AuthRateLimitSupport authRateLimitSupport;
    private final AuthPasswordSupport authPasswordSupport;
    private final AuthCheckinSupport authCheckinSupport;
    private final AvatarSupport avatarSupport;
    private final TurnstileVerifier turnstileVerifier;
    private final AuthTokenStateSupport authTokenStateSupport;
    private final AuthRoleSupport authRoleSupport;
    private final SharedAuthLoginService sharedAuthLoginService;
    private final CurrentUserCacheSupport currentUserCacheSupport;

    public AuthServiceImpl(
            AuthRateLimitSupport authRateLimitSupport,
            AuthPasswordSupport authPasswordSupport,
            AuthCheckinSupport authCheckinSupport,
            AvatarSupport avatarSupport,
            TurnstileVerifier turnstileVerifier,
            AuthTokenStateSupport authTokenStateSupport,
            AuthRoleSupport authRoleSupport,
            SharedAuthLoginService sharedAuthLoginService,
            CurrentUserCacheSupport currentUserCacheSupport
    ) {
        this.authRateLimitSupport = authRateLimitSupport;
        this.authPasswordSupport = authPasswordSupport;
        this.authCheckinSupport = authCheckinSupport;
        this.avatarSupport = avatarSupport;
        this.turnstileVerifier = turnstileVerifier;
        this.authTokenStateSupport = authTokenStateSupport;
        this.authRoleSupport = authRoleSupport;
        this.sharedAuthLoginService = sharedAuthLoginService;
        this.currentUserCacheSupport = currentUserCacheSupport;
    }

    @Override
    public Map<String, Object> guestLogin() {
        String guestName = "guest_" + UUID.randomUUID();
        User guest = new User();
        guest.setId(-1L);
        guest.setUsername(guestName);
        guest.setNickname(guestName);
        guest.setRole("GUEST");
        guest.setCreatedAt(LocalDateTime.now());
        return sharedAuthLoginService.buildAuthResult(guest, false, AuthPortal.CLIENT);
    }

    @Override
    public void logout(String username) {
        String normalizedUsername = normalizeUsernameInput(username);
        if (!StringUtils.hasText(normalizedUsername)) {
            return;
        }
        authTokenStateSupport.invalidateUserTokens(normalizedUsername);
    }

    @Override
    public Map<String, Object> register(
            String username,
            String password,
            String confirmPassword,
            String nickname,
            Boolean consent,
            String ip,
            String turnstileToken
    ) {
        String normalizedUsername = normalizeRegisterIdentifier(username);
        String passwordValue = authPasswordSupport.safeTrim(password);
        String confirmPasswordValue = authPasswordSupport.safeTrim(confirmPassword);

        if (!StringUtils.hasText(normalizedUsername)) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (!StringUtils.hasText(passwordValue)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (!StringUtils.hasText(confirmPasswordValue)) {
            throw new IllegalArgumentException("确认密码不能为空");
        }
        if (!Objects.equals(passwordValue, confirmPasswordValue)) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        if (!Boolean.TRUE.equals(consent)) {
            throw new IllegalArgumentException("请先同意用户协议和隐私政策");
        }

        authPasswordSupport.validatePasswordStrength(passwordValue);

        String ipKey = authRateLimitSupport.normalizeClientIp(ip);
        authRateLimitSupport.checkRegisterRateLimit(ipKey);
        turnstileVerifier.verifyIfEnabled(turnstileToken, ip);

        if (emailExists(normalizedUsername)) {
            throw new IllegalArgumentException("该邮箱已注册");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(authPasswordSupport.encode(passwordValue));
        user.setNickname(authPasswordSupport.defaultNickname(authPasswordSupport.safeTrim(nickname), normalizedUsername));
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        try {
            save(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("该邮箱已注册");
        }
        currentUserCacheSupport.writeByUsername(user.getUsername(), user);
        return sharedAuthLoginService.buildAuthResult(user, true, AuthPortal.CLIENT);
    }

    @Override
    public boolean emailExists(String email) {
        String normalizedUsername = normalizeUsernameInput(email);
        if (!StringUtils.hasText(normalizedUsername)) {
            return false;
        }
        return findUserByLookupValue(normalizeUsernameLookup(normalizedUsername)) != null;
    }

    @Override
    public void resetPassword(String username, String newPassword, String clientIp, String userAgent) {
        String normalizedUsername = normalizeUsernameInput(username);
        String newPasswordValue = authPasswordSupport.safeTrim(newPassword);
        if (!StringUtils.hasText(normalizedUsername) || !StringUtils.hasText(newPasswordValue)) {
            throw new IllegalArgumentException(RESET_GENERIC_ERROR);
        }

        authPasswordSupport.validatePasswordStrength(newPasswordValue);

        String usernameKey = normalizeUsernameLookup(normalizedUsername);
        User user = findUserByLookupValue(usernameKey);
        if (user == null) {
            throw new IllegalArgumentException(RESET_GENERIC_ERROR);
        }

        user.setPassword(authPasswordSupport.encode(newPasswordValue));
        updateById(user);
        currentUserCacheSupport.writeByUsername(user.getUsername(), user);
        authTokenStateSupport.invalidateUserTokens(user.getUsername());
    }

    @Override
    public Map<String, Object> getProfile(String username, String role) {
        if (!StringUtils.hasText(username)) {
            throw new UnauthorizedException("请先登录");
        }

        if ("GUEST".equalsIgnoreCase(role)) {
            Map<String, Object> profile = new HashMap<>();
            profile.put("username", username);
            profile.put("nickname", username);
            profile.put("role", "GUEST");
            profile.put("accountType", "guest");
            profile.put("account", username);
            profile.put("createdAt", LocalDateTime.now());
            profile.put("avatar", avatarSupport.resolveAvatar(username));
            return profile;
        }

        User user = getUserByUsernameOrThrow(username);
        return buildProfilePayload(user);
    }

    @Override
    public Map<String, Object> updateProfile(String username, String role, String nickname, String password, String avatar) {
        if (!StringUtils.hasText(username)) {
            throw new UnauthorizedException("请先登录");
        }
        if ("GUEST".equalsIgnoreCase(role)) {
            throw new ForbiddenException("游客账号不支持修改资料");
        }

        User user = getUserByUsernameOrThrow(username);

        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname.trim());
        }
        if (StringUtils.hasText(password)) {
            String passwordValue = password.trim();
            authPasswordSupport.validatePasswordStrength(passwordValue);
            user.setPassword(authPasswordSupport.encode(passwordValue));
        }
        String normalizedAvatar = avatarSupport.normalizeAvatarInput(avatar);
        if (StringUtils.hasText(normalizedAvatar)) {
            if (!avatarSupport.isAllowedAvatar(normalizedAvatar)) {
                throw new IllegalArgumentException("不支持该头像");
            }
            avatarSupport.saveAvatar(username, normalizedAvatar);
        }
        updateById(user);
        currentUserCacheSupport.writeByUsername(user.getUsername(), user);
        return buildProfilePayload(user);
    }

    @Override
    public Map<String, Object> getCheckinInfo(String username, String role) {
        if (!StringUtils.hasText(username)) {
            throw new UnauthorizedException("请先登录");
        }
        if ("GUEST".equalsIgnoreCase(role)) {
            return authCheckinSupport.resolveCheckinPayload(null, false);
        }
        User user = getUserByUsernameOrThrow(username);
        return authCheckinSupport.resolveCheckinPayload(user.getId(), false);
    }

    private User findUserByLookupValue(String lookupValue) {
        if (!StringUtils.hasText(lookupValue)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>()
                .apply("LOWER(username) = {0}", lookupValue)
                .last("LIMIT 1"));
    }

    private User getUserByUsernameOrThrow(String username) {
        User user = currentUserCacheSupport.readByUsername(username);
        if (user == null) {
            user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (user != null) {
                currentUserCacheSupport.writeByUsername(username, user);
            }
        }
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return user;
    }

    private User buildSafeUser(User user, String role) {
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setNickname(user.getNickname());
        safeUser.setCreatedAt(user.getCreatedAt());
        safeUser.setPassword(null);
        safeUser.setRole(role);
        return safeUser;
    }

    private Map<String, Object> buildProfilePayload(User user) {
        String role = authRoleSupport.normalizeRole(user.getRole());
        User safeUser = buildSafeUser(user, role);

        Map<String, Object> data = new HashMap<>();
        data.put("id", safeUser.getId());
        data.put("username", safeUser.getUsername());
        data.put("nickname", safeUser.getNickname());
        data.put("role", safeUser.getRole());
        data.put("createdAt", safeUser.getCreatedAt());
        data.put("accountType", isEmailIdentifier(safeUser.getUsername()) ? "email" : "username");
        data.put("account", safeUser.getUsername());
        data.put("avatar", avatarSupport.resolveAvatar(safeUser.getUsername()));
        return data;
    }

    private String normalizeRegisterIdentifier(String username) {
        String normalized = normalizeUsernameInput(username);
        if (isEmailIdentifier(normalized)) {
            return normalized.toLowerCase(Locale.ROOT);
        }
        return normalized;
    }

    private boolean isEmailIdentifier(String value) {
        return StringUtils.hasText(value) && value.contains("@");
    }

    private String normalizeUsernameInput(String username) {
        return authPasswordSupport.normalizeIdentifier(authPasswordSupport.safeTrim(username));
    }

    private String normalizeUsernameLookup(String username) {
        return normalizeUsernameInput(username).toLowerCase(Locale.ROOT);
    }

}
