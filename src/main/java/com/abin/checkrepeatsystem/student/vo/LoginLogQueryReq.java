package com.abin.checkrepeatsystem.student.vo;

import lombok.Data;

@Data
public class LoginLogQueryReq {
    private Long userId;
    private String username;
    private String loginIp;
    private Integer loginStatus;
    private Integer pageNo = 1;
    private Integer pageSize = 5;
}

