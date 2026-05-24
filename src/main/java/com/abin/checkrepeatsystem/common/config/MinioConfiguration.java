package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.component.MinioProp;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Configuration
@Slf4j
public class MinioConfiguration {

    private final MinioProp minioProp;

    @Bean
    public MinioClient minioClient() {
        log.info("创建MinioClient - endpoint: {}, accessKey: {}, secretKey: {}",
                minioProp.getEndpoint(),
                minioProp.getAccessKey() != null ? minioProp.getAccessKey().substring(0, Math.min(4, minioProp.getAccessKey().length())) + "****" : "null",
                minioProp.getSecretKey() != null ? "****" : "null");
                
        MinioClient client = new MinioClient.Builder()
                .endpoint(minioProp.getEndpoint())
                .credentials(minioProp.getAccessKey(), minioProp.getSecretKey())
                .build();
        
        log.info("MinioClient创建成功");
        return client;
    }
}
