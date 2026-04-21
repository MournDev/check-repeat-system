package com.abin.checkrepeatsystem.student.controller;


import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.student.dto.CheckTaskResultDTO;
import com.abin.checkrepeatsystem.student.service.CheckTaskService;
import com.abin.checkrepeatsystem.user.vo.CheckResultVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生端查重任务控制器：仅学生角色可访问
 */
@RestController
@RequestMapping("/api/student/check-tasks")
public class StudentCheckTaskController {

    @Resource
    private CheckTaskService checkTaskService;

    /**
     * 1. 学生发起查重任务
     */
    @OperationLog(type = "check_task_create", description = "学生发起查重任务", recordResult = true)
    @PostMapping("/create")
    public Result<CheckResultVO> createCheckTask(@Valid @RequestParam Long paperId) {
        return checkTaskService.createCheckTask(paperId);
    }

    /**
     * 2. 学生查询自己的查重任务列表
     * @param paperId 论文ID（可选）
     * @param checkStatus 任务状态（可选：0-待执行，1-执行中，2-执行成功，3-执行失败）
     */
    @OperationLog(type = "check_task_list", description = "查询查重任务列表")
    @GetMapping("/list")
    public Result<List<CheckTaskResultDTO>> getMyCheckTaskList(
            @RequestParam(required = false) Long paperId,
            @RequestParam(required = false) Integer checkStatus) {
        return checkTaskService.getMyCheckTaskList(paperId, checkStatus);
    }

    /**
     * 3. 学生查询查重任务详情
     * @param paperId 论文ID
     */
    @OperationLog(type = "check_task_detail", description = "查询查重任务详情")
    @GetMapping("/taskDetail")
    public Result<CheckTaskResultDTO> getCheckTaskDetail(@RequestParam Long paperId) {
        return checkTaskService.getCheckTaskDetail(paperId);
    }

    /**
     * 4. 学生取消待执行的查重任务
     * @param taskId 任务ID
     */
    @OperationLog(type = "check_task_cancel", description = "取消查重任务", recordResult = true)
    @DeleteMapping("/cancel")
    public Result<String> cancelCheckTask(@RequestParam Long taskId) {
        return checkTaskService.cancelCheckTask(taskId);
    }

    /**
     * 5. 学生查询指定任务详情
     * @param taskId 任务ID
     */
    @OperationLog(type = "check_task_by_id", description = "查询指定任务详情")
    @GetMapping("/{taskId}")
    public Result<CheckTaskResultDTO> getCheckTaskById(@PathVariable Long taskId) {
        return checkTaskService.getCheckTaskById(taskId);
    }

    /**
     * 6. 学生删除查重任务
     * @param taskId 任务ID
     */
    @OperationLog(type = "check_task_delete", description = "删除查重任务", recordResult = true)
    @DeleteMapping("/{taskId}/delete")
    public Result<String> deleteCheckTask(@PathVariable Long taskId) {
        return checkTaskService.deleteCheckTask(taskId);
    }

    /**
     * 7. 学生重新发起查重
     * @param paperId 论文ID
     */
    @OperationLog(type = "check_task_recheck", description = "重新发起查重", recordResult = true)
    @PostMapping("/recheck")
    public Result<CheckResultVO> recheckPlagiarism(@RequestParam Long paperId) {
        return checkTaskService.createCheckTask(paperId);
    }

    /**
     * 8. 批量创建查重任务
     * @param paperIds 论文ID列表
     */
    @OperationLog(type = "check_task_batch_create", description = "批量创建查重任务", recordResult = true)
    @PostMapping("/batch/create")
    public Result<String> createBatchCheckTasks(@RequestParam List<Long> paperIds) {
        return checkTaskService.createBatchCheckTasks(paperIds);
    }

    /**
     * 9. 获取查重任务状态
     * @param taskId 任务ID
     */
    @OperationLog(type = "check_task_status", description = "获取查重任务状态")
    @GetMapping("/status")
    public Result<CheckTaskResultDTO> getCheckStatus(@RequestParam Long taskId) {
        return checkTaskService.getCheckTaskById(taskId);
    }
}
