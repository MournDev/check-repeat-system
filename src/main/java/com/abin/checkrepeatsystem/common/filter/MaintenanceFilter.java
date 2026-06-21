package com.abin.checkrepeatsystem.common.filter;

import com.abin.checkrepeatsystem.admin.mapper.SystemParamMapper;
import com.abin.checkrepeatsystem.common.utils.JwtUtils;
import com.abin.checkrepeatsystem.pojo.entity.SystemParam;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 维护模式过滤器：系统维护时只允许管理员和公开路径访问
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class MaintenanceFilter extends OncePerRequestFilter {

    private final SystemParamMapper systemParamMapper;
    private final JwtUtils jwtUtils;

    private volatile long lastCheckTime = 0;
    private volatile boolean cachedMaintenanceMode = false;
    private volatile String cachedMaintenanceNotice = "系统维护中，请稍后再试...";
    private static final long CACHE_TTL_MS = 30_000;

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/v1/auth/",
            "/actuator/health",
            "/ws/",
            "/api/v1/file/preview/",
            "/api/v1/preview/"
    );

    private static final Set<String> PUBLIC_GET_PATHS = Set.of(
            "/api/v1/knowledge/categories",
            "/api/v1/knowledge/articles",
            "/api/v1/knowledge/articles/popular",
            "/api/v1/knowledge/search"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException {
        refreshCache();

        if (!cachedMaintenanceMode) {
            try {
                filterChain.doFilter(request, response);
            } catch (jakarta.servlet.ServletException e) {
                throw new IOException(e);
            }
            return;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        String pathWithoutContext = path.startsWith(contextPath) ? path.substring(contextPath.length()) : path;

        // 公开路径放行
        if (isPublicPath(pathWithoutContext, request.getMethod())) {
            try {
                filterChain.doFilter(request, response);
            } catch (jakarta.servlet.ServletException e) {
                throw new IOException(e);
            }
            return;
        }

        // 检查管理员
        String token = extractToken(request);
        if (token != null) {
            try {
                String roleCode = jwtUtils.extractRoleCode(token);
                if (("ADMIN".equals(roleCode) || "SUPER_ADMIN".equals(roleCode)) && !jwtUtils.isTokenExpired(token)) {
                    try {
                        filterChain.doFilter(request, response);
                    } catch (jakarta.servlet.ServletException e) {
                        throw new IOException(e);
                    }
                    return;
                }
            } catch (Exception e) {
                log.debug("维护模式解析JWT失败: {}", e.getMessage());
            }
        }

        // 拒绝访问
        log.info("维护模式拒绝访问: {} {}", request.getMethod(), pathWithoutContext);
        writeMaintenanceResponse(response);
    }

    private boolean isPublicPath(String path, String method) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        if ("GET".equalsIgnoreCase(method)) {
            for (String p : PUBLIC_GET_PATHS) {
                if (path.equals(p) || path.startsWith(p + "/")) return true;
            }
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void writeMaintenanceResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 503);
        body.put("errorCode", 503);
        body.put("message", cachedMaintenanceNotice);
        body.put("data", null);
        response.getWriter().write(JSON.toJSONString(body));
    }

    private void refreshCache() {
        long now = System.currentTimeMillis();
        if (now - lastCheckTime > CACHE_TTL_MS) {
            synchronized (this) {
                if (now - lastCheckTime > CACHE_TTL_MS) {
                    try {
                        SystemParam sp = systemParamMapper.selectOne(
                                new LambdaQueryWrapper<SystemParam>()
                                        .eq(SystemParam::getIsDeleted, 0)
                                        .last("LIMIT 1"));
                        cachedMaintenanceMode = sp != null
                                && sp.getMaintenanceStatus() != null
                                && sp.getMaintenanceStatus() == 1;
                        if (sp != null && sp.getMaintenanceNotice() != null
                                && !sp.getMaintenanceNotice().isBlank()) {
                            cachedMaintenanceNotice = sp.getMaintenanceNotice();
                        } else {
                            cachedMaintenanceNotice = "系统维护中，请稍后再试...";
                        }
                    } catch (Exception e) {
                        log.error("读取维护状态失败: {}", e.getMessage());
                        cachedMaintenanceMode = false;
                    }
                    lastCheckTime = now;
                }
            }
        }
    }
}
