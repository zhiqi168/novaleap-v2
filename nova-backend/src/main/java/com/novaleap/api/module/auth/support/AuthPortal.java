package com.novaleap.api.module.auth.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

public enum AuthPortal {
    CLIENT,
    ADMIN;

    public String claimValue() {
        return name();
    }

    public String authority() {
        return "PORTAL_" + name();
    }

    public String redisNamespace() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AuthPortal fromClaim(String value) {
        if (!StringUtils.hasText(value)) {
            return CLIENT;
        }
        for (AuthPortal portal : values()) {
            if (portal.name().equalsIgnoreCase(value.trim())) {
                return portal;
            }
        }
        return CLIENT;
    }
}
