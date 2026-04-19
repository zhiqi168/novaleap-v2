package com.novaleap.api.module.auth.service;

import com.novaleap.api.dto.LoginRequest;
import com.novaleap.api.dto.PasswordResetRequest;
import com.novaleap.api.dto.ProfileUpdateRequest;
import com.novaleap.api.dto.RegisterRequest;
import com.novaleap.api.entity.User;
import com.novaleap.api.module.auth.support.AuthPortal;
import com.novaleap.api.module.auth.support.TurnstileVerifier;
import com.novaleap.api.module.system.security.CurrentUser;
import com.novaleap.api.module.system.security.CurrentUserService;
import com.novaleap.api.module.system.web.ClientRequestService;
import com.novaleap.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class ClientAuthApplicationService {

    private static final String EMAIL_NOT_REGISTERED_ERROR = "邮箱未注册";

    private final AuthService authService;
    private final SharedAuthLoginService sharedAuthLoginService;
    private final CurrentUserService currentUserService;
    private final ClientRequestService clientRequestService;
    private final EmailService emailService;
    private final TurnstileVerifier turnstileVerifier;

    public ClientAuthApplicationService(
            AuthService authService,
            SharedAuthLoginService sharedAuthLoginService,
            CurrentUserService currentUserService,
            ClientRequestService clientRequestService,
            EmailService emailService,
            TurnstileVerifier turnstileVerifier
    ) {
        this.authService = authService;
        this.sharedAuthLoginService = sharedAuthLoginService;
        this.currentUserService = currentUserService;
        this.clientRequestService = clientRequestService;
        this.emailService = emailService;
        this.turnstileVerifier = turnstileVerifier;
    }

    public Map<String, Object> login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = clientRequestService.resolveClientIp(httpRequest);
        if (request.isCodeLogin()) {
            String email = emailService.normalizeEmail(request.getUsername());
            emailService.assertValidEmail(email);
            String emailCode = request.getEmailCode();
            if (!StringUtils.hasText(emailCode)) {
                throw new IllegalArgumentException("验证码不能为空");
            }

            EmailService.VerificationCheckResult checkResult = emailService.verifyCodeWithPolicy(email, "login", emailCode);
            if (!checkResult.passed()) {
                throw new IllegalArgumentException(checkResult.message());
            }

            User user = sharedAuthLoginService.authenticateByVerifiedIdentity(
                    email,
                    clientIp,
                    request.getTurnstileToken(),
                    AuthPortal.CLIENT
            );
            emailService.consumeCode(email, "login");
            sharedAuthLoginService.recordSuccess(user, clientIp, AuthPortal.CLIENT, "code");
            return sharedAuthLoginService.buildAuthResult(user, true, AuthPortal.CLIENT);
        }

        User user = sharedAuthLoginService.authenticateByPassword(
                request.getUsername(),
                request.getPassword(),
                clientIp,
                request.getTurnstileToken(),
                AuthPortal.CLIENT,
                authService::updateById
        );
        sharedAuthLoginService.recordSuccess(user, clientIp, AuthPortal.CLIENT, "password");
        return sharedAuthLoginService.buildAuthResult(user, true, AuthPortal.CLIENT);
    }

    public Map<String, Object> guestLogin() {
        return authService.guestLogin();
    }

    public void logout(Authentication authentication) {
        CurrentUser currentUser = currentUserService.current(authentication);
        if (!currentUser.isAuthenticated() || currentUser.guest()) {
            return;
        }
        authService.logout(currentUser.safeUsername());
    }

    public Map<String, Object> register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = emailService.normalizeEmail(request.getUsername());
        EmailService.VerificationCheckResult checkResult = emailService.verifyCodeWithPolicy(email, "register", request.getEmailCode());
        if (!checkResult.passed()) {
            throw new IllegalArgumentException(checkResult.message());
        }

        Map<String, Object> result = authService.register(
                email,
                request.getPassword(),
                request.getConfirmPassword(),
                request.getNickname(),
                request.getConsent(),
                clientRequestService.resolveClientIp(httpRequest),
                request.getTurnstileToken()
        );
        emailService.consumeCode(email, "register");
        return result;
    }

    public void sendEmailCode(String email, String type, String turnstileToken, HttpServletRequest httpRequest) {
        String normalizedEmail = emailService.normalizeEmail(email);
        emailService.assertValidEmail(normalizedEmail);

        String normalizedType = normalizeCodeType(type);
        boolean accountExists = authService.emailExists(normalizedEmail);
        if (requiresExistingAccount(normalizedType) && !accountExists) {
            throw new IllegalArgumentException(EMAIL_NOT_REGISTERED_ERROR);
        }

        boolean allowDelivery = shouldDeliverCode(normalizedType, accountExists);
        String clientIp = clientRequestService.resolveClientIp(httpRequest);
        if (emailService.shouldRequireHumanCheck(normalizedEmail, normalizedType, clientIp)) {
            turnstileVerifier.verifyIfEnabled(turnstileToken, clientIp);
        }

        emailService.sendVerificationCode(
                normalizedEmail,
                normalizedType,
                clientIp,
                resolveUserAgent(httpRequest),
                allowDelivery
        );
    }

    public void resetPassword(PasswordResetRequest request, HttpServletRequest httpRequest) {
        String email = emailService.normalizeEmail(request.getUsername());
        EmailService.VerificationCheckResult checkResult = emailService.verifyCodeWithPolicy(email, "reset", request.getEmailCode());
        if (!checkResult.passed()) {
            throw new IllegalArgumentException(checkResult.message());
        }

        authService.resetPassword(
                email,
                request.getNewPassword(),
                clientRequestService.resolveClientIp(httpRequest),
                resolveUserAgent(httpRequest)
        );
        emailService.consumeCode(email, "reset");
    }

    public Map<String, Object> getProfile(Authentication authentication) {
        CurrentUser currentUser = requireAuthenticatedUser(authentication);
        return authService.getProfile(currentUser.safeUsername(), currentUser.role());
    }

    public Map<String, Object> updateProfile(Authentication authentication, ProfileUpdateRequest request) {
        CurrentUser currentUser = requireAuthenticatedUser(authentication);
        return authService.updateProfile(
                currentUser.safeUsername(),
                currentUser.role(),
                request.getNickname(),
                request.getPassword(),
                request.getAvatar()
        );
    }

    public Map<String, Object> getCheckinStreak(Authentication authentication) {
        CurrentUser currentUser = requireAuthenticatedUser(authentication);
        return authService.getCheckinInfo(currentUser.safeUsername(), currentUser.role());
    }

    private CurrentUser requireAuthenticatedUser(Authentication authentication) {
        currentUserService.requireUsername(authentication);
        return currentUserService.current(authentication);
    }

    private boolean requiresExistingAccount(String type) {
        return "login".equals(type) || "reset".equals(type);
    }

    private boolean shouldDeliverCode(String type, boolean accountExists) {
        return switch (type) {
            case "register" -> !accountExists;
            case "login", "reset" -> accountExists;
            default -> false;
        };
    }

    private String normalizeCodeType(String type) {
        if ("login".equalsIgnoreCase(type)) {
            return "login";
        }
        if ("reset".equalsIgnoreCase(type)) {
            return "reset";
        }
        return "register";
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return request == null ? "" : request.getHeader("User-Agent");
    }
}
