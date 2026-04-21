package com.abin.checkrepeatsystem.teacher.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核结果DTO：用于返回审核详情
 */
@Data
public class ReviewResultDTO {
    /**
     * 审核记录ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long reviewId;

    /**
     * 论文基础信息
     */
    private PaperBaseInfoDTO paperBaseInfo;

    /**
     * 查重任务信息
     */
    private CheckTaskBaseDTO taskBaseInfo;

    /**
     * 审核操作信息
     */
    private ReviewOperateInfoDTO reviewOperateInfo;

    /**
     * 审核附件信息（可选）
     */
    private ReviewAttachDTO reviewAttach;

    // ------------------------------ 顶层字段（方便前端直接使用） ------------------------------
    /**
     * 论文ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long paperId;

    /**
     * 论文标题
     */
    private String paperTitle;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 学生学号
     */
    private String studentNo;

    /**
     * 学生ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long studentId;

    /**
     * 学生邮箱
     */
    private String email;

    /**
     * 学院
     */
    private String college;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submitTime;

    /**
     * 论文状态
     */
    private String paperStatus;

    /**
     * 相似度
     */
    private Double similarity;

    /**
     * 等待时间（天）
     */
    private Integer waitingTime;

    /**
     * 优先级（normal, high, urgent）
     */
    private String priority;

    /**
     * 截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;

    /**
     * 审核时长（分钟）
     */
    private Integer reviewDuration;

    // ------------------------------ 内部DTO ------------------------------
    /**
     * 论文基础信息
     */
    @Data
    public static class PaperBaseInfoDTO {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Long paperId;
        private String paperTitle;
        private String studentName;
        private String college;
        private String email;
        private String studentNo; // 学生学号
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime submitTime;
        private String paperStatus; // 审核后的论文状态
    }

    /**
     * 查重任务基础信息
     */
    @Data
    public static class CheckTaskBaseDTO {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Long taskId;
        private String taskNo;
        private Double checkRate; // 重复率
        private LocalDateTime checkEndTime;
        private String reportNo; // 报告编号
    }

    /**
     * 审核操作信息
     */
    @Data
    public static class ReviewOperateInfoDTO {
        private String reviewStatus; // completed-审核通过, rejected-审核不通过
        private String reviewStatusDesc; // 状态描述（通过/不通过）
        private String reviewOpinion; // 审核意见（富文本）
        private String reviewerName; // 审核教师姓名
        private LocalDateTime reviewTime;
    }

    /**
     * 审核附件信息
     */
    @Data
    public static class ReviewAttachDTO {
        private String attachName; // 附件原文件名
        private String attachPath; // 附件存储路径
        private Long attachSize; // 附件大小（字节）
        private String attachType; // 附件类型（如pdf）
        private String downloadUrl; // 附件下载URL
    }
}
