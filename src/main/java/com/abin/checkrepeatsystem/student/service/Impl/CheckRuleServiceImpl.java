package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.pojo.entity.CheckRule;
import com.abin.checkrepeatsystem.student.mapper.CheckRuleMapper;
import com.abin.checkrepeatsystem.student.service.CheckRuleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class CheckRuleServiceImpl extends ServiceImpl<CheckRuleMapper, CheckRule> implements CheckRuleService {

    @Resource
    private CheckRuleMapper checkRuleMapper;

    @Override
    public List<CheckRule> getAllRules() {
        try {
            return checkRuleMapper.selectList(
                new LambdaQueryWrapper<CheckRule>()
                    .eq(CheckRule::getIsDeleted, 0)
                    .orderByDesc(CheckRule::getIsDefault)
                    .orderByDesc(CheckRule::getCreateTime)
            );
        } catch (Exception e) {
            log.error("获取所有查重规则失败", e);
            return null;
        }
    }

    @Override
    public CheckRule getDefaultRule() {
        try {
            return checkRuleMapper.selectOne(
                new LambdaQueryWrapper<CheckRule>()
                    .eq(CheckRule::getIsDefault, 1)
                    .eq(CheckRule::getIsDeleted, 0)
            );
        } catch (Exception e) {
            log.error("获取默认查重规则失败", e);
            return null;
        }
    }

    @Override
    public CheckRule getRuleByCode(String ruleCode) {
        try {
            return checkRuleMapper.selectOne(
                new LambdaQueryWrapper<CheckRule>()
                    .eq(CheckRule::getRuleCode, ruleCode)
                    .eq(CheckRule::getIsDeleted, 0)
            );
        } catch (Exception e) {
            log.error("根据规则编码获取规则失败 - 规则编码: {}", ruleCode, e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRule(CheckRule rule) {
        try {
            // 如果设置为默认规则，先将其他规则设置为非默认
            if (rule.getIsDefault() != null && rule.getIsDefault() == 1) {
                CheckRule updateRule = new CheckRule();
                updateRule.setIsDefault(0);
                checkRuleMapper.update(updateRule, 
                    new LambdaQueryWrapper<CheckRule>()
                        .eq(CheckRule::getIsDefault, 1)
                        .eq(CheckRule::getIsDeleted, 0)
                );
            }
            
            int result = checkRuleMapper.insert(rule);
            return result > 0;
        } catch (Exception e) {
            log.error("创建查重规则失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRule(CheckRule rule) {
        try {
            // 如果设置为默认规则，先将其他规则设置为非默认
            if (rule.getIsDefault() != null && rule.getIsDefault() == 1) {
                CheckRule updateRule = new CheckRule();
                updateRule.setIsDefault(0);
                checkRuleMapper.update(updateRule, 
                    new LambdaQueryWrapper<CheckRule>()
                        .eq(CheckRule::getIsDefault, 1)
                        .ne(CheckRule::getId, rule.getId())
                        .eq(CheckRule::getIsDeleted, 0)
                );
            }
            
            int result = checkRuleMapper.updateById(rule);
            return result > 0;
        } catch (Exception e) {
            log.error("更新查重规则失败 - 规则ID: {}", rule.getId(), e);
            return false;
        }
    }

    @Override
    public boolean deleteRule(Long id) {
        try {
            CheckRule rule = checkRuleMapper.selectById(id);
            if (rule != null && rule.getIsDefault() == 1) {
                log.warn("无法删除默认规则 - 规则ID: {}", id);
                return false;
            }
            
            int result = checkRuleMapper.deleteById(id);
            return result > 0;
        } catch (Exception e) {
            log.error("删除查重规则失败 - 规则ID: {}", id, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultRule(Long id) {
        try {
            // 先将所有规则设置为非默认
            CheckRule updateRule = new CheckRule();
            updateRule.setIsDefault(0);
            checkRuleMapper.update(updateRule, 
                new LambdaQueryWrapper<CheckRule>()
                    .eq(CheckRule::getIsDefault, 1)
                    .eq(CheckRule::getIsDeleted, 0)
            );
            
            // 将指定规则设置为默认
            CheckRule defaultRule = new CheckRule();
            defaultRule.setId(id);
            defaultRule.setIsDefault(1);
            int result = checkRuleMapper.updateById(defaultRule);
            return result > 0;
        } catch (Exception e) {
            log.error("设置默认规则失败 - 规则ID: {}", id, e);
            return false;
        }
    }

    @Override
    public boolean toggleRuleStatus(Long id, Integer enabled) {
        try {
            CheckRule rule = checkRuleMapper.selectById(id);
            if (rule != null && rule.getIsDefault() == 1 && enabled == 0) {
                log.warn("无法禁用默认规则 - 规则ID: {}", id);
                return false;
            }
            
            CheckRule updateRule = new CheckRule();
            updateRule.setId(id);
            updateRule.setIsDeleted(enabled == 1 ? 0 : 1);
            int result = checkRuleMapper.updateById(updateRule);
            return result > 0;
        } catch (Exception e) {
            log.error("启用/禁用规则失败 - 规则ID: {}", id, e);
            return false;
        }
    }
}
