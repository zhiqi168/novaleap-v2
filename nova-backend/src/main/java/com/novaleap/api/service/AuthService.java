package com.novaleap.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.novaleap.api.entity.User;

import java.util.Map;

public interface AuthService extends IService<User> {

    Map<String, Object> guestLogin();

    Map<String, Object> register(
            String username,
            String password,
            String confirmPassword,
            String nickname,
            Boolean consent,
            String ip,
            String turnstileToken
    );

    boolean emailExists(String email);

    void logout(String username);

    void resetPassword(String username, String newPassword, String clientIp, String userAgent);

    Map<String, Object> getProfile(String username, String role);

    Map<String, Object> updateProfile(String username, String role, String nickname, String password, String avatar);

    Map<String, Object> getCheckinInfo(String username, String role);
}
