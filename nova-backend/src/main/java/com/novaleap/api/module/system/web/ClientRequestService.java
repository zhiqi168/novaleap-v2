package com.novaleap.api.module.system.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                String parsed = lastNonPrivateIp(value);
                if (isUsableIp(parsed)) {
                    return parsed;
                }
            }
        }
        return remoteAddr;
    }

    /**
     * 取 X-Forwarded-For 中最后一个非私有 IP 地址。
     * 在有可信反向代理的场景下，客户端真实 IP 是链中最后一个可路由的地址。
     * 攻击者在最前面添加伪造 IP（如 X-Forwarded-For: 1.2.3.4, <real-ip>），
     * 取末尾 IP 可避免 IP 限流和锁定被绕过。
     */
    private String lastNonPrivateIp(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        List<String> candidates = Stream.of(raw.split(","))
                .map(String::trim)
                .filter(ip -> isUsableIp(ip) && !isPrivateIp(ip))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return "";
        }
        return candidates.get(candidates.size() - 1);
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

    private boolean isPrivateIp(String ip) {
        if (!isUsableIp(ip)) {
            return true;
        }
        return ip.startsWith("127.")
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.2")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.")
                || "::1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip);
    }

    private boolean shouldTrustForwardedHeaders(String remoteAddr) {
        if (!isUsableIp(remoteAddr)) {
            return false;
        }
        return isPrivateIp(remoteAddr);
    }
}
