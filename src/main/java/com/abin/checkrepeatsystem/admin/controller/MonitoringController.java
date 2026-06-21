package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.monitor.service.ApplicationMonitorService;
import com.abin.checkrepeatsystem.monitor.service.DatabaseMonitorService;
import com.abin.checkrepeatsystem.monitor.service.MetricSampleService;
import com.abin.checkrepeatsystem.monitor.service.SystemMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 系统监控控制器
 * 提供CPU、内存、磁盘、数据库等真实性能监控数据
 */
@RestController
@RequestMapping("/api/v1/admin/monitoring")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "系统监控", description = "系统性能监控、健康检查、安全审计等综合监控接口")
public class MonitoringController {

    private final SystemMonitorService systemMonitorService;
    private final DatabaseMonitorService databaseMonitorService;
    private final ApplicationMonitorService applicationMonitorService;
    private final MetricSampleService metricSampleService;

    @GetMapping("/performance")
    @Operation(summary = "系统性能监控", description = "获取CPU、内存、磁盘、数据库等实时性能数据")
    public Result<Map<String, Object>> getPerformanceData(
            @RequestParam(defaultValue = "all") String metric,
            @RequestParam(defaultValue = "1h") String period) {
        log.info("接收获取系统性能监控数据请求: metric={}, period={}", metric, period);
        Map<String, Object> data = new HashMap<>();
        switch (metric.toLowerCase()) {
            case "cpu" -> data.putAll(getCpuPerformanceData(period));
            case "memory" -> data.putAll(getMemoryPerformanceData(period));
            case "disk" -> data.putAll(getDiskPerformanceData(period));
            case "database" -> data.putAll(getDatabasePerformanceData(period));
            default -> data.putAll(getAllPerformanceData(period));
        }
        log.info("系统性能监控数据获取成功");
        return Result.success("系统性能监控数据获取成功", data);
    }

    @GetMapping("/resources")
    @Operation(summary = "当前资源使用率", description = "获取当前实时的系统资源使用率")
    public Result<Map<String, Object>> getResourceUsage() {
        log.info("接收获取当前资源使用率请求");
        Map<String, Object> resources = new HashMap<>();
        var systemStatus = systemMonitorService.getSystemStatus();
        if (systemStatus.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> statusData = (Map<String, Object>) systemStatus.getData();
            extractResourceFromStatus(resources, statusData);
        }
        var dbStatus = databaseMonitorService.getDatabaseStatus();
        if (dbStatus.isSuccess()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dbData = (Map<String, Object>) dbStatus.getData();
            @SuppressWarnings("unchecked")
            Map<String, Object> poolInfo = (Map<String, Object>) dbData.get("connectionPool");
            if (poolInfo != null) {
                Double usageRate = (Double) poolInfo.get("usageRate");
                if (usageRate != null) {
                    resources.put("dbPoolUsage", Math.round(usageRate * 100.0) / 100.0);
                }
            }
        }
        log.info("当前资源使用率获取成功");
        return Result.success("当前资源使用率获取成功", resources);
    }

    @GetMapping("/response-times")
    @Operation(summary = "API响应时间统计", description = "获取API平均响应时间、最小/最大响应时间、95百分位响应时间等详细指标")
    public Result<Map<String, Object>> getApiResponseTimes(
            @RequestParam(defaultValue = "1h") String period) {
        log.info("接收获取API响应时间统计请求: period={}", period);
        int minutes = switch (period.toLowerCase()) {
            case "24h" -> 1440;
            case "7d" -> 10080;
            default -> 60;
        };
        var result = applicationMonitorService.getResponseTimeTrend(minutes);
        if (result.isSuccess() && result.getData() != null) {
            return Result.success("API响应时间统计获取成功", result.getData());
        }
        return Result.success("API响应时间统计获取成功", Collections.emptyMap());
    }

    @GetMapping("/overview")
    @Operation(summary = "系统监控概览", description = "获取CPU、内存、磁盘等系统资源使用情况")
    public Result<Map<String, Object>> getSystemOverview() {
        return systemMonitorService.getSystemStatus();
    }

    @GetMapping("/database")
    @Operation(summary = "数据库监控", description = "获取数据库连接池和性能监控信息")
    public Result<Map<String, Object>> getDatabaseMonitor() {
        return databaseMonitorService.getDatabaseStatus();
    }

    @GetMapping("/application")
    @Operation(summary = "应用性能监控", description = "获取应用层面的性能指标和统计信息")
    public Result<Map<String, Object>> getApplicationMetrics() {
        return applicationMonitorService.getApplicationMetrics();
    }

    @GetMapping("/cache")
    @Operation(summary = "缓存性能监控", description = "获取Redis和本地缓存的性能指标")
    public Result<Map<String, Object>> getCacheMetrics() {
        return applicationMonitorService.getCacheMetrics();
    }

    @GetMapping("/metrics")
    @Operation(summary = "业务指标监控", description = "获取自定义业务指标，包括事件计数、队列深度、方法耗时等")
    public Result<Map<String, Object>> getMetrics() {
        return applicationMonitorService.getBusinessMetrics();
    }

    @GetMapping("/response-time-trend")
    @Operation(summary = "响应时间趋势", description = "获取最近N分钟的响应时间趋势（平均值、P95、请求量），用于前端图表展示")
    public Result<Map<String, Object>> getResponseTimeTrend(
            @RequestParam(defaultValue = "15") int minutes) {
        return applicationMonitorService.getResponseTimeTrend(Math.min(minutes, 30));
    }

    // ===== 私有辅助方法 =====

    @SuppressWarnings("unchecked")
    private void extractResourceFromStatus(Map<String, Object> resources, Map<String, Object> statusData) {
        Map<String, Object> cpuInfo = (Map<String, Object>) statusData.get("cpu");
        if (cpuInfo != null) {
            Double cpuUsage = (Double) cpuInfo.get("processCpuUsage");
            if (cpuUsage != null && cpuUsage >= 0) {
                resources.put("cpuUsage", Math.round(cpuUsage * 100.0) / 100.0);
            }
        }
        Map<String, Object> memoryInfo = (Map<String, Object>) statusData.get("memory");
        if (memoryInfo != null) {
            Double heapUsage = (Double) memoryInfo.get("heapUsagePercent");
            if (heapUsage != null) {
                resources.put("memoryUsage", Math.round(heapUsage * 100.0) / 100.0);
            }
        }
        Map<String, Object> diskInfo = (Map<String, Object>) statusData.get("disk");
        if (diskInfo != null) {
            Double diskUsage = (Double) diskInfo.get("usagePercent");
            if (diskUsage != null) {
                resources.put("diskUsage", Math.round(diskUsage * 100.0) / 100.0);
            }
        }
        Map<String, Object> threadInfo = (Map<String, Object>) statusData.get("threads");
        if (threadInfo != null) {
            Integer threadCount = (Integer) threadInfo.get("threadCount");
            if (threadCount != null) {
                resources.put("connections", threadCount);
            }
        }
    }

    private Map<String, Object> getCpuPerformanceData(String period) {
        Map<String, Object> trend = metricSampleService.getTrendData(period);
        Map<String, Object> data = new HashMap<>();
        data.put("cpuUsage", trend.get("cpuUsage"));
        data.put("timestamps", trend.get("timestamps"));
        return data;
    }

    private Map<String, Object> getMemoryPerformanceData(String period) {
        Map<String, Object> trend = metricSampleService.getTrendData(period);
        Map<String, Object> data = new HashMap<>();
        data.put("memoryUsage", trend.get("memoryUsage"));
        data.put("timestamps", trend.get("timestamps"));
        return data;
    }

    private Map<String, Object> getDiskPerformanceData(String period) {
        Map<String, Object> trend = metricSampleService.getTrendData(period);
        Map<String, Object> data = new HashMap<>();
        data.put("diskUsage", trend.get("diskUsage"));
        data.put("timestamps", trend.get("timestamps"));
        return data;
    }

    private Map<String, Object> getDatabasePerformanceData(String period) {
        Map<String, Object> trend = metricSampleService.getTrendData(period);
        Map<String, Object> data = new HashMap<>();
        data.put("dbConnections", trend.get("dbConnections"));
        data.put("timestamps", trend.get("timestamps"));
        return data;
    }

    private Map<String, Object> getAllPerformanceData(String period) {
        return metricSampleService.getTrendData(period);
    }
}
