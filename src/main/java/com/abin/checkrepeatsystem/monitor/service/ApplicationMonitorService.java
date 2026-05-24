package com.abin.checkrepeatsystem.monitor.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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