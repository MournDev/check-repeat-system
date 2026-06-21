package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.admin.service.AlertRuleService;
import com.abin.checkrepeatsystem.admin.service.AlertService;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.dto.AlertRuleBatchSaveReq;
import com.abin.checkrepeatsystem.pojo.dto.AlertRuleDTO;
import com.abin.checkrepeatsystem.pojo.dto.HandleAlertReq;
import com.abin.checkrepeatsystem.pojo.entity.AlertRecord;
import com.abin.checkrepeatsystem.pojo.entity.AlertRule;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/alerts")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertRuleService alertRuleService;

    private final AlertService alertService;

    @GetMapping("/config")
    public Result<Map<String, Object>> getAlertConfig(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageResult = alertRuleService.listPage(page, size);
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", pageResult.getTotal() > 0);
        config.put("rules", pageResult.getRecords());
        config.put("total", pageResult.getTotal());
        config.put("page", pageResult.getCurrent());
        config.put("size", pageResult.getSize());
        log.info("获取告警配置成功, 共{}条", pageResult.getTotal());
        return Result.success("告警配置获取成功", config);
    }

    @GetMapping("/config/{id}")
    public Result<AlertRule> getAlertRule(@PathVariable Long id) {
        AlertRule rule = alertRuleService.getById(id);
        if (rule == null) {
            return Result.error(404, "告警规则不存在");
        }
        return Result.success("获取告警规则成功", rule);
    }

    @PostMapping("/config")
    public Result<AlertRule> createAlertRule(@Valid @RequestBody AlertRuleDTO dto) {
        AlertRule rule = new AlertRule();
        rule.setRuleName(dto.getRuleName());
        rule.setRuleType(dto.getRuleType());
        rule.setMetricName(dto.getMetricName());
        rule.setThreshold(dto.getThreshold());
        rule.setDuration(dto.getDuration());
        rule.setSeverity(dto.getSeverity());
        rule.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        rule.setNotifyEmail(dto.getNotifyEmail());
        rule.setDescription(dto.getDescription());
        AlertRule created = alertRuleService.create(rule);
        return Result.success("告警规则创建成功", created);
    }

    @PutMapping("/config/{id}")
    public Result<AlertRule> updateAlertRule(@PathVariable Long id, @Valid @RequestBody AlertRuleDTO dto) {
        AlertRule rule = new AlertRule();
        rule.setRuleName(dto.getRuleName());
        rule.setRuleType(dto.getRuleType());
        rule.setMetricName(dto.getMetricName());
        rule.setThreshold(dto.getThreshold());
        rule.setDuration(dto.getDuration());
        rule.setSeverity(dto.getSeverity());
        rule.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        rule.setNotifyEmail(dto.getNotifyEmail());
        rule.setDescription(dto.getDescription());
        AlertRule updated = alertRuleService.update(id, rule);
        if (updated == null) {
            return Result.error(404, "告警规则不存在");
        }
        return Result.success("告警规则更新成功", updated);
    }

    @PutMapping("/config")
    public Result<String> updateAlertConfig(@Valid @RequestBody AlertRuleBatchSaveReq config) {
        List<AlertRule> rules = config.getRules().stream().map(dto -> {
            AlertRule rule = new AlertRule();
            rule.setId(dto.getId());
            rule.setRuleName(dto.getRuleName());
            rule.setRuleType(dto.getRuleType());
            rule.setMetricName(dto.getMetricName());
            rule.setThreshold(dto.getThreshold());
            rule.setDuration(dto.getDuration());
            rule.setSeverity(dto.getSeverity());
            rule.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
            rule.setNotifyEmail(dto.getNotifyEmail());
            rule.setDescription(dto.getDescription());
            return rule;
        }).toList();
        int count = alertRuleService.saveBatch(rules);
        log.info("告警配置更新成功, 共{}条", count);
        return Result.success("告警配置更新成功, 共" + count + "条");
    }

    @GetMapping("/active")
    public Result<List<AlertRecord>> getActiveAlerts() {
        List<AlertRecord> alerts = alertService.listActive();
        log.info("获取活跃告警成功, 共{}条", alerts.size());
        return Result.success("活跃告警获取成功", alerts);
    }

    @DeleteMapping("/config/{id}")
    public Result<String> deleteAlertRule(@PathVariable Long id) {
        boolean deleted = alertRuleService.delete(id);
        if (deleted) {
            log.info("删除告警规则成功: id={}", id);
            return Result.success("告警规则删除成功");
        }
        return Result.error(404, "告警规则不存在");
    }

    @PutMapping("/config/{id}/toggle")
    public Result<String> toggleAlertRule(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        boolean success = alertRuleService.toggleEnabled(id, enabled);
        if (success) {
            log.info("切换告警规则状态: id={}, enabled={}", id, enabled);
            return Result.success(enabled ? "告警规则已启用" : "告警规则已禁用");
        }
        return Result.error(404, "告警规则不存在");
    }

    @PostMapping("/handle")
    public Result<String> handleAlert(@Valid @RequestBody HandleAlertReq req) {
        if ("resolve".equals(req.getAction())) {
            alertService.resolve(req.getAlertId(), "admin");
        } else if ("dismiss".equals(req.getAction())) {
            alertService.dismiss(req.getAlertId(), "admin");
        } else {
            return Result.error(400, "不支持的操作: " + req.getAction());
        }

        log.info("预警处理成功: alertId={}, action={}, remark={}", req.getAlertId(), req.getAction(), req.getRemark());
        return Result.success("预警处理成功");
    }
}