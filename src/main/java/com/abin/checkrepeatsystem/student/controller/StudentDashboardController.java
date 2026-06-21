package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.JwtUtils;
import com.abin.checkrepeatsystem.student.dto.*;
import com.abin.checkrepeatsystem.student.service.Impl.StudentDashboardService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/student/dashboard")
@RequiredArgsConstructor
@Slf4j
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;
    private final JwtUtils JwtUtil;

    // 获取仪表盘统计数据
    @GetMapping("/stats")
    public Result<StudentDashboardStatsDTO> getDashboardStats() {
        Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
        StudentDashboardStatsDTO stats = dashboardService.getDashboardStats(studentId);
        return Result.success(stats);
    }

    // 获取最新论文信息
    @GetMapping("/latest-paper")
    public Result<LatestPaperDTO> getLatestPaper() {
        Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
        LatestPaperDTO paper = dashboardService.getLatestPaper(studentId);
        return Result.success(paper);
    }

    // 获取导师信息
    @GetMapping("/advisor")
    public Result<AdvisorInfoDTO> getAdvisorInfo(
            @RequestParam(required = false) Long studentId) {
        Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
        // 如果提供了 studentId，且当前用户是教师或管理员，则使用提供的 studentId
        // 否则使用当前用户的 ID（学生）
        Long targetStudentId = studentId;
        if (targetStudentId == null) {
            targetStudentId = userId;
        }
        AdvisorInfoDTO advisor = dashboardService.getAdvisorInfo(targetStudentId);
        return Result.success(advisor);
    }

    // 获取时间节点信息
    @GetMapping("/deadlines")
    public Result<DeadlinesDTO> getDeadlines(
            @RequestHeader(value = "Authorization", required = false) String token) {
        DeadlinesDTO deadlines = dashboardService.getDeadlines();
        return Result.success(deadlines);
    }

    // 获取能力评估雷达图数据
    @GetMapping("/ability-radar")
    public Result<AbilityRadarDTO> getAbilityRadar() {
        Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
        AbilityRadarDTO radar = dashboardService.getAbilityRadar(studentId);
        return Result.success(radar);
    }

    // 获取相似度变化趋势
    @GetMapping("/similarity-trend")
    public Result<SimilarityTrendDTO> getSimilarityTrend() {
        Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
        SimilarityTrendDTO trend = dashboardService.getSimilarityTrend(studentId);
        return Result.success(trend);
    }

    // 获取专业对比数据
    @GetMapping("/major-comparison")
    public Result<MajorComparisonDTO> getMajorComparison() {
        Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
        MajorComparisonDTO comparison = dashboardService.getMajorComparison(studentId);
        return Result.success(comparison);
    }

    // 获取待办事项列表
    @GetMapping("/todo-list")
    public Result<List<TodoItemDTO>> getTodoList() {
        Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
        List<TodoItemDTO> todoList = dashboardService.getTodoList(studentId);
        return Result.success(todoList);
    }

    // 获取通知消息列表
    @GetMapping("/notifications")
    public Result<List<NotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "5") Integer limit) {
        Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
        List<NotificationDTO> notifications = dashboardService.getNotifications(studentId, limit);
        return Result.success(notifications);
    }

    // 获取论文处理进度
    @GetMapping("/progress-tracking")
    public Result<ProgressTrackingDTO> getProgressTracking() {
        Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
        ProgressTrackingDTO progress = dashboardService.getProgressTracking(studentId);
        return Result.success(progress);
    }

    // 导出仪表盘数据为Excel
    @GetMapping("/export")
    public void exportDashboardData(HttpServletResponse response) {
        try {
            Long studentId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            dashboardService.exportDashboardData(studentId, response);
        } catch (Exception e) {
            log.error("导出仪表盘数据失败", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("导出失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("发送错误响应失败", ioException);
            }
        }
    }
}
