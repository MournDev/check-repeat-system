package com.abin.checkrepeatsystem.common.interceptor;

import com.abin.checkrepeatsystem.monitor.service.ApplicationMonitorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.RequiredArgsConstructor;


/**
 * 监控拦截器：记录HTTP请求性能指标
 */
@RequiredArgsConstructor
@Component
public class MonitoringInterceptor implements HandlerInterceptor {

    private final ApplicationMonitorService monitorService;

    private static final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        startTimeThreadLocal.set(startTime);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        try {
            Long startTime = startTimeThreadLocal.get();
            if (startTime != null) {
                long durationMs = System.currentTimeMillis() - startTime;
                String uri = request.getRequestURI();
                String method = request.getMethod();
                int status = response.getStatus();
                monitorService.recordHttpRequest(uri, method, status, durationMs);
            }
        } finally {
            startTimeThreadLocal.remove();
        }
    }
}