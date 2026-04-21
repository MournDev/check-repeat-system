package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.mapper.SystemParamMapper;
import com.abin.checkrepeatsystem.admin.service.SystemParamService;
import com.abin.checkrepeatsystem.pojo.entity.SystemParam;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class SystemParamServiceImpl extends ServiceImpl<SystemParamMapper, SystemParam> implements SystemParamService {

    @Resource
    private SystemParamMapper systemParamMapper;

    private static final Long DEFAULT_MAX_PAPER_SIZE = 209715200L; // 200MB
    private static final Integer DEFAULT_MAX_CONCURRENT_CHECK = 10;
    private static final Long DEFAULT_JWT_EXPIRATION = 86400000L; // 24小时
    private static final Double DEFAULT_THRESHOLD = 20.00;
    private static final String DEFAULT_STORAGE_TYPE = "LOCAL";
    private static final Integer DEFAULT_MAINTENANCE_STATUS = 0;

    @Override
    public SystemParam getSystemParam() {
        try {
            SystemParam param = systemParamMapper.selectById(1L);
            if (param == null) {
                // 初始化参数
                initSystemParam();
                param = systemParamMapper.selectById(1L);
            }
            return param;
        } catch (Exception e) {
            log.error("获取系统参数失败", e);
            return initDefaultParam();
        }
    }

    @Override
    public boolean updateSystemParam(SystemParam systemParam) {
        try {
            systemParam.setId(1L); // 确保ID为1
            systemParam.setUpdateTime(LocalDateTime.now());
            int result = systemParamMapper.updateById(systemParam);
            return result > 0;
        } catch (Exception e) {
            log.error("更新系统参数失败", e);
            return false;
        }
    }

    @Override
    public boolean initSystemParam() {
        try {
            SystemParam existingParam = systemParamMapper.selectById(1L);
            if (existingParam == null) {
                SystemParam param = initDefaultParam();
                param.setId(1L);
                param.setCreateTime(LocalDateTime.now());
                param.setUpdateTime(LocalDateTime.now());
                int result = systemParamMapper.insert(param);
                return result > 0;
            }
            return true;
        } catch (Exception e) {
            log.error("初始化系统参数失败", e);
            return false;
        }
    }

    @Override
    public Long getMaxPaperSize() {
        SystemParam param = getSystemParam();
        return param != null ? param.getMaxPaperSize() : DEFAULT_MAX_PAPER_SIZE;
    }

    @Override
    public Integer getMaxConcurrentCheck() {
        SystemParam param = getSystemParam();
        return param != null ? param.getMaxConcurrentCheck() : DEFAULT_MAX_CONCURRENT_CHECK;
    }

    @Override
    public Long getJwtExpiration() {
        SystemParam param = getSystemParam();
        return param != null ? param.getJwtExpiration() : DEFAULT_JWT_EXPIRATION;
    }

    @Override
    public Double getDefaultThreshold() {
        SystemParam param = getSystemParam();
        return param != null ? param.getDefaultThreshold() : DEFAULT_THRESHOLD;
    }

    @Override
    public String getStorageType() {
        SystemParam param = getSystemParam();
        return param != null ? param.getStorageType() : DEFAULT_STORAGE_TYPE;
    }

    @Override
    public Integer getMaintenanceStatus() {
        SystemParam param = getSystemParam();
        return param != null ? param.getMaintenanceStatus() : DEFAULT_MAINTENANCE_STATUS;
    }

    @Override
    public boolean setMaintenanceStatus(Integer status, String notice) {
        try {
            SystemParam param = getSystemParam();
            if (param == null) {
                param = initDefaultParam();
                param.setId(1L);
            }
            param.setMaintenanceStatus(status);
            param.setMaintenanceNotice(notice);
            if (status == 1) {
                param.setMaintenanceStartTime(LocalDateTime.now());
            } else {
                param.setMaintenanceEndTime(LocalDateTime.now());
            }
            return updateSystemParam(param);
        } catch (Exception e) {
            log.error("设置系统维护状态失败", e);
            return false;
        }
    }

    /**
     * 初始化默认参数
     */
    private SystemParam initDefaultParam() {
        SystemParam param = new SystemParam();
        param.setMaxPaperSize(DEFAULT_MAX_PAPER_SIZE);
        param.setMaxConcurrentCheck(DEFAULT_MAX_CONCURRENT_CHECK);
        param.setJwtExpiration(DEFAULT_JWT_EXPIRATION);
        param.setDefaultThreshold(DEFAULT_THRESHOLD);
        param.setStorageType(DEFAULT_STORAGE_TYPE);
        param.setMaintenanceStatus(DEFAULT_MAINTENANCE_STATUS);
        return param;
    }
}
