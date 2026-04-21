package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.service.CheckTaskService;
import com.abin.checkrepeatsystem.student.dto.CheckTaskResultDTO;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查重状态控制器
 * 用于获取查重任务的状态
 */
@Slf4j
@RestController
@RequestMapping("/ws")
public class CheckStatusWebSocketController {

    @Resource
    private CheckTaskMapper checkTaskMapper;

    @Autowired
    @Lazy
    private CheckTaskService checkTaskService;

    /**
     * 获取查重任务状态
     * @param paperId 论文ID
     */
    @RequestMapping("/check-status/{paperId}")
    public Result<CheckTaskResultDTO> getCheckStatus(@PathVariable Long paperId) {
        try {
            Long currentUserId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("获取查重状态 - 论文ID: {}, 用户ID: {}", paperId, currentUserId);

            // 查询最新的查重任务
            CheckTask checkTask = checkTaskMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CheckTask>()
                    .eq(CheckTask::getPaperId, paperId)
                    .eq(CheckTask::getIsDeleted, 0)
                    .orderByDesc(CheckTask::getCreateTime)
                    .last("LIMIT 1")
            );

            if (checkTask != null) {
                // 转换为DTO
                CheckTaskResultDTO resultDTO = ((com.abin.checkrepeatsystem.student.service.Impl.CheckTaskServiceImpl) checkTaskService)
                    .convertToTaskResultDTO(checkTask, true);

                log.info("获取查重状态成功 - 论文ID: {}, 状态: {}", paperId, checkTask.getCheckStatus());
                return Result.success("获取查重状态成功", resultDTO);
            } else {
                return Result.error(404, "未找到查重任务");
            }
        } catch (Exception e) {
            log.error("获取查重状态失败 - 论文ID: {}", paperId, e);
            return Result.error(500, "获取查重状态失败: " + e.getMessage());
        }
    }

    /**
     * 当查重任务状态发生变化时调用此方法
     * @param paperId 论文ID
     */
    public void onTaskStatusChange(Long paperId) {
        // 由于使用的是HTTP接口，不需要推送状态，客户端可以通过轮询获取最新状态
        log.info("查重任务状态发生变化 - 论文ID: {}", paperId);
    }
}
