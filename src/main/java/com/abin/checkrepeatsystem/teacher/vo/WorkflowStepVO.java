package com.abin.checkrepeatsystem.teacher.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

/**
 * 工作流步骤VO
 */
@Data
public class WorkflowStepVO {
    /**
     * 步骤ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 步骤名称
     */
    private String name;

    /**
     * 审批人ID列表
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private List<Long> approvers;

    /**
     * 审批方式：all-全部审批，any-任意审批
     */
    private String approvalType;

    /**
     * 超时时间（天）
     */
    private Integer timeout;
}
