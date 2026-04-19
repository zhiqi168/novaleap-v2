package com.novaleap.api.module.admin.auth.service;

import com.novaleap.api.dto.LoginRequest;
import com.novaleap.api.entity.User;
import com.novaleap.api.module.auth.service.SharedAuthLoginService;
import com.novaleap.api.module.auth.support.AuthPortal;
import com.novaleap.api.module.auth.support.AuthRoleSupport;
import com.novaleap.api.module.system.web.ClientRequestService;
import com.novaleap.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminAuthApplicationService {

    private final SharedAuthLoginService sharedAuthLoginService;
    private final AuthRoleSupport authRoleSupport;
    private final ClientRequestService clientRequestService;
    private final AuthService authService;

    public AdminAuthApplicationService(
            SharedAuthLoginService sharedAuthLoginService,
            AuthRoleSupport authRoleSupport,
            ClientRequestService clientRequestService,
            AuthService authService
    ) {
        this.sharedAuthLoginService = sharedAuthLoginService;
        this.authRoleSupport = authRoleSupport;
        this.clientRequestService = clientRequestService;
        this.authService = authService;
    }

    public Map<String, Object> login(LoginRequest request, HttpServletRequest httpRequest) {
        if (request.isCodeLogin()) {
            throw new IllegalArgumentException("管理端暂不支持验证码登录");
        }

        String clientIp = clientRequestService.resolveClientIp(httpRequest);
        User user = sharedAuthLoginService.authenticateByPassword(
                request.getUsername(),
                request.getPassword(),
                clientIp,
                request.getTurnstileToken(),
                AuthPortal.ADMIN,
                authService::updateById
        );

        if (!authRoleSupport.isAdminUser(user)) {
            sharedAuthLoginService.recordDenied(user, clientIp, AuthPortal.ADMIN, "password", "NO_ADMIN_ACCESS");
            throw new IllegalArgumentException("该账号无管理端访问权限");
        }

        sharedAuthLoginService.recordSuccess(user, clientIp, AuthPortal.ADMIN, "password");
        return sharedAuthLoginService.buildAuthResult(user, true, AuthPortal.ADMIN);
    }
}
