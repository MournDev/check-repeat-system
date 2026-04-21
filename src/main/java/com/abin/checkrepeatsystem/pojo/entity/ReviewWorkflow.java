package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;

/**
 * 审核工作流实体类
 */
@Data
@TableName("review_workflow")
public class ReviewWorkflow extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * 工作流步骤（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String steps;

    /**
     * 启用状态
     */
    private Boolean enabled;
}
