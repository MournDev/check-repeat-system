package com.abin.checkrepeatsystem.user.service.Impl;

import com.abin.checkrepeatsystem.pojo.entity.MessageTemplate;
import com.abin.checkrepeatsystem.user.mapper.MessageTemplateMapper;
import com.abin.checkrepeatsystem.user.service.MessageTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MessageTemplateServiceImpl extends ServiceImpl<MessageTemplateMapper, MessageTemplate> implements MessageTemplateService {

    @Resource
    private MessageTemplateMapper messageTemplateMapper;

    @Override
    public MessageTemplate getByCode(String templateCode) {
        try {
            return messageTemplateMapper.selectByCode(templateCode);
        } catch (Exception e) {
            log.error("根据模板代码获取模板失败 - 模板代码: {}", templateCode, e);
            return null;
        }
    }

    @Override
    public List<MessageTemplate> getByType(String templateType) {
        try {
            return messageTemplateMapper.selectByType(templateType);
        } catch (Exception e) {
            log.error("根据模板类型获取模板列表失败 - 模板类型: {}", templateType, e);
            return null;
        }
    }

    @Override
    public boolean createTemplate(MessageTemplate template) {
        try {
            template.setCreateTime(LocalDateTime.now());
            template.setUpdateTime(LocalDateTime.now());
            template.setIsActive(1); // 默认启用
            int result = messageTemplateMapper.insert(template);
            return result > 0;
        } catch (Exception e) {
            log.error("创建消息模板失败", e);
            return false;
        }
    }

    @Override
    public boolean updateTemplate(MessageTemplate template) {
        try {
            template.setUpdateTime(LocalDateTime.now());
            int result = messageTemplateMapper.updateById(template);
            return result > 0;
        } catch (Exception e) {
            log.error("更新消息模板失败 - 模板ID: {}", template.getId(), e);
            return false;
        }
    }

    @Override
    public boolean deleteTemplate(Long id) {
        try {
            int result = messageTemplateMapper.deleteById(id);
            return result > 0;
        } catch (Exception e) {
            log.error("删除消息模板失败 - 模板ID: {}", id, e);
            return false;
        }
    }

    @Override
    public boolean toggleTemplateStatus(Long id, Integer isActive) {
        try {
            MessageTemplate template = new MessageTemplate();
            template.setId(id);
            template.setIsActive(isActive);
            template.setUpdateTime(LocalDateTime.now());
            int result = messageTemplateMapper.updateById(template);
            return result > 0;
        } catch (Exception e) {
            log.error("切换消息模板状态失败 - 模板ID: {}", id, e);
            return false;
        }
    }

    @Override
    public String renderTemplate(String templateCode, Map<String, Object> variables) {
        try {
            MessageTemplate template = getByCode(templateCode);
            if (template == null) {
                log.warn("模板不存在 - 模板代码: {}", templateCode);
                return "";
            }

            String content = template.getContentTemplate();
            if (!StringUtils.hasText(content)) {
                return "";
            }

            // 替换模板中的变量
            Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(content);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String variable = matcher.group(1);
                Object value = variables.get(variable);
                String replacement = value != null ? value.toString() : "";
                matcher.appendReplacement(sb, replacement);
            }
            matcher.appendTail(sb);

            return sb.toString();
        } catch (Exception e) {
            log.error("渲染模板失败 - 模板代码: {}", templateCode, e);
            return "";
        }
    }
}
