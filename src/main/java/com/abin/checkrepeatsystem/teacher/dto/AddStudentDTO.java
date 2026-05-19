package com.abin.checkrepeatsystem.teacher.dto;

import lombok.Data;

/**
 * 添加学生请求DTO
 */
@Data
public class AddStudentDTO {
    /**
     * 学号（必填，唯一）
     */
    private String username;
    
    /**
     * 学生姓名（必填）
     */
    private String studentName;
    
    /**
     * 学院名称（必填，用于显示）
     */
    private String collegeName;

    /**
     * 学院ID（必填，用于关联）
     */
    private Long collegeId;

    /**
     * 专业名称（必填，用于显示）
     */
    private String major;

    /**
     * 专业ID（必填，用于关联）
     */
    private Long majorId;
    
    /**
     * 年级（必填）
     */
    private String grade;
    
    /**
     * 班级（可选）
     */
    private String className;
    
    /**
     * 邮箱（可选）
     */
    private String email;
    
    /**
     * 手机号（可选）
     */
    private String phone;
}