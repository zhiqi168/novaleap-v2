package com.novaleap.api.controller;

import com.novaleap.api.common.Result;
import com.novaleap.api.dto.LoginRequest;
import com.novaleap.api.dto.PasswordResetRequest;
import com.novaleap.api.dto.ProfileUpdateRequest;
import com.novaleap.api.dto.RegisterRequest;
import com.novaleap.api.module.auth.dto.SendEmailCodeRequest;
import com.novaleap.api.module.auth.service.ClientAuthApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ClientAuthController {

    private final ClientAuthApplicationService clientAuthApplicationService;

    public ClientAuthController(ClientAuthApplicationService clientAuthApplicationService) {
        this.clientAuthApplicationService = clientAuthApplicationService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return Result.success(clientAuthApplicationService.login(request, httpServletRequest));
    }

    @PostMapping("/guest")
    public Result<Map<String, Object>> guestLogin() {
        return Result.success(clientAuthApplicationService.guestLogin());
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(
            @RequestBody @Valid RegisterRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return Result.success(clientAuthApplicationService.register(request, httpServletRequest));
    }

    @PostMapping("/logout")
    public Result<Void> logout(Authentication authentication) {
        clientAuthApplicationService.logout(authentication);
        return Result.success();
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(
            @RequestBody @Valid PasswordResetRequest request,
            HttpServletRequest httpServletRequest
    ) {
        clientAuthApplicationService.resetPassword(request, httpServletRequest);
        return Result.success();
    }

    @PostMapping("/email/send-code")
    public Result<Void> sendEmailCode(
            @RequestBody @Valid SendEmailCodeRequest request,
            HttpServletRequest httpServletRequest
    ) {
        clientAuthApplicationService.sendEmailCode(
                request.getEmail(),
                request.getType(),
                request.getTurnstileToken(),
                httpServletRequest
        );
        return Result.success();
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile(Authentication authentication) {
        return Result.success(clientAuthApplicationService.getProfile(authentication));
    }

    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(
            Authentication authentication,
            @RequestBody @Valid ProfileUpdateRequest request
    ) {
        return Result.success(clientAuthApplicationService.updateProfile(authentication, request));
    }

    @GetMapping("/streak")
    public Result<Map<String, Object>> getCheckinStreak(Authentication authentication) {
        return Result.success(clientAuthApplicationService.getCheckinStreak(authentication));
    }
}
