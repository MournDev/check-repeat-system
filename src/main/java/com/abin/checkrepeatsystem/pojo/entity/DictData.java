package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 字典数据实体类
 * 存储系统中各种字典数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dict_data")
public class DictData extends BaseEntity {

    /**
     * 字典类型
     */
    @TableField("dict_type")
    private String dictType;

    /**
     * 字典标签
     */
    @TableField("dict_label")
    private String dictLabel;

    /**
     * 字典值
     */
    @TableField("dict_value")
    private String dictValue;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 状态（0-禁用, 1-启用）
     */
    @TableField("status")
    private Integer status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    // 冗余字段 - 便于查询显示
    /**
     * 状态文本
     */
    @TableField(exist = false)
    private String statusText;
}
