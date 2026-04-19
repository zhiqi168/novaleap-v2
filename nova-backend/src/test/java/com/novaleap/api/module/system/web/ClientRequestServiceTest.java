package com.novaleap.api.module.system.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientRequestServiceTest {

    private final ClientRequestService clientRequestService = new ClientRequestService();

    @Test
    void shouldUseForwardedHeaderWhenRemoteAddressIsTrustedProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.10, 127.0.0.1");

        String clientIp = clientRequestService.resolveClientIp(request);

        assertEquals("198.51.100.10", clientIp);
    }

    @Test
    void shouldIgnoreForwardedHeaderWhenRemoteAddressIsUntrusted() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.20");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.10");

        String clientIp = clientRequestService.resolveClientIp(request);

        assertEquals("203.0.113.20", clientIp);
    }
}
