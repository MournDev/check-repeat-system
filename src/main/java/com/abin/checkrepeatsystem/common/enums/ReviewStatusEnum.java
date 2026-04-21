package com.abin.checkrepeatsystem.common.enums;

import lombok.Getter;

/**
 * 审核状态枚举类
 * 统一管理审核记录的状态
 */
@Getter
public enum ReviewStatusEnum {
    // 审核状态定义
    PASS("completed", "审核通过"),       // 审核通过
    REJECT("rejected", "审核不通过");     // 审核不通过

    /**
     * 状态值（存储在数据库中）
     */
    private final String value;

    /**
     * 状态描述（用于显示）
     */
    private final String description;

    ReviewStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 根据状态值获取枚举实例
     * @param value 状态值
     * @return 枚举实例
     */
    public static ReviewStatusEnum getByValue(String value) {
        for (ReviewStatusEnum status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据状态描述获取枚举实例
     * @param description 状态描述
     * @return 枚举实例
     */
    public static ReviewStatusEnum getByDescription(String description) {
        for (ReviewStatusEnum status : values()) {
            if (status.getDescription().equals(description)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 检查状态值是否有效
     * @param value 状态值
     * @return 是否有效
     */
    public static boolean isValid(String value) {
        return getByValue(value) != null;
    }
}
