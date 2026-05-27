package com.abin.checkrepeatsystem.monitor.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

/**
 * 应用性能监控服务
 * 基于Micrometer提供应用层面的性能监控
 */
@RequiredArgsConstructor
@Service
public class ApplicationMonitorService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationMonitorService.class);

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ConcurrentHashMap<String, Counter> counterCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timerCache = new ConcurrentHashMap<>();

    // 响应时间滑动窗口（最近30分钟的采样，最多5000条）
    private static final int MAX_SAMPLES = 5000;
    private static final long SAMPLE_WINDOW_MS = 30 * 60 * 1000;
    private final ConcurrentLinkedDeque<ResponseTimeSample> responseTimeSamples = new ConcurrentLinkedDeque<>();

    /**
     * 响应时间采样记录
     */
    public static class ResponseTimeSample {
        private final long timestamp;
        private final String uri;
        private final String method;
        private final int status;
        private final long durationMs;

        public ResponseTimeSample(long timestamp, String uri, String method, int status, long durationMs) {
            this.timestamp = timestamp;
            this.uri = uri;
            this.method = method;
            this.status = status;
            this.durationMs = durationMs;
        }

        public long getTimestamp() { return timestamp; }
        public String getUri() { return uri; }
        public String getMethod() { return method; }
        public int getStatus() { return status; }
        public long getDurationMs() { return durationMs; }
    }

    /**
     * 记录HTTP请求指标
     */
    public void recordHttpRequest(String uri, String method, int status, long durationMs) {
        if (meterRegistry != null) {
            try {
                String countKey = "http.request.count|uri=" + uri + "|method=" + method + "|status=" + status;
                counterCache.computeIfAbsent(countKey, k ->
                    Counter.builder("http.request.count")
                            .tag("uri", uri)
                            .tag("method", method)
                            .tag("status", String.valueOf(status))
                            .register(meterRegistry)
                ).increment();

                String timerKey = "http.response.time|uri=" + uri + "|method=" + method + "|status=" + status;
                timerCache.computeIfAbsent(timerKey, k ->
                    Timer.builder("http.response.time")
                            .tag("uri", uri)
                            .tag("method", method)
                            .tag("status", String.valueOf(status))
                            .register(meterRegistry)
                ).record(durationMs, TimeUnit.MILLISECONDS);

                if (status >= 400) {
                    String errorKey = "http.error.count|uri=" + uri + "|method=" + method + "|status=" + status;
                    counterCache.computeIfAbsent(errorKey, k ->
                        Counter.builder("http.error.count")
                                .tag("uri", uri)
                                .tag("method", method)
                                .tag("status", String.valueOf(status))
                                .register(meterRegistry)
                    ).increment();
                }
            } catch (Exception e) {
                log.debug("记录HTTP请求指标失败: {}", e.getMessage());
            }
        }

        // 存储采样数据用于趋势图（非Micrometer路径，独立维护）
        try {
            responseTimeSamples.addLast(new ResponseTimeSample(
                System.currentTimeMillis(), uri, method, status, durationMs));
            // 清理过期样本
            long cutoff = System.currentTimeMillis() - SAMPLE_WINDOW_MS;
            while (!responseTimeSamples.isEmpty()) {
                ResponseTimeSample oldest = responseTimeSamples.peekFirst();
                if (oldest != null && oldest.timestamp < cutoff) {
                    responseTimeSamples.pollFirst();
                } else {
                    break;
                }
            }
            // 限制最大容量
            while (responseTimeSamples.size() > MAX_SAMPLES) {
                responseTimeSamples.pollFirst();
            }
        } catch (Exception e) {
            log.debug("存储响应时间采样失败: {}", e.getMessage());
        }
    }

    /**
     * 记录查重任务执行时间
     */
    public void recordCheckTaskTime(long paperId, long durationMs, boolean success) {
        if (meterRegistry != null) {
            try {
                String countKey = "check.task.count|success=" + success;
                counterCache.computeIfAbsent(countKey, k ->
                    Counter.builder("check.task.count")
                            .tag("success", String.valueOf(success))
                            .register(meterRegistry)
                ).increment();

                String timerKey = "check.task.time|paperId=" + paperId + "|success=" + success;
                timerCache.computeIfAbsent(timerKey, k ->
                    Timer.builder("check.task.time")
                            .tag("paperId", String.valueOf(paperId))
                            .tag("success", String.valueOf(success))
                            .register(meterRegistry)
                ).record(durationMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                log.debug("记录查重任务指标失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 记录文件上传指标
     */
    public void recordFileUpload(long sizeBytes, boolean success) {
        if (meterRegistry != null) {
            try {
                String countKey = "file.upload.count|success=" + success;
                counterCache.computeIfAbsent(countKey, k ->
                    Counter.builder("file.upload.count")
                            .tag("success", String.valueOf(success))
                            .register(meterRegistry)
                ).increment();

                String sizeKey = "file.upload.size";
                counterCache.computeIfAbsent(sizeKey, k ->
                    Counter.builder("file.upload.size")
                            .register(meterRegistry)
                ).increment(sizeBytes);
            } catch (Exception e) {
                log.debug("记录文件上传指标失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 注册Gauge指标（用于暴露瞬时值，如队列深度、活跃用户数等）
     */
    public void registerGauge(String name, String description, Supplier<Number> supplier) {
        if (meterRegistry != null) {
            try {
                Gauge.builder(name, supplier)
                        .description(description)
                        .register(meterRegistry);
            } catch (Exception e) {
                log.debug("注册Gauge指标失败: {} - {}", name, e.getMessage());
            }
        }
    }

    /**
     * 记录业务事件计数（login/submit/review等）
     */
    public void recordBusinessEvent(String event, String result, double count) {
        if (meterRegistry != null) {
            try {
                String key = "business.event|event=" + event + "|result=" + result;
                counterCache.computeIfAbsent(key, k ->
                    Counter.builder("business.event.count")
                            .tag("event", event)
                            .tag("result", result)
                            .description("业务事件计数")
                            .register(meterRegistry)
                ).increment(count);
            } catch (Exception e) {
                log.debug("记录业务事件指标失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 记录用户操作指标
     */
    public void recordUserOperation(String operation, boolean success) {
        if (meterRegistry != null) {
            try {
                String countKey = "user.operation.count|operation=" + operation + "|success=" + success;
                counterCache.computeIfAbsent(countKey, k ->
                    Counter.builder("user.operation.count")
                            .tag("operation", operation)
                            .tag("success", String.valueOf(success))
                            .register(meterRegistry)
                ).increment();
            } catch (Exception e) {
                log.debug("记录用户操作指标失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取应用性能指标
     */
    public Result<Map<String, Object>> getApplicationMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            Map<String, Object> appInfo = new HashMap<>();
            appInfo.put("name", "check-repeat-system");
            appInfo.put("version", "1.0.0");
            metrics.put("application", appInfo);

            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("javaVersion", System.getProperty("java.version"));
            systemInfo.put("osName", System.getProperty("os.name"));
            systemInfo.put("osVersion", System.getProperty("os.version"));
            metrics.put("system", systemInfo);

            return Result.success("应用性能指标获取成功", metrics);
        } catch (Exception e) {
            log.error("获取应用性能指标失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,
                              "获取应用性能指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存性能指标
     */
    public Result<Map<String, Object>> getCacheMetrics() {
        Map<String, Object> cacheMetrics = new HashMap<>();

        try {
            Map<String, Object> redisMetrics = new HashMap<>();
            try {
                if (redisTemplate != null) {
                    redisMetrics.put("status", "healthy");
                    redisMetrics.put("connectionAvailable", true);
                } else {
                    redisMetrics.put("status", "not_available");
                    redisMetrics.put("connectionAvailable", false);
                }
            } catch (Exception e) {
                redisMetrics.put("status", "error");
                redisMetrics.put("connectionAvailable", false);
                redisMetrics.put("error", e.getMessage());
            }
            cacheMetrics.put("redis", redisMetrics);

            return Result.success("缓存性能指标获取成功", cacheMetrics);
        } catch (Exception e) {
            log.error("获取缓存性能指标失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,
                              "获取缓存性能指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取自定义业务指标（从MeterRegistry读取实时数据）
     */
    public Result<Map<String, Object>> getBusinessMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            if (meterRegistry != null) {
                Map<String, Object> counters = new HashMap<>();
                Map<String, Object> gauges = new HashMap<>();
                Map<String, Object> timers = new HashMap<>();

                for (Meter meter : meterRegistry.getMeters()) {
                    String name = meter.getId().getName();

                    if (meter instanceof Counter counter) {
                        double value = counter.count();
                        if (value > 0 || name.startsWith("business.") || name.startsWith("check.")) {
                            counters.put(meter.getId().getName() + formatTags(meter), value);
                        }
                    } else if (meter instanceof Gauge gauge) {
                        gauges.put(meter.getId().getName() + formatTags(meter), gauge.value());
                    } else if (meter instanceof Timer timer) {
                        Map<String, Object> timerData = new HashMap<>();
                        timerData.put("count", timer.count());
                        timerData.put("mean", timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
                        timerData.put("max", timer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
                        timers.put(meter.getId().getName() + formatTags(meter), timerData);
                    }
                }

                if (!counters.isEmpty()) metrics.put("counters", counters);
                if (!gauges.isEmpty()) metrics.put("gauges", gauges);
                if (!timers.isEmpty()) metrics.put("timers", timers);
            }

            metrics.put("timestamp", System.currentTimeMillis());
            return Result.success("业务指标获取成功", metrics);
        } catch (Exception e) {
            log.error("获取业务指标失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取业务指标失败: " + e.getMessage());
        }
    }

    private static final Set<String> SYSTEM_TAG_KEYS = Set.of("application", "version");

    private String formatTags(Meter meter) {
        var tags = meter.getId().getTags();
        StringBuilder sb = new StringBuilder("[");
        boolean hasTag = false;
        for (var tag : tags) {
            if (SYSTEM_TAG_KEYS.contains(tag.getKey())) continue;
            sb.append(tag.getKey()).append("=").append(tag.getValue()).append(",");
            hasTag = true;
        }
        if (!hasTag) return "";
        sb.setLength(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    /**
     * 获取响应时间趋势数据（最近N分钟，按分钟聚合）
     */
    public Result<Map<String, Object>> getResponseTimeTrend(int minutes) {
        Map<String, Object> data = new LinkedHashMap<>();

        try {
            minutes = Math.min(minutes, 30); // 最多30分钟
            long cutoff = System.currentTimeMillis() - (long) minutes * 60 * 1000;

            // 过滤出时间窗口内的样本
            List<ResponseTimeSample> recentSamples = new ArrayList<>(responseTimeSamples);
            List<ResponseTimeSample> windowSamples = recentSamples.stream()
                    .filter(s -> s.timestamp >= cutoff)
                    .collect(Collectors.toList());

            if (windowSamples.isEmpty()) {
                data.put("timestamps", List.of());
                data.put("avgResponseTime", List.of());
                data.put("p95ResponseTime", List.of());
                data.put("requestCount", List.of());
                return Result.success("暂无响应时间数据", data);
            }

            // 按分钟分桶
            Map<Long, List<Long>> bucketMap = new LinkedHashMap<>();
            for (ResponseTimeSample sample : windowSamples) {
                long bucketKey = sample.timestamp / 60000; // 分钟级分桶
                bucketMap.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(sample.durationMs);
            }

            // 生成时序数据
            List<String> timestamps = new ArrayList<>();
            List<Double> avgTimes = new ArrayList<>();
            List<Double> p95Times = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");

            for (Map.Entry<Long, List<Long>> entry : bucketMap.entrySet()) {
                long bucketTime = entry.getKey() * 60000;
                List<Long> durations = entry.getValue();
                durations.sort(Long::compareTo);

                double avg = durations.stream().mapToLong(Long::longValue).average().orElse(0);
                int p95Index = (int) Math.ceil(durations.size() * 0.95) - 1;
                if (p95Index < 0) p95Index = 0;
                double p95 = durations.get(p95Index);

                timestamps.add(sdf.format(new java.util.Date(bucketTime)));
                avgTimes.add(Math.round(avg * 100.0) / 100.0);
                p95Times.add((double) p95);
                counts.add(durations.size());
            }

            data.put("timestamps", timestamps);
            data.put("avgResponseTime", avgTimes);
            data.put("p95ResponseTime", p95Times);
            data.put("requestCount", counts);
            data.put("totalSamples", windowSamples.size());
            data.put("periodMinutes", minutes);

            return Result.success("响应时间趋势获取成功", data);
        } catch (Exception e) {
            log.error("获取响应时间趋势失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取响应时间趋势失败: " + e.getMessage());
        }
    }

    /**
     * 记录方法执行时间
     */
    public void recordMethodExecutionTime(String methodName, Runnable task) {
        if (meterRegistry != null) {
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                task.run();
            } finally {
                String timerKey = "method.execution.time|method=" + methodName;
                Timer timer = timerCache.computeIfAbsent(timerKey, k ->
                    Timer.builder("method.execution.time")
                            .tag("method", methodName)
                            .register(meterRegistry)
                );
                sample.stop(timer);
            }
        } else {
            task.run();
        }
    }
}