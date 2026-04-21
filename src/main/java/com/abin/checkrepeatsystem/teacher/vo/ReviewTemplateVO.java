package com.abin.checkrepeatsystem.teacher.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;
import java.util.Date;

/**
 * 审核意见模板VO
 */
@Data
public class ReviewTemplateVO {
    /**
     * 模板ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
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

    /**
     * 使用次数
     */
    private Integer usageCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 最后使用时间
     */
    private Date lastUsed;
}
