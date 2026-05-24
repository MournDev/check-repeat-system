package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.mapper.AlertRecordMapper;
import com.abin.checkrepeatsystem.admin.service.AlertService;
import com.abin.checkrepeatsystem.pojo.entity.AlertRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class AlertServiceImpl implements AlertService {

    private final AlertRecordMapper alertRecordMapper;

    @Override
    public List<AlertRecord> listActive() {
        return alertRecordMapper.selectList(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStatus, "ACTIVE")
                .orderByDesc(AlertRecord::getTriggerTime)
        );
    }

    @Override
    public Page<AlertRecord> listActivePage(int page, int size) {
        Page<AlertRecord> p = new Page<>(page, size);
        return alertRecordMapper.selectPage(p,
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStatus, "ACTIVE")
                .orderByDesc(AlertRecord::getTriggerTime)
        );
    }

    @Override
    public List<AlertRecord> listByStatus(String status, int page, int size) {
        Page<AlertRecord> p = new Page<>(page, size);
        alertRecordMapper.selectPage(p,
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStatus, status)
                .orderByDesc(AlertRecord::getTriggerTime)
        );
        return p.getRecords();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord create(AlertRecord record) {
        record.setTriggerTime(LocalDateTime.now());
        record.setStatus("ACTIVE");
        alertRecordMapper.insert(record);
        log.warn("告警触发: [{}] {}", record.getSeverity(), record.getTitle());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord resolve(Long id, String resolvedBy) {
        AlertRecord record = alertRecordMapper.selectById(id);
        if (record == null) return null;
        record.setStatus("RESOLVED");
        record.setResolvedBy(resolvedBy);
        record.setResolveTime(LocalDateTime.now());
        alertRecordMapper.updateById(record);
        log.info("告警已处理: id={}, by={}", id, resolvedBy);
        return record;
    }

    @Override
    public boolean existsActiveByRuleId(Long ruleId) {
        return alertRecordMapper.selectCount(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getRuleId, ruleId)
                .eq(AlertRecord::getStatus, "ACTIVE")
        ) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupOldAlerts(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        
        List<AlertRecord> expiredRecords = alertRecordMapper.selectList(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getStatus, "ACTIVE")
                .lt(AlertRecord::getTriggerTime, cutoff)
        );
        
        for (AlertRecord record : expiredRecords) {
            record.setStatus("EXPIRED");
            record.setResolveTime(LocalDateTime.now());
            record.setResolvedBy("system");
            alertRecordMapper.updateById(record);
        }
        
        int count = alertRecordMapper.delete(
            new LambdaQueryWrapper<AlertRecord>()
                .in(AlertRecord::getStatus, "RESOLVED", "DISMISSED", "EXPIRED")
                .lt(AlertRecord::getTriggerTime, cutoff)
        );
        log.info("清理旧告警记录: retention={}天, 软删除{}条", retentionDays, count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord dismiss(Long id, String resolvedBy) {
        AlertRecord record = alertRecordMapper.selectById(id);
        if (record == null) return null;
        record.setStatus("DISMISSED");
        record.setResolvedBy(resolvedBy);
        record.setResolveTime(LocalDateTime.now());
        alertRecordMapper.updateById(record);
        log.info("告警已忽略: id={}, by={}", id, resolvedBy);
        return record;
    }
}