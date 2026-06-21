package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SystemConfig extends BaseEntity {

    /**
     * 配置键
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 配置值（JSON格式存储）
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 配置描述
     */
    @TableField("description")
    private String description;
}