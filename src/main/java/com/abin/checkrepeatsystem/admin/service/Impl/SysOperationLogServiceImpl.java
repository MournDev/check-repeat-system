package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.mapper.SysOperationLogMapper;
import com.abin.checkrepeatsystem.admin.service.SysOperationLogService;
import com.abin.checkrepeatsystem.pojo.entity.SysOperationLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务实现类
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog> implements SysOperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    @Override
    public Page<SysOperationLog> selectPage(Page<SysOperationLog> page, LambdaQueryWrapper<SysOperationLog> wrapper) {
        return super.page(page, wrapper);
    }

    @Override
    public List<SysOperationLog> selectList(LambdaQueryWrapper<SysOperationLog> wrapper) {
        return super.list(wrapper);
    }

    @Override
    public Long selectCount(LambdaQueryWrapper<SysOperationLog> wrapper) {
        return super.count(wrapper);
    }

    @Override
    public void saveOperationLog(SysOperationLog operationLog) {
        try {
            // 设置操作时间
            if (operationLog.getOperationTime() == null) {
                operationLog.setOperationTime(LocalDateTime.now());
            }
            
            // 设置创建时间
            if (operationLog.getCreateTime() == null) {
                operationLog.setCreateTime(LocalDateTime.now());
            }
            
            // 保存到数据库
            boolean saved = this.save(operationLog);
            if (saved) {
                log.debug("操作日志保存成功: type={}, user={}, ip={}", 
                         operationLog.getOperationType(), 
                         operationLog.getUserName(), 
                         operationLog.getIpAddress());
            } else {
                log.warn("操作日志保存失败: type={}, user={}", 
                        operationLog.getOperationType(), 
                        operationLog.getUserName());
            }
        } catch (Exception e) {
            log.error("保存操作日志异常: type={}, user={}, error={}", 
                     operationLog.getOperationType(), 
                     operationLog.getUserName(), 
                     e.getMessage(), e);
        }
    }

    @Override
    public Page<SysOperationLog> getOperationLogPage(Integer page, Integer size,
                                                     String operationType, String username,
                                                     Integer status, LocalDateTime startTime,
                                                     LocalDateTime endTime) {
        Page<SysOperationLog> pageObj = new Page<>(page, size);
        return sysOperationLogMapper.selectOperationLogPage(pageObj, operationType, username, status, startTime, endTime);
    }

    @Override
    public Map<String, Object> getOperationStatistics(Integer days) {
        if (days == null || days <= 0) {
            days = 7; // 默认7天
        }
        return sysOperationLogMapper.getOperationStatistics(days);
    }

    @Override
    public List<Map<String, Object>> getHotOperations(Integer days, Integer limit) {
        if (days == null || days <= 0) {
            days = 7;
        }
        if (limit == null || limit <= 0) {
            limit = 10; // 默认显示前10个
        }
        return sysOperationLogMapper.getHotOperations(days, limit);
    }

    @Override
    public List<Map<String, Object>> getUserActivityStatistics(Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }
        return sysOperationLogMapper.getUserActivityStatistics(days);
    }

    @Override
    public List<Map<String, Object>> getModuleUsageStatistics(Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }
        return sysOperationLogMapper.getModuleUsageStatistics(days);
    }

    @Override
    public SysOperationLog getOperationLogById(Long id) {
        return this.getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteOperationLogs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        int deleted = sysOperationLogMapper.batchDeleteOperationLogs(ids);
        log.info("批量删除操作日志完成: ids={}, deleted={}", ids, deleted);
        return deleted > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cleanExpiredLogs(Integer days) {
        if (days == null || days <= 0) {
            days = 30; // 默认保留30天
        }

        int softDeleted = sysOperationLogMapper.softDeleteExpiredLogs(days);
        int hardDeleted = sysOperationLogMapper.cleanExpiredLogs(days);
        log.info("清理过期操作日志完成: days={}, softDeleted={}, hardDeleted={}", days, softDeleted, hardDeleted);
        return true;
    }

    @Override
    public List<SysOperationLog> exportOperationLogs(String operationType, String username,
                                                     Integer status, LocalDateTime startTime,
                                                     LocalDateTime endTime) {
        // 使用大分页获取所有数据
        Page<SysOperationLog> page = new Page<>(1, 10000); // 最多导出1万条
        Page<SysOperationLog> result = sysOperationLogMapper.selectOperationLogPage(
            page, operationType, username, status, startTime, endTime);
        return result.getRecords();
    }
}