package com.abin.checkrepeatsystem.common.utils;

import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MinIO连接测试工具
 * 用于诊断MinIO连接问题
 */
@Component
@Slf4j
public class MinioConnectionTest {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket.main:check-repeat-system}")
    private String bucketName;

    /**
     * 测试MinIO连接
     * @return 连接是否成功
     */
    public boolean testConnection() {
        try {
            log.info("开始测试MinIO连接...");
            log.info("Endpoint: {}", minioClient.toString());
            log.info("Bucket: {}", bucketName);
            
            // 尝试获取bucket信息
            StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object("test-connection.txt")
                    .build()
            );
            
            log.info("MinIO连接测试成功");
            return true;
        } catch (Exception e) {
            log.error("MinIO连接测试失败: ", e);
            return false;
        }
    }

    /**
     * 获取详细的连接信息
     */
    public void printConnectionInfo() {
        log.info("=== MinIO连接信息 ===");
        log.info("Endpoint: {}", System.getProperty("minio.endpoint", "未设置"));
        log.info("AccessKey: {}", System.getProperty("minio.accesskey", "未设置"));
        log.info("SecretKey: {}", System.getProperty("minio.secretkey", "未设置"));
        log.info("Bucket: {}", bucketName);
        log.info("====================");
    }
}