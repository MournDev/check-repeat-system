package com.abin.checkrepeatsystem.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.abin.checkrepeatsystem.pojo.entity.AlertRecord;
import java.util.List;

public interface AlertService {

    List<AlertRecord> listActive();

    Page<AlertRecord> listActivePage(int page, int size);

    List<AlertRecord> listByStatus(String status, int page, int size);

    AlertRecord create(AlertRecord record);

    AlertRecord resolve(Long id, String resolvedBy);

    AlertRecord dismiss(Long id, String resolvedBy);

    boolean existsActiveByRuleId(Long ruleId);

    int cleanupOldAlerts(int retentionDays);
}