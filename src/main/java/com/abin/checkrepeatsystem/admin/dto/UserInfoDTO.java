package com.abin.checkrepeatsystem.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息DTO
 */
@Data
public class UserInfoDTO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String userType;
    private String roleName;
    private Integer status;
    private String collegeName;
    private String major;
    private String grade;
    private String className;
    
    // 管理员专用字段
    private String position;
    private String department;
    private String officeAddress;
    
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
}