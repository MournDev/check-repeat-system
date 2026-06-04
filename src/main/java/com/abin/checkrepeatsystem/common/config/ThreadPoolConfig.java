package com.abin.checkrepeatsystem.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 通用线程池配置
 * 用途：通知发送、文件异步处理、自动分配等通用异步任务
 * 注意：查重任务专用线程池见 AsyncConfig.checkTaskExecutor
 */
@Configuration
public class ThreadPoolConfig {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(getEnvInt("TASK_CORE_POOL", 5));
        executor.setMaxPoolSize(getEnvInt("TASK_MAX_POOL", 10));
        executor.setQueueCapacity(getEnvInt("TASK_QUEUE", 100));
        executor.setKeepAliveSeconds(60);

        // 线程名称前缀
        executor.setThreadNamePrefix("async-task-");

        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        return executor;
    }

    private int getEnvInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("解析环境变量 {} 失败: {}, 使用默认值 {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }
}
