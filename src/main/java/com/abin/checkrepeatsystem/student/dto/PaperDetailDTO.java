package com.abin.checkrepeatsystem.student.dto;

import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 论文详情DTO：在PaperInfo基础上丰富导师信息、审核记录等
 */
@Data
public class PaperDetailDTO {

    // ===== PaperInfo 原始字段 =====
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String paperTitle;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long studentId;
    private String author;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long collegeId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long majorId;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long teacherId;
    private String teacherName;
    private String paperType;
    private String paperAbstract;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fileId;
    private String fileMd5;
    private Integer wordCount;
    private Integer pageCount;
    private String paperStatus;
    private BigDecimal similarityRate;
    private String checkResult;
    private String checkEngineType;
    private Integer checkCompleted;
    private String checkSource;
    private LocalDateTime submitTime;
    private LocalDateTime checkTime;
    private String filePath;
    private BigDecimal finalScore;
    private String subjectCode;
    private String subjectName;
    private String collegeName;
    private String majorName;
    private String studentName;
    private String studentUsername;
    private String keywords;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ===== 丰富字段 =====

    /**
     * 导师电话
     */
    private String teacherPhone;

    /**
     * 导师邮箱
     */
    private String teacherEmail;

    /**
     * 导师头像
     */
    private String teacherAvatar;

    /**
     * 最新审核反馈（审核意见）
     */
    private String feedback;

    /**
     * 反馈时间
     */
    private LocalDateTime feedbackTime;

    /**
     * 最新审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 文件格式（从文件名推导）
     */
    private String fileFormat;

    /**
     * 相似度（与similarityRate相同，兼容前端）
     */
    private BigDecimal similarity;

    /**
     * 审核历史列表
     */
    private List<ReviewHistoryItem> reviewHistory;

    /**
     * 审核历史条目
     */
    @Data
    public static class ReviewHistoryItem {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Long id;
        private String type;
        private String comments;
        private LocalDateTime reviewTime;
        private String reviewerName;
        private String reviewerAvatar;
    }

    /**
     * 从PaperInfo复制基础字段
     */
    public static PaperDetailDTO fromPaperInfo(PaperInfo paper) {
        PaperDetailDTO dto = new PaperDetailDTO();
        dto.setId(paper.getId());
        dto.setPaperTitle(paper.getPaperTitle());
        dto.setStudentId(paper.getStudentId());
        dto.setAuthor(paper.getAuthor());
        dto.setCollegeId(paper.getCollegeId());
        dto.setMajorId(paper.getMajorId());
        dto.setTeacherId(paper.getTeacherId());
        dto.setTeacherName(paper.getTeacherName());
        dto.setPaperType(paper.getPaperType());
        dto.setPaperAbstract(paper.getPaperAbstract());
        dto.setFileId(paper.getFileId());
        dto.setFileMd5(paper.getFileMd5());
        dto.setWordCount(paper.getWordCount());
        dto.setPageCount(paper.getPageCount());
        dto.setPaperStatus(paper.getPaperStatus());
        dto.setSimilarityRate(paper.getSimilarityRate());
        dto.setSimilarity(paper.getSimilarityRate());
        dto.setCheckResult(paper.getCheckResult());
        dto.setCheckEngineType(paper.getCheckEngineType());
        dto.setCheckCompleted(paper.getCheckCompleted());
        dto.setCheckSource(paper.getCheckSource());
        dto.setSubmitTime(paper.getSubmitTime());
        dto.setCheckTime(paper.getCheckTime());
        dto.setFilePath(paper.getFilePath());
        dto.setFinalScore(paper.getFinalScore());
        dto.setSubjectCode(paper.getSubjectCode());
        dto.setSubjectName(paper.getSubjectName());
        dto.setCollegeName(paper.getCollegeName());
        dto.setMajorName(paper.getMajorName());
        dto.setStudentName(paper.getStudentName());
        dto.setStudentUsername(paper.getStudentUsername());
        dto.setKeywords(paper.getKeywords());
        dto.setVersion(paper.getVersion());
        dto.setCreateTime(paper.getCreateTime());
        dto.setUpdateTime(paper.getUpdateTime());
        return dto;
    }
}
