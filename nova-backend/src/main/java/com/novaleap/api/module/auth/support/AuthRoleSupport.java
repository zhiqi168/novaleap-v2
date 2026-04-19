package com.novaleap.api.module.auth.support;

import com.novaleap.api.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthRoleSupport {

    private static final List<String> ADMIN_ROLES = List.of("SUPER_ADMIN", "ADMIN", "OPS");

    public String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "USER";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if ("GUEST".equals(normalized)) {
            return "GUEST";
        }
        if (isAdminRole(normalized)) {
            return normalized;
        }
        return "USER";
    }

    public boolean isAdminRole(String role) {
        if (!StringUtils.hasText(role)) {
            return false;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return ADMIN_ROLES.stream().anyMatch(item -> item.equalsIgnoreCase(normalized));
    }

    public boolean isAdminUser(User user) {
        return user != null && isAdminRole(user.getRole());
    }

    public boolean hasAdminAuthority(Authentication authentication) {
        return resolveRole(authentication, false)
                .map(this::isAdminRole)
                .orElse(false);
    }

    public String resolveRole(Authentication authentication) {
        return resolveRole(authentication, true).orElse("ANONYMOUS");
    }

    private Optional<String> resolveRole(Authentication authentication, boolean normalize) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return Optional.empty();
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (String adminRole : ADMIN_ROLES) {
            if (hasAuthority(authorities, "ROLE_" + adminRole)) {
                return Optional.of(normalize ? normalizeRole(adminRole) : adminRole);
            }
        }
        if (hasAuthority(authorities, "ROLE_GUEST")) {
            return Optional.of("GUEST");
        }
        if (hasAuthority(authorities, "ROLE_USER")) {
            return Optional.of("USER");
        }
        return Optional.empty();
    }

    private boolean hasAuthority(Collection<? extends GrantedAuthority> authorities, String target) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(value -> target.equalsIgnoreCase(value));
    }
}
