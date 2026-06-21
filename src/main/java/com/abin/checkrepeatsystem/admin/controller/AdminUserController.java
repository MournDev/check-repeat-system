package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.admin.dto.UserInfoDTO;
import com.abin.checkrepeatsystem.admin.service.AdminUserService;
import com.abin.checkrepeatsystem.admin.vo.UserCreateReq;
import com.abin.checkrepeatsystem.admin.vo.UserUpdateReq;
import com.abin.checkrepeatsystem.admin.vo.BatchDeleteReq;
import com.abin.checkrepeatsystem.admin.vo.ResetPasswordReq;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.pojo.entity.SysLoginLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Map;
import lombok.RequiredArgsConstructor;


/**
 * 管理员用户管理控制器
 * 职责：接收HTTP请求，参数校验，调用服务层，返回响应结果
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/list")
    public Result<Page<UserInfoDTO>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        log.info("接收获取用户列表请求: page={}, size={}, userType={}, status={}, keyword={}",
                page, size, userType, status, keyword);
        return adminUserService.getUserList(page, size, userType, status, keyword);
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PostMapping("/create")
    @OperationLog(type = "admin_user_create", description = "管理员创建用户", recordResult = true)
    public Result<Map<String, Object>> createUser(@Valid @RequestBody UserCreateReq createReq) {
        log.info("接收创建用户请求: username={}", createReq.getUsername());
        return adminUserService.createUser(createReq);
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PutMapping("/{userId}")
    @OperationLog(type = "admin_user_update", description = "管理员更新用户信息")
    public Result<String> updateUser(@PathVariable Long userId,
                                     @Valid @RequestBody UserUpdateReq updateReq) {
        log.info("接收更新用户信息请求: userId={}", userId);
        return adminUserService.updateUser(userId, updateReq);
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @DeleteMapping("/{userId}")
    @OperationLog(type = "admin_user_delete", description = "管理员删除用户")
    public Result<String> deleteUser(@PathVariable Long userId) {
        log.info("接收删除用户请求: userId={}", userId);
        return adminUserService.deleteUser(userId);
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PostMapping("/batch-delete")
    @OperationLog(type = "admin_user_batch_delete", description = "管理员批量删除用户")
    public Result<String> batchDeleteUsers(@Valid @RequestBody BatchDeleteReq batchReq) {
        log.info("接收批量删除用户请求: userIds={}", batchReq.getUserIds());
        return adminUserService.batchDeleteUsers(batchReq);
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PutMapping("/{userId}/status")
    @OperationLog(type = "admin_user_status_update", description = "管理员更新用户状态")
    public Result<String> updateUserStatus(@PathVariable Long userId,
                                           @RequestBody Map<String, Integer> requestBody) {
        Integer status = requestBody.get("status");
        log.info("接收更新用户状态请求: userId={}, status={}", userId, status);
        return adminUserService.updateUserStatus(userId, status);
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PutMapping("/{userId}/reset-password")
    @OperationLog(type = "admin_user_password_reset", description = "管理员重置用户密码")
    public Result<String> resetPassword(@PathVariable Long userId,
                                        @Valid @RequestBody ResetPasswordReq resetReq) {
        log.info("接收重置用户密码请求: userId={}", userId);
        return adminUserService.resetPassword(userId, resetReq);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{userId}")
    public Result<UserInfoDTO> getUserDetail(@PathVariable Long userId) {
        log.info("接收获取用户详细信息请求: userId={}", userId);
        return adminUserService.getUserDetail(userId);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{userId}/login-history")
    public Result<Page<SysLoginLog>> getUserLoginHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("接收获取用户登录历史请求: userId={}, page={}, size={}", userId, page, size);
        return adminUserService.getUserLoginHistory(userId, page, size);
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @GetMapping("/export")
    public void exportUserList(@RequestParam Map<String, Object> params, HttpServletResponse response) {
        log.info("接收导出用户列表请求: params={}", params);
        adminUserService.exportUserList(params, response);
    }
}