package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.teacher.dto.ReviewWorkflowDTO;
import com.abin.checkrepeatsystem.teacher.service.TeacherReviewWorkflowService;
import com.abin.checkrepeatsystem.teacher.vo.ReviewWorkflowVO;
import com.abin.checkrepeatsystem.teacher.vo.StudentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审核工作流配置控制器
 */
@RestController
@RequestMapping("/api/teacher/review-workflow")
public class TeacherReviewWorkflowController {

    @Autowired
    private TeacherReviewWorkflowService reviewWorkflowService;

    /**
     * 获取审核工作流配置
     *
     * @return 审核工作流配置
     */
    @GetMapping
    public Result<ReviewWorkflowVO> getWorkflow() {
        return reviewWorkflowService.getWorkflow();
    }

    /**
     * 更新审核工作流配置
     *
     * @param workflowDTO 审核工作流DTO
     * @return 更新结果
     */
    @PutMapping
    public Result<Void> updateWorkflow(@RequestBody ReviewWorkflowDTO workflowDTO) {
        return reviewWorkflowService.updateWorkflow(workflowDTO);
    }

    /**
     * 获取教师列表
     *
     * @return 教师列表
     */
    @GetMapping("/teachers")
    public Result<List<StudentVO>> getTeachers() {
        return reviewWorkflowService.getTeachers();
    }
}
