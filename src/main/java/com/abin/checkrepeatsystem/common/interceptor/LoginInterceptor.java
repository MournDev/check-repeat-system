package com.abin.checkrepeatsystem.common.interceptor;

import com.alibaba.fastjson2.JSON;
import com.abin.checkrepeatsystem.common.utils.JwtUtils;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.user.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;



/**
 * 登录拦截器：解析Token，获取当前登录用户ID
 */
@RequiredArgsConstructor
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils; // 自定义Jwt工具类，用于解析Token
    private final SysUserService sysUserService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头获取Token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未登录，请先获取Token");
            return false;
        }

        // 2. 移除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 3. 解析Token中的用户ID（JwtUtil需自定义，确保Token未过期、未篡改）
        Long userId;
        try {
            userId = jwtUtils.getUserIdFromToken(token);
        } catch (Exception e) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "Token无效：" + e.getMessage());
            return false;
        }

        // 3. 校验用户是否存在且状态正常
        SysUser user = sysUserService.getById(userId);
        if (user == null || user.getIsDeleted() == 1 || user.getStatus() == 0) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "用户不存在或已禁用");
            return false;
        }

        // 4. 将用户ID放入RequestAttribute，供Controller使用
        request.setAttribute("loginUserId", userId);
        return true;
    }

    private void writeJsonError(HttpServletResponse response, int httpStatus, int errorCode, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", httpStatus);
        body.put("errorCode", errorCode);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(JSON.toJSONString(body));
    }
}
