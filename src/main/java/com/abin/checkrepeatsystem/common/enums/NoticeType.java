package com.abin.checkrepeatsystem.common.enums;

// 通知类型枚举
public enum NoticeType {
    PAPER_SUBMITTED("论文提交完成", "您的论文已成功提交，系统正在处理中"),
    PAPER_CHECK_COMPLETED("论文查重完成", "您的论文查重已完成，请查看结果"),
    PAPER_NEEDS_REVISION("论文需修改", "您的论文需要修改，请查看审核意见"),
    PAPER_APPROVED("论文审核通过", "恭喜！您的论文已通过审核"),
    ADVISOR_ASSIGNED("指导老师分配", "您的论文已分配指导老师"),
    SYSTEM_ANNOUNCEMENT("系统公告", "系统发布重要公告"),
    ACCOUNT_ACTIVATION("账户激活", "请激活您的账户"),
    PASSWORD_RESET("密码重置", "请重置您的密码"),
    PAPER_PENDING_TIMEOUT("论文待分配超时", "您的论文已提交较长时间但尚未分配审核老师，请耐心等待或联系管理员"),
    PAPER_SUBMITTED_TIMEOUT("论文查重流程延迟", "您的论文已提交较长时间但尚未进入查重流程，请关注处理进度"),
    PAPER_AUDITING_TIMEOUT("论文审核催办", "有待审核论文已等待较长时间，请尽快完成审核"),
    PAPER_REVISED_TIMEOUT("论文修改提醒", "您的论文已被驳回较长时间但尚未重新提交，请尽快修改并重新提交");

    private final String title;
    private final String defaultContent;

    NoticeType(String title, String defaultContent) {
        this.title = title;
        this.defaultContent = defaultContent;
    }

    public String getTitle() {
        return title;
    }

    public String getDefaultContent() {
        return defaultContent;

    }
}
