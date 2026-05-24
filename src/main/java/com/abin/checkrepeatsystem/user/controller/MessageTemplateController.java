package com.abin.checkrepeatsystem.user.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.MessageTemplate;
import com.abin.checkrepeatsystem.user.service.MessageTemplateService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/message-templates")
public class MessageTemplateController {

    private final MessageTemplateService messageTemplateService;

    /**
     * 获取消息模板列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Page<MessageTemplate>> getTemplateList(
            @RequestParam(required = false) String templateType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<MessageTemplate> page = new Page<>(pageNum, pageSize);
        Page<MessageTemplate> result = messageTemplateService.page(page);
        return Result.success(result);
    }

    /**
     * 根据模板代码获取模板
     */
    @GetMapping("/code/{templateCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<MessageTemplate> getTemplateByCode(@PathVariable String templateCode) {
        MessageTemplate template = messageTemplateService.getByCode(templateCode);
        if (template == null) {
            return Result.error(ResultCode.SYSTEM_ERROR,"模板不存在");
        }
        return Result.success(template);
    }

    /**
     * 根据模板类型获取模板列表
     */
    @GetMapping("/type/{templateType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<MessageTemplate>> getTemplatesByType(@PathVariable String templateType) {
        List<MessageTemplate> templates = messageTemplateService.getByType(templateType);
        return Result.success(templates);
    }

    /**
     * 创建消息模板
     */
    @PostMapping("/create")
    @OperationLog(type = "message_template_create", description = "创建消息模板", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MessageTemplate> createTemplate(@RequestBody MessageTemplate template) {
        boolean success = messageTemplateService.createTemplate(template);
        if (success) {
            return Result.success("创建模板成功", template);
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR,"创建模板失败");
        }
    }

    /**
     * 更新消息模板
     */
    @PutMapping("/update")
    @OperationLog(type = "message_template_update", description = "更新消息模板", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<MessageTemplate> updateTemplate(@RequestBody MessageTemplate template) {
        boolean success = messageTemplateService.updateTemplate(template);
        if (success) {
            return Result.success("更新模板成功", template);
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR,"更新模板失败");
        }
    }

    /**
     * 删除消息模板
     */
    @DeleteMapping("/delete/{id}")
    @OperationLog(type = "message_template_delete", description = "删除消息模板", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteTemplate(@PathVariable Long id) {
        boolean success = messageTemplateService.deleteTemplate(id);
        if (success) {
            return Result.success("删除模板成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR,"删除模板失败");
        }
    }

    /**
     * 启用/禁用消息模板
     */
    @PutMapping("/status/{id}")
    @OperationLog(type = "message_template_status", description = "切换消息模板状态", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> toggleTemplateStatus(@PathVariable Long id, @RequestParam Integer isActive) {
        boolean success = messageTemplateService.toggleTemplateStatus(id, isActive);
        if (success) {
            return Result.success(isActive == 1 ? "启用模板成功" : "禁用模板成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR,"操作失败");
        }
    }

    /**
     * 渲染模板
     */
    @PostMapping("/render/{templateCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<String> renderTemplate(@PathVariable String templateCode, @RequestBody java.util.Map<String, Object> variables) {
        String renderedContent = messageTemplateService.renderTemplate(templateCode, variables);
        return Result.success(renderedContent);
    }
}
