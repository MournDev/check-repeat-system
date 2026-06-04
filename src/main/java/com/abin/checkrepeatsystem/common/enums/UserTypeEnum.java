package com.abin.checkrepeatsystem.common.enums;

import lombok.Getter;

/**
 * 用户角色枚举（统一管理角色，避免硬编码）
 */
@Getter
public enum UserTypeEnum {
    ADMIN(0, "ADMIN", "管理员"),
    STUDENT(1, "STUDENT", "学生"),
    TEACHER(2, "TEACHER", "教师"),
    SUPER_ADMIN(3, "SUPER_ADMIN", "超级管理员");

    /** SQL中role_id字段的数值 */
    public static final Long ROLE_ID_ADMIN = 0L;
    public static final Long ROLE_ID_STUDENT = 1L;
    public static final Long ROLE_ID_TEACHER = 2L;
    public static final Long ROLE_ID_SUPER_ADMIN = 3L;

    /** 业务层的角色编码常量，用于替代硬编码字符串 */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    private final Integer code;
    private final String roleCode;
    private final String desc;

    UserTypeEnum(Integer code, String roleCode, String desc) {
        this.code = code;
        this.roleCode = roleCode;
        this.desc = desc;
    }

    public static UserTypeEnum getByCode(Integer code) {
        for (UserTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的用户角色码：" + code);
    }

    public static UserTypeEnum getByRoleCode(String roleCode) {
        for (UserTypeEnum type : values()) {
            if (type.getRoleCode().equalsIgnoreCase(roleCode)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的用户角色编码：" + roleCode);
    }
}
