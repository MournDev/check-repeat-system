package com.abin.checkrepeatsystem.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * 注意：@EnableAsync 在 Application 类上统一声明，此处只定义线程池 Bean
 * 线程池参数支持通过环境变量覆盖，未设置时使用默认值
 */
@Configuration
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * 查重任务专用线程池
     */
    @Bean("checkTaskExecutor")
    public Executor checkTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(getEnvInt("CHECK_TASK_CORE_POOL", 5));
        executor.setMaxPoolSize(getEnvInt("CHECK_TASK_MAX_POOL", 20));
        executor.setQueueCapacity(getEnvInt("CHECK_TASK_QUEUE", 100));
        executor.setThreadNamePrefix("check-task-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    /**
     * 通用异步任务线程池
     */
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(getEnvInt("ASYNC_CORE_POOL", 3));
        executor.setMaxPoolSize(getEnvInt("ASYNC_MAX_POOL", 10));
        executor.setQueueCapacity(getEnvInt("ASYNC_QUEUE", 50));
        executor.setThreadNamePrefix("async-");
        executor.setKeepAliveSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

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
