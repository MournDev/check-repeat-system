package com.abin.checkrepeatsystem.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * 文件清理定时任务：清理临时文件和过期报告
 */
@Slf4j
@Component
public class FileCleanupScheduler {

    @Value("${check.task.temp-file-path:/data/check-temp/}")
    private String tempFilePath;

    @Value("${report.storage.base-path:/data/report}")
    private String reportStoragePath;

    @Value("${report.storage.expire-days:365}")
    private int reportExpireDays;

    @Value("${file.upload.base-path:/data/upload/}")
    private String uploadBasePath;

    @Value("${file.cleanup.temp-file-retention-hours:24}")
    private int tempFileRetentionHours;

    /**
     * 每天凌晨4点清理临时文件
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupTempFiles() {
        log.info("开始清理临时文件...");
        cleanDirectory(tempFilePath, tempFileRetentionHours);
    }

    /**
     * 每天凌晨3点清理过期报告文件
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredReports() {
        log.info("开始清理过期报告文件（{}天）...", reportExpireDays);
        cleanDirectory(reportStoragePath, reportExpireDays * 24);
    }

    /**
     * 每周日凌晨2点清理孤儿上传文件（无DB引用）
     */
    @Scheduled(cron = "0 0 2 ? * SUN")
    public void cleanupOrphanedUploads() {
        log.info("开始清理孤儿上传文件...");
        int orphanHours = 7 * 24;
        cleanDirectory(uploadBasePath, orphanHours);
    }

    private void cleanDirectory(String dirPath, int retentionHours) {
        try {
            Path dir = Paths.get(dirPath);
            if (!Files.exists(dir)) {
                log.info("目录不存在，跳过清理: {}", dirPath);
                return;
            }
            Instant cutoff = Instant.now().minus(Duration.ofHours(retentionHours));
            try (Stream<Path> files = Files.walk(dir)) {
                files.filter(Files::isRegularFile)
                     .filter(f -> isOlderThan(f, cutoff))
                     .forEach(f -> {
                         try {
                             Files.delete(f);
                             log.debug("已删除过期文件: {}", f);
                         } catch (IOException e) {
                             log.warn("删除文件失败: {}", f, e);
                         }
                     });
            }
            // 清理空目录
            try (Stream<Path> dirs = Files.walk(dir)) {
                dirs.filter(Files::isDirectory)
                    .filter(d -> !d.equals(dir))
                    .filter(this::isEmptyDir)
                    .forEach(d -> {
                        try {
                            Files.delete(d);
                            log.debug("已删除空目录: {}", d);
                        } catch (IOException e) {
                            log.warn("删除空目录失败: {}", d, e);
                        }
                    });
            }
            log.info("文件清理完成: {}", dirPath);
        } catch (IOException e) {
            log.error("文件清理异常: {}", dirPath, e);
        }
    }

    private boolean isOlderThan(Path file, Instant cutoff) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            return attrs.lastModifiedTime().toInstant().isBefore(cutoff);
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isEmptyDir(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findAny().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }
}