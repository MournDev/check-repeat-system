package com.abin.checkrepeatsystem.admin.service;

import com.abin.checkrepeatsystem.admin.mapper.SystemParamMapper;
import com.abin.checkrepeatsystem.mapper.SysBackupLogMapper;
import com.abin.checkrepeatsystem.pojo.entity.SysBackupLog;
import com.abin.checkrepeatsystem.pojo.entity.SystemParam;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseBackupService {

    private final SysBackupLogMapper backupLogMapper;

    private final SystemParamMapper systemParamMapper;

    @Value("${backup.db.enabled:true}")
    private boolean backupEnabled;

    @Value("${backup.db.dir:/data/backups/database}")
    private String backupDir;

    @Value("${backup.db.retention-days:30}")
    private int retentionDays;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    private SystemParam loadSystemParam() {
        return systemParamMapper.selectOne(
            new LambdaQueryWrapper<SystemParam>()
                .eq(SystemParam::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    public boolean isBackupEnabled() {
        SystemParam sp = loadSystemParam();
        if (sp != null && sp.getBackupEnabled() != null) {
            return sp.getBackupEnabled() == 1;
        }
        return backupEnabled;
    }

    public int getEffectiveRetentionDays() {
        SystemParam sp = loadSystemParam();
        if (sp != null && sp.getBackupRetentionDays() != null) {
            return sp.getBackupRetentionDays();
        }
        return retentionDays;
    }

    public Map<String, Object> getBackupSettings() {
        SystemParam sp = loadSystemParam();
        return Map.of(
            "enabled", sp != null && sp.getBackupEnabled() != null ? sp.getBackupEnabled() == 1 : backupEnabled,
            "retentionDays", sp != null && sp.getBackupRetentionDays() != null ? sp.getBackupRetentionDays() : retentionDays
        );
    }

    public void updateBackupSettings(boolean enabled, int retentionDays) {
        SystemParam sp = loadSystemParam();
        if (sp == null) {
            sp = new SystemParam();
            sp.setBackupEnabled(enabled ? 1 : 0);
            sp.setBackupRetentionDays(retentionDays);
            systemParamMapper.insert(sp);
        } else {
            sp.setBackupEnabled(enabled ? 1 : 0);
            sp.setBackupRetentionDays(retentionDays);
            systemParamMapper.updateById(sp);
        }
    }

    public Map<String, Object> performBackup(String backupType) {
        if (!isBackupEnabled()) {
            log.info("数据库备份已禁用，跳过本次备份");
            return Map.of("status", "SKIPPED", "message", "备份功能已禁用");
        }

        SysBackupLog logEntry = new SysBackupLog();
        logEntry.setBackupType(backupType);
        logEntry.setStartTime(LocalDateTime.now());

        try {
            Path dir = Paths.get(backupDir);
            Files.createDirectories(dir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String baseName = "check-repeat-system_" + timestamp;
            Path sqlFile = dir.resolve(baseName + ".sql");
            Path gzFile = dir.resolve(baseName + ".sql.gz");

            String[] jdbcParts = parseJdbcUrl();
            String host = jdbcParts[0];
            String port = jdbcParts[1];
            String dbName = jdbcParts[2];

            long startMs = System.currentTimeMillis();

            // 第1步：mysqldump → .sql 文件
            ProcessBuilder dumpPb = new ProcessBuilder(
                    "mysqldump",
                    "-h", host,
                    "-P", port,
                    "-u", datasourceUsername,
                    "--single-transaction",
                    "--routines",
                    "--triggers",
                    "--default-character-set=utf8mb4",
                    dbName
            );
            dumpPb.redirectOutput(sqlFile.toFile());
            dumpPb.redirectError(ProcessBuilder.Redirect.PIPE);
            if (datasourcePassword != null && !datasourcePassword.isEmpty()) {
                dumpPb.environment().put("MYSQL_PWD", datasourcePassword);
            }

            Process dumpProcess = dumpPb.start();
            boolean dumpDone = dumpProcess.waitFor(300, TimeUnit.SECONDS);
            if (!dumpDone) {
                dumpProcess.destroyForcibly();
                Files.deleteIfExists(sqlFile);
                throw new IOException("mysqldump超时(5分钟)");
            }
            if (dumpProcess.exitValue() != 0) {
                byte[] err = dumpProcess.getErrorStream().readAllBytes();
                Files.deleteIfExists(sqlFile);
                throw new IOException("mysqldump失败, exit code: " + dumpProcess.exitValue() + ", " + new String(err));
            }

            // 第2步：GZIP 压缩 → .sql.gz 文件（使用 Java 内置 GZIPOutputStream，无需外部命令）
            try {
                Files.copy(sqlFile, gzFile);
            } catch (IOException e) {
                // gzFile 已存在但无法覆盖
                Files.deleteIfExists(sqlFile);
                throw new IOException("无法创建压缩文件: " + gzFile, e);
            }
            try (InputStream in = Files.newInputStream(sqlFile);
                 GZIPOutputStream gzOut = new GZIPOutputStream(Files.newOutputStream(gzFile))) {
                in.transferTo(gzOut);
            }
            Files.deleteIfExists(sqlFile);

            long fileSize = Files.size(gzFile);
            long duration = System.currentTimeMillis() - startMs;

            logEntry.setFileName(baseName + ".sql.gz");
            logEntry.setFileSize(fileSize);
            logEntry.setStatus("SUCCESS");
            logEntry.setEndTime(LocalDateTime.now());
            logEntry.setDurationMs(duration);
            logEntry.setErrorMessage(null);
            logEntry.setCreateTime(LocalDateTime.now());
            logEntry.setUpdateTime(LocalDateTime.now());
            backupLogMapper.insert(logEntry);

            log.info("数据库备份完成: fileName={}, size={} bytes, duration={}ms", gzFile.getFileName(), fileSize, duration);
            return Map.of("status", "SUCCESS", "fileName", gzFile.getFileName().toString(), "fileSize", fileSize, "durationMs", duration);
        } catch (Exception e) {
            log.error("数据库备份失败", e);
            logEntry.setStatus("FAILED");
            logEntry.setEndTime(LocalDateTime.now());
            long startMs = logEntry.getStartTime() != null
                    ? logEntry.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : System.currentTimeMillis();
            logEntry.setDurationMs(System.currentTimeMillis() - startMs);
            logEntry.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 1000)) : "未知错误");
            logEntry.setCreateTime(LocalDateTime.now());
            logEntry.setUpdateTime(LocalDateTime.now());
            backupLogMapper.insert(logEntry);
            return Map.of("status", "FAILED", "message", e.getMessage());
        }
    }

    public int cleanupExpiredBackups() {
        try {
            Path dir = Paths.get(backupDir);
            if (!Files.isDirectory(dir)) return 0;

            LocalDateTime cutoff = LocalDateTime.now().minusDays(getEffectiveRetentionDays());
            int deleted = 0;
            for (var file : dir.toFile().listFiles()) {
                if (file.isFile() && file.getName().endsWith(".sql.gz")) {
                    if (file.lastModified() < cutoff.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) {
                        if (file.delete()) deleted++;
                    }
                }
            }
            if (deleted > 0) log.info("清理过期备份文件完成: 删除{}个文件", deleted);
            return deleted;
        } catch (Exception e) {
            log.error("清理过期备份文件失败", e);
            return 0;
        }
    }

    public Page<SysBackupLog> getBackupHistory(int page, int size) {
        LambdaQueryWrapper<SysBackupLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysBackupLog::getCreateTime);
        return backupLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public SysBackupLog getLastBackup() {
        LambdaQueryWrapper<SysBackupLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysBackupLog::getStatus, "SUCCESS")
                .orderByDesc(SysBackupLog::getCreateTime)
                .last("LIMIT 1");
        return backupLogMapper.selectOne(wrapper);
    }

    public String getBackupDir() {
        return backupDir;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    private String[] cachedJdbcParts = null;

    private String[] parseJdbcUrl() {
        if (cachedJdbcParts != null) return cachedJdbcParts;
        // URL format: jdbc:mysql://host:port/db?params
        String stripped = datasourceUrl.replaceFirst("^jdbc:mysql://", "");
        String host = stripped.substring(0, stripped.indexOf(':'));
        String remainder = stripped.substring(stripped.indexOf(':') + 1);
        String port = remainder.substring(0, remainder.indexOf('/'));
        String db = remainder.substring(remainder.indexOf('/') + 1, remainder.indexOf('?'));
        cachedJdbcParts = new String[]{host, port, db};
        return cachedJdbcParts;
    }
}