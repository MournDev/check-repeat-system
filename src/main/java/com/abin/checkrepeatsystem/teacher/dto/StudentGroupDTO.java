package com.abin.checkrepeatsystem.teacher.dto;

import lombok.Data;

/**
 * 学生分组DTO
 */
@Data
public class StudentGroupDTO {
    /**
     * 分组ID
     */
    private Long id;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 分组描述
     */
    private String description;
}
