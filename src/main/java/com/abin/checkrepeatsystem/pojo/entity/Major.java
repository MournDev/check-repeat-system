package com.abin.checkrepeatsystem.pojo.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
     * 用户ID（用于 sys_user_major 关联查询）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 当前指导任务数
     */
    @TableField("current_advisor_count")
    private Integer currentAdvisorCount;

    /**
     * 最大指导任务上限
     */
    @TableField("max_advisor_count")
    private Integer maxAdvisorCount;

}
