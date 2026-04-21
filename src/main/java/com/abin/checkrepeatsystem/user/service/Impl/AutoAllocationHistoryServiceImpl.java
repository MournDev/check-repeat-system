package com.abin.checkrepeatsystem.user.service.Impl;

import com.abin.checkrepeatsystem.pojo.entity.AutoAllocationHistory;
import com.abin.checkrepeatsystem.user.mapper.AutoAllocationHistoryMapper;
import com.abin.checkrepeatsystem.user.service.AutoAllocationHistoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class AutoAllocationHistoryServiceImpl extends ServiceImpl<AutoAllocationHistoryMapper, AutoAllocationHistory> implements AutoAllocationHistoryService {

    @Resource
    private AutoAllocationHistoryMapper autoAllocationHistoryMapper;

    @Override
    public boolean createHistory(AutoAllocationHistory history) {
        try {
            // 计算执行时长
            if (history.getStartTime() != null && history.getEndTime() != null) {
                long duration = ChronoUnit.SECONDS.between(history.getStartTime(), history.getEndTime());
                history.setExecutionDuration((int) duration);
            }
            
            // 生成批次号
            if (history.getBatchNo() == null) {
                history.setBatchNo("AUTO" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            }
            
            history.setCreateTime(LocalDateTime.now());
            history.setUpdateTime(LocalDateTime.now());
            
            int result = autoAllocationHistoryMapper.insert(history);
            return result > 0;
        } catch (Exception e) {
            log.error("创建自动分配历史记录失败", e);
            return false;
        }
    }

    @Override
    public List<AutoAllocationHistory> getHistoryList(int page, int size, String strategy, String result) {
        try {
            LambdaQueryWrapper<AutoAllocationHistory> queryWrapper = new LambdaQueryWrapper<AutoAllocationHistory>()
                .eq(AutoAllocationHistory::getIsDeleted, 0);
            
            if (strategy != null && !strategy.isEmpty()) {
                queryWrapper.eq(AutoAllocationHistory::getAllocationStrategy, strategy);
            }
            
            if (result != null && !result.isEmpty()) {
                queryWrapper.eq(AutoAllocationHistory::getAllocationResult, result);
            }
            
            queryWrapper.orderByDesc(AutoAllocationHistory::getCreateTime);
            
            int offset = (page - 1) * size;
            queryWrapper.last("LIMIT " + offset + ", " + size);
            
            List<AutoAllocationHistory> histories = autoAllocationHistoryMapper.selectList(queryWrapper);
            
            // 计算执行时长文本
            for (AutoAllocationHistory history : histories) {
                if (history.getExecutionDuration() != null) {
                    int duration = history.getExecutionDuration();
                    if (duration < 60) {
                        history.setDurationText(duration + "秒");
                    } else if (duration < 3600) {
                        int minutes = duration / 60;
                        int seconds = duration % 60;
                        history.setDurationText(minutes + "分" + seconds + "秒");
                    } else {
                        int hours = duration / 3600;
                        int minutes = (duration % 3600) / 60;
                        history.setDurationText(hours + "小时" + minutes + "分");
                    }
                }
            }
            
            return histories;
        } catch (Exception e) {
            log.error("获取分配历史列表失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public AutoAllocationHistory getHistoryById(Long id) {
        try {
            AutoAllocationHistory history = autoAllocationHistoryMapper.selectById(id);
            if (history != null && history.getIsDeleted() == 0) {
                // 计算执行时长文本
                if (history.getExecutionDuration() != null) {
                    int duration = history.getExecutionDuration();
                    if (duration < 60) {
                        history.setDurationText(duration + "秒");
                    } else if (duration < 3600) {
                        int minutes = duration / 60;
                        int seconds = duration % 60;
                        history.setDurationText(minutes + "分" + seconds + "秒");
                    } else {
                        int hours = duration / 3600;
                        int minutes = (duration % 3600) / 60;
                        history.setDurationText(hours + "小时" + minutes + "分");
                    }
                }
            }
            return history;
        } catch (Exception e) {
            log.error("获取分配历史详情失败 - ID: {}", id, e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getHistoryStats(String startDate, String endDate) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            LambdaQueryWrapper<AutoAllocationHistory> queryWrapper = new LambdaQueryWrapper<AutoAllocationHistory>()
                .eq(AutoAllocationHistory::getIsDeleted, 0);
            
            // 统计总分配次数
            long totalCount = autoAllocationHistoryMapper.selectCount(queryWrapper);
            stats.put("totalCount", totalCount);
            
            // 统计成功次数
            long successCount = autoAllocationHistoryMapper.selectCount(
                queryWrapper.clone()
                    .eq(AutoAllocationHistory::getAllocationResult, "SUCCESS")
            );
            stats.put("successCount", successCount);
            
            // 统计失败次数
            long failedCount = autoAllocationHistoryMapper.selectCount(
                queryWrapper.clone()
                    .eq(AutoAllocationHistory::getAllocationResult, "FAILED")
            );
            stats.put("failedCount", failedCount);
            
            // 统计部分成功次数
            long partialCount = autoAllocationHistoryMapper.selectCount(
                queryWrapper.clone()
                    .eq(AutoAllocationHistory::getAllocationResult, "PARTIAL")
            );
            stats.put("partialCount", partialCount);
            
            // 统计策略分布
            Map<String, Long> strategyStats = new HashMap<>();
            List<AutoAllocationHistory> histories = autoAllocationHistoryMapper.selectList(queryWrapper);
            for (AutoAllocationHistory history : histories) {
                String strategy = history.getAllocationStrategy();
                strategyStats.put(strategy, strategyStats.getOrDefault(strategy, 0L) + 1);
            }
            stats.put("strategyStats", strategyStats);
            
            // 统计平均执行时长
            if (!histories.isEmpty()) {
                long totalDuration = 0;
                int count = 0;
                for (AutoAllocationHistory history : histories) {
                    if (history.getExecutionDuration() != null) {
                        totalDuration += history.getExecutionDuration();
                        count++;
                    }
                }
                if (count > 0) {
                    stats.put("averageDuration", totalDuration / count);
                }
            }
            
            return stats;
        } catch (Exception e) {
            log.error("获取分配统计信息失败", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<AutoAllocationHistory> getLatestHistory(int limit) {
        try {
            List<AutoAllocationHistory> histories = autoAllocationHistoryMapper.selectList(
                new LambdaQueryWrapper<AutoAllocationHistory>()
                    .eq(AutoAllocationHistory::getIsDeleted, 0)
                    .orderByDesc(AutoAllocationHistory::getCreateTime)
                    .last("LIMIT " + limit)
            );
            
            // 计算执行时长文本
            for (AutoAllocationHistory history : histories) {
                if (history.getExecutionDuration() != null) {
                    int duration = history.getExecutionDuration();
                    if (duration < 60) {
                        history.setDurationText(duration + "秒");
                    } else if (duration < 3600) {
                        int minutes = duration / 60;
                        int seconds = duration % 60;
                        history.setDurationText(minutes + "分" + seconds + "秒");
                    } else {
                        int hours = duration / 3600;
                        int minutes = (duration % 3600) / 60;
                        history.setDurationText(hours + "小时" + minutes + "分");
                    }
                }
            }
            
            return histories;
        } catch (Exception e) {
            log.error("获取最新分配历史失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean cleanExpiredHistory(int days) {
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(days);
            
            int result = autoAllocationHistoryMapper.delete(
                new LambdaQueryWrapper<AutoAllocationHistory>()
                    .eq(AutoAllocationHistory::getIsDeleted, 0)
                    .lt(AutoAllocationHistory::getCreateTime, expireTime)
            );
            
            log.info("清理过期分配历史记录成功，清理数量: {}", result);
            return result > 0;
        } catch (Exception e) {
            log.error("清理过期分配历史记录失败", e);
            return false;
        }
    }
}
