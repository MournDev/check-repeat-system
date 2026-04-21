package com.abin.checkrepeatsystem.student.vo;

import lombok.Data;

/**
 * 学生信息VO
 */
@Data
public class StudentInfoVO {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String avatar;
}