package com.novaleap.api.module.system.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novaleap.api.common.exception.ForbiddenException;
import com.novaleap.api.common.exception.UnauthorizedException;
import com.novaleap.api.entity.User;
import com.novaleap.api.mapper.UserMapper;
import com.novaleap.api.module.auth.support.AuthRoleSupport;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CurrentUserService {

    private final UserMapper userMapper;
    private final AuthRoleSupport authRoleSupport;

    public CurrentUserService(UserMapper userMapper, AuthRoleSupport authRoleSupport) {
        this.userMapper = userMapper;
        this.authRoleSupport = authRoleSupport;
    }

    public CurrentUser current(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new CurrentUser("", "ANONYMOUS", true, false, false);
        }
        String username = safe(authentication.getName());
        if (!StringUtils.hasText(username) || "anonymousUser".equals(username)) {
            return new CurrentUser("", "ANONYMOUS", true, false, false);
        }

        String role = authRoleSupport.resolveRole(authentication);
        boolean guest = "GUEST".equalsIgnoreCase(role);
        boolean admin = authRoleSupport.hasAdminAuthority(authentication);
        return new CurrentUser(username, role, false, guest, admin);
    }

    public boolean isAnonymous(Authentication authentication) {
        return current(authentication).anonymous();
    }

    public boolean isGuest(Authentication authentication) {
        return current(authentication).guest();
    }

    public boolean isAdmin(Authentication authentication) {
        return current(authentication).admin();
    }

    public String requireUsername(Authentication authentication) {
        CurrentUser currentUser = current(authentication);
        if (currentUser.anonymous()) {
            throw new UnauthorizedException("请先登录");
        }
        return currentUser.safeUsername();
    }

    public User loadDatabaseUser(Authentication authentication) {
        CurrentUser currentUser = current(authentication);
        if (!currentUser.isMember()) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, currentUser.safeUsername())
                .last("LIMIT 1"));
    }

    public User requireDatabaseUser(Authentication authentication, String guestMessage) {
        CurrentUser currentUser = current(authentication);
        if (currentUser.anonymous()) {
            throw new UnauthorizedException("请先登录");
        }
        if (currentUser.guest()) {
            throw new ForbiddenException(StringUtils.hasText(guestMessage) ? guestMessage : "游客账号无权访问该资源");
        }
        User user = loadDatabaseUser(authentication);
        if (user == null) {
            throw new UnauthorizedException("请先登录");
        }
        return user;
    }

    public ActorIdentity resolveActor(Authentication authentication) {
        CurrentUser currentUser = current(authentication);
        if (currentUser.anonymous()) {
            return new ActorIdentity("", "");
        }
        if (currentUser.guest()) {
            return new ActorIdentity("guest", currentUser.safeUsername());
        }
        return new ActorIdentity("user", currentUser.safeUsername());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
