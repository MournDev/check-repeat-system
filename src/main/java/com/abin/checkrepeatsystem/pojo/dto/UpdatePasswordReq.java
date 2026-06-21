package com.abin.checkrepeatsystem.pojo.dto;


import com.abin.checkrepeatsystem.common.annotation.ValidPassword;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class UpdatePasswordReq {
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @ValidPassword
    private String newPassword;

}

