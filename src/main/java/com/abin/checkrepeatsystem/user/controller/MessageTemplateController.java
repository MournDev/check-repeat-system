package com.abin.checkrepeatsystem.user.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.MessageTemplate;
import com.abin.checkrepeatsystem.user.service.MessageTemplateService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/message-templates")
public class MessageTemplateController {

    @Resource
    private MessageTemplateService messageTemplateService;

    /**
     * 获取消息模板列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Page<MessageTemplate>> getTemplateList(
            @RequestParam(required = false) String templateType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Page<MessageTemplate> page = new Page<>(pageNum, pageSize);
            Page<MessageTemplate> result = messageTemplateService.page(page);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取消息模板列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,"获取消息模板列表失败");
        }
    }

    /**
     * 根据模板代码获取模板
     */
    @GetMapping("/code/{templateCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<MessageTemplate> getTemplateByCode(@PathVariable String templateCode) {
        try {
            MessageTemplate template = messageTemplateService.getByCode(templateCode);
            if (template == null) {
                return Result.error(ResultCode.SYSTEM_ERROR,"模板不存在");
            }
            return Result.success(template);
        } catch (Exception e) {
            log.error("根据模板代码获取模板失败 - 模板代码: {}", templateCode, e);
            return Result.error(ResultCode.SYSTEM_ERROR,"获取模板失败");
        }
    }

    /**
     * 根据模板类型获取模板列表
     */
    @GetMapping("/type/{templateType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<MessageTemplate>> getTemplatesByType(@PathVariable String templateType) {
        try {
            List<MessageTemplate> templates = messageTemplateService.getByType(templateType);
            return Result.success(templates);
        } catch (Exception e) {
            log.error("根据模板类型获取模板列表失败 - 模板类型: {}", templateType, e);
            return Result.error(ResultCode.SYSTEM_ERROR,"获取模板列表失败");
        }
    }

    /**
     * 创建消息模板
     */
    @PostMapping("/create")
    @OperationLog(type = "message_template_create", description = "创建消息模板", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MessageTemplate> createTemplate(@RequestBody MessageTemplate template) {
        try {
            boolean success = messageTemplateService.createTemplate(template);
            if (success) {
                return Result.success("创建模板成功", template);
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR,"创建模板失败");
            }
        } catch (Exception e) {
            log.error("创建消息模板失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,"创建模板失败: " + e.getMessage());
        }
    }

    /**
     * 更新消息模板
     */
    @PutMapping("/update")
    @OperationLog(type = "message_template_update", description = "更新消息模板", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MessageTemplate> updateTemplate(@RequestBody MessageTemplate template) {
        try {
            boolean success = messageTemplateService.updateTemplate(template);
            if (success) {
                return Result.success("更新模板成功", template);
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR,"更新模板失败");
            }
        } catch (Exception e) {
            log.error("更新消息模板失败 - 模板ID: {}", template.getId(), e);
            return Result.error(ResultCode.SYSTEM_ERROR,"更新模板失败: " + e.getMessage());
        }
    }

    /**
     * 删除消息模板
     */
    @DeleteMapping("/delete/{id}")
    @OperationLog(type = "message_template_delete", description = "删除消息模板", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteTemplate(@PathVariable Long id) {
        try {
            boolean success = messageTemplateService.deleteTemplate(id);
            if (success) {
                return Result.success("删除模板成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR,"删除模板失败");
            }
        } catch (Exception e) {
            log.error("删除消息模板失败 - 模板ID: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR,"删除模板失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用消息模板
     */
    @PutMapping("/status/{id}")
    @OperationLog(type = "message_template_status", description = "切换消息模板状态", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> toggleTemplateStatus(@PathVariable Long id, @RequestParam Integer isActive) {
        try {
            boolean success = messageTemplateService.toggleTemplateStatus(id, isActive);
            if (success) {
                return Result.success(isActive == 1 ? "启用模板成功" : "禁用模板成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR,"操作失败");
            }
        } catch (Exception e) {
            log.error("切换消息模板状态失败 - 模板ID: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR,"操作失败: " + e.getMessage());
        }
    }

    /**
     * 渲染模板
     */
    @PostMapping("/render/{templateCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<String> renderTemplate(@PathVariable String templateCode, @RequestBody java.util.Map<String, Object> variables) {
        try {
            String renderedContent = messageTemplateService.renderTemplate(templateCode, variables);
            return Result.success(renderedContent);
        } catch (Exception e) {
            log.error("渲染模板失败 - 模板代码: {}", templateCode, e);
            return Result.error(ResultCode.SYSTEM_ERROR,"渲染模板失败: " + e.getMessage());
        }
    }
}
