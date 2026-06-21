package com.abin.checkrepeatsystem.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 启动配置校验器：确保关键安全配置项已正确设置
 */
@Slf4j
@Component
public class StartupValidator {

    @Value("${spring.jwt.secret-key:}")
    private String jwtSecretKey;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${app.init.super-admin.password:}")
    private String superAdminPassword;

    @PostConstruct
    public void validate() {
        validateJwtSecret();
        validateSuperAdminPassword();
        validateProductionConfig();
    }

    private void validateJwtSecret() {
        if (jwtSecretKey == null || jwtSecretKey.isBlank()) {
            if ("prod".equals(activeProfile)) {
                String msg = "致命错误：JWT密钥未配置！请设置环境变量 JWT_SECRET_KEY。"
                        + "示例：export JWT_SECRET_KEY=your-secure-secret-key-at-least-32-chars";
                log.error(msg);
                throw new IllegalStateException(msg);
            } else {
                log.warn("JWT密钥未配置，开发环境使用不安全的默认密钥。生产环境请设置环境变量 JWT_SECRET_KEY。");
            }
        } else if (jwtSecretKey.length() < 32) {
            String msg = "安全警告：JWT密钥长度不足32位，当前长度=" + jwtSecretKey.length()
                    + "。建议使用至少32位的随机密钥以确保安全性。";
            log.warn(msg);
        }
    }

    private void validateSuperAdminPassword() {
        if (superAdminPassword == null || superAdminPassword.isBlank()) {
            log.warn("超级管理员密码未配置(app.init.super-admin.password)，请通过环境变量 SUPER_ADMIN_PASSWORD 设置。"
                    + "首次登录后请立即修改默认密码。");
        }
    }

    private void validateProductionConfig() {
        if (!"prod".equals(activeProfile)) {
            return;
        }

        // 生产环境必须配置的环境变量
        String[] requiredEnvVars = {"JWT_SECRET_KEY", "MYSQL_PASSWORD", "REDIS_PASSWORD"};
        StringBuilder missing = new StringBuilder();
        for (String envVar : requiredEnvVars) {
            String value = System.getenv(envVar);
            if (value == null || value.isBlank()) {
                if (missing.length() > 0) missing.append(", ");
                missing.append(envVar);
            }
        }
        if (missing.length() > 0) {
            String msg = "生产环境缺少必要的环境变量: " + missing + "。请在部署前配置完成。";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        log.info("生产环境配置校验通过");
    }
}
