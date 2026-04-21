package com.abin.checkrepeatsystem.teacher.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.List;

/**
 * 审核工作流VO
 */
@Data
public class ReviewWorkflowVO {
    /**
     * 工作流ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 工作流描述
     */
    private String description;

    /**
     * 工作流步骤
     */
    private List<WorkflowStepVO> steps;

    /**
     * 启用状态
     */
    private Boolean enabled;
}
