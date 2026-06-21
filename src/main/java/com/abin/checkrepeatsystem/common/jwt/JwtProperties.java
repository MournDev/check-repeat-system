package com.abin.checkrepeatsystem.common.jwt;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConfigurationProperties(prefix = "spring.jwt")
@Data
public class JwtProperties {
    private String secretKey;
    private Long expiration;
    private String tokenHeader;
    private String tokenPrefix;

    @PostConstruct
    public void validate() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT密钥未配置，请设置环境变量 JWT_SECRET_KEY");
        }
        if (secretKey.length() < 32) {
            throw new IllegalStateException("JWT密钥长度不足32位，当前长度: " + secretKey.length());
        }
        String lower = secretKey.toLowerCase();
        if (lower.contains("devonly") || lower.contains("test") || lower.contains("example")) {
            throw new IllegalStateException("JWT密钥包含不安全关键词，请使用强随机密钥");
        }
        log.info("JWT密钥校验通过（长度={}位）", secretKey.length());
    }
}

