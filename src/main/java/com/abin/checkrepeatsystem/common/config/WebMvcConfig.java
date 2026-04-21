package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.interceptor.LoginInterceptor;
import com.abin.checkrepeatsystem.common.interceptor.MonitoringInterceptor;
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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 监控拦截器：记录所有请求的性能指标
        registry.addInterceptor(monitoringInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/check/actuator/**", "/ws/**");

        // 登录拦截器：验证用户登录状态
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/avatar/**",
                        "/api/papers/public/**",
                        "/api/minio/test-connection",
                        "/check/actuator/**"
                );
    }
}