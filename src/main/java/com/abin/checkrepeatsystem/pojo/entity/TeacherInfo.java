package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName("teacher_info")
@Data
public class TeacherInfo extends BaseEntity {

    /**
     * 用户ID（关联sys_user.id）
     */
    @TableField("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 职称
     */
    @TableField("professional_title")
    private String professionalTitle;

    /**
     * 科研方向
     */
    @TableField("research_direction")
    private String researchDirection;

    /**
     * 当前指导教师数量
     */
    @TableField("current_advisor_count")
    private Integer currentAdvisorCount;

    /**
     * 办公地点
     */
    @TableField("office")
    private String office;

    /**
     * 办公时间
     */
    @TableField("office_hours")
    private String officeHours;

    /**
     * 最大审核数量
     */
    @TableField("max_review_count")
    private Integer maxReviewCount;

    /**
     * 审核期限（天）
     */
    @TableField("review_deadline")
    private Integer reviewDeadline;

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

    /**
     * 专业名称
     */
    @TableField("major")
    private String major;
}
