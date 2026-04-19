package com.novaleap.api.service.impl;

import com.novaleap.api.module.auth.service.SharedAuthLoginService;
import com.novaleap.api.module.auth.support.AuthCheckinSupport;
import com.novaleap.api.module.auth.support.AuthLoginAuditSupport;
import com.novaleap.api.module.auth.support.AuthPasswordSupport;
import com.novaleap.api.module.auth.support.AuthPortal;
import com.novaleap.api.module.auth.support.AuthRateLimitSupport;
import com.novaleap.api.module.auth.support.AuthRoleSupport;
import com.novaleap.api.module.auth.support.AvatarSupport;
import com.novaleap.api.module.auth.support.TurnstileVerifier;
import com.novaleap.api.mapper.UserMapper;
import com.novaleap.api.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthRateLimitSupport authRateLimitSupport;

    @Mock
    private AuthPasswordSupport authPasswordSupport;

    @Mock
    private AuthCheckinSupport authCheckinSupport;

    @Mock
    private AvatarSupport avatarSupport;

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @Mock
    private AuthRoleSupport authRoleSupport;

    @Mock
    private AuthLoginAuditSupport authLoginAuditSupport;

    @Test
    void shouldShowEmailNotRegisteredWhenPasswordLoginUsesMissingEmail() {
        SharedAuthLoginService service = spy(new SharedAuthLoginService(
                userMapper,
                jwtUtils,
                authRateLimitSupport,
                authPasswordSupport,
                authCheckinSupport,
                avatarSupport,
                turnstileVerifier,
                authRoleSupport,
                authLoginAuditSupport
        ));

        when(authPasswordSupport.safeTrim("secret123")).thenReturn("secret123");
        when(authPasswordSupport.safeTrim("missing@example.com")).thenReturn("missing@example.com");
        when(authPasswordSupport.normalizeIdentifier("missing@example.com")).thenReturn("missing@example.com");
        when(authRateLimitSupport.normalizeClientIp("127.0.0.1")).thenReturn("127.0.0.1");
        when(authRateLimitSupport.isLoginLocked("127.0.0.1", "missing@example.com", AuthPortal.CLIENT)).thenReturn(false);
        doReturn(null).when(service).findUserByLookupValue("missing@example.com");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.authenticateByPassword(
                        "missing@example.com",
                        "secret123",
                        "127.0.0.1",
                        "",
                        AuthPortal.CLIENT,
                        user -> {}
                )
        );

        assertEquals("邮箱未注册", error.getMessage());
        verify(authRateLimitSupport).recordLoginFailure("127.0.0.1", "missing@example.com", AuthPortal.CLIENT);
    }
}
