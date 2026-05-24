package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.student.service.CheckTaskService;
import com.abin.checkrepeatsystem.student.dto.CheckTaskResultDTO;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 查重状态控制器
 * 用于获取查重任务的状态
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/ws")
public class CheckStatusWebSocketController {

    private final CheckTaskService checkTaskService;

    /**
     * 获取查重任务状态
     * @param paperId 论文ID
     */
    @RequestMapping("/check-status/{paperId}")
    public Result<CheckTaskResultDTO> getCheckStatus(@PathVariable Long paperId) {
        Long currentUserId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("获取查重状态 - 论文ID: {}, 用户ID: {}", paperId, currentUserId);

        CheckTask checkTask = checkTaskService.getLatestCheckTaskByPaperId(paperId);

        if (checkTask != null) {
            CheckTaskResultDTO resultDTO = checkTaskService.convertToTaskResultDTO(checkTask, true);
            log.info("获取查重状态成功 - 论文ID: {}, 状态: {}", paperId, checkTask.getCheckStatus());
            return Result.success("获取查重状态成功", resultDTO);
        } else {
            return Result.error(404, "未找到查重任务");
        }
    }

    /**
     * 当查重任务状态发生变化时调用此方法
     * @param paperId 论文ID
     */
    public void onTaskStatusChange(Long paperId) {
        log.info("查重任务状态发生变化 - 论文ID: {}", paperId);
    }
}
