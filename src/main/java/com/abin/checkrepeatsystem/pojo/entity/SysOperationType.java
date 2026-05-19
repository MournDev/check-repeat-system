package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作类型实体类
 */
@Data
@TableName("sys_operation_type")
public class SysOperationType{
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    /**
     * 操作类型编码
     */
    @TableField("type")
    private String type;

    /**
     * 操作类型名称
     */
    @TableField("name")
    private String name;

    /**
     * 操作类型描述
     */
    @TableField("description")
    private String description;

    /**
     * 所属模块
     */
    @TableField("module")
    private String module;

    /**
     * 权限等级(LOW/MEDIUM/HIGH)
     */
    @TableField("permission_level")
    private String permissionLevel;

    /**
     * 状态(1启用,0禁用)
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("update_time")
    private LocalDateTime updateTime;
}