package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.filter.MaintenanceFilter;
import com.abin.checkrepeatsystem.common.jwt.JwtAuthenticationEntryPoint;
import com.abin.checkrepeatsystem.common.jwt.JwtAuthenticationFilter;
import com.abin.checkrepeatsystem.user.service.Impl.UserDetailsServiceImpl;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Spring Security配置：权限控制、JWT集成、安全过滤
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 启用方法级别的权限控制
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;

    private final JwtAuthenticationEntryPoint unauthorizedHandler;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final MaintenanceFilter maintenanceFilter;

    @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 密码编码器（BCrypt加密）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证提供者：关联用户详情服务与密码编码器
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 认证管理器：处理认证请求
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * CORS配置：允许跨域请求
     * 生产环境通过环境变量 CORS_ALLOWED_ORIGINS 指定允许的域名（逗号分隔）
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 开发环境默认允许本地前端，生产环境通过环境变量指定
        String envOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        String allowedOrigins;
        if (envOrigins != null && !envOrigins.isBlank()) {
            allowedOrigins = envOrigins;
        } else {
            allowedOrigins = "http://localhost:3000,http://localhost:5173";
            if ("prod".equals(activeProfile)) {
                log.warn("CORS_ALLOWED_ORIGINS 环境变量未配置，生产环境默认允许 localhost 跨域。"
                        + "请设置环境变量 CORS_ALLOWED_ORIGINS 为实际前端域名（逗号分隔）。");
            }
        }
        config.setAllowedOriginPatterns(
                java.util.Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toList())
        );
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 安全过滤链：配置URL权限、会话策略、异常处理
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 关闭CSRF（前后端分离项目无需CSRF保护）
                .csrf(csrf -> csrf.disable())
                // 配置CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 配置未认证请求的异常处理器
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                // 配置会话策略：无状态（JWT认证无需会话）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 允许iframe嵌入预览页面（禁用默认的 X-Frame-Options: DENY）
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                )
                // 配置URL权限规则
                .authorizeHttpRequests(auth -> auth
                        // 放行公开接口
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/avatar/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasAuthority("SUPER_ADMIN")
                        // WebSocket连接路径放行
                        .requestMatchers("/ws/**").permitAll()
                        // 预览文件访问接口放行（通过临时令牌验证）
                        .requestMatchers("/api/v1/file/preview/**").permitAll()
                        // 智能预览接口放行（iframe加载，通过临时预览令牌验证）
                        .requestMatchers("/api/v1/file/smartPreview").permitAll()
                        .requestMatchers("/api/v1/file/smartPreviewReport").permitAll()
                        // 预览API接口放行（获取预览信息不需要认证）
                        .requestMatchers("/api/v1/preview/**").permitAll()
                        // 报告下载接口需要认证
                        .requestMatchers("/api/v1/file/downloadReport/**").authenticated()
                        // MinIO 接口需要认证
                        .requestMatchers("/api/v1/minio/**").authenticated()
                        // 文件下载接口需要认证
                        .requestMatchers("/api/v1/file/download/**").authenticated()
                        .requestMatchers("/api/v1/file/download/export").authenticated()
                        // 学生接口：允许学生、教师和管理员访问
                        .requestMatchers("/api/v1/student/check-tasks/taskDetail").hasAnyAuthority("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers("/api/v1/student/dashboard/advisor").hasAnyAuthority("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers("/api/v1/student/reports/list").hasAnyAuthority("STUDENT", "TEACHER", "ADMIN")
                        // 查重报告接口：允许学生、教师和管理员访问（根据论文ID验证权限）
                        .requestMatchers("/api/v1/student/check-report/**").hasAnyAuthority("STUDENT", "TEACHER", "ADMIN")
                        // 其他学生接口：仅学生角色可访问
                        .requestMatchers("/api/v1/student/**").hasAuthority("STUDENT")
                        // 教师接口：教师和超级管理员可访问
                        .requestMatchers("/api/v1/teacher/**").hasAnyAuthority("TEACHER", "SUPER_ADMIN")
                        // 教师分配操作接口：教师和超级管理员可访问
                        .requestMatchers("/api/v1/assignment/**").hasAnyAuthority("TEACHER", "SUPER_ADMIN")
                        // 管理员接口：管理员和超级管理员可访问（细粒度控制由@PreAuthorize注解处理）
                        .requestMatchers("/api/v1/admin/**").hasAnyAuthority("ADMIN", "SUPER_ADMIN")
                        // 知识库公开接口（帮助中心，无需登录）
                        .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/articles/popular").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/articles/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/search").permitAll()
                        // 知识库管理接口（仅管理员）
                        .requestMatchers("/api/v1/knowledge/admin/**").hasAuthority("ADMIN")
                        // 其他接口需认证
                        .anyRequest().authenticated()
                )
                // 注册认证提供者
                .authenticationProvider(authenticationProvider())
                // 维护模式过滤器（在JWT认证之前，解析JWT判断管理员身份）
                .addFilterBefore(maintenanceFilter, UsernamePasswordAuthenticationFilter.class)
                // 在UsernamePasswordAuthenticationFilter之前添加JWT认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
