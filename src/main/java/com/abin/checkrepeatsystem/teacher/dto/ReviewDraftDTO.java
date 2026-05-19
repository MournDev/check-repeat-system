package com.abin.checkrepeatsystem.teacher.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审核草稿DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDraftDTO {

    /**
     * 草稿ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 论文ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long paperId;

    /**
     * 教师ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long teacherId;

    /**
     * 审核状态 (approve/reject/modify/defer)
     */
    private String reviewStatus;

    /**
     * 审核意见
     */
    private String reviewOpinion;

    /**
     * 附件路径
     */
    private String reviewAttach;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}