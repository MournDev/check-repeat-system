package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.CheckRule;
import com.abin.checkrepeatsystem.student.service.CheckRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/check-rules")
public class CheckRuleController {

    private final CheckRuleService checkRuleService;

    /**
     * 获取所有查重规则
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<List<CheckRule>> getRuleList() {
        List<CheckRule> rules = checkRuleService.getAllRules();
        return Result.success(rules);
    }

    /**
     * 获取默认规则
     */
    @GetMapping("/default")
    @PreAuthorize("hasAnyAuthority('STUDENT','TEACHER','ADMIN')")
    public Result<CheckRule> getDefaultRule() {
        CheckRule rule = checkRuleService.getDefaultRule();
        if (rule == null) {
            return Result.error(ResultCode.SYSTEM_ERROR, "默认规则不存在");
        }
        return Result.success(rule);
    }

    /**
     * 根据规则编码获取规则
     */
    @GetMapping("/code/{ruleCode}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'TEACHER')")
    public Result<CheckRule> getRuleByCode(@PathVariable String ruleCode) {
        CheckRule rule = checkRuleService.getRuleByCode(ruleCode);
        if (rule == null) {
            return Result.error(ResultCode.SYSTEM_ERROR, "规则不存在");
        }
        return Result.success(rule);
    }

    /**
     * 创建查重规则
     */
    @PostMapping("/create")
    @OperationLog(type = "check_rule_create", description = "创建查重规则", recordResult = true)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public Result<CheckRule> createRule(@RequestBody CheckRule rule) {
        boolean success = checkRuleService.createRule(rule);
        if (success) {
            return Result.success("创建规则成功", rule);
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "创建规则失败");
        }
    }

    /**
     * 更新查重规则
     */
    @PutMapping("/update")
    @OperationLog(type = "check_rule_update", description = "更新查重规则", recordResult = true)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public Result<CheckRule> updateRule(@RequestBody CheckRule rule) {
        boolean success = checkRuleService.updateRule(rule);
        if (success) {
            return Result.success("更新规则成功", rule);
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "更新规则失败");
        }
    }

    /**
     * 删除查重规则
     */
    @DeleteMapping("/delete/{id}")
    @OperationLog(type = "check_rule_delete", description = "删除查重规则", recordResult = true)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public Result<String> deleteRule(@PathVariable Long id) {
        boolean success = checkRuleService.deleteRule(id);
        if (success) {
            return Result.success("删除规则成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "删除规则失败，默认规则无法删除");
        }
    }

    /**
     * 设置默认规则
     */
    @PutMapping("/set-default/{id}")
    @OperationLog(type = "check_rule_set_default", description = "设置默认查重规则", recordResult = true)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public Result<String> setDefaultRule(@PathVariable Long id) {
        boolean success = checkRuleService.setDefaultRule(id);
        if (success) {
            return Result.success("设置默认规则成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "设置默认规则失败");
        }
    }

    /**
     * 启用/禁用规则
     */
    @PutMapping("/status/{id}")
    @OperationLog(type = "check_rule_status", description = "启用/禁用查重规则", recordResult = true)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public Result<String> toggleRuleStatus(@PathVariable Long id, @RequestParam Integer enabled) {
        boolean success = checkRuleService.toggleRuleStatus(id, enabled);
        if (success) {
            return Result.success(enabled == 1 ? "启用规则成功" : "禁用规则成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "操作失败，默认规则无法禁用");
        }
    }
}
