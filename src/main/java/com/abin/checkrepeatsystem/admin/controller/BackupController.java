package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.admin.service.DatabaseBackupService;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.SysBackupLog;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "数据库备份管理", description = "手动触发备份、查看备份历史和状态")
@RestController
@RequestMapping("/api/v1/admin/backup")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class BackupController {

    private final DatabaseBackupService backupService;

    @Operation(summary = "手动触发备份")
    @PostMapping("/trigger")
    public Result<Map<String, Object>> triggerBackup() {
        log.info("接收手动备份请求");
        Map<String, Object> result = backupService.performBackup("MANUAL");
        return Result.success(result);
    }

    @Operation(summary = "查询备份历史")
    @GetMapping("/history")
    public Result<Page<SysBackupLog>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(backupService.getBackupHistory(page, size));
    }

    @Operation(summary = "查询最近备份")
    @GetMapping("/last")
    public Result<SysBackupLog> getLastBackup() {
        return Result.success(backupService.getLastBackup());
    }

    @Operation(summary = "查询备份状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        SysBackupLog lastBackup = backupService.getLastBackup();
        return Result.success(Map.of(
                "backupDir", backupService.getBackupDir(),
                "retentionDays", backupService.getEffectiveRetentionDays(),
                "lastBackup", lastBackup != null ? lastBackup : ""
        ));
    }

    @Operation(summary = "查询备份设置")
    @GetMapping("/settings")
    public Result<Map<String, Object>> getSettings() {
        return Result.success(backupService.getBackupSettings());
    }

    @Operation(summary = "更新备份设置")
    @PutMapping("/settings")
    public Result<String> updateSettings(@RequestBody Map<String, Object> body) {
        boolean enabled = body.containsKey("enabled") ? (Boolean) body.get("enabled") : true;
        int retentionDays = body.containsKey("retentionDays") ? ((Number) body.get("retentionDays")).intValue() : 30;
        backupService.updateBackupSettings(enabled, retentionDays);
        log.info("备份设置已更新: enabled={}, retentionDays={}", enabled, retentionDays);
        return Result.success("备份设置已更新");
    }
}