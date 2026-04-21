package com.abin.checkrepeatsystem.teacher.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 学生VO
 */
@Data
public class StudentVO {
    /**
     * 学生ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 学生姓名
     */
    private String name;

    /**
     * 学号
     */
    private String studentId;

    /**
     * 学院名称
     */
    private String collegeName;

    /**
     * 专业名称
     */
    private String majorName;
}
