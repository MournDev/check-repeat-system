package com.abin.checkrepeatsystem.student.service;

import com.abin.checkrepeatsystem.pojo.entity.CheckRule;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CheckRuleService extends IService<CheckRule> {

    /**
     * 获取所有查重规则
     * @return 规则列表
     */
    List<CheckRule> getAllRules();

    /**
     * 获取默认规则
     * @return 默认规则
     */
    CheckRule getDefaultRule();

    /**
     * 根据规则编码获取规则
     * @param ruleCode 规则编码
     * @return 规则
     */
    CheckRule getRuleByCode(String ruleCode);

    /**
     * 创建查重规则
     * @param rule 规则
     * @return 创建结果
     */
    boolean createRule(CheckRule rule);

    /**
     * 更新查重规则
     * @param rule 规则
     * @return 更新结果
     */
    boolean updateRule(CheckRule rule);

    /**
     * 删除查重规则
     * @param id 规则ID
     * @return 删除结果
     */
    boolean deleteRule(Long id);

    /**
     * 设置默认规则
     * @param id 规则ID
     * @return 设置结果
     */
    boolean setDefaultRule(Long id);

    /**
     * 启用/禁用规则
     * @param id 规则ID
     * @param enabled 是否启用
     * @return 操作结果
     */
    boolean toggleRuleStatus(Long id, Integer enabled);
}
