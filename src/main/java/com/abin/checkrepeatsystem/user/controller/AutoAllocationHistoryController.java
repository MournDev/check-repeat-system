package com.abin.checkrepeatsystem.user.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.AutoAllocationHistory;
import com.abin.checkrepeatsystem.user.service.AutoAllocationHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/auto-allocation-history")
public class AutoAllocationHistoryController {

    private final AutoAllocationHistoryService autoAllocationHistoryService;

    /**
     * 创建自动分配历史记录
     */
    @PostMapping("/create")
    @OperationLog(type = "auto_allocation_create", description = "创建自动分配历史记录", recordResult = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<AutoAllocationHistory> createHistory(@RequestBody AutoAllocationHistory history) {
        boolean success = autoAllocationHistoryService.createHistory(history);
        if (success) {
            return Result.success("创建分配历史记录成功", history);
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "创建分配历史记录失败");
        }
    }

    /**
     * 获取分配历史列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<AutoAllocationHistory>> getHistoryList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String strategy,
            @RequestParam(required = false) String result) {
        List<AutoAllocationHistory> histories = autoAllocationHistoryService.getHistoryList(page, size, strategy, result);
        return Result.success(histories);
    }

    /**
     * 获取分配历史详情
     */
    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<AutoAllocationHistory> getHistoryById(@PathVariable Long id) {
        AutoAllocationHistory history = autoAllocationHistoryService.getHistoryById(id);
        if (history == null) {
            return Result.error(ResultCode.SYSTEM_ERROR, "历史记录不存在");
        }
        return Result.success(history);
    }

    /**
     * 获取分配统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getHistoryStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> stats = autoAllocationHistoryService.getHistoryStats(startDate, endDate);
        return Result.success(stats);
    }

    /**
     * 获取最新的分配历史记录
     */
    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<AutoAllocationHistory>> getLatestHistory(@RequestParam(defaultValue = "5") int limit) {
        List<AutoAllocationHistory> histories = autoAllocationHistoryService.getLatestHistory(limit);
        return Result.success(histories);
    }

    /**
     * 清理过期的分配历史记录
     */
    @DeleteMapping("/clean")
    @OperationLog(type = "auto_allocation_clean", description = "清理过期分配历史记录", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> cleanExpiredHistory(@RequestParam(defaultValue = "30") int days) {
        boolean success = autoAllocationHistoryService.cleanExpiredHistory(days);
        if (success) {
            return Result.success("清理过期分配历史记录成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "清理过期分配历史记录失败");
        }
    }
}
