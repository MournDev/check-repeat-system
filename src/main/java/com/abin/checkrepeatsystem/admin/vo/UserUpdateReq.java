package com.abin.checkrepeatsystem.admin.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户更新请求VO
 */
@Data
public class UserUpdateReq {
    private String realName;
    private String email;
    private String phone;
    private String collegeName;
    private String major;
    private String grade;
    private String className;
    
    // 管理员专用字段
    private String position;
    private String department;
    private String officeAddress;
    private String introduce;
    
    private Integer status;
}