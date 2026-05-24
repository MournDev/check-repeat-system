package com.abin.checkrepeatsystem.monitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.io.File;

/**
 * 系统级资源告警服务
 */
@Slf4j
@Service
public class SystemAlertService {

    @Value("${monitor.alert.cpu-threshold:90.0}")
    private double cpuThreshold;

    @Value("${monitor.alert.memory-threshold:85.0}")
    private double memoryThreshold;

    @Value("${monitor.alert.disk-threshold:90.0}")
    private double diskThreshold;

    @Value("${monitor.alert.enabled:true}")
    private boolean alertEnabled;

    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    /**
     * 评估系统健康状态，返回告警信息（无告警返回null）
     */
    public String evaluateSystemHealth() {
        if (!alertEnabled) return null;

        StringBuilder alerts = new StringBuilder();

        // CPU 检查
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            double cpuLoad = sunOsBean.getCpuLoad() * 100;
            if (cpuLoad > cpuThreshold) {
                alerts.append(String.format("[CPU告警] 当前CPU使用率: %.1f%%, 阈值: %.0f%%; ", cpuLoad, cpuThreshold));
            }
        }

        // 堆内存检查
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        if (heapMax > 0) {
            double heapUsagePercent = (double) heapUsed / heapMax * 100;
            if (heapUsagePercent > memoryThreshold) {
                alerts.append(String.format("[内存告警] 堆内存使用率: %.1f%%, 阈值: %.0f%%; ", heapUsagePercent, memoryThreshold));
            }
        }

        // 线程检查
        int threadCount = threadBean.getThreadCount();
        if (threadCount > 500) {
            alerts.append(String.format("[线程告警] 当前线程数: %d (偏高); ", threadCount));
        }

        // 磁盘检查
        File dataDir = new File("/data");
        if (dataDir.exists()) {
            long usableSpace = dataDir.getUsableSpace();
            long totalSpace = dataDir.getTotalSpace();
            if (totalSpace > 0) {
                double diskUsage = (1.0 - (double) usableSpace / totalSpace) * 100;
                if (diskUsage > diskThreshold) {
                    alerts.append(String.format("[磁盘告警] /data 使用率: %.1f%%, 阈值: %.0f%%; ", diskUsage, diskThreshold));
                }
            }
        }

        if (alerts.length() > 0) {
            String alertMsg = alerts.toString();
            log.warn("系统资源告警: {}", alertMsg);
            return alertMsg;
        }
        return null;
    }
}