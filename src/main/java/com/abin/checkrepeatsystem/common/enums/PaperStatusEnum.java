package com.abin.checkrepeatsystem.common.enums;

import lombok.Getter;

/**
 * 论文状态枚举 —— 唯一权威定义
 *
 * 状态流转：
 *   PENDING → ASSIGNED → CHECKING → AUDITING → COMPLETED
 *                                            → REJECTED → (resubmit) → AUDITING
 *           → WITHDRAWN
 */
@Getter
public enum PaperStatusEnum {

    PENDING("pending", "待分配"),
    ASSIGNED("assigned", "已分配"),
    CHECKING("checking", "查重中"),
    AUDITING("auditing", "待审核"),
    COMPLETED("completed", "审核通过"),
    REJECTED("rejected", "审核不通过"),
    WITHDRAWN("withdrawn", "已撤回");

    private final String value;
    private final String description;

    PaperStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getCode() {
        return value;
    }

    public static PaperStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PaperStatusEnum status : values()) {
            if (status.value.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown paper status: " + code);
    }

    /**
     * 是否为终态（不可再流转）
     */
    public boolean isTerminalStatus() {
        return this == COMPLETED || this == WITHDRAWN;
    }

    /**
     * 是否允许发起查重
     */
    public boolean isCheckable() {
        return this == ASSIGNED || this == CHECKING || this == AUDITING;
    }

    /**
     * 是否允许教师审核
     */
    public boolean isReviewable() {
        return this == AUDITING;
    }
}