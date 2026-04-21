package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName("student_info")
@Data
public class StudentInfo extends BaseEntity {

    /**
     * 用户ID（关联sys_user.id）
     */
    @TableField("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 年级
     */
    @TableField("grade")
    private String grade;

    /**
     * 专业
     */
    @TableField("major")
    private String major;

    /**
     * 班级
     */
    @TableField("class_name")
    private String className;

    /**
     * 学院名称
     */
    @TableField("college_name")
    private String collegeName;

    /**
     * 专业ID
     */
    @TableField("major_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long majorId;

}
