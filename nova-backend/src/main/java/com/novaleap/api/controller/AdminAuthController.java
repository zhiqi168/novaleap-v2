package com.novaleap.api.controller;

import com.novaleap.api.common.Result;
import com.novaleap.api.dto.LoginRequest;
import com.novaleap.api.module.admin.auth.service.AdminAuthApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthApplicationService adminAuthApplicationService;

    public AdminAuthController(AdminAuthApplicationService adminAuthApplicationService) {
        this.adminAuthApplicationService = adminAuthApplicationService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return Result.success(adminAuthApplicationService.login(request, httpServletRequest));
    }
}
