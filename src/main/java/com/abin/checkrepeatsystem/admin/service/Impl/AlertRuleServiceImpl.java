package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.mapper.AlertRuleMapper;
import com.abin.checkrepeatsystem.admin.service.AlertRuleService;
import com.abin.checkrepeatsystem.pojo.entity.AlertRule;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class AlertRuleServiceImpl implements AlertRuleService {

    @Resource
    private AlertRuleMapper alertRuleMapper;

    @Override
    public List<AlertRule> listAll() {
        return alertRuleMapper.selectList(
            new LambdaQueryWrapper<AlertRule>().orderByAsc(AlertRule::getRuleName)
        );
    }

    @Override
    public Page<AlertRule> listPage(int page, int size) {
        Page<AlertRule> p = new Page<>(page, size);
        return alertRuleMapper.selectPage(p,
            new LambdaQueryWrapper<AlertRule>().orderByAsc(AlertRule::getRuleName));
    }

    @Override
    public List<AlertRule> listEnabled() {
        return alertRuleMapper.selectList(
            new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getEnabled, true)
                .orderByAsc(AlertRule::getRuleName)
        );
    }

    @Override
    public AlertRule getById(Long id) {
        return alertRuleMapper.selectById(id);
    }

    @Override
    @Transactional
    public AlertRule create(AlertRule rule) {
        alertRuleMapper.insert(rule);
        log.info("创建告警规则: {}", rule.getRuleName());
        return rule;
    }

    @Override
    @Transactional
    public AlertRule update(Long id, AlertRule rule) {
        rule.setId(id);
        alertRuleMapper.updateById(rule);
        log.info("更新告警规则: id={}", id);
        return alertRuleMapper.selectById(id);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        int result = alertRuleMapper.deleteById(id);
        log.info("删除告警规则: id={}, result={}", id, result);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveBatch(List<AlertRule> rules) {
        int count = 0;
        for (AlertRule rule : rules) {
            if (rule.getId() != null) {
                if (alertRuleMapper.updateById(rule) > 0) count++;
            } else {
                if (alertRuleMapper.insert(rule) > 0) count++;
            }
        }
        log.info("批量保存告警规则: 成功{}条", count);
        return count;
    }

    @Override
    @Transactional
    public boolean toggleEnabled(Long id, boolean enabled) {
        AlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null) return false;
        rule.setEnabled(enabled);
        return alertRuleMapper.updateById(rule) > 0;
    }
}