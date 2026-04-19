package com.novaleap.api.module.system.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ClientRequestService {

    private static final String[] HEADER_KEYS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String remoteAddr = normalizeRemoteAddr(request.getRemoteAddr());
        if (shouldTrustForwardedHeaders(remoteAddr)) {
            for (String key : HEADER_KEYS) {
                String value = request.getHeader(key);
                String parsed = firstIp(value);
                if (isUsableIp(parsed)) {
                    return parsed;
                }
            }
        }
        return remoteAddr;
    }

    private String firstIp(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String[] parts = raw.split(",");
        if (parts.length == 0) {
            return "";
        }
        return parts[0].trim();
    }

    private boolean isUsableIp(String ip) {
        return StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip);
    }

    private String normalizeRemoteAddr(String remoteAddr) {
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return "127.0.0.1";
        }
        return remoteAddr == null ? "" : remoteAddr;
    }

    private boolean shouldTrustForwardedHeaders(String remoteAddr) {
        if (!isUsableIp(remoteAddr)) {
            return false;
        }
        return remoteAddr.startsWith("127.")
                || remoteAddr.startsWith("10.")
                || remoteAddr.startsWith("192.168.")
                || remoteAddr.startsWith("172.16.")
                || remoteAddr.startsWith("172.17.")
                || remoteAddr.startsWith("172.18.")
                || remoteAddr.startsWith("172.19.")
                || remoteAddr.startsWith("172.2")
                || remoteAddr.startsWith("172.30.")
                || remoteAddr.startsWith("172.31.");
    }
}
