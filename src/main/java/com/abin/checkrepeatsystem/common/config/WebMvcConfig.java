package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.interceptor.IdempotencyInterceptor;
import com.abin.checkrepeatsystem.common.interceptor.LoginInterceptor;
import com.abin.checkrepeatsystem.common.interceptor.MonitoringInterceptor;
import com.abin.checkrepeatsystem.common.interceptor.RateLimitInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置：添加拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Resource
    private MonitoringInterceptor monitoringInterceptor;

    @Resource
    private RateLimitInterceptor rateLimitInterceptor;

    @Resource
    private IdempotencyInterceptor idempotencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流拦截器：基于注解@RateLimit检查频率
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        // 幂等性拦截器：基于注解@Idempotent防止重复提交
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/api/**")
                .order(2);

        // 监控拦截器：记录所有请求的性能指标
        registry.addInterceptor(monitoringInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/check/actuator/**", "/ws/**")
                .order(10);

        // 登录拦截器：验证用户登录状态
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/avatar/**",
                        "/api/papers/public/**",
                        "/api/minio/test-connection",
                        "/api/file/preview/**",
                        "/api/file/smartPreview",
                        "/api/file/smartPreviewReport",
                        "/api/file/downloadReport/**",
                        "/api/preview/**",
                        "/check/actuator/**"
                );
    }
}