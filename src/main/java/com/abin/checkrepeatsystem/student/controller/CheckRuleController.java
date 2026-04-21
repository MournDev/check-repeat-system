package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.CheckRule;
import com.abin.checkrepeatsystem.student.service.CheckRuleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/check-rules")
public class CheckRuleController {

    @Resource
    private CheckRuleService checkRuleService;

    /**
     * 获取所有查重规则
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<CheckRule>> getRuleList() {
        try {
            List<CheckRule> rules = checkRuleService.getAllRules();
            return Result.success(rules);
        } catch (Exception e) {
            log.error("获取查重规则列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取查重规则列表失败");
        }
    }

    /**
     * 获取默认规则
     */
    @GetMapping("/default")
    public Result<CheckRule> getDefaultRule() {
        try {
            CheckRule rule = checkRuleService.getDefaultRule();
            if (rule == null) {
                return Result.error(ResultCode.SYSTEM_ERROR, "默认规则不存在");
            }
            return Result.success(rule);
        } catch (Exception e) {
            log.error("获取默认规则失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取默认规则失败");
        }
    }

    /**
     * 根据规则编码获取规则
     */
    @GetMapping("/code/{ruleCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<CheckRule> getRuleByCode(@PathVariable String ruleCode) {
        try {
            CheckRule rule = checkRuleService.getRuleByCode(ruleCode);
            if (rule == null) {
                return Result.error(ResultCode.SYSTEM_ERROR, "规则不存在");
            }
            return Result.success(rule);
        } catch (Exception e) {
            log.error("根据规则编码获取规则失败 - 规则编码: {}", ruleCode, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取规则失败");
        }
    }

    /**
     * 创建查重规则
     */
    @PostMapping("/create")
    @OperationLog(type = "check_rule_create", description = "创建查重规则", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CheckRule> createRule(@RequestBody CheckRule rule) {
        try {
            boolean success = checkRuleService.createRule(rule);
            if (success) {
                return Result.success("创建规则成功", rule);
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "创建规则失败");
            }
        } catch (Exception e) {
            log.error("创建查重规则失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "创建规则失败: " + e.getMessage());
        }
    }

    /**
     * 更新查重规则
     */
    @PutMapping("/update")
    @OperationLog(type = "check_rule_update", description = "更新查重规则", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CheckRule> updateRule(@RequestBody CheckRule rule) {
        try {
            boolean success = checkRuleService.updateRule(rule);
            if (success) {
                return Result.success("更新规则成功", rule);
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "更新规则失败");
            }
        } catch (Exception e) {
            log.error("更新查重规则失败 - 规则ID: {}", rule.getId(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "更新规则失败: " + e.getMessage());
        }
    }

    /**
     * 删除查重规则
     */
    @DeleteMapping("/delete/{id}")
    @OperationLog(type = "check_rule_delete", description = "删除查重规则", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteRule(@PathVariable Long id) {
        try {
            boolean success = checkRuleService.deleteRule(id);
            if (success) {
                return Result.success("删除规则成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "删除规则失败，默认规则无法删除");
            }
        } catch (Exception e) {
            log.error("删除查重规则失败 - 规则ID: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "删除规则失败: " + e.getMessage());
        }
    }

    /**
     * 设置默认规则
     */
    @PutMapping("/set-default/{id}")
    @OperationLog(type = "check_rule_set_default", description = "设置默认查重规则", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> setDefaultRule(@PathVariable Long id) {
        try {
            boolean success = checkRuleService.setDefaultRule(id);
            if (success) {
                return Result.success("设置默认规则成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "设置默认规则失败");
            }
        } catch (Exception e) {
            log.error("设置默认规则失败 - 规则ID: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "设置默认规则失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用规则
     */
    @PutMapping("/status/{id}")
    @OperationLog(type = "check_rule_status", description = "启用/禁用查重规则", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> toggleRuleStatus(@PathVariable Long id, @RequestParam Integer enabled) {
        try {
            boolean success = checkRuleService.toggleRuleStatus(id, enabled);
            if (success) {
                return Result.success(enabled == 1 ? "启用规则成功" : "禁用规则成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "操作失败，默认规则无法禁用");
            }
        } catch (Exception e) {
            log.error("启用/禁用规则失败 - 规则ID: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "操作失败: " + e.getMessage());
        }
    }
}
