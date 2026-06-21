package com.abin.checkrepeatsystem.pojo.dto;

import com.abin.checkrepeatsystem.common.annotation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ForgotPasswordReq {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String verificationCode;

    @NotBlank(message = "新密码不能为空")
    @ValidPassword
    private String newPassword;
}
