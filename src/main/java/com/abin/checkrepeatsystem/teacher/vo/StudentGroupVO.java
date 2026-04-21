package com.abin.checkrepeatsystem.teacher.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 学生分组VO
 */
@Data
public class StudentGroupVO {
    /**
     * 分组ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组描述
     */
    private String description;

    /**
     * 学生数量
     */
    private Integer studentCount;

    /**
     * 学生列表
     */
    private List<StudentVO> students;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
