package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.admin.service.SystemConfigService;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.constant.DefaultConfigConstants;
import com.abin.checkrepeatsystem.pojo.entity.SystemConfig;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/system/config")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
public class SystemConfigManagementController {

    private final SystemConfigService systemConfigService;

    @OperationLog(type = "config_export", description = "导出配置", recordResult = true)
    @GetMapping("/export")
    public void exportConfig(HttpServletResponse response) {
        log.info("接收导出配置请求");
        try {
            Map<String, Object> allConfig = buildAllConfig();

            String fileName = "system-config-" + LocalDateTime.now().toString().substring(0, 19).replace(":", "-") + ".json";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());

            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName);

            String jsonContent = JSON.toJSONString(allConfig, JSONWriter.Feature.PrettyFormat);
            response.getWriter().write(jsonContent);
            response.getWriter().flush();

            log.info("配置导出成功: {}", fileName);
        } catch (Exception e) {
            log.error("配置导出失败: {}", e.getMessage(), e);
            try {
                response.reset();
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败：" + e.getMessage() + "\"}");
            } catch (IOException ex) {
                log.error("设置错误响应失败", ex);
            }
        }
    }

    private Map<String, Object> buildAllConfig() {
        Map<String, Object> allConfig = new HashMap<>();
        allConfig.put("basicConfig", getConfigOrDefault("system_basic", getDefaultBasicConfig()));
        allConfig.put("plagiarismConfig", getConfigOrDefault("plagiarism_config", getDefaultPlagiarismConfig()));
        allConfig.put("securityConfig", getConfigOrDefault("security_config", getDefaultSecurityConfig()));
        allConfig.put("emailConfig", getConfigOrDefault("email_config", getDefaultEmailConfig()));
        allConfig.put("performanceConfig", getConfigOrDefault("performance", getDefaultPerformanceConfig()));
        return allConfig;
    }

    private Map<String, Object> getConfigOrDefault(String key, Map<String, Object> defaultConfig) {
        SystemConfig config = systemConfigService.getConfigByKey(key);
        if (config != null) {
            return JSON.parseObject(config.getConfigValue(), Map.class);
        }
        return defaultConfig;
    }

    private Map<String, Object> getDefaultBasicConfig() {
        return DefaultConfigConstants.defaultBasicConfig();
    }

    private Map<String, Object> getDefaultPlagiarismConfig() {
        return DefaultConfigConstants.defaultPlagiarismConfig();
    }

    private Map<String, Object> getDefaultSecurityConfig() {
        return DefaultConfigConstants.defaultSecurityConfig();
    }

    private Map<String, Object> getDefaultEmailConfig() {
        return DefaultConfigConstants.defaultEmailConfig();
    }

    private Map<String, Object> getDefaultPerformanceConfig() {
        return DefaultConfigConstants.defaultPerformanceConfig();
    }
}