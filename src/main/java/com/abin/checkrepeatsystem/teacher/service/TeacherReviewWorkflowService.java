package com.abin.checkrepeatsystem.teacher.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.teacher.dto.ReviewWorkflowDTO;
import com.abin.checkrepeatsystem.teacher.vo.ReviewWorkflowVO;
import com.abin.checkrepeatsystem.teacher.vo.StudentVO;

import java.util.List;

/**
 * 审核工作流配置服务接口
 */
public interface TeacherReviewWorkflowService {

    /**
     * 获取审核工作流配置
     *
     * @return 审核工作流配置
     */
    Result<ReviewWorkflowVO> getWorkflow();

    /**
     * 更新审核工作流配置
     *
     * @param workflowDTO 审核工作流DTO
     * @return 更新结果
     */
    Result<Void> updateWorkflow(ReviewWorkflowDTO workflowDTO);

    /**
     * 获取教师列表
     *
     * @return 教师列表
     */
    Result<List<StudentVO>> getTeachers();
}
