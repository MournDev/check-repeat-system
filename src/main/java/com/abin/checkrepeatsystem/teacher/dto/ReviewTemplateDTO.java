package com.abin.checkrepeatsystem.teacher.dto;

import lombok.Data;

import java.util.List;

/**
 * 审核意见模板DTO
 */
@Data
public class ReviewTemplateDTO {
    /**
     * 模板ID
     */
    private Long id;

    /**
     * 模板标题
     */
    private String title;

    /**
     * 模板分类
     */
    private String category;

    /**
     * 模板内容
     */
    private String content;

    /**
     * 使用场景
     */
    private List<String> scenarios;

    /**
     * 是否公开
     */
    private Boolean isPublic;
}
