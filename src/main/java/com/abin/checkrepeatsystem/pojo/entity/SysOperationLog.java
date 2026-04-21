package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_operation_log")
public class SysOperationLog extends BaseEntity {

    /**
     * 操作类型（如：user_login、admin_user_create等）
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 操作描述
     */
    @TableField("operation_desc")  // 修复：字段名应为operation_desc
    private String description;

    /**
     * 操作参数（JSON格式）
     */
    @TableField("operation_param")
    private String requestParams;

    /**
     * 操作用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 操作用户名
     */
    @TableField("user_name")
    private String userName;

    /**
     * 操作用户类型
     */
    @TableField("user_type")
    private String userType;

    /**
     * 操作对象
     */
    @TableField("target")
    private String target;

    /**
     * 操作状态（1成功，0失败）
     */
    @TableField("status")
    private Integer status;

    /**
     * 详细信息
     */
    @TableField("details")
    private String details;

    /**
     * 操作用户IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 操作时间
     */
    @TableField("operation_time")
    private LocalDateTime operationTime;
}