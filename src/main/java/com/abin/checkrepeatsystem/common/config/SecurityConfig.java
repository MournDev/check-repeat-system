package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.jwt.JwtAuthenticationEntryPoint;
import com.abin.checkrepeatsystem.common.jwt.JwtAuthenticationFilter;
import com.abin.checkrepeatsystem.user.service.Impl.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置：权限控制、JWT集成、安全过滤
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 启用方法级别的权限控制
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
        String allowedOrigins = System.getenv().getOrDefault("CORS_ALLOWED_ORIGINS",
            "http://localhost:3000,http://127.0.0.1:3000");
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
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
                        .xssProtection(xss -> xss.disable())
                        .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                )
                // 配置URL权限规则
                .authorizeHttpRequests(auth -> auth
                        // 放行公开接口
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/avatar/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasAuthority("ADMIN")
                        // WebSocket连接路径放行
                        .requestMatchers("/ws/**").permitAll()
                        // 预览文件访问接口放行（通过临时令牌验证）
                        .requestMatchers("/api/file/preview/**").permitAll()
                        // 智能预览接口放行（在iframe中加载，无法携带Auth header）
                        .requestMatchers("/api/file/smartPreview").permitAll()
                        .requestMatchers("/api/file/smartPreviewReport").permitAll()
                        // 预览API接口放行（获取预览信息不需要认证）
                        .requestMatchers("/api/preview/**").permitAll()
                        // 报告下载接口放行（kkFileView回调，通过报告ID访问）
                        .requestMatchers("/api/file/downloadReport/**").permitAll()
                        // MinIO 接口需要认证
                        .requestMatchers("/api/minio/**").authenticated()
                        // 文件下载接口需要认证
                        .requestMatchers("/api/file/download/**").authenticated()
                        .requestMatchers("/api/file/download/export").authenticated()
                        // 学生接口：允许学生、教师和管理员访问
                        .requestMatchers("/api/student/check-tasks/taskDetail").hasAnyAuthority("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers("/api/student/dashboard/advisor").hasAnyAuthority("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers("/api/student/reports/list").hasAnyAuthority("STUDENT", "TEACHER", "ADMIN")
                        // 查重报告接口：允许学生、教师和管理员访问（根据论文ID验证权限）
                        .requestMatchers("/api/student/check-report/**").hasAnyAuthority("STUDENT", "TEACHER", "ADMIN")
                        // 其他学生接口：仅学生角色可访问
                        .requestMatchers("/api/student/**").hasAuthority("STUDENT")
                        // 教师接口：仅教师角色可访问
                        .requestMatchers("/api/teacher/**").hasAuthority("TEACHER")
                        // 管理员接口：仅管理员角色可访问
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        // 知识库公开接口（帮助中心，无需登录）
                        .requestMatchers(HttpMethod.GET, "/api/knowledge/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/knowledge/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/knowledge/articles/popular").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/knowledge/articles/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/knowledge/search").permitAll()
                        // 知识库管理接口（仅管理员）
                        .requestMatchers("/api/knowledge/admin/**").hasAuthority("ADMIN")
                        // 其他接口需认证
                        .anyRequest().authenticated()
                )
                // 注册认证提供者
                .authenticationProvider(authenticationProvider())
                // 在UsernamePasswordAuthenticationFilter之前添加JWT认证过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
