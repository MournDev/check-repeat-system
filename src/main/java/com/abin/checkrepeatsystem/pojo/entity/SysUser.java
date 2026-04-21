package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@TableName("sys_user") // 关键注解：指定关联的数据库表为sys_user
@Data
public class SysUser extends BaseEntity {

    /**
     * 头像（图片URL）
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 登录账号（唯一，学生用学号、教师用工号、管理员自定义）
     */
    @TableField("username")
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 50, message = "登录账号长度不能超过50字符")
    private String username;

    /**
     * 密码（BCrypt加密存储）
     */
    @TableField("password")
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50字符")
    private String realName;

    /**
     * 角色ID（关联sys_role.id）
     */
    @TableField("role_id")
    @NotNull(message = "角色ID不能为空")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long roleId;

    /**
     * 学院ID（关联sys_college.id）
     */
    @TableField("college_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long collegeId;

    /**
     * 邮箱（用于密码重置、消息通知，唯一）
     */
    @TableField("email")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100字符")
    private String email;

    /**
     * 邮箱验证状态（0-未验证，1-已验证）
     */
    @TableField("email_verified")
    private Integer emailVerified;

    /**
     * 手机号（可为空）
     */
    @TableField("phone")
    private String phone;

    /**
     * 个人介绍
     */
    @TableField("introduce")
    private String introduce;

    /**
     * 账号状态（0-禁用，1-正常）
     */
    @TableField("status")
    private Integer status;

    /**
     * 最后登录时间（可为空）
     */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 用户类型（ADMIN-管理员，STUDENT-学生，TEACHER-教师）
     */
    @TableField("user_type")
    private String userType;

    /**
     * 角色名称（冗余字段，便于显示）
     */
    @TableField(exist = false)
    private String roleName;

    /**
     * 年级（冗余字段，便于显示）
     */
    @TableField(exist = false)
    private String grade;

    /**
     * 学院名称（冗余字段，便于显示）
     */
    @TableField(exist = false)
    private String collegeDisplayName;

    /**
     * 专业名称（冗余字段，便于显示）
     */
    @TableField(exist = false)
    private String majorDisplayName;

}