package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审核草稿实体：对应review_draft表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_draft")
public class ReviewDraft extends BaseEntity {

    /**
     * 论文ID（关联paper_info.id）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long paperId;

    /**
     * 教师ID（关联sys_user.id）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long teacherId;

    /**
     * 审核状态（approve-审核通过，reject-审核不通过，modify-需要修改，defer-暂缓审核）
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
}