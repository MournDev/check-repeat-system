package com.abin.checkrepeatsystem.pojo.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 专业实体类
 * 注意：当前major表同时承担专业定义和用户-专业关联两种职责（user_id、current_advisor_count、
 * max_advisor_count等字段属于关联职责）。后续应拆分为major（专业定义）和sys_user_major（关联表）。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("major")
public class Major extends BaseEntity {

    /**
     * 专业编码（唯一）
     */
    @TableField("major_code")
    private String majorCode;

    /**
     * 专业名称
     */
    @TableField("major_name")
    private String majorName;

    /**
     * 所属学院ID（关联college.id）
     */
    @TableField("college_id")
    private Long collegeId;

    /**
     * 专业描述（可选）
     */
    @TableField("major_desc")
    private String majorDesc;

    /**
     * 用户ID（用户-专业关联字段，属于关联表职责，后续应迁移至sys_user_major表）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 当前指导任务数（用户-专业关联字段，后续应迁移至sys_user_major表）
     */
    @TableField("current_advisor_count")
    private Integer currentAdvisorCount;

    /**
     * 最大指导任务上限（用户-专业关联字段，后续应迁移至sys_user_major表）
     */
    @TableField("max_advisor_count")
    private Integer maxAdvisorCount;

}
