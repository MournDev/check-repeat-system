package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.RateLimit;
import com.abin.checkrepeatsystem.student.service.CheckTaskService;
import com.abin.checkrepeatsystem.user.vo.CheckResultVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/check")
@PreAuthorize("hasAuthority('STUDENT')")
@RequiredArgsConstructor
public class CheckTaskController {
    private final CheckTaskService checkTaskService;

    /**
     * 创建查重任务
     */
    @PostMapping("/create")
    @RateLimit(maxRequests = 5, windowSeconds = 60, message = "查重请求过于频繁，请60秒后重试")
    public Result<CheckResultVO> createCheckTask(@RequestParam Long paperId) {
        return checkTaskService.createCheckTask(paperId);
    }

    /**
     * 查询查重结果
     */
    @GetMapping("/result")
    public Result<CheckResultVO> getCheckResult(@RequestParam Long paperId) {
        CheckResultVO resultVO = checkTaskService.getCheckResult(paperId);
        return Result.success("查询成功", resultVO);
    }
}
