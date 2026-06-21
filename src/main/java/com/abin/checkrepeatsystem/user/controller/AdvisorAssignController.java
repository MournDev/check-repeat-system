package com.abin.checkrepeatsystem.user.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.user.service.AdvisorAssignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;



/**
 * 指导老师分配控制器（对外提供分配、任务查询接口）
 */
@RestController
@RequestMapping("/api/v1/advisor/assign")
@Tag(name = "指导老师分配管理")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdvisorAssignController {

    private final AdvisorAssignService advisorAssignService;

    /**
     * 接口1：自动分配指导老师（学生提交论文后调用，或管理员触发）
     */
    @PostMapping("/auto")
    @Operation(summary = "自动分配指导老师", description = "仅'待分配'状态的论文可调用")
    @OperationLog(type = "admin_auto_assign", description = "管理员自动分配指导老师")
    public Result<Boolean> autoAssign(
            @RequestParam @NotNull Long paperSubmitId
    ) {
        return advisorAssignService.autoAssignAdvisor(paperSubmitId);
    }

    /**
     * 接口2：手动分配指导老师（仅管理员可调用）
     */
    @PostMapping("/manual")
    @Operation(summary = "手动分配指导老师", description = "需管理员权限，指定论文ID和目标教师ID")
   @OperationLog(type = "admin_manual_assign", description = "管理员手动分配指导老师")
   public Result< ?> manualAssign(@RequestParam @NotNull Long paperId,
                                  @RequestParam @NotNull Long teacherId,
                                  @RequestParam String reason
    ) {
       return advisorAssignService.manualAssignAdvisor(paperId,teacherId,reason);
   }
}
