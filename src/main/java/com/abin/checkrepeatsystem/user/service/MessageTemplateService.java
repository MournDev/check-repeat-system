package com.abin.checkrepeatsystem.user.service;

import com.abin.checkrepeatsystem.pojo.entity.MessageTemplate;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MessageTemplateService extends IService<MessageTemplate> {

    /**
     * 根据模板代码获取模板
     * @param templateCode 模板代码
     * @return 消息模板
     */
    MessageTemplate getByCode(String templateCode);

    /**
     * 根据模板类型获取模板列表
     * @param templateType 模板类型
     * @return 模板列表
     */
    List<MessageTemplate> getByType(String templateType);

    /**
     * 创建消息模板
     * @param template 消息模板
     * @return 创建结果
     */
    boolean createTemplate(MessageTemplate template);

    /**
     * 更新消息模板
     * @param template 消息模板
     * @return 更新结果
     */
    boolean updateTemplate(MessageTemplate template);

    /**
     * 删除消息模板
     * @param id 模板ID
     * @return 删除结果
     */
    boolean deleteTemplate(Long id);

    /**
     * 启用/禁用消息模板
     * @param id 模板ID
     * @param isActive 是否启用
     * @return 操作结果
     */
    boolean toggleTemplateStatus(Long id, Integer isActive);

    /**
     * 渲染模板
     * @param templateCode 模板代码
     * @param variables 变量
     * @return 渲染后的内容
     */
    String renderTemplate(String templateCode, java.util.Map<String, Object> variables);
}
