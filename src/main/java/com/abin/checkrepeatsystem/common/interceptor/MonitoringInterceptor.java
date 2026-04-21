package com.abin.checkrepeatsystem.common.interceptor;

import com.abin.checkrepeatsystem.monitor.service.ApplicationMonitorService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 监控拦截器：记录HTTP请求性能指标
 */
@Component
public class MonitoringInterceptor implements HandlerInterceptor {

    @Resource
    private ApplicationMonitorService monitorService;

    private static final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        startTimeThreadLocal.set(startTime);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 计算请求处理时间
        Long startTime = startTimeThreadLocal.get();
        if (startTime != null) {
            long durationMs = System.currentTimeMillis() - startTime;
            startTimeThreadLocal.remove();

            // 记录HTTP请求指标
            String uri = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();

            monitorService.recordHttpRequest(uri, method, status, durationMs);
        }
    }
}