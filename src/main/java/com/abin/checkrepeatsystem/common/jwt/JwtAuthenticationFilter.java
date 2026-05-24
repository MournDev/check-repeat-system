package com.abin.checkrepeatsystem.common.jwt;

import com.alibaba.fastjson2.JSON;
import com.abin.checkrepeatsystem.user.service.Impl.UserDetailsServiceImpl;
import com.abin.checkrepeatsystem.common.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWT认证过滤器：拦截请求，解析令牌并设置认证信息到Security上下文
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    private final UserDetailsServiceImpl userDetailsService;

    private final RedisTemplate<String, String> redisTemplate;

    // 从配置文件获取JWT请求头与前缀
    @Value("${jwt.token-header:Authorization}")
    private String tokenHeader;

    @Value("${jwt.token-prefix:Bearer }")
    private String tokenPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. 获取请求URI（包含上下文路径）
            String requestUri = request.getRequestURI();
            String contextPath = request.getContextPath();

            // 获取去除上下文路径后的路径
            String pathWithoutContext = requestUri.startsWith(contextPath)
                    ? requestUri.substring(contextPath.length())
                    : requestUri;



            // 2. 放行登录、注册、忘记密码接口、Actuator监控接口和预览文件接口
            boolean isPreviewFile = pathWithoutContext.startsWith("/api/v1/file/preview/");
            boolean isPreviewApi = pathWithoutContext.startsWith("/api/v1/preview/");
            boolean isRefreshToken = pathWithoutContext.startsWith("/api/v1/auth/refresh-token");

            if (pathWithoutContext.startsWith("/api/v1/auth/login")
                    || pathWithoutContext.startsWith("/api/v1/auth/register")
                    || pathWithoutContext.startsWith("/api/v1/auth/forgot-password")
                    || pathWithoutContext.startsWith("/actuator")
                    || isPreviewFile
                    || isPreviewApi
            ) {
                log.debug("放行路径: {}", requestUri);
                filterChain.doFilter(request, response);
                return;
            }

            // 对refresh-token端点：解析header中的JWT并检查黑名单，但不强制要求认证
            if (isRefreshToken) {
                String jwt = parseJwt(request);
                if (jwt != null && Boolean.TRUE.equals(redisTemplate.hasKey("token_blacklist:" + jwt))) {
                    log.warn("refresh-token请求使用的令牌已在黑名单中，拒绝访问");
                    writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "令牌已失效，请重新登录");
                    return;
                }
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 从请求头获取JWT令牌
            String jwt = parseJwt(request);
            // 4. 检查令牌是否在黑名单中（已注销）
            if (jwt != null && Boolean.TRUE.equals(redisTemplate.hasKey("token_blacklist:" + jwt))) {
                log.debug("令牌已被注销，拒绝访问");
                filterChain.doFilter(request, response);
                return;
            }
            // 5. 验证令牌有效性
            if (jwt != null && !jwtUtils.isTokenExpired(jwt)) {
                // 从令牌中提取用户名
                String username = jwtUtils.extractUsername(jwt);
                // 从用户详情服务获取用户信息
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                // 构建认证令牌并设置到Security上下文
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("无法设置用户认证信息: {}", e);
        }

        // 继续执行过滤链
        filterChain.doFilter(request, response);
    }

    private void writeJsonError(HttpServletResponse response, int httpStatus, int errorCode, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", httpStatus);
        body.put("errorCode", errorCode);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(JSON.toJSONString(body));
    }

    /**
     * 从请求头解析JWT令牌（去除前缀）
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(tokenHeader);
        if (StringUtils.hasText(headerAuth)) {
            // 移除Bearer前缀（如果存在）
            if (headerAuth.startsWith(tokenPrefix)) {
                headerAuth = headerAuth.substring(tokenPrefix.length());
            }
            return headerAuth;
        }
        return null;
    }
}