package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.student.service.StudentMessageService;
import com.abin.checkrepeatsystem.student.vo.AdvisorInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生导师信息控制器
 * 提供学生获取导师相关信息的接口
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/student/advisor")
@Tag(name = "学生导师接口", description = "学生获取导师相关信息接口")
public class StudentAdvisorController {

    private final StudentMessageService studentMessageService;

    /**
     * 获取导师信息
     * GET /api/student/advisor/info
     */
    @GetMapping("/info")
    @Operation(summary = "获取导师信息")
    public Result<AdvisorInfoVO> getAdvisorInfo() {
        Long studentId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("获取导师信息 - 学生ID: {}", studentId);

        AdvisorInfoVO advisorInfo = studentMessageService.getAdvisorInfo(studentId);
        return Result.success("获取导师信息成功", advisorInfo);
    }
}