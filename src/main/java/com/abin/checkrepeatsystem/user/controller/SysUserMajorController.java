package com.abin.checkrepeatsystem.user.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.Major;
import com.abin.checkrepeatsystem.user.service.SysUserMajorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;



/**
 * 用户-专业关联控制器（指导任务上限相关接口）
 */
@RestController
@RequestMapping("/api/v1/sys/user-major")
@Tag(name = "指导任务上限管理")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class SysUserMajorController {

    private final SysUserMajorService sysUserMajorService;

    /**
     * 修改指导任务上限接口（仅管理员可调用）
     * @param operatorId 当前登录用户ID（由拦截器从Token中解析，放入RequestAttribute）
     * @param targetUserId 目标教师ID
     * @param majorId 专业ID
     * @param newMaxCount 新上限（1-50）
     * @return 结果VO
     */
    @PostMapping("/modify-advisor-max-count")
    @Operation(summary = "修改指导任务上限", description = "仅管理员可操作，新上限需在1-50之间")
    public Result<Major> modifyAdvisorMaxCount(
            // 从RequestAttribute获取当前登录用户ID（避免前端传参篡改）
            @RequestAttribute("loginUserId") Long operatorId,
            @Parameter(description = "目标教师ID", required = true) @RequestParam @NotNull Long targetUserId,
            @Parameter(description = "专业ID", required = true) @RequestParam @NotNull Long majorId,
            @Parameter(description = "新指导任务上限（1-50）", required = true)
            @RequestParam @NotNull @Min(1) @Max(50) Integer newMaxCount
    ) {
        Major result = sysUserMajorService.modifyAdvisorMaxCount(
                operatorId, targetUserId, majorId, newMaxCount
        );
        return Result.success("指导任务上限修改成功", result);
    }
}
