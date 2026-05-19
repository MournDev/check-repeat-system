package com.abin.checkrepeatsystem.user.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.dto.ForgotPasswordReq;
import com.abin.checkrepeatsystem.pojo.dto.LoginReq;
import com.abin.checkrepeatsystem.pojo.dto.RefreshTokenReq;
import com.abin.checkrepeatsystem.pojo.dto.RegisterReq;
import com.abin.checkrepeatsystem.user.dto.UpdateUserInfoReq;
import com.abin.checkrepeatsystem.user.service.AuthService;
import com.abin.checkrepeatsystem.user.vo.LoginVO;
import com.abin.checkrepeatsystem.user.vo.RefreshTokenVO;
import jakarta.annotation.Resource;
import com.abin.checkrepeatsystem.common.annotation.Idempotent;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.annotation.RateLimit;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器（仅负责请求接收与响应，无业务逻辑）
 */
@Slf4j
@RestController
@RequestMapping("/api/auth") // 接口前缀，便于版本管理（后续可扩展为 /api/v1/auth）
public class AuthController {

    // 仅依赖Service接口，不依赖具体实现（符合依赖倒置原则）
    @Resource
    private AuthService authService;

    /**
     * 用户注册接口（请求→Service→响应）
     */
    @PostMapping("/register")
    @OperationLog(type = "user_register", description = "用户注册")
    @Idempotent(message = "请勿重复提交注册请求")
    public Result<String> register(@Valid @RequestBody RegisterReq registerReq) {
        log.info("接收用户注册请求：用户名={}，角色ID={}", registerReq.getUsername(), registerReq.getRoleId());
        return authService.register(registerReq);
    }

    /**
     * 用户登录接口（请求→Service→响应）
     */
    @PostMapping("/login")
    @OperationLog(type = "user_login", description = "用户登录", recordParams = false)
    @RateLimit(maxRequests = 10, windowSeconds = 60, message = "登录过于频繁，请60秒后重试")
    public Result<LoginVO> login(@Valid @RequestBody LoginReq loginReq) {
        log.info("接收用户登录请求：用户名={}", loginReq.getUsername());
        return authService.login(loginReq);
    }

    /**
     * 令牌刷新接口（请求→Service→响应）
     */
    @PostMapping("/refresh-token")
    public Result<RefreshTokenVO> refreshToken(@Valid @RequestBody RefreshTokenReq refreshTokenReq) {
        log.info("接收令牌刷新请求：旧令牌={}", refreshTokenReq.getOldToken().substring(0, 20) + "..."); // 隐藏部分令牌，避免日志泄露
        return authService.refreshToken(refreshTokenReq);
    }

    /**
     * 用户退出登录接口（请求→Service→响应）
     */
    @PostMapping("/logout")
    @OperationLog(type = "user_logout", description = "用户退出登录")
    public Result<String> logout() {
        log.info("接收用户退出登录请求");
        return authService.logout();
    }
    /**
     * 发送密码重置验证码（发送到注册邮箱）
     */
    @PostMapping("/send-password-reset-code")
    public Result<String> sendPasswordResetCode(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        log.info("接收发送密码重置验证码请求：用户名={}", username);
        return authService.sendPasswordResetCode(username);
    }

    /**
     * 忘记密码接口（通过验证码+用户名+邮箱验证身份后重置密码）
     */
    @PostMapping("/forgot-password")
    @RateLimit(maxRequests = 3, windowSeconds = 300, message = "重置密码请求过于频繁，请5分钟后重试")
    @Idempotent(message = "请勿重复提交重置密码请求")
    public Result<String> forgotPassword(@Valid @RequestBody ForgotPasswordReq forgotPasswordReq) {
        log.info("接收忘记密码请求：用户名={}", forgotPasswordReq.getUsername());
        return authService.forgotPassword(forgotPasswordReq);
    }

}