package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.mapper.CheckResultMapper;
import com.abin.checkrepeatsystem.admin.mapper.SysOperationLogMapper;
import com.abin.checkrepeatsystem.admin.service.AdminDashboardService;
import com.abin.checkrepeatsystem.mapper.SysBackupLogMapper;
import com.abin.checkrepeatsystem.pojo.entity.SysBackupLog;
import com.abin.checkrepeatsystem.admin.vo.CollegePaperStatsVO;
import com.abin.checkrepeatsystem.admin.vo.MajorPaperStatsVO;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.enums.UserTypeEnum;
import com.abin.checkrepeatsystem.monitor.service.DatabaseMonitorService;
import com.abin.checkrepeatsystem.monitor.service.SystemMonitorService;
import com.abin.checkrepeatsystem.pojo.entity.CheckResult;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysLoginLog;
import com.abin.checkrepeatsystem.pojo.entity.SysOperationLog;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.user.mapper.SysLoginLogMapper;
import com.abin.checkrepeatsystem.user.service.SysUserService;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 管理员仪表板服务实现类
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final SysUserService sysUserService;
    
    private final PaperInfoMapper paperInfoMapper;
    
    private final CheckResultMapper checkResultMapper;
    
    private final SysLoginLogMapper sysLoginLogMapper;

    private final SysOperationLogMapper sysOperationLogMapper;

    private final SystemMonitorService systemMonitorService;

    private final DatabaseMonitorService databaseMonitorService;

    private final MeterRegistry meterRegistry;

    private final SysBackupLogMapper sysBackupLogMapper;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Result<Map<String, Object>> getSystemStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 用户统计
            Long totalUsers = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getIsDeleted, 0));
            stats.put("totalUsers", totalUsers);
            
            // 各类型用户统计
            Long adminCount = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUserType, UserTypeEnum.ROLE_ADMIN)
                    .eq(SysUser::getIsDeleted, 0));
            Long studentCount = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUserType, UserTypeEnum.ROLE_STUDENT)
                    .eq(SysUser::getIsDeleted, 0));
            Long teacherCount = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUserType, UserTypeEnum.ROLE_TEACHER)
                    .eq(SysUser::getIsDeleted, 0));
            
            stats.put("admins", adminCount);
            stats.put("students", studentCount);
            stats.put("teachers", teacherCount);
            
            // 论文统计
            Long totalPapers = paperInfoMapper.selectCount(null);

            stats.put("totalPapers", totalPapers);
            
            // 【新增】有效论文数（排除已撤回）
            Long validPapers = paperInfoMapper.selectCount(
                new LambdaQueryWrapper<PaperInfo>()
                    .ne(PaperInfo::getPaperStatus, DictConstants.PaperStatus.WITHDRAWN)
                    .eq(PaperInfo::getIsDeleted, 0)
            );
            stats.put("validPapers", validPapers);
            
            // 【新增】撤回论文统计
            Long withdrawnPapers = paperInfoMapper.selectCount(
                new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.WITHDRAWN)
                    .eq(PaperInfo::getIsDeleted, 0)
            );
            stats.put("withdrawnPapers", withdrawnPapers);
            
            // 【新增】撤回率计算
            if (totalPapers > 0) {
                Double withdrawalRate = withdrawnPapers.doubleValue() / totalPapers * 100;
                stats.put("withdrawalRate", String.format("%.2f%%", withdrawalRate));
            }
            
            // 不同状态论文统计
            Long pendingPapers = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.PENDING));
            Long checkingPapers = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.CHECKING));
            Long auditingPapers = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.AUDITING));
            Long completedPapers = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.COMPLETED));
            Long rejectedPapers = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.REJECTED));
            
            stats.put("pendingPapers", pendingPapers);
            stats.put("checkingPapers", checkingPapers);
            stats.put("auditingPapers", auditingPapers);
            stats.put("completedPapers", completedPapers);
            stats.put("rejectedPapers", rejectedPapers);
            
            // 高相似度论文统计
            Long highSimilarityPapers = checkResultMapper.selectCount(new LambdaQueryWrapper<CheckResult>()
                    .ge(CheckResult::getRepeatRate, new BigDecimal("80")));
            stats.put("highSimilarityPapers", highSimilarityPapers);
            
            // 时间维度统计
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
            LocalDateTime monthStart = LocalDateTime.now().minusDays(30);

            Long todaySubmissions = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                    .ge(PaperInfo::getCreateTime, todayStart));
            Long weekSubmissions = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                    .ge(PaperInfo::getCreateTime, weekStart));
            Long monthSubmissions = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                    .ge(PaperInfo::getCreateTime, monthStart));

            stats.put("todaySubmissions", todaySubmissions);
            stats.put("weekSubmissions", weekSubmissions);
            stats.put("monthSubmissions", monthSubmissions);

            // 今日访问量（今日登录次数）
            Long todayVisits = sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLog>()
                    .ge(SysLoginLog::getLoginTime, todayStart));
            stats.put("todayVisits", todayVisits);
            
            // 本周审核统计
            Long weekReviews = checkResultMapper.selectCount(new LambdaQueryWrapper<CheckResult>()
                    .ge(CheckResult::getCreateTime, weekStart));
            stats.put("weekReviews", weekReviews);
            
            // 平均相似度
            Page<CheckResult> resultPage = new Page<>(0, 100);
            List<CheckResult> recentResults = checkResultMapper.selectPage(resultPage,
                    new LambdaQueryWrapper<CheckResult>()
                    .orderByDesc(CheckResult::getCreateTime)).getRecords();
            if (!recentResults.isEmpty()) {
                BigDecimal avgSimilarity = recentResults.stream()
                        .map(CheckResult::getRepeatRate)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(new BigDecimal(recentResults.size()), 2, RoundingMode.HALF_UP);
                stats.put("avgSimilarity", avgSimilarity);
            } else {
                stats.put("avgSimilarity", BigDecimal.ZERO);
            }
            
            // 学院分布统计 - 使用Mapper XML实现
            Map<String, Object> collegeStats = getCollegeDistributionStats();
            stats.put("collegeStats", collegeStats);
            
            // 专业分布统计 - 使用Mapper XML实现
            Map<String, Object> majorStats = getMajorDistributionStats();
            stats.put("majorStats", majorStats);
            
            // 7. 系统监控信息
            Map<String, Object> systemMonitor = getSystemMonitorInfo();
            stats.put("systemMonitor", systemMonitor);
            
            // 8. 日志审计统计
            Map<String, Object> auditStats = getAuditStatistics();
            stats.put("auditStats", auditStats);
            
            // 9. 性能指标
            Map<String, Object> performanceMetrics = getPerformanceMetrics();
            stats.put("performanceMetrics", performanceMetrics);

            // 10. 最近备份时间
            SysBackupLog lastBackup = sysBackupLogMapper.selectOne(
                new LambdaQueryWrapper<SysBackupLog>()
                    .eq(SysBackupLog::getStatus, "SUCCESS")
                    .orderByDesc(SysBackupLog::getCreateTime)
                    .last("LIMIT 1")
            );
            stats.put("lastBackup", lastBackup != null ? lastBackup.getEndTime() : null);

            log.info("获取系统统计数据成功: totalUsers={}, totalPapers={}", totalUsers, totalPapers);
            return Result.success("系统统计数据获取成功", stats);
        } catch (Exception e) {
            log.error("获取系统统计数据失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR,"获取系统统计数据失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getQuickActions() {
        List<Map<String, Object>> actions = Arrays.asList(
            createAction(1L, "用户管理", "user", "/admin/users", "管理系统用户"),
            createAction(2L, "论文审核", "file-text", "/admin/papers/review", "审核待处理论文"),
            createAction(3L, "系统配置", "setting", "/admin/config", "配置系统参数"),
            createAction(4L, "数据统计", "bar-chart", "/admin/statistics", "查看系统统计数据"),
            createAction(5L, "日志中心", "file-search", "/admin/logs", "查看系统操作日志"),
            createAction(6L, "权限管理", "lock", "/admin/permissions", "管理角色和权限")
        );

        log.debug("获取快捷操作菜单成功: count={}", actions.size());
        return Result.success("快捷操作菜单获取成功", actions);
    }

    @Override
    public Result<Map<String, Object>> getRealtimeStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 今日论文提交数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long todayPapers = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                .ge(PaperInfo::getCreateTime, todayStart));
        stats.put("todayPapers", todayPapers);
        
        // 本周审核数
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        Long weekReviews = checkResultMapper.selectCount(
            new LambdaQueryWrapper<CheckResult>()
                .ge(CheckResult::getCreateTime, weekStart)
        );
        stats.put("weekReviews", weekReviews);
        
        // 在线用户数（最近1小时内登录）
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long onlineUsers = sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLog>()
                .ge(SysLoginLog::getLoginTime, oneHourAgo));
        stats.put("onlineUsers", onlineUsers);
        
        // 系统CPU使用率（从数据库查询）
        Integer systemLoad = getSystemLoadFromDatabase();
        stats.put("systemLoad", systemLoad);
        
        // 当前活跃任务数
        Long activeTasks = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                .in(PaperInfo::getPaperStatus, Arrays.asList(
                    DictConstants.PaperStatus.CHECKING,
                    DictConstants.PaperStatus.AUDITING)));
        stats.put("activeTasks", activeTasks);
        
        // 今日新增用户数
        Long todayNewUsers = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .ge(SysUser::getCreateTime, todayStart)
                .eq(SysUser::getIsDeleted, 0));
        stats.put("todayNewUsers", todayNewUsers);
        
        log.debug("获取实时统计数据成功");
        return Result.success("实时统计数据获取成功", stats);
    }

    /**
     * 获取学院分布统计 - 使用VO形式
     */
    private Map<String, Object> getCollegeDistributionStats() {
        Map<String, Object> collegeStats = new HashMap<>();
        
        try {
            // 使用VO形式的查询方法
            List<CollegePaperStatsVO> collegePaperStats = paperInfoMapper.selectCollegePaperStats();
            
            // 转换为Map格式（如果前端需要）
            List<Map<String, Object>> result = collegePaperStats.stream()
                .map(vo -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("collegeId", vo.getCollegeId());
                    map.put("collegeName", vo.getCollegeName());
                    map.put("paperCount", vo.getPaperCount());
                    return map;
                })
                .collect(Collectors.toList());
            
            collegeStats.put("distribution", result);
            collegeStats.put("totalCount", result.size());
            
        } catch (Exception e) {
            log.error("获取学院分布统计失败", e);
            collegeStats.put("distribution", new ArrayList<>());
            collegeStats.put("totalCount", 0);
        }
        
        return collegeStats;
    }
    
    /**
     * 获取专业分布统计 - 修复类型问题
     */
    private Map<String, Object> getMajorDistributionStats() {
        Map<String, Object> majorStats = new HashMap<>();
        
        try {
            // 明确指定泛型类型
            List<MajorPaperStatsVO> majorPaperStats = paperInfoMapper.selectMajorPaperStats();

            List<Map<String, Object>> result = majorPaperStats.stream()
                    .map(vo -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("majorId", vo.getMajorId());
                        map.put("majorName", vo.getMajorName());
                        map.put("paperCount", vo.getPaperCount());
                        return map;
                    })
                    .collect(Collectors.toList());

            majorStats.put("distribution", result);
            majorStats.put("totalCount", result.size());

        } catch (Exception e) {
            log.error("获取专业分布统计失败", e);
            majorStats.put("distribution", new ArrayList<>());
            majorStats.put("totalCount", 0);
        }

        return majorStats;
    }


    /**
     * 创建快捷操作
     */
    private Map<String, Object> createAction(Long id, String name, String icon, 
                                           String path, String description) {
        Map<String, Object> action = new HashMap<>();
        action.put("id", id);
        action.put("name", name);
        action.put("icon", icon);
        action.put("path", path);
        action.put("description", description);
        return action;
    }
    
    /**
     * 从系统监控服务获取真实系统负载
     */
    private Integer getSystemLoadFromDatabase() {
        try {
            var systemStatus = systemMonitorService.getSystemStatus();
            if (systemStatus.isSuccess()) {
                Map<String, Object> statusData = (Map<String, Object>) systemStatus.getData();
                Map<String, Object> cpuInfo = (Map<String, Object>) statusData.get("cpu");
                if (cpuInfo != null) {
                    Double cpuUsage = (Double) cpuInfo.get("processCpuUsage");
                    if (cpuUsage != null && cpuUsage >= 0) {
                        return (int) Math.round(cpuUsage);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取系统负载信息失败: {}", e.getMessage());
        }
        return 0;
    }
    
    /**
     * 获取系统监控信息
     */
    private Map<String, Object> getSystemMonitorInfo() {
        Map<String, Object> monitorInfo = new HashMap<>();
        
        try {
            // 使用真实的系统监控服务获取数据
            Map<String, Object> systemStatus = systemMonitorService.getSystemStatus().getData();
            
            if (systemStatus != null && !systemStatus.isEmpty()) {
                // CPU使用率（真实数据）
                Map<String, Object> cpuInfo = (Map<String, Object>) systemStatus.get("cpu");
                if (cpuInfo != null) {
                    Double cpuUsage = (Double) cpuInfo.get("processCpuUsage");
                    if (cpuUsage != null) {
                        monitorInfo.put("cpuUsage", cpuUsage);
                        monitorInfo.put("cpuStatus", cpuUsage < 70 ? "normal" : cpuUsage < 90 ? "warning" : "danger");
                    }
                }
                
                // 内存使用率（真实数据）
                Map<String, Object> memoryInfo = (Map<String, Object>) systemStatus.get("memory");
                if (memoryInfo != null) {
                    Double heapUsage = (Double) memoryInfo.get("heapUsagePercent");
                    if (heapUsage != null) {
                        monitorInfo.put("memoryUsage", heapUsage);
                        monitorInfo.put("memoryStatus", heapUsage < 75 ? "normal" : heapUsage < 90 ? "warning" : "danger");
                    }
                }
                
                // 磁盘使用率（真实数据）
                Map<String, Object> diskInfo = (Map<String, Object>) systemStatus.get("disk");
                if (diskInfo != null) {
                    Double diskUsage = (Double) diskInfo.get("usagePercent");
                    if (diskUsage != null) {
                        monitorInfo.put("diskUsage", diskUsage);
                        monitorInfo.put("diskStatus", diskUsage < 80 ? "normal" : diskUsage < 95 ? "warning" : "danger");
                    }
                }
                
                // 线程信息
                Map<String, Object> threadInfo = (Map<String, Object>) systemStatus.get("threads");
                if (threadInfo != null) {
                    Integer threadCount = (Integer) threadInfo.get("threadCount");
                    if (threadCount != null) {
                        monitorInfo.put("connections", threadCount);
                    }
                }
                
                // 系统运行时间
                Map<String, Object> runtimeInfo = (Map<String, Object>) systemStatus.get("runtime");
                if (runtimeInfo != null) {
                    String uptime = (String) runtimeInfo.get("uptime");
                    if (uptime != null) {
                        monitorInfo.put("uptime", uptime);
                    }
                    String javaVersion = (String) runtimeInfo.get("javaVersion");
                    if (javaVersion != null) {
                        monitorInfo.put("javaVersion", javaVersion);
                    }
                }

                // 数据库类型
                Map<String, Object> dbMonitorData = databaseMonitorService.getDatabaseStatus().getData();
                if (dbMonitorData != null) {
                    Map<String, Object> dbInfo = (Map<String, Object>) dbMonitorData.get("databaseInfo");
                    if (dbInfo != null) {
                        String productName = (String) dbInfo.get("productName");
                        String productVersion = (String) dbInfo.get("productVersion");
                        if (productName != null) {
                            monitorInfo.put("databaseType", productName + " " + (productVersion != null ? productVersion : ""));
                        }
                    }
                }
                
                // 健康度评分
                Integer healthScore = (Integer) systemStatus.get("healthScore");
                if (healthScore != null) {
                    monitorInfo.put("healthScore", healthScore);
                    monitorInfo.put("healthStatus", healthScore >= 80 ? "healthy" : healthScore >= 60 ? "warning" : "critical");
                }
            } else {
                // 如果无法获取真实数据，回退到基本监控
                fallbackToBasicMonitoring(monitorInfo);
            }
            
        } catch (Exception e) {
            log.warn("获取系统监控信息失败，使用备用方案: {}", e.getMessage());
            fallbackToBasicMonitoring(monitorInfo);
        }
        
        return monitorInfo;
    }
    
    /**
     * 获取日志审计统计
     */
    private Map<String, Object> getAuditStatistics() {
        Map<String, Object> auditStats = new HashMap<>();
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0);
            LocalDateTime weekStart = now.minusDays(7);
            LocalDateTime monthStart = now.minusDays(30);
            
            // 今日登录次数
            Long todayLogins = sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLog>()
                    .ge(SysLoginLog::getLoginTime, todayStart));
            auditStats.put("todayLogins", todayLogins);
            
            // 本周操作日志数
            Long weekOperations = sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLog>()
                    .ge(SysLoginLog::getLoginTime, weekStart));
            auditStats.put("weekOperations", weekOperations);
            
            // 本月异常登录尝试
            Long monthFailedLogins = sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLog>()
                    .eq(SysLoginLog::getLoginResult, 0) // 登录失败
                    .ge(SysLoginLog::getLoginTime, monthStart));
            auditStats.put("monthFailedLogins", monthFailedLogins);
            
            // 活跃用户数（最近一周）
            // 使用子查询方式获取活跃用户数
            List<Long> activeUserIds = sysLoginLogMapper.selectList(new LambdaQueryWrapper<SysLoginLog>()
                    .select(SysLoginLog::getUserId)
                    .ge(SysLoginLog::getLoginTime, weekStart)
                    .isNotNull(SysLoginLog::getUserId)
                    .groupBy(SysLoginLog::getUserId))
                    .stream()
                    .map(SysLoginLog::getUserId)
                    .distinct()
                    .collect(Collectors.toList());
            Long activeUsers = (long) activeUserIds.size();
            auditStats.put("activeUsers", activeUsers);
            
            // 系统错误日志数（最近一天）
            Long todayErrors = 0L; // 需要从系统日志表查询
            auditStats.put("todayErrors", todayErrors);
            
            // 安全事件统计
            Map<String, Object> securityEvents = new HashMap<>();
            securityEvents.put("failedLogins", monthFailedLogins);
            securityEvents.put("suspiciousActivities", 0L); // 需要额外的日志分析
            securityEvents.put("securityAlerts", 0L);
            auditStats.put("securityEvents", securityEvents);
            
        } catch (Exception e) {
            log.warn("获取日志审计统计失败: {}", e.getMessage());
            // 返回默认值
            auditStats.put("todayLogins", 0L);
            auditStats.put("weekOperations", 0L);
            auditStats.put("monthFailedLogins", 0L);
            auditStats.put("activeUsers", 0L);
            auditStats.put("todayErrors", 0L);
        }
        
        return auditStats;
    }
    
    /**
     * 获取性能指标
     */
    private Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // 获取真实的数据库性能数据
            Map<String, Object> dbMetrics = getDatabasePerformanceMetrics();
            metrics.put("dbPerformance", dbMetrics);
            
            // 获取真实的API响应时间（从操作日志中统计）
            Map<String, Object> apiMetrics = getApiResponseTimeMetrics();
            metrics.put("apiResponseTimes", apiMetrics);
            
            // 获取真实的缓存命中率（如果有Redis）
            Double cacheHitRate = getCacheHitRate();
            metrics.put("cacheHitRate", cacheHitRate);
            
            // 获取并发用户数（从登录日志统计）
            Integer concurrentUsers = getConcurrentUsers();
            metrics.put("concurrentUsers", concurrentUsers);
            
            // 获取事务处理速率
            Double tps = getTransactionsPerSecond();
            metrics.put("transactionsPerSecond", tps);
            
            // 获取错误率
            Double errorRate = getErrorRate();
            metrics.put("errorRate", errorRate);
            
        } catch (Exception e) {
            log.warn("获取性能指标失败: {}", e.getMessage());
            // 返回默认值
            metrics.put("apiResponseTimes", new HashMap<>());
            metrics.put("dbPerformance", new HashMap<>());
            metrics.put("cacheHitRate", 0.0);
            metrics.put("concurrentUsers", 0);
            metrics.put("transactionsPerSecond", 0.0);
            metrics.put("errorRate", 0.0);
        }
        
        return metrics;
    }
    
    /**
     * 回退到基本监控方案
     */
    private void fallbackToBasicMonitoring(Map<String, Object> monitorInfo) {
        // 基本的监控信息
        monitorInfo.put("cpuUsage", 0.0);
        monitorInfo.put("memoryUsage", 0.0);
        monitorInfo.put("diskUsage", 0.0);
        monitorInfo.put("connections", 0);
        monitorInfo.put("uptime", "系统监控服务不可用");
        monitorInfo.put("healthScore", 0);
        monitorInfo.put("healthStatus", "unknown");
        
        log.warn("已回退到基本监控方案");
    }
    
    /**
     * 获取数据库性能指标
     */
    private Map<String, Object> getDatabasePerformanceMetrics() {
        Map<String, Object> dbMetrics = new HashMap<>();
        
        try {
            // 获取真实的数据库监控数据
            var dbStatus = databaseMonitorService.getDatabaseStatus();
            if (dbStatus.isSuccess()) {
                Map<String, Object> dbData = (Map<String, Object>) dbStatus.getData();
                Map<String, Object> poolInfo = (Map<String, Object>) dbData.get("connectionPool");
                if (poolInfo != null) {
                    // 真实的连接池数据
                    Integer activeConnections = (Integer) poolInfo.get("activeConnections");
                    Integer idleConnections = (Integer) poolInfo.get("idleConnections");
                    Integer totalConnections = (Integer) poolInfo.get("totalConnections");
                    Double usageRate = (Double) poolInfo.get("usageRate");
                    
                    dbMetrics.put("activeConnections", activeConnections != null ? activeConnections : 0);
                    dbMetrics.put("idleConnections", idleConnections != null ? idleConnections : 0);
                    dbMetrics.put("totalConnections", totalConnections != null ? totalConnections : 0);
                    dbMetrics.put("connectionPoolUsage", usageRate != null ? Math.round(usageRate * 100.0) / 100.0 : 0.0);
                    
                    // 估算查询时间（基于连接池使用率）
                    long avgQueryTime = usageRate != null ? 
                        Math.max(10, Math.min(500, (long)(usageRate * 5))) : 50;
                    dbMetrics.put("avgQueryTime", avgQueryTime);
                    
                    // 估算慢查询数
                    int slowQueries = activeConnections != null ? 
                        Math.max(0, activeConnections / 20) : 0;
                    dbMetrics.put("slowQueries", slowQueries);
                }
            } else {
                // 回退到原来的估算方法
                LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
                Long totalOperations = sysLoginLogMapper.selectCount(
                    new LambdaQueryWrapper<SysLoginLog>()
                        .ge(SysLoginLog::getLoginTime, oneHourAgo)
                );
                
                long avgQueryTime = totalOperations > 0 ? Math.max(10, 1000 / Math.max(totalOperations, 1)) : 50;
                dbMetrics.put("avgQueryTime", avgQueryTime);
                dbMetrics.put("slowQueries", Math.max(0, totalOperations.intValue() / 100));
                dbMetrics.put("connectionPoolUsage", Math.min(95, totalOperations.intValue() * 2));
                dbMetrics.put("activeConnections", Math.min(50, totalOperations.intValue() / 2));
            }
            
        } catch (Exception e) {
            log.warn("获取数据库性能指标失败: {}", e.getMessage());
            // 默认值
            dbMetrics.put("avgQueryTime", 50);
            dbMetrics.put("slowQueries", 0);
            dbMetrics.put("connectionPoolUsage", 0);
            dbMetrics.put("activeConnections", 0);
        }
        
        return dbMetrics;
    }
    
    /**
     * 获取API响应时间指标
     */
    /**
     * 获取API响应时间指标 — 从 Micrometer http.response.time 聚合真实数据
     * 该指标由 ApplicationMonitorService.recordHttpRequest() 在每个请求完成时写入
     */
    private Map<String, Object> getApiResponseTimeMetrics() {
        Map<String, Object> apiMetrics = new HashMap<>();

        try {
            // 查询实际记录中的 http.response.time Timer（由 MonitoringInterceptor → ApplicationMonitorService 写入）
            Collection<Timer> timers = meterRegistry.get("http.response.time").timers();
            if (timers.isEmpty()) {
                apiMetrics.put("avgResponseTime", -1L);
                apiMetrics.put("minResponseTime", -1L);
                apiMetrics.put("maxResponseTime", -1L);
                apiMetrics.put("p95ResponseTime", -1L);
                return apiMetrics;
            }

            // 聚合所有 URI 的响应时间数据
            long totalCount = 0;
            double totalTimeMs = 0;
            double maxMs = 0;
            double p95Sum = 0;
            int timerCount = 0;

            for (Timer timer : timers) {
                if (timer.count() == 0) continue;
                HistogramSnapshot snapshot = timer.takeSnapshot();
                totalCount += snapshot.count();
                totalTimeMs += snapshot.total(TimeUnit.MILLISECONDS);
                double tMax = snapshot.max(TimeUnit.MILLISECONDS);
                if (tMax > maxMs) maxMs = tMax;
                
                // 获取P95响应时间
                double p95 = 0;
                if (snapshot.percentileValues() != null && snapshot.percentileValues().length > 0) {
                    for (var percentileValue : snapshot.percentileValues()) {
                        if (Math.abs(percentileValue.percentile() - 0.95) < 0.001) {
                            p95 = percentileValue.value();
                            break;
                        }
                    }
                }
                p95Sum += p95;
                timerCount++;
            }

            if (totalCount > 0 && timerCount > 0) {
                apiMetrics.put("avgResponseTime", Math.round(totalTimeMs / totalCount));
                apiMetrics.put("maxResponseTime", Math.round(maxMs));
                apiMetrics.put("p95ResponseTime", Math.round(p95Sum / timerCount));
            } else {
                apiMetrics.put("avgResponseTime", -1L);
                apiMetrics.put("maxResponseTime", -1L);
                apiMetrics.put("p95ResponseTime", -1L);
            }
            // Micrometer 直方图不直接暴露 min，需额外配置 DistributionSummary
            apiMetrics.put("minResponseTime", -1L);

        } catch (Exception e) {
            log.warn("获取API响应时间指标失败: {}", e.getMessage());
            apiMetrics.put("avgResponseTime", -1L);
            apiMetrics.put("minResponseTime", -1L);
            apiMetrics.put("maxResponseTime", -1L);
            apiMetrics.put("p95ResponseTime", -1L);
        }

        return apiMetrics;
    }
    
    /**
     * 获取缓存命中率
     */
    private Double getCacheHitRate() {
        try {
            // 通过Redis INFO stats获取真实的缓存命中率
            Properties info = redisTemplate.getRequiredConnectionFactory().getConnection()
                    .serverCommands().info("stats");
            String hitsStr = info.getProperty("keyspace_hits");
            String missesStr = info.getProperty("keyspace_misses");
            if (hitsStr != null && missesStr != null) {
                long hits = Long.parseLong(hitsStr);
                long misses = Long.parseLong(missesStr);
                long total = hits + misses;
                if (total > 0) {
                    return Math.round(hits * 10000.0 / total) / 100.0;
                }
            }
        } catch (Exception e) {
            log.warn("获取Redis缓存命中率失败: {}", e.getMessage());
        }
        return -1.0; // -1 表示不可用
    }
    
    /**
     * 获取并发用户数
     */
    private Integer getConcurrentUsers() {
        try {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            Long activeUsers = sysLoginLogMapper.selectCount(
                new LambdaQueryWrapper<SysLoginLog>()
                    .ge(SysLoginLog::getLoginTime, fiveMinutesAgo)
            );
            return activeUsers.intValue();
        } catch (Exception e) {
            log.warn("获取并发用户数失败: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 获取事务处理速率
     */
    private Double getTransactionsPerSecond() {
        try {
            LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
            Long recentOperations = sysLoginLogMapper.selectCount(
                new LambdaQueryWrapper<SysLoginLog>()
                    .ge(SysLoginLog::getLoginTime, oneMinuteAgo)
            );
            return recentOperations.doubleValue() / 60.0;
        } catch (Exception e) {
            log.warn("获取事务处理速率失败: {}", e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 获取错误率
     */
    private Double getErrorRate() {
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            Long totalOps = sysOperationLogMapper.selectCount(
                new LambdaQueryWrapper<SysOperationLog>()
                    .ge(SysOperationLog::getOperationTime, oneHourAgo));
            Long failedOps = sysOperationLogMapper.selectCount(
                new LambdaQueryWrapper<SysOperationLog>()
                    .ge(SysOperationLog::getOperationTime, oneHourAgo)
                    .eq(SysOperationLog::getStatus, 0));
            if (totalOps != null && totalOps > 0 && failedOps != null) {
                return Math.round(failedOps * 10000.0 / totalOps) / 100.0;
            }
        } catch (Exception e) {
            log.warn("获取错误率失败: {}", e.getMessage());
        }
        return 0.0;
    }
}