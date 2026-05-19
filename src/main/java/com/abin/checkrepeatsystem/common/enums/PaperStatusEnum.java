package com.abin.checkrepeatsystem.common.enums;

import lombok.Getter;

@Getter
public enum PaperStatusEnum {
    
    DRAFT("draft", "草稿"),
    SUBMITTED("submitted", "已提交"),
    AUDITING("auditing", "审核中"),
    COMPLETED("completed", "已完成"),
    REJECTED("rejected", "已驳回"),
    REVISED("revised", "修改中"),
    REVIEWED("reviewed", "已评审"),
    PENDING("pending", "待处理");

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

    public boolean isTerminalStatus() {
        return this == COMPLETED || this == REJECTED;
    }

    public boolean isReviewable() {
        return this == AUDITING || this == REVISED;
    }
}