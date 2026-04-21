package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.JwtUtils;
import com.abin.checkrepeatsystem.student.dto.*;
import com.abin.checkrepeatsystem.student.service.Impl.StudentDashboardService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/dashboard")
@RequiredArgsConstructor
@Slf4j
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;
    @Resource
    private JwtUtils JwtUtil;

    // 获取仪表盘统计数据
    @GetMapping("/stats")
    public Result<StudentDashboardStatsDTO> getDashboardStats() {
        try {
            Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            StudentDashboardStatsDTO stats = dashboardService.getDashboardStats(studentId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取仪表盘统计数据失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取统计数据失败");
        }
    }

    // 获取最新论文信息
    @GetMapping("/latest-paper")
    public Result<LatestPaperDTO> getLatestPaper() {
        try {
            Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            LatestPaperDTO paper = dashboardService.getLatestPaper(studentId);
            return Result.success(paper);
        } catch (Exception e) {
            log.error("获取最新论文信息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取论文信息失败");
        }
    }

    // 获取导师信息
    @GetMapping("/advisor")
    public Result<AdvisorInfoDTO> getAdvisorInfo(
            @RequestParam(required = false) Long studentId) {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            // 如果提供了 studentId，且当前用户是教师或管理员，则使用提供的 studentId
            // 否则使用当前用户的 ID（学生）
            Long targetStudentId = studentId;
            if (targetStudentId == null) {
                targetStudentId = userId;
            }
            AdvisorInfoDTO advisor = dashboardService.getAdvisorInfo(targetStudentId);
            return Result.success(advisor);
        } catch (Exception e) {
            log.error("获取导师信息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取导师信息失败");
        }
    }

    // 获取时间节点信息
    @GetMapping("/deadlines")
    public Result<DeadlinesDTO> getDeadlines(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            DeadlinesDTO deadlines = dashboardService.getDeadlines();
            return Result.success(deadlines);
        } catch (Exception e) {
            log.error("获取时间节点信息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取时间节点信息失败");
        }
    }

    // 获取能力评估雷达图数据
    @GetMapping("/ability-radar")
    public Result<AbilityRadarDTO> getAbilityRadar() {
        try {
            Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            AbilityRadarDTO radar = dashboardService.getAbilityRadar(studentId);
            return Result.success(radar);
        } catch (Exception e) {
            log.error("获取能力评估数据失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取能力评估数据失败");
        }
    }

    // 获取相似度变化趋势
    @GetMapping("/similarity-trend")
    public Result<SimilarityTrendDTO> getSimilarityTrend() {
        try {
            Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            SimilarityTrendDTO trend = dashboardService.getSimilarityTrend(studentId);
            return Result.success(trend);
        } catch (Exception e) {
            log.error("获取相似度趋势数据失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取相似度趋势数据失败");
        }
    }

    // 获取专业对比数据
    @GetMapping("/major-comparison")
    public Result<MajorComparisonDTO> getMajorComparison() {
        try {
            Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            MajorComparisonDTO comparison = dashboardService.getMajorComparison(studentId);
            return Result.success(comparison);
        } catch (Exception e) {
            log.error("获取专业对比数据失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取专业对比数据失败");
        }
    }

    // 获取待办事项列表
    @GetMapping("/todo-list")
    public Result<List<TodoItemDTO>> getTodoList() {
        try {
            Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            List<TodoItemDTO> todoList = dashboardService.getTodoList(studentId);
            return Result.success(todoList);
        } catch (Exception e) {
            log.error("获取待办事项列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取待办事项列表失败");
        }
    }

    // 获取通知消息列表
    @GetMapping("/notifications")
    public Result<List<NotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "5") Integer limit) {
        try {
            Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            List<NotificationDTO> notifications = dashboardService.getNotifications(studentId, limit);
            return Result.success(notifications);
        } catch (Exception e) {
            log.error("获取通知消息列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取通知消息列表失败");
        }
    }

    // 获取论文处理进度
    @GetMapping("/progress-tracking")
    public Result<ProgressTrackingDTO> getProgressTracking() {
        try {
            Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            ProgressTrackingDTO progress = dashboardService.getProgressTracking(studentId);
            return Result.success(progress);
        } catch (Exception e) {
            log.error("获取进度跟踪信息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取进度跟踪信息失败");
        }
    }
}
