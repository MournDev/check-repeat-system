package com.abin.checkrepeatsystem.common.interceptor;

import com.abin.checkrepeatsystem.common.annotation.RateLimit;
import com.abin.checkrepeatsystem.common.utils.HttpIpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;

/**
 * 限流拦截器 — 基于Redis滑动窗口算法
 * time bucket 粒度：1秒一个bucket，windowSeconds个bucket组成窗口
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String clientIp = HttpIpUtils.getRealIp(request);
        String key = RATE_LIMIT_PREFIX + rateLimit.keyPrefix() + ":" + clientIp;

        try {
            boolean allowed = checkSlidingWindow(key, rateLimit);
            if (!allowed) {
                response.setStatus(429);
                writeJson(response, Map.of(
                    "code", 429,
                    "errorCode", "TOO_MANY_REQUESTS",
                    "message", rateLimit.message(),
                    "data", null
                ));
                log.warn("限流拦截：IP={}, keyPrefix={}", clientIp, rateLimit.keyPrefix());
                return false;
            }
        } catch (Exception e) {
            log.warn("Redis限流检查异常，放行请求: {}", e.getMessage());
            // Redis不可用时放行，避免影响业务
        }

        return true;
    }

    /**
     * 滑动窗口限流 — 基于Redis sorted set
     */
    private boolean checkSlidingWindow(String key, RateLimit config) {
        long now = System.currentTimeMillis();
        long windowStart = now - config.windowSeconds() * 1000L;

        // 移除窗口外的旧记录
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // 统计窗口内的请求数
        Long count = redisTemplate.opsForZSet().count(key, windowStart, now);
        if (count != null && count >= config.maxRequests()) {
            return false;
        }

        // 添加当前请求记录（score = 当前时间戳，value = 纳秒级唯一ID避免毫秒碰撞）
        String memberId = now + ":" + System.nanoTime();
        redisTemplate.opsForZSet().add(key, memberId, now);
        // 设置TTL兜底：窗口的2倍时间后自动过期
        redisTemplate.expire(key, java.time.Duration.ofSeconds(config.windowSeconds() * 2L));
        return true;
    }

    private void writeJson(HttpServletResponse response, Map<String, Object> body) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
