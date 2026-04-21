package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核意见模板实体类
 */
@Data
@TableName("review_template")
public class ReviewTemplate extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

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
    @TableField(typeHandler = JacksonTypeHandler.class)
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
     * 最后使用时间
     */
    private LocalDateTime lastUsed;
}
