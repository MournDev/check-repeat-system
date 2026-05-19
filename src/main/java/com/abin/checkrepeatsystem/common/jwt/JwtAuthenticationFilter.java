package com.abin.checkrepeatsystem.common.jwt;

import com.abin.checkrepeatsystem.user.service.Impl.UserDetailsServiceImpl;
import com.abin.checkrepeatsystem.common.utils.JwtUtils;
import jakarta.annotation.Resource;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器：拦截请求，解析令牌并设置认证信息到Security上下文
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private UserDetailsServiceImpl userDetailsService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

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



            // 2. 放行登录、注册、刷新令牌接口、Actuator监控接口和预览文件接口
            // 使用去除上下文后的路径进行匹配
            boolean isPreviewFile = pathWithoutContext.startsWith("/api/file/preview/");
            boolean isPreviewApi = pathWithoutContext.startsWith("/api/preview/");
            
            if (pathWithoutContext.startsWith("/api/auth/login")
                    || pathWithoutContext.startsWith("/api/auth/register")
                    || pathWithoutContext.startsWith("/api/auth/refresh-token")
                    || pathWithoutContext.startsWith("/api/auth/forgot-password")
                    || pathWithoutContext.startsWith("/actuator")
                    || isPreviewFile
                    || isPreviewApi
            ) {
                log.debug("放行路径: {}", requestUri);
                // 直接放行，不进行JWT认证
                filterChain.doFilter(request, response);
                return;
            }
            // 1. 从请求头获取JWT令牌
            String jwt = parseJwt(request);
            // 2. 检查令牌是否在黑名单中（已注销）
            if (jwt != null && redisTemplate.opsForValue().get("token_blacklist:" + jwt) != null) {
                log.debug("令牌已被注销，拒绝访问");
                filterChain.doFilter(request, response);
                return;
            }
            // 3. 验证令牌有效性
            if (jwt != null && !jwtUtils.isTokenExpired(jwt)) {
                // 3. 从令牌中提取用户名
                String username = jwtUtils.extractUsername(jwt);
                // 4. 从用户详情服务获取用户信息
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                // 5. 构建认证令牌并设置到Security上下文
                // 登录成功后设置认证信息
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