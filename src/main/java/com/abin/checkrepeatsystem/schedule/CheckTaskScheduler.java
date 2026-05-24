package com.abin.checkrepeatsystem.schedule;

import com.abin.checkrepeatsystem.common.enums.CheckTaskStatusEnum;
import com.abin.checkrepeatsystem.common.statemachine.CheckTaskStateMachine;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 查重任务定时任务
 * 负责清理超时任务、状态监控等
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class CheckTaskScheduler {
    
    private final CheckTaskStateMachine stateMachine;
    
    /**
     * 每5分钟检查一次超时的查重任务
     * 将超过1小时仍在执行中的任务标记为失败
     */
    @Scheduled(fixedRate = 300000) // 5分钟执行一次
    public void checkTimeoutTasks() {
        try {
            log.info("开始检查超时查重任务...");

            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

            int affected = stateMachine.batchTransitionStatus(
                CheckTaskStatusEnum.FAILURE,
                "任务执行超时（超过1小时）",
                w -> w.eq(CheckTask::getCheckStatus, CheckTaskStatusEnum.CHECKING.getCode())
                      .lt(CheckTask::getStartTime, oneHourAgo)
            );

            if (affected > 0) {
                log.warn("发现{}个超时查重任务，已标记为失败", affected);
            }

        } catch (Exception e) {
            log.error("检查超时任务失败", e);
        }
    }
    
    /**
     * 每天凌晨2点清理已完成很久的任务数据
     * 保留最近30天的数据
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void cleanupOldTasks() {
        try {
            log.info("开始清理旧查重任务数据...");

            // 清理条件：已完成/失败/已取消 且 30天前结束的任务
            // 暂时只记录日志，不实际删除数据
            log.info("计划清理30天前结束的已完成/失败/已取消任务");

        } catch (Exception e) {
            log.error("清理旧任务数据失败", e);
        }
    }
    
    /**
     * 每30秒统计一次任务状态
     */
    @Scheduled(fixedRate = 30000)
    public void taskStatusStatistics() {
        try {
            // 可以在这里收集各种统计指标，如：
            // - 各状态任务数量
            // - 平均执行时间
            // - 成功率等
            log.debug("任务状态统计检查完成");
        } catch (Exception e) {
            log.error("任务状态统计失败", e);
        }
    }
}