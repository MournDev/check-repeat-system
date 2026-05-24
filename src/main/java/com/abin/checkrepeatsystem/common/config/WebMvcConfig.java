package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.interceptor.IdempotencyInterceptor;
import com.abin.checkrepeatsystem.common.interceptor.LoginInterceptor;
import com.abin.checkrepeatsystem.common.interceptor.MonitoringInterceptor;
import com.abin.checkrepeatsystem.common.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.RequiredArgsConstructor;


/**
 * Web MVC配置：添加拦截器
 */
@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    private final MonitoringInterceptor monitoringInterceptor;

    private final RateLimitInterceptor rateLimitInterceptor;

    private final IdempotencyInterceptor idempotencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流拦截器：基于注解@RateLimit检查频率（含actuator端点防止爬取）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/**", "/actuator/**")
                .order(1);

        // 幂等性拦截器：基于注解@Idempotent防止重复提交
        registry.addInterceptor(idempotencyInterceptor)
                .addPathPatterns("/api/v1/**")
                .order(2);

        // 监控拦截器：记录所有请求的性能指标
        registry.addInterceptor(monitoringInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/check/actuator/**", "/ws/**")
                .order(10);

        // 登录拦截器：验证用户登录状态
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",
                        "/api/v1/avatar/**",
                        "/api/v1/papers/public/**",
                        "/api/v1/minio/test-connection",
                        "/api/v1/file/preview/**",
                        "/api/v1/file/smartPreview",
                        "/api/v1/file/smartPreviewReport",
                        "/api/v1/file/downloadReport/**",
                        "/api/v1/preview/**",
                        "/check/actuator/**"
                );
    }
}