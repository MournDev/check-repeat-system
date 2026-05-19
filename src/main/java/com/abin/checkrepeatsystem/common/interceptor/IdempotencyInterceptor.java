package com.abin.checkrepeatsystem.common.interceptor;

import com.abin.checkrepeatsystem.common.annotation.Idempotent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 幂等性拦截器 — 基于Redis检查Idempotency-Key
 */
@Slf4j
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";
    private static final String HEADER_NAME = "Idempotency-Key";

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (idempotent == null) {
            return true;
        }

        // 仅拦截写操作
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)
                && !"DELETE".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method)) {
            return true;
        }

        String idempotencyKey = request.getHeader(HEADER_NAME);
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            response.setStatus(400);
            writeJson(response, Map.of(
                "code", 400,
                "errorCode", "400",
                "message", "缺少Idempotency-Key请求头",
                "data", null
            ));
            return false;
        }

        String redisKey = IDEMPOTENT_KEY_PREFIX + request.getRequestURI() + ":" + idempotencyKey;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", idempotent.ttlSeconds(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(success)) {
            response.setStatus(409);
            writeJson(response, Map.of(
                "code", 409,
                "errorCode", "409",
                "message", idempotent.message(),
                "data", null
            ));
            log.warn("幂等性拦截：重复请求 — key={}", idempotencyKey);
            return false;
        }

        return true;
    }

    private void writeJson(HttpServletResponse response, Map<String, Object> body) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
