package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserContextHolder;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.teacher.dto.UpdateTeacherInfoReq;
import com.abin.checkrepeatsystem.teacher.service.TeacherInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 教师个人信息控制器
 */
@Slf4j
@RestController
@PreAuthorize("hasAuthority('TEACHER')")
@RequiredArgsConstructor
@RequestMapping("/api/v1/teacher/info")
public class TeacherController {

    private final TeacherInfoService teacherService;

    /**
     * 教师信息更新
     *
     * @param updateReq
     * @return
     */
    @OperationLog(type = "teacher_update_info", description = "教师更新个人信息", recordResult = true)
    @PostMapping("/update")
    public Result<String> updateInfo(@Valid @RequestBody UpdateTeacherInfoReq updateReq) {
        Long currentUserId = UserContextHolder.getUserId();
        if (!currentUserId.equals(updateReq.getUserId())) {
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "只能修改自己的信息");
        }
        log.info("接收教师信息更新请求：用户ID={}", updateReq.getUserId());
        teacherService.updateInfo(updateReq);
        return Result.success("信息更新成功");
    }

    /**
     * 获取教师信息（只能查看自己的信息）
     */
    @OperationLog(type = "teacher_get_info", description = "获取教师信息")
    @GetMapping("/get")
    public Result<UpdateTeacherInfoReq> getInfo() {
        Long currentUserId = UserContextHolder.getUserId();
        log.info("接收获取教师信息请求：用户ID={}", currentUserId);
        return teacherService.getInfoByUserId(currentUserId);
    }

    /**
     * 教师修改密码（需验证旧密码，只能修改自己的密码）
     */
    @OperationLog(type = "teacher_change_password", description = "教师修改密码", recordResult = true)
    @PostMapping("/changePassword")
    public Result<String> changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        Long currentUserId = UserContextHolder.getUserId();
        log.info("接收教师修改密码请求：用户ID={}", currentUserId);
        return teacherService.changePasswordByUserId(currentUserId, oldPassword, newPassword);
    }
}
