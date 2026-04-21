package com.abin.checkrepeatsystem.common.enums;

import lombok.Getter;

/**
 * 论文状态枚举类
 * 统一管理所有论文状态，确保前后端使用一致的状态值
 */
@Getter
public enum PaperStatusEnum {
    // 论文状态定义
    PENDING("pending", "待分配"),           // 初始状态，等待分配指导老师
    ASSIGNED("assigned", "已分配"),         // 已分配指导老师，等待学生确认
    CHECKING("checking", "待查重"),         // 学生确认后，进入待查重状态
    AUDITING("auditing", "待审核"),         // 查重完成后，进入待审核状态
    COMPLETED("completed", "审核通过"),     // 审核通过，论文完成
    REJECTED("rejected", "审核不通过"),     // 审核不通过，论文被驳回
    WITHDRAWN("withdrawn", "已取消");       // 学生主动取消论文

    /**
     * 状态值（存储在数据库中）
     */
    private final String value;

    /**
     * 状态描述（用于显示）
     */
    private final String description;

    PaperStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 根据状态值获取枚举实例
     * @param value 状态值
     * @return 枚举实例
     */
    public static PaperStatusEnum getByValue(String value) {
        for (PaperStatusEnum status : values()) {
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
    public static PaperStatusEnum getByDescription(String description) {
        for (PaperStatusEnum status : values()) {
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
