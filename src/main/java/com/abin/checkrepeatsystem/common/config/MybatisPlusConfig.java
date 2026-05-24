package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.handler.ListStringTypeHandler;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    private static final Logger log = LoggerFactory.getLogger(MybatisPlusConfig.class);

    /**
     * Snowflake ID 生成器（支持容器化部署时通过环境变量指定 worker-id 和 datacenter-id）
     */
    @Bean
    public IdentifierGenerator identifierGenerator() {
        long workerId = getEnvLong("SNOWFLAKE_WORKER_ID", 1L);
        long dataCenterId = getEnvLong("SNOWFLAKE_DATACENTER_ID", 1L);
        return new DefaultIdentifierGenerator(workerId, dataCenterId);
    }

    /**
     * MyBatis-Plus 插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 配置自定义TypeHandler
     */
    @Bean
    public MybatisConfiguration mybatisConfiguration() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.getTypeHandlerRegistry().register(ListStringTypeHandler.class);
        return configuration;
    }

    private long getEnvLong(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("解析环境变量 {} 失败: {}, 使用默认值 {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }
}
