package com.abin.checkrepeatsystem.schedule;

import com.abin.checkrepeatsystem.admin.service.AlertRuleService;
import com.abin.checkrepeatsystem.admin.service.AlertService;
import com.abin.checkrepeatsystem.monitor.service.SystemMonitorService;
import com.abin.checkrepeatsystem.notification.service.IntelligentNotificationService;
import com.abin.checkrepeatsystem.pojo.entity.AlertRecord;
import com.abin.checkrepeatsystem.pojo.entity.AlertRule;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class AlertEvaluationScheduler {

    private final AlertRuleService alertRuleService;
    private final AlertService alertService;
    private final SystemMonitorService systemMonitorService;
    private final IntelligentNotificationService notificationService;
    private final MeterRegistry meterRegistry;

    private double lastCpuValue = 0;
    private double lastMemoryValue = 0;
    private double lastDiskValue = 0;
    private final Map<Long, Integer> consecutiveOverCounters = new java.util.concurrent.ConcurrentHashMap<>();

    // 上一轮累积计数器值，用于 LOGIN_FAIL/CHECK_FAIL 差值计算
    private double lastLoginFailCount = -1;
    private double lastCheckFailCount = -1;

    @Scheduled(fixedRate = 60000)
    public void evaluateRules() {
        try {
            List<AlertRule> rules = alertRuleService.listEnabled();
            if (rules.isEmpty()) {
                log.debug("没有启用的告警规则");
                return;
            }

            collectCurrentMetrics();

            for (AlertRule rule : rules) {
                evaluateRule(rule);
            }
        } catch (Exception e) {
            log.error("告警规则评估失败", e);
        }
    }

    private void collectCurrentMetrics() {
        try {
            Map<String, Object> cpuInfo = systemMonitorService.getCpuInfo();
            Double cpuUsage = (Double) cpuInfo.get("processCpuUsage");
            lastCpuValue = (cpuUsage != null && cpuUsage >= 0) ? cpuUsage : lastCpuValue;

            Map<String, Object> memoryInfo = systemMonitorService.getMemoryInfo();
            Double heapUsage = (Double) memoryInfo.get("heapUsagePercent");
            lastMemoryValue = (heapUsage != null) ? heapUsage : lastMemoryValue;

            Map<String, Object> diskInfo = systemMonitorService.getDiskInfo();
            Double diskUsage = (Double) diskInfo.get("usagePercent");
            lastDiskValue = (diskUsage != null) ? diskUsage : lastDiskValue;
        } catch (Exception e) {
            log.debug("指标采集失败: {}", e.getMessage());
        }
    }

    private void evaluateRule(AlertRule rule) {
        try {
            double currentValue = getMetricValue(rule);
            boolean exceeded = currentValue > rule.getThreshold();

            if ("CPU".equals(rule.getRuleType()) || "MEMORY".equals(rule.getRuleType())
                || "DISK".equals(rule.getRuleType()) || "STORAGE".equals(rule.getRuleType())) {
                evaluateResourceRule(rule, exceeded);
            } else if ("LOGIN_FAIL".equals(rule.getRuleType())) {
                evaluateLoginFailRule(rule);
            } else if ("CHECK_FAIL".equals(rule.getRuleType())) {
                evaluateCheckFailRule(rule);
            }
        } catch (Exception e) {
            log.debug("评估规则 [{}] 失败: {}", rule.getRuleName(), e.getMessage());
        }
    }

    private void evaluateResourceRule(AlertRule rule, boolean exceeded) {
        Long ruleId = rule.getId();
        int ticks = consecutiveOverCounters.getOrDefault(ruleId, 0);
        if (exceeded) {
            ticks++;
            consecutiveOverCounters.put(ruleId, ticks);
            if (ticks * 60 >= rule.getDuration()) {
                fireAlert(rule, getMetricValue(rule));
                consecutiveOverCounters.put(ruleId, 0);
            }
        } else {
            consecutiveOverCounters.put(ruleId, 0);
        }
    }

    private void evaluateLoginFailRule(AlertRule rule) {
        if (meterRegistry == null) return;
        var counter = meterRegistry.get("http.error.count")
            .tag("uri", "/api/v1/auth/login").counter();
        if (counter == null) return;
        double current = counter.count();
        if (lastLoginFailCount < 0) {
            lastLoginFailCount = current;
            return;
        }
        double delta = current - lastLoginFailCount;
        lastLoginFailCount = current;
        if (delta > 0) {
            log.debug("LOGIN_FAIL 规则 [{}]: 周期内新增失败 {} 次", rule.getRuleName(), delta);
        }
        if (delta >= rule.getThreshold()) {
            // 去重：检查是否已有同规则未处理的告警
            if (!alertService.existsActiveByRuleId(rule.getId())) {
                fireAlert(rule, delta);
            }
        }
    }

    private void evaluateCheckFailRule(AlertRule rule) {
        if (meterRegistry == null) return;
        var counter = meterRegistry.get("check.task.count")
            .tag("success", "false").counter();
        if (counter == null) return;
        double current = counter.count();
        if (lastCheckFailCount < 0) {
            lastCheckFailCount = current;
            return;
        }
        double delta = current - lastCheckFailCount;
        lastCheckFailCount = current;
        if (delta > 0) {
            log.debug("CHECK_FAIL 规则 [{}]: 周期内新增失败 {} 次", rule.getRuleName(), delta);
        }
        if (delta >= rule.getThreshold()) {
            if (!alertService.existsActiveByRuleId(rule.getId())) {
                fireAlert(rule, delta);
            }
        }
    }

    private double getMetricValue(AlertRule rule) {
        return switch (rule.getRuleType()) {
            case "CPU" -> lastCpuValue;
            case "MEMORY" -> lastMemoryValue;
            case "DISK", "STORAGE" -> lastDiskValue;
            default -> 0;
        };
    }

    private void fireAlert(AlertRule rule, double value) {
        AlertRecord record = new AlertRecord();
        record.setRuleId(rule.getId());
        record.setRuleName(rule.getRuleName());
        record.setSeverity(rule.getSeverity());
        record.setTitle(rule.getRuleName() + " 超过阈值");
        record.setMessage(String.format("指标 %s 当前值 %.1f 超过阈值 %.1f，持续时间 %d 秒",
            rule.getMetricName(), value, rule.getThreshold(), rule.getDuration()));
        record.setMetricValue(value);
        record.setTriggerTime(LocalDateTime.now());

        alertService.create(record);

        if (rule.getNotifyEmail() != null && !rule.getNotifyEmail().isBlank()) {
            try {
                String title = "[告警] " + rule.getRuleName();
                String content = String.format("""
                    规则名称: %s
                    指标: %s
                    当前值: %.1f
                    阈值: %.1f
                    严重级别: %s
                    触发时间: %s
                    """, rule.getRuleName(), rule.getMetricName(), value,
                    rule.getThreshold(), rule.getSeverity(),
                    LocalDateTime.now().toString());
                notificationService.sendEmailNotification(rule.getNotifyEmail(), title, content);
            } catch (Exception e) {
                log.error("告警邮件发送失败: rule={}, email={}", rule.getRuleName(), rule.getNotifyEmail(), e);
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldAlerts() {
        try {
            int count = alertService.cleanupOldAlerts(30);
            log.info("清理旧告警记录完成: 保留最近30天, 清理{}条", count);
        } catch (Exception e) {
            log.error("清理旧告警记录失败", e);
        }
    }
}