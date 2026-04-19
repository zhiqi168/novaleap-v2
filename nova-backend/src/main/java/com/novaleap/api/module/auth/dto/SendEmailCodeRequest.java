package com.novaleap.api.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendEmailCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 个字符")
    private String email;

    @Size(max = 16, message = "验证码类型长度不能超过 16 个字符")
    private String type;

    @Size(max = 2048, message = "人机校验 token 长度不能超过 2048 个字符")
    private String turnstileToken;
}
