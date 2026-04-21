package com.abin.checkrepeatsystem.teacher.dto;

import lombok.Data;

import java.util.List;

/**
 * 审核工作流DTO
 */
@Data
public class ReviewWorkflowDTO {
    /**
     * 工作流ID
     */
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
    private List<WorkflowStepDTO> steps;

    /**
     * 启用状态
     */
    private Boolean enabled;
}
