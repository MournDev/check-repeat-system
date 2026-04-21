package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 自动分配历史实体类
 * 记录自动分配教师的历史操作
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auto_allocation_history")
public class AutoAllocationHistory extends BaseEntity {

    /**
     * 分配批次号
     */
    @TableField("batch_no")
    private String batchNo;

    /**
     * 分配策略（RANDOM-随机分配, LOAD_BALANCE-负载均衡, SPECIALIZED-专业匹配）
     */
    @TableField("allocation_strategy")
    private String allocationStrategy;

    /**
     * 分配学生数量
     */
    @TableField("student_count")
    private Integer studentCount;

    /**
     * 分配教师数量
     */
    @TableField("teacher_count")
    private Integer teacherCount;

    /**
     * 分配结果（SUCCESS-成功, FAILED-失败, PARTIAL-部分成功）
     */
    @TableField("allocation_result")
    private String allocationResult;

    /**
     * 分配成功数量
     */
    @TableField("success_count")
    private Integer successCount;

    /**
     * 分配失败数量
     */
    @TableField("failed_count")
    private Integer failedCount;

    /**
     * 分配开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 分配结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 执行时长（秒）
     */
    @TableField("execution_duration")
    private Integer executionDuration;

    /**
     * 操作人ID（关联sys_user.id）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 备注信息
     */
    @TableField("remark")
    private String remark;

    // 冗余字段 - 便于查询显示
    /**
     * 操作人姓名
     */
    @TableField(exist = false)
    private String operatorName;

    /**
     * 执行时长文本
     */
    @TableField(exist = false)
    private String durationText;
}
