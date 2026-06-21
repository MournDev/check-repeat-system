package com.abin.checkrepeatsystem.monitor.service;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统指标采样服务
 * 定期采集CPU、内存、磁盘、数据库连接等指标，存储在滑动窗口中供趋势查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricSampleService {

    private final DataSource dataSource;

    private static final int MAX_SAMPLES = 2880; // 24小时 @ 30秒间隔
    private final Deque<MetricSample> samples = new ArrayDeque<>(MAX_SAMPLES + 100);

    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    /**
     * 单次采样数据
     */
    public static class MetricSample {
        private final long timestamp;
        private final double cpuUsage;
        private final double memoryUsage;
        private final double diskUsage;
        private final int dbActiveConnections;
        private final int dbTotalConnections;

        public MetricSample(long timestamp, double cpuUsage, double memoryUsage,
                            double diskUsage, int dbActiveConnections, int dbTotalConnections) {
            this.timestamp = timestamp;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.diskUsage = diskUsage;
            this.dbActiveConnections = dbActiveConnections;
            this.dbTotalConnections = dbTotalConnections;
        }

        public long getTimestamp() { return timestamp; }
        public double getCpuUsage() { return cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public double getDiskUsage() { return diskUsage; }
        public int getDbActiveConnections() { return dbActiveConnections; }
        public int getDbTotalConnections() { return dbTotalConnections; }
    }

    /**
     * 每30秒采集一次系统指标
     */
    @Scheduled(fixedRate = 30_000)
    public void collectSample() {
        try {
            double cpu = getCpuUsage();
            double memory = getMemoryUsage();
            double disk = getDiskUsage();
            int dbActive = 0;
            int dbTotal = 0;

            if (dataSource instanceof HikariDataSource hikari) {
                var pool = hikari.getHikariPoolMXBean();
                if (pool != null) {
                    dbActive = pool.getActiveConnections();
                    dbTotal = pool.getTotalConnections();
                }
            }

            MetricSample sample = new MetricSample(
                    System.currentTimeMillis(), cpu, memory, disk, dbActive, dbTotal);

            synchronized (samples) {
                samples.addLast(sample);
                while (samples.size() > MAX_SAMPLES) {
                    samples.pollFirst();
                }
            }
        } catch (Exception e) {
            log.debug("采集系统指标失败: {}", e.getMessage());
        }
    }

    /**
     * 查询指定时间范围内的采样数据
     *
     * @param minutes 最近N分钟
     * @param maxPoints 最多返回的数据点数
     */
    public List<MetricSample> getSamples(int minutes, int maxPoints) {
        long cutoff = System.currentTimeMillis() - (long) minutes * 60 * 1000;
        List<MetricSample> result;

        synchronized (samples) {
            result = samples.stream()
                    .filter(s -> s.timestamp >= cutoff)
                    .toList();
        }

        if (result.size() <= maxPoints) {
            return result;
        }

        // 降采样：均匀选取 maxPoints 个点
        List<MetricSample> downsampled = new ArrayList<>(maxPoints);
        double step = (double) result.size() / maxPoints;
        for (int i = 0; i < maxPoints; i++) {
            int index = (int) (i * step);
            downsampled.add(result.get(index));
        }
        return downsampled;
    }

    /**
     * 格式化后的趋势数据，直接返回给前端
     */
    public Map<String, Object> getTrendData(String period) {
        int minutes = switch (period.toLowerCase()) {
            case "1h" -> 60;
            case "24h" -> 1440;
            case "7d" -> 10080;
            default -> 60;
        };
        int maxPoints = switch (period.toLowerCase()) {
            case "1h" -> 12;
            case "24h" -> 24;
            case "7d" -> 7;
            default -> 12;
        };

        List<MetricSample> data = getSamples(minutes, maxPoints);
        DateTimeFormatter formatter = switch (period.toLowerCase()) {
            case "7d" -> DateTimeFormatter.ofPattern("MM-dd");
            case "24h" -> DateTimeFormatter.ofPattern("HH:00");
            default -> DateTimeFormatter.ofPattern("HH:mm");
        };

        List<String> timestamps = new ArrayList<>();
        List<Double> cpuUsages = new ArrayList<>();
        List<Double> memoryUsages = new ArrayList<>();
        List<Double> diskUsages = new ArrayList<>();
        List<Integer> dbConnections = new ArrayList<>();

        for (MetricSample s : data) {
            LocalDateTime time = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(s.timestamp), ZoneId.systemDefault());
            timestamps.add(time.format(formatter));
            cpuUsages.add(Math.round(s.cpuUsage * 100.0) / 100.0);
            memoryUsages.add(Math.round(s.memoryUsage * 100.0) / 100.0);
            diskUsages.add(Math.round(s.diskUsage * 100.0) / 100.0);
            dbConnections.add(s.getDbActiveConnections());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("timestamps", timestamps);
        result.put("cpuUsage", cpuUsages);
        result.put("memoryUsage", memoryUsages);
        result.put("diskUsage", diskUsages);
        result.put("dbConnections", dbConnections);
        return result;
    }

    private double getCpuUsage() {
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                double load = sunOs.getProcessCpuLoad() * 100;
                return load >= 0 ? Math.round(load * 100.0) / 100.0 : 0;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private double getMemoryUsage() {
        try {
            var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            double pct = (double) heap.getUsed() / heap.getMax() * 100;
            return Math.round(pct * 100.0) / 100.0;
        } catch (Exception ignored) {}
        return 0;
    }

    private double getDiskUsage() {
        try {
            File root = new File(".").getAbsoluteFile().getParentFile();
            if (root == null) root = new File("/");
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            if (total > 0) {
                double pct = (double) (total - free) / total * 100;
                return Math.round(pct * 100.0) / 100.0;
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
