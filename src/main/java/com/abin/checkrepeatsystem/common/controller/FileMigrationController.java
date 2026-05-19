package com.abin.checkrepeatsystem.common.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.service.FileMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件迁移控制器
 * 用于触发文件从本地存储到 MinIO 的迁移
 */
@RestController
@RequestMapping("/api/file/migration")
@Slf4j
public class FileMigrationController {

    @Autowired
    private FileMigrationService fileMigrationService;

    /**
     * 迁移所有旧文件到 MinIO
     */
    @PostMapping("/migrate-all")
    public Result<?> migrateAllFiles() {
        log.info("接收到文件迁移请求");
        try {
            fileMigrationService.migrateAllFiles();
            return Result.success("文件迁移任务已开始执行，请查看日志了解进度");
        } catch (Exception e) {
            log.error("文件迁移失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "文件迁移失败：" + e.getMessage());
        }
    }

    /**
     * 清理本地文件（可选）
     */
    @PostMapping("/cleanup-local")
    public Result<?> cleanupLocalFiles() {
        log.info("接收到本地文件清理请求");
        try {
            fileMigrationService.cleanupLocalFiles();
            return Result.success("本地文件清理任务已开始执行，请查看日志了解进度");
        } catch (Exception e) {
            log.error("本地文件清理失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "本地文件清理失败：" + e.getMessage());
        }
    }
}