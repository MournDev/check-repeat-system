package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName("admin_info")
@Data
public class AdminInfo extends BaseEntity {

    /**
     * 用户ID（关联sys_user.id）
     */
    @TableField("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 职位
     */
    @TableField("position")
    private String position;

    /**
     * 所属部门
     */
    @TableField("department")
    private String department;

    /**
     * 办公地址
     */
    @TableField("office_address")
    private String officeAddress;
}
