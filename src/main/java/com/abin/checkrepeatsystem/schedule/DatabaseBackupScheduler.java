package com.abin.checkrepeatsystem.schedule;

import com.abin.checkrepeatsystem.admin.service.DatabaseBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseBackupScheduler {

    private final DatabaseBackupService backupService;

    @Scheduled(cron = "${backup.db.cron:0 0 2 * * ?}")
    public void scheduledBackup() {
        log.info("开始执行数据库自动备份...");
        try {
            var result = backupService.performBackup("AUTO");
            if ("SUCCESS".equals(result.get("status"))) {
                log.info("数据库自动备份完成: fileName={}, size={} bytes", result.get("fileName"), result.get("fileSize"));
            } else {
                log.warn("数据库自动备份未正常完成: {}", result);
            }
            int deleted = backupService.cleanupExpiredBackups();
            if (deleted > 0) log.info("过期备份清理完成: 删除{}个文件", deleted);
        } catch (Exception e) {
            log.error("数据库自动备份失败", e);
        }
    }
}