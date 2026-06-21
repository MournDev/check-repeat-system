package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.admin.dto.PerformanceConfigDTO;
import com.abin.checkrepeatsystem.admin.vo.CheckRuleOperateReq;
import com.abin.checkrepeatsystem.admin.vo.CompareLibOperateReq;
import com.abin.checkrepeatsystem.admin.dto.RuleLibRelationDTO;
import com.abin.checkrepeatsystem.admin.mapper.SystemParamMapper;
import com.abin.checkrepeatsystem.admin.vo.SystemParamReq;
import com.abin.checkrepeatsystem.admin.service.AdminRuleConfigService;
import com.abin.checkrepeatsystem.admin.service.SystemConfigService;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.constant.DefaultConfigConstants;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.CheckRule;
import com.abin.checkrepeatsystem.pojo.entity.CompareLib;
import com.abin.checkrepeatsystem.pojo.entity.SystemConfig;
import com.abin.checkrepeatsystem.pojo.entity.SystemParam;
import com.abin.checkrepeatsystem.student.dto.DeadlinesDTO;
import com.abin.checkrepeatsystem.user.service.Impl.OptimizedEmailService;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;


/**
 * 管理员规则配置控制器：仅管理员角色可访问，统一用@RequestParam传参
 */
@RestController
@RequestMapping("/api/v1/admin/config")
@RequiredArgsConstructor
public class AdminRuleConfigController {

    private static final Logger log = LoggerFactory.getLogger(AdminRuleConfigController.class);

    private final AdminRuleConfigService adminRuleConfigService;

    private final SystemConfigService systemConfigService;

    private final SystemParamMapper systemParamMapper;

    private final OptimizedEmailService optimizedEmailService;

    @Value("${spring.mail.host:localhost}")
    private String mailHost;
    
    @Value("${spring.mail.port:25}")
    private String mailPort;
    
    @Value("${spring.mail.username:admin@example.com}")
    private String mailUsername;

    // ========================== 查重规则管理 ==========================
    /**
     * 1. 管理员查询查重规则列表
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/check-rule/list")
    public Result<Page<CheckRule>> getCheckRuleList(
            @RequestParam(value = "ruleName", required = false) String ruleName,
            @RequestParam(value = "isDefault", required = false) Integer isDefault,
            @RequestParam("currentPage") Integer currentPage,
            @RequestParam("pageSize") Integer pageSize) {
        return adminRuleConfigService.getCheckRuleList(ruleName, isDefault, currentPage, pageSize);
    }

    /**
     * 2. 管理员新增/编辑查重规则
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/check-rule/save-or-update")
    public Result<Map<String, Object>> saveOrUpdateCheckRule(
            @Valid @RequestBody CheckRuleOperateReq operateReq) {
        return adminRuleConfigService.saveOrUpdateCheckRule(operateReq);
    }

    /**
     * 3. 管理员删除查重规则
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/check-rule/delete")
    public Result<String> deleteCheckRule(
            @RequestParam("ruleId") Long ruleId) {
        return adminRuleConfigService.deleteCheckRule(ruleId);
    }

    /**
     * 4. 管理员查询规则关联的比对库
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/check-rule/related-libs")
    public Result<RuleLibRelationDTO> getRuleRelatedLibs(
            @RequestParam("ruleId") Long ruleId) {
        return adminRuleConfigService.getRuleRelatedLibs(ruleId);
    }

    // ========================== 比对库管理 ==========================
    /**
     * 5. 管理员查询比对库列表
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/compare-lib/list")
    public Result<Page<CompareLib>> getCompareLibList(
            @RequestParam(value = "libName", required = false) String libName,
            @RequestParam(value = "libType", required = false) String libType,
            @RequestParam(value = "isEnabled", required = false) Integer isEnabled,
            @RequestParam("currentPage") Integer currentPage,
            @RequestParam("pageSize") Integer pageSize) {
        return adminRuleConfigService.getCompareLibList(libName, libType, isEnabled, currentPage, pageSize);
    }

    /**
     * 6. 管理员新增/编辑比对库
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/compare-lib/save-or-update")
    public Result<Map<String, Object>> saveOrUpdateCompareLib(
            @Valid @RequestBody CompareLibOperateReq operateReq) {
        return adminRuleConfigService.saveOrUpdateCompareLib(operateReq);
    }

    /**
     * 7. 管理员启用/禁用比对库
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/compare-lib/toggle-enabled")
    public Result<String> toggleLibEnabled(
            @RequestParam("libId") Long libId,
            @RequestParam("isEnabled") Integer isEnabled) {
        return adminRuleConfigService.toggleLibEnabled(libId, isEnabled);
    }

    // ========================== 系统参数配置 ==========================
    /**
     * 8. 管理员查询当前系统参数
     */
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/system-param/current")
    public Result<SystemParam> getCurrentSystemParam() {
        return adminRuleConfigService.getCurrentSystemParam();
    }

    /**
     * 获取所有系统配置
     * 确保所有数据都来自真实数据库
     */
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @GetMapping("/system")
    public Result<Map<String, Object>> getAllConfig() {
        log.info("接收获取所有系统配置请求（真实数据）");
        
        Map<String, Object> allConfig = new HashMap<>();

        // 1. 获取基础配置（来自数据库）
        Map<String, Object> basicConfig = getBasicConfigFromDB();
        allConfig.put("basicConfig", basicConfig);

        // 2. 获取查重配置（来自数据库）
        Map<String, Object> plagiarismConfig = getPlagiarismConfigFromDB();
        allConfig.put("plagiarismConfig", plagiarismConfig);

        // 3. 获取安全配置（来自数据库）
        Map<String, Object> securityConfig = getSecurityConfigFromDB();
        allConfig.put("securityConfig", securityConfig);

        // 4. 获取邮件配置（来自数据库，不再是配置文件）
        Map<String, Object> emailConfig = getEmailConfigFromDB();
        allConfig.put("emailConfig", emailConfig);

        // 5. 获取性能配置（来自数据库）
        Map<String, Object> performanceConfig = getPerformanceConfigFromDB();
        allConfig.put("performanceConfig", performanceConfig);

        // 6. 获取查重规则配置（来自数据库）
        Result<Page<CheckRule>> ruleResult = adminRuleConfigService.getCheckRuleList(null, null, 1, 10);
        allConfig.put("checkRules", ruleResult.getData());

        // 7. 获取比对库配置（来自数据库）
        Result<Page<CompareLib>> libResult = adminRuleConfigService.getCompareLibList(null, null, null, 1, 10);
        allConfig.put("compareLibs", libResult.getData());

        // 8. 获取系统参数（来自数据库）
        Result<SystemParam> paramResult = adminRuleConfigService.getCurrentSystemParam();
        allConfig.put("systemParams", paramResult.getData());

        log.info("获取所有系统配置成功，共{}个配置项", allConfig.size());
        return Result.success("系统配置获取成功", allConfig);

    }

    private Map<String, Object> getConfigFromDB(String configKey, Map<String, Object> defaultConfig) {
        try {
            SystemConfig config = systemConfigService.getConfigByKey(configKey);
            if (config != null) {
                return JSON.parseObject(config.getConfigValue(), Map.class);
            }
        } catch (Exception e) {
            log.warn("获取配置失败: key={}, error={}", configKey, e.getMessage());
        }
        return defaultConfig;
    }

    private Map<String, Object> getBasicConfigFromDB() {
        return getConfigFromDB("system_basic", DefaultConfigConstants.defaultBasicConfig());
    }

    private Map<String, Object> getPlagiarismConfigFromDB() {
        return getConfigFromDB("plagiarism_config", DefaultConfigConstants.defaultPlagiarismConfig());
    }

    private Map<String, Object> getSecurityConfigFromDB() {
        return getConfigFromDB("security_config", DefaultConfigConstants.defaultSecurityConfig());
    }

    private Map<String, Object> getEmailConfigFromDB() {
        return getConfigFromDB("email_config", DefaultConfigConstants.defaultEmailConfig());
    }

    private Map<String, Object> getPerformanceConfigFromDB() {
        return getConfigFromDB("performance", DefaultConfigConstants.defaultPerformanceConfig());
    }
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PutMapping("/basic")
    public Result<String> updateBasicConfig(@RequestBody Map<String, Object> config) {
        log.info("接收更新基础配置请求: {}", config);
        saveConfig("system_basic", config, "系统基础配置");
        syncMaintenanceToSystemParam(config);
        log.info("基础配置更新成功");
        return Result.success("基础配置更新成功");
    }

    /**
     * 更新查重配置
     */
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PutMapping("/plagiarism")
    public Result<String> updatePlagiarismConfig(@RequestBody Map<String, Object> config) {
        log.info("接收更新查重配置请求: {}", config);
        saveConfig("plagiarism_config", config, "查重配置");
        log.info("查重配置更新成功");
        return Result.success("查重配置更新成功");
    }

    /**
     * 更新安全配置
     */
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PutMapping("/security")
    public Result<String> updateSecurityConfig(@RequestBody Map<String, Object> config) {
        log.info("接收更新安全配置请求: {}", config);
        saveConfig("security_config", config, "安全配置");
        log.info("安全配置更新成功");
        return Result.success("安全配置更新成功");
    }

    /**
     * 更新邮件配置
     */
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PutMapping("/email")
    public Result<String> updateEmailConfig(@RequestBody Map<String, Object> config) {
        log.info("接收更新邮件配置请求: {}", config);
        saveConfig("email_config", config, "邮件配置");
        log.info("邮件配置更新成功");
        return Result.success("邮件配置更新成功");
    }

    /**
     * 测试邮件配置
     */
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PostMapping("/test-email")
    public Result<String> testEmailConfig(@RequestBody Map<String, String> body) {
        String testEmail = body.get("testEmail");
        if (testEmail == null || testEmail.isBlank()) {
            return Result.error(ResultCode.PARAM_ERROR, "测试邮箱地址不能为空");
        }
        log.info("接收测试邮件配置请求: 目标邮箱={}", testEmail);
        try {
            optimizedEmailService.sendNoticeEmail(
                    testEmail,
                    "论文查重系统 - 邮件配置测试",
                    "<p>这是一封测试邮件，如果您收到此邮件，说明邮件配置正确。</p>"
            );
            log.info("测试邮件发送成功: {}", testEmail);
            return Result.success("测试邮件发送成功，请检查收件箱");
        } catch (Exception e) {
            log.error("测试邮件发送失败: {}", testEmail, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "测试邮件发送失败，请查看服务器日志");
        }
    }

    /**
     * 保存所有配置
     */
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PostMapping("/save-all")
    public Result<String> saveAllConfig(@RequestBody(required = false) Map<String, Object> allConfig) {
        log.info("接收保存所有配置请求");
        
        // 检查请求体是否存在
        if (allConfig == null) {
            log.warn("请求体为空，返回参数错误");
            return Result.error(ResultCode.PARAM_ERROR, "请求体不能为空，请提供要保存的配置数据");
        }
        
        log.info("接收到的配置数据: {}", allConfig.keySet());
        
        // 保存各个配置项
        if (allConfig.containsKey("basicConfig")) {
            log.info("保存基础配置");
            Map<String, Object> basicConfig = (Map<String, Object>) allConfig.get("basicConfig");
            saveConfig("system_basic", basicConfig, "系统基础配置");
        }

        if (allConfig.containsKey("plagiarismConfig")) {
            log.info("保存查重配置");
            Map<String, Object> plagiarismConfig = (Map<String, Object>) allConfig.get("plagiarismConfig");
            saveConfig("plagiarism_config", plagiarismConfig, "查重配置");
        }

        if (allConfig.containsKey("securityConfig")) {
            log.info("保存安全配置");
            Map<String, Object> securityConfig = (Map<String, Object>) allConfig.get("securityConfig");
            saveConfig("security_config", securityConfig, "安全配置");
        }

        if (allConfig.containsKey("emailConfig")) {
            log.info("保存邮件配置");
            Map<String, Object> emailConfig = (Map<String, Object>) allConfig.get("emailConfig");
            saveConfig("email_config", emailConfig, "邮件配置");
        }

        if (allConfig.containsKey("performanceConfig")) {
            log.info("保存性能配置");
            Map<String, Object> performanceConfig = (Map<String, Object>) allConfig.get("performanceConfig");
            saveConfig("performance", performanceConfig, "性能配置");
        }

        log.info("所有配置保存成功");
        return Result.success("所有配置保存成功");

    }

    /**
     * 恢复默认配置
     */
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PostMapping("/reset-default")
    public Result<String> resetDefaultConfig() {
        log.info("接收恢复默认配置请求");
        // 删除现有配置
        systemConfigService.deleteAllConfigs();

        // 插入默认配置
        insertDefaultConfigs();

        log.info("默认配置恢复成功");
        return Result.success("默认配置恢复成功");
    }
    
    private void syncMaintenanceToSystemParam(Map<String, Object> config) {
        if (!config.containsKey("maintenanceMode") && !config.containsKey("maintenanceNotice")) {
            return;
        }
        try {
            SystemParam sp = systemParamMapper.selectOne(
                    new LambdaQueryWrapper<SystemParam>()
                            .eq(SystemParam::getIsDeleted, 0)
                            .last("LIMIT 1"));
            if (sp == null) {
                sp = new SystemParam();
                if (config.containsKey("maintenanceMode")) {
                    sp.setMaintenanceStatus(Boolean.TRUE.equals(config.get("maintenanceMode")) ? 1 : 0);
                }
                if (config.containsKey("maintenanceNotice")) {
                    sp.setMaintenanceNotice((String) config.get("maintenanceNotice"));
                }
                systemParamMapper.insert(sp);
            } else {
                if (config.containsKey("maintenanceMode")) {
                    sp.setMaintenanceStatus(Boolean.TRUE.equals(config.get("maintenanceMode")) ? 1 : 0);
                }
                if (config.containsKey("maintenanceNotice")) {
                    sp.setMaintenanceNotice((String) config.get("maintenanceNotice"));
                }
                systemParamMapper.updateById(sp);
            }
            log.info("维护状态已同步到system_param: maintenanceMode={}", config.get("maintenanceMode"));
        } catch (Exception e) {
            log.error("同步维护状态到system_param失败: {}", e.getMessage());
        }
    }

    /**
     * 保存配置的辅助方法
     */
    private void saveConfig(String configKey, Map<String, Object> config, String description) {
        try {
            String configJson = JSON.toJSONString(config);
            systemConfigService.saveConfig(configKey, configJson, description);
            log.info("配置保存成功: {} = {}", configKey, configJson);
        } catch (Exception e) {
            log.error("保存配置失败: configKey={}, error={}", configKey, e.getMessage(), e);
            throw new RuntimeException("保存配置失败", e);
        }
    }
    
    /**
     * 插入默认配置
     */
    private void insertDefaultConfigs() {
        try {
            saveConfig("system_basic", DefaultConfigConstants.defaultBasicConfig(), "系统基础配置");
            saveConfig("plagiarism_config", DefaultConfigConstants.defaultPlagiarismConfig(), "查重配置");
            saveConfig("security_config", DefaultConfigConstants.defaultSecurityConfig(), "安全配置");
            saveConfig("email_config", DefaultConfigConstants.defaultEmailConfig(), "邮件配置");
            saveConfig("performance", DefaultConfigConstants.defaultPerformanceConfig(), "性能配置");
            log.info("默认配置插入成功");
        } catch (Exception e) {
            log.error("插入默认配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("插入默认配置失败", e);
        }
    }

    // ========================== 性能配置（合并自 SystemConfigController） ==========================

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PutMapping("/performance")
    public Result<Void> updatePerformanceConfig(@Valid @RequestBody PerformanceConfigDTO performanceConfig) {
        log.info("接收更新性能配置请求: maxConcurrent={}, queueSize={}, cacheStrategy={}",
                performanceConfig.getMaxConcurrent(),
                performanceConfig.getQueueSize(),
                performanceConfig.getCacheStrategy());
        return systemConfigService.updatePerformanceConfig(performanceConfig);
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @GetMapping("/performance")
    public Result<PerformanceConfigDTO> getPerformanceConfig() {
        log.info("接收获取性能配置请求");
        return systemConfigService.getPerformanceConfig();
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PostMapping("/performance/reset")
    public Result<Void> resetPerformanceConfig() {
        log.info("接收重置性能配置请求");
        PerformanceConfigDTO defaultConfig = systemConfigService.getDefaultPerformanceConfig();
        return systemConfigService.updatePerformanceConfig(defaultConfig);
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @PostMapping("/performance/test")
    public Result<Void> testPerformanceConfig(@Valid @RequestBody PerformanceConfigDTO performanceConfig) {
        log.info("接收测试性能配置请求: {}", performanceConfig);
        systemConfigService.applyPerformanceConfig(performanceConfig);
        return Result.success("性能配置测试应用成功");
    }


    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/deadlines")
    public Result<DeadlinesDTO> getDeadlinesConfig() {
        DeadlinesDTO deadlines = systemConfigService.getDeadlines();
        return Result.success(deadlines);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/deadlines")
    public Result<Void> updateDeadlinesConfig(@RequestBody DeadlinesDTO deadlines) {
        log.info("更新时间节点配置：{}", deadlines);
        systemConfigService.updateDeadlines(deadlines);
        return Result.success("时间节点配置更新成功");
    }
}
