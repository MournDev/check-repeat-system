package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.admin.service.SysOperationTypeService;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.SysOperationType;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 操作类型管理控制器
 */
@RestController
@RequestMapping("/api/admin/operation-types")
@PreAuthorize("hasAuthority('ADMIN')")
@Slf4j
public class SysOperationTypeController {

    @Resource
    private SysOperationTypeService sysOperationTypeService;

    /**
     * 分页查询操作类型
     */
    @GetMapping("/page")
    public Result<Page<SysOperationType>> getOperationTypePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String keyword) {

        log.info("接收分页查询操作类型请求: page={}, size={}, module={}, keyword={}",
                page, size, module, keyword);

        try {
            Page<SysOperationType> result = sysOperationTypeService.getOperationTypePage(page, size, module, keyword);
            log.info("分页查询操作类型成功: 总记录数={}", result.getTotal());
            return Result.success("查询成功", result);

        } catch (Exception e) {
            log.error("分页查询操作类型失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有操作类型
     */
    @GetMapping("/all")
    public Result<List<SysOperationType>> getAllOperationTypes() {
        log.info("接收获取所有操作类型请求");

        try {
            List<SysOperationType> operationTypes = sysOperationTypeService.getAllOperationTypes();
            log.info("获取所有操作类型成功: 数量={}", operationTypes.size());
            return Result.success("获取成功", operationTypes);

        } catch (Exception e) {
            log.error("获取所有操作类型失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 按模块分组获取操作类型
     */
    @GetMapping("/by-module")
    public Result<Map<String, List<SysOperationType>>> getOperationTypesByModule() {
        log.info("接收按模块分组获取操作类型请求");

        try {
            Map<String, List<SysOperationType>> operationTypesByModule = sysOperationTypeService.getOperationTypesByModule();
            log.info("按模块分组获取操作类型成功: 模块数={}", operationTypesByModule.size());
            return Result.success("获取成功", operationTypesByModule);

        } catch (Exception e) {
            log.error("按模块分组获取操作类型失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 根据类型获取操作类型信息
     */
    @GetMapping("/by-type/{type}")
    public Result<SysOperationType> getOperationTypeByType(@PathVariable String type) {
        log.info("接收根据类型获取操作类型请求: type={}", type);

        try {
            SysOperationType operationType = sysOperationTypeService.getOperationTypeByType(type);
            if (operationType == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "操作类型不存在");
            }
            return Result.success("获取成功", operationType);

        } catch (Exception e) {
            log.error("根据类型获取操作类型失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取失败: " + e.getMessage());
        }
    }

    /**
     * 创建操作类型
     */
    @PostMapping
    public Result<String> createOperationType(@RequestBody SysOperationType operationType) {
        log.info("接收创建操作类型请求: {}", operationType.getType());

        try {
            boolean result = sysOperationTypeService.createOperationType(operationType);
            if (result) {
                log.info("创建操作类型成功: {}", operationType.getType());
                return Result.success("创建成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "操作类型已存在");
            }

        } catch (Exception e) {
            log.error("创建操作类型失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新操作类型
     */
    @PutMapping
    public Result<String> updateOperationType(@RequestBody SysOperationType operationType) {
        log.info("接收更新操作类型请求: {}", operationType.getType());

        try {
            boolean result = sysOperationTypeService.updateOperationType(operationType);
            if (result) {
                log.info("更新操作类型成功: {}", operationType.getType());
                return Result.success("更新成功");
            } else {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "操作类型不存在");
            }

        } catch (Exception e) {
            log.error("更新操作类型失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除操作类型
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteOperationType(@PathVariable String id) {
        log.info("接收删除操作类型请求: id={}", id);

        try {
            boolean result = sysOperationTypeService.deleteOperationType(id);
            if (result) {
                log.info("删除操作类型成功: id={}", id);
                return Result.success("删除成功");
            } else {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "操作类型不存在");
            }

        } catch (Exception e) {
            log.error("删除操作类型失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除操作类型
     */
    @DeleteMapping("/batch")
    public Result<String> batchDeleteOperationTypes(@RequestBody List<String> ids) {
        log.info("接收批量删除操作类型请求: ids={}", ids);

        try {
            if (ids == null || ids.isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR, "请选择要删除的操作类型");
            }

            boolean result = sysOperationTypeService.batchDeleteOperationTypes(ids);
            if (result) {
                log.info("批量删除操作类型成功: 删除条数={}", ids.size());
                return Result.success("删除成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "删除失败");
            }

        } catch (Exception e) {
            log.error("批量删除操作类型失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用操作类型
     */
    @PutMapping("/status")
    public Result<String> updateOperationTypeStatus(@RequestParam String id, @RequestParam Integer status) {
        log.info("接收更新操作类型状态请求: id={}, status={}", id, status);

        try {
            boolean result = sysOperationTypeService.updateOperationTypeStatus(id, status);
            if (result) {
                log.info("更新操作类型状态成功: id={}, status={}", id, status);
                return Result.success("更新成功");
            } else {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "操作类型不存在");
            }

        } catch (Exception e) {
            log.error("更新操作类型状态失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "更新失败: " + e.getMessage());
        }
    }
}
