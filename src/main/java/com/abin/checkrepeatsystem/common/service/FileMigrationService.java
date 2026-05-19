package com.abin.checkrepeatsystem.common.service;

import com.abin.checkrepeatsystem.common.component.MinioProp;
import com.abin.checkrepeatsystem.mapper.FileInfoMapper;
import com.abin.checkrepeatsystem.pojo.entity.FileInfo;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 文件迁移服务
 * 将旧的本地存储文件迁移到 MinIO
 */
@Service
@Slf4j
public class FileMigrationService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProp minioProp;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Value("${file.upload.base-path}")
    private String uploadPath;

    /**
     * 迁移所有旧文件到 MinIO
     */
    public void migrateAllFiles() {
        log.info("开始迁移旧文件到 MinIO...");
        
        // 查询所有文件记录
        List<FileInfo> allFiles = fileInfoMapper.selectList(null);
        log.info("总文件数：{}", allFiles.size());
        
        int successCount = 0;
        int failedCount = 0;
        
        for (FileInfo fileInfo : allFiles) {
            try {
                if (migrateFile(fileInfo)) {
                    successCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("迁移文件失败 - 文件ID: {}", fileInfo.getId(), e);
                failedCount++;
            }
        }
        
        log.info("文件迁移完成：成功 {} 个，失败 {} 个", successCount, failedCount);
    }

    /**
     * 迁移单个文件到 MinIO
     */
    private boolean migrateFile(FileInfo fileInfo) throws Exception {
        // 跳过已经是 MinIO 存储的文件
        if (fileInfo.getAccessUrl() != null && fileInfo.getAccessUrl().contains(minioProp.getEndpoint())) {
            log.debug("文件已经在 MinIO 中 - 文件ID: {}", fileInfo.getId());
            return true;
        }
        
        String localPath = fileInfo.getStoragePath();
        if (localPath == null || localPath.isEmpty()) {
            log.warn("文件路径为空 - 文件ID: {}", fileInfo.getId());
            return false;
        }
        
        // 构建本地文件完整路径
        File localFile = new File(uploadPath, localPath);
        if (!localFile.exists()) {
            log.warn("本地文件不存在 - 文件ID: {}, 路径: {}", fileInfo.getId(), localFile.getAbsolutePath());
            return false;
        }
        
        // 生成 MinIO 存储路径
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = fileInfo.getId() + "_" + fileInfo.getOriginalFilename().replaceAll("[<>:\"|?*]", "_").replaceAll("[/\\]", "_");
        String objectName = "files/migrated/" + datePath + "/" + fileName;
        
        // 上传到 MinIO
        try (InputStream inputStream = new FileInputStream(localFile)) {
            minioClient.putObject(io.minio.PutObjectArgs.builder()
                    .bucket(minioProp.getBucket().getFile())
                    .object(objectName)
                    .stream(inputStream, localFile.length(), -1)
                    .contentType(Files.probeContentType(localFile.toPath()))
                    .build());
        }
        
        // 更新数据库
        fileInfo.setStoragePath(objectName);
        fileInfo.setAccessUrl(minioProp.getEndpoint() + "/" + minioProp.getBucket().getFile() + "/" + objectName);
        fileInfoMapper.updateById(fileInfo);
        
        log.info("文件迁移成功 - 文件ID: {}, 文件名: {}", fileInfo.getId(), fileInfo.getOriginalFilename());
        return true;
    }

    /**
     * 清理本地文件（可选）
     */
    public void cleanupLocalFiles() {
        log.info("开始清理本地文件...");
        
        List<FileInfo> allFiles = fileInfoMapper.selectList(null);
        int cleanedCount = 0;
        
        for (FileInfo fileInfo : allFiles) {
            try {
                if (fileInfo.getAccessUrl() != null && fileInfo.getAccessUrl().contains(minioProp.getEndpoint())) {
                    String localPath = fileInfo.getStoragePath();
                    if (localPath != null && !localPath.contains("files/")) {
                        File localFile = new File(uploadPath, localPath);
                        if (localFile.exists() && localFile.delete()) {
                            cleanedCount++;
                            log.debug("清理本地文件成功: {}", localFile.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("清理本地文件失败 - 文件ID: {}", fileInfo.getId(), e);
            }
        }
        
        log.info("本地文件清理完成，清理了 {} 个文件", cleanedCount);
    }
}