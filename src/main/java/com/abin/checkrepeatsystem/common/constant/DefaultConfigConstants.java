package com.abin.checkrepeatsystem.common.constant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统默认配置常量
 * 避免在多个Controller中重复定义相同的默认配置
 */
public final class DefaultConfigConstants {

    private DefaultConfigConstants() {}

    public static Map<String, Object> defaultBasicConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("systemName", "论文查重管理系统");
        config.put("version", "v2.1.0");
        config.put("defaultLanguage", "zh-CN");
        config.put("timezone", "Asia/Shanghai");
        config.put("maintenanceMode", false);
        config.put("maintenanceNotice", "系统维护中，请稍后再试...");
        return config;
    }

    public static Map<String, Object> defaultPlagiarismConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("internalThreshold", 25);
        config.put("thirdPartyThreshold", 20);
        config.put("algorithm", "combined");
        config.put("minMatchLength", 15);
        config.put("cacheHours", 48);
        return config;
    }

    public static Map<String, Object> defaultSecurityConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("passwordMinLength", 8);
        config.put("passwordRequirements", List.of("uppercase", "lowercase", "numbers"));
        config.put("loginLockEnabled", true);
        config.put("maxFailedAttempts", 5);
        config.put("lockDuration", 30);
        config.put("sessionTimeout", 60);
        config.put("ipRestriction", false);
        return config;
    }

    public static Map<String, Object> defaultEmailConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("smtpServer", "smtp.example.com");
        config.put("smtpPort", 587);
        config.put("encryption", "tls");
        config.put("senderEmail", "noreply@example.com");
        config.put("senderName", "论文查重系统");
        return config;
    }

    public static Map<String, Object> defaultPerformanceConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxConcurrent", 20);
        config.put("queueSize", 100);
        config.put("cacheStrategy", "lru");
        config.put("cacheSize", 1024);
        config.put("autoCleanup", true);
        config.put("cleanupInterval", 24);
        return config;
    }
}
