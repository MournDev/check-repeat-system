package com.abin.checkrepeatsystem.admin.service;

import com.abin.checkrepeatsystem.admin.dto.PerformanceConfigDTO;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.SystemConfig;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {
    
    /**
     * 更新性能配置
     */
    Result<Void> updatePerformanceConfig(PerformanceConfigDTO performanceConfig);
    
    /**
     * 获取性能配置
     */
    Result<PerformanceConfigDTO> getPerformanceConfig();
    
    /**
     * 应用性能配置到系统
     */
    void applyPerformanceConfig(PerformanceConfigDTO config);
    
    /**
     * 获取默认性能配置
     */
    PerformanceConfigDTO getDefaultPerformanceConfig();
    
    // ==================== 新增配置管理方法 ====================
    
    /**
     * 根据配置键获取配置
     */
    SystemConfig getConfigByKey(String configKey);
    
    /**
     * 保存配置
     */
    void saveConfig(String configKey, String configValue, String description);
    
    /**
     * 删除所有配置
     */
    void deleteAllConfigs();
    
    /**
     * 刷新配置缓存
     */
    void refreshConfigCache();
    
    /**
     * 获取时间节点配置
     */
    com.abin.checkrepeatsystem.student.dto.DeadlinesDTO getDeadlines();
    
    /**
     * 更新时间节点配置
     */
    void updateDeadlines(com.abin.checkrepeatsystem.student.dto.DeadlinesDTO deadlines);
}