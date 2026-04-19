package com.novaleap.api.module.auth.service;

import com.novaleap.api.module.auth.support.TurnstileVerifier;
import com.novaleap.api.module.system.security.CurrentUserService;
import com.novaleap.api.module.system.web.ClientRequestService;
import com.novaleap.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private SharedAuthLoginService sharedAuthLoginService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ClientRequestService clientRequestService;

    @Mock
    private EmailService emailService;

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Test
    void shouldRejectLoginCodeSendWhenEmailIsNotRegistered() {
        ClientAuthApplicationService service = new ClientAuthApplicationService(
                authService,
                sharedAuthLoginService,
                currentUserService,
                clientRequestService,
                emailService,
                turnstileVerifier
        );

        when(emailService.normalizeEmail("missing@example.com")).thenReturn("missing@example.com");
        when(authService.emailExists("missing@example.com")).thenReturn(false);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.sendEmailCode("missing@example.com", "login", "", httpServletRequest)
        );

        assertEquals("邮箱未注册", error.getMessage());
        verify(emailService, never()).sendVerificationCode(
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyBoolean()
        );
    }
}
