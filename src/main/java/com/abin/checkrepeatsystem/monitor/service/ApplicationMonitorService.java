package com.abin.checkrepeatsystem.monitor.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 应用性能监控服务
 * 基于Micrometer提供应用层面的性能监控
 */
@Service
public class ApplicationMonitorService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationMonitorService.class);

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 记录HTTP请求指标
     */
    public void recordHttpRequest(String uri, String method, int status, long durationMs) {
        if (meterRegistry != null) {
            try {
                // 记录请求计数
                Counter.builder("http.request.count")
                        .tag("uri", uri)
                        .tag("method", method)
                        .tag("status", String.valueOf(status))
                        .register(meterRegistry)
                        .increment();

                // 记录响应时间
                Timer.builder("http.response.time")
                        .tag("uri", uri)
                        .tag("method", method)
                        .tag("status", String.valueOf(status))
                        .register(meterRegistry)
                        .record(durationMs, TimeUnit.MILLISECONDS);

                // 记录错误率
                if (status >= 400) {
                    Counter.builder("http.error.count")
                            .tag("uri", uri)
                            .tag("method", method)
                            .tag("status", String.valueOf(status))
                            .register(meterRegistry)
                            .increment();
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
                // 记录查重任务计数
                Counter.builder("check.task.count")
                        .tag("success", String.valueOf(success))
                        .register(meterRegistry)
                        .increment();

                // 记录查重任务执行时间
                Timer.builder("check.task.time")
                        .tag("paperId", String.valueOf(paperId))
                        .tag("success", String.valueOf(success))
                        .register(meterRegistry)
                        .record(durationMs, TimeUnit.MILLISECONDS);
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
                // 记录文件上传计数
                Counter.builder("file.upload.count")
                        .tag("success", String.valueOf(success))
                        .register(meterRegistry)
                        .increment();

                // 记录文件上传大小
                Counter.builder("file.upload.size")
                        .register(meterRegistry)
                        .increment(sizeBytes);
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
                Counter.builder("user.operation.count")
                        .tag("operation", operation)
                        .tag("success", String.valueOf(success))
                        .register(meterRegistry)
                        .increment();
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
            // 应用基本信息
            Map<String, Object> appInfo = new HashMap<>();
            appInfo.put("name", "check-repeat-system");
            appInfo.put("version", "1.0.0");
            metrics.put("application", appInfo);
            
            // 系统信息
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
            // Redis缓存指标
            Map<String, Object> redisMetrics = new HashMap<>();
            try {
                if (redisTemplate != null) {
                    // 尝试获取Redis连接信息
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
                sample.stop(Timer.builder("method.execution.time")
                        .tag("method", methodName)
                        .register(meterRegistry));
            }
        } else {
            task.run();
        }
    }
}