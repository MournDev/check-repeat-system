package com.abin.checkrepeatsystem.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.abin.checkrepeatsystem.pojo.entity.AlertRule;
import java.util.List;

public interface AlertRuleService {

    List<AlertRule> listAll();

    Page<AlertRule> listPage(int page, int size);

    List<AlertRule> listEnabled();

    AlertRule getById(Long id);

    AlertRule create(AlertRule rule);

    AlertRule update(Long id, AlertRule rule);

    boolean delete(Long id);

    boolean toggleEnabled(Long id, boolean enabled);

    int saveBatch(List<AlertRule> rules);
}