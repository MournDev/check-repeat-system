package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 学生分组与学生关系实体类
 */
@Data
@TableName("student_group_student_rel")
public class StudentGroupStudentRel extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    private Long groupId;

    /**
     * 学生ID
     */
    private Long studentId;
}
