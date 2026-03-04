package com.abin.checkrepeatsystem.student.dto;

import lombok.Data;

@Data
public class UpdateEmailReq {
    private Long userId;
    private String newEmail;
    private String verificationCode;
}
