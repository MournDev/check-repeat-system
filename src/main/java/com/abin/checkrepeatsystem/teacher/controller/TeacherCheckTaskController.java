package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.student.dto.CheckTaskResultDTO;
import com.abin.checkrepeatsystem.student.service.CheckTaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.RequiredArgsConstructor;


/**
 * 教师端查重任务控制器：仅教师角色可访问
 */
@RestController
@RequestMapping("/api/v1/teacher/check-tasks")
@PreAuthorize("hasAuthority('TEACHER')") // 权限控制：仅教师可访问
@RequiredArgsConstructor
public class TeacherCheckTaskController {

    private final CheckTaskService checkTaskService;

    /**
     * 1. 教师查询自己指导学生的查重任务列表
     * @param paperId 论文ID（可选）
     * @param checkStatus 任务状态（可选）
     */
    @GetMapping("/list")
    public Result<List<CheckTaskResultDTO>> getGuideCheckTaskList(
            @RequestParam(required = false) Long paperId,
            @RequestParam(required = false) Integer checkStatus) {
        return checkTaskService.getMyCheckTaskList(paperId, checkStatus);
    }

    /**
     * 2. 教师查询查重任务详情（仅自己指导的学生）
     * @param taskId 任务ID
     */
    @GetMapping("/{taskId}")
    public Result<CheckTaskResultDTO> getCheckTaskDetail(@PathVariable Long taskId) {
        return checkTaskService.getCheckTaskDetail(taskId);
    }
}
