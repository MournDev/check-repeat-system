package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.admin.service.SystemParamService;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.SystemParam;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/system-params")
public class SystemParamController {

    @Resource
    private SystemParamService systemParamService;

    /**
     * 获取系统参数
     */
    @GetMapping("/get")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SystemParam> getSystemParam() {
        try {
            SystemParam param = systemParamService.getSystemParam();
            return Result.success(param);
        } catch (Exception e) {
            log.error("获取系统参数失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,"获取系统参数失败");
        }
    }

    /**
     * 更新系统参数
     */
    @PutMapping("/update")
    @OperationLog(type = "system_param_update", description = "更新系统参数", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<SystemParam> updateSystemParam(@RequestBody SystemParam systemParam) {
        try {
            boolean success = systemParamService.updateSystemParam(systemParam);
            if (success) {
                SystemParam updatedParam = systemParamService.getSystemParam();
                return Result.success("更新系统参数成功", updatedParam);
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR,"更新系统参数失败");
            }
        } catch (Exception e) {
            log.error("更新系统参数失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,"更新系统参数失败: " + e.getMessage());
        }
    }

    /**
     * 初始化系统参数
     */
    @PostMapping("/init")
    @OperationLog(type = "system_param_init", description = "初始化系统参数", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> initSystemParam() {
        try {
            boolean success = systemParamService.initSystemParam();
            if (success) {
                return Result.success("初始化系统参数成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR,"初始化系统参数失败");
            }
        } catch (Exception e) {
            log.error("初始化系统参数失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,"初始化系统参数失败: " + e.getMessage());
        }
    }

    /**
     * 获取论文最大大小
     */
    @GetMapping("/max-paper-size")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Long> getMaxPaperSize() {
        try {
            Long maxSize = systemParamService.getMaxPaperSize();
            return Result.success(maxSize);
        } catch (Exception e) {
            log.error("获取论文最大大小失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,"获取论文最大大小失败");
        }
    }

    /**
     * 获取最大并发查重数
     */
    @GetMapping("/max-concurrent-check")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Integer> getMaxConcurrentCheck() {
        try {
            Integer maxConcurrent = systemParamService.getMaxConcurrentCheck();
            return Result.success(maxConcurrent);
        } catch (Exception e) {
            log.error("获取最大并发查重数失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,"获取最大并发查重数失败");
        }
    }

    /**
     * 获取默认重复率阈值
     */
    @GetMapping("/default-threshold")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Double> getDefaultThreshold() {
        try {
            Double threshold = systemParamService.getDefaultThreshold();
            return Result.success(threshold);
        } catch (Exception e) {
            log.error("获取默认重复率阈值失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,"获取默认重复率阈值失败");
        }
    }

    /**
     * 设置系统维护状态
     */
    @PutMapping("/maintenance-status")
    @OperationLog(type = "system_maintenance_status", description = "设置系统维护状态", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> setMaintenanceStatus(@RequestParam Integer status, @RequestParam(required = false) String notice) {
        try {
            boolean success = systemParamService.setMaintenanceStatus(status, notice);
            if (success) {
                return Result.success(status == 1 ? "系统进入维护状态" : "系统恢复正常运行");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR,"设置维护状态失败");
            }
        } catch (Exception e) {
            log.error("设置系统维护状态失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,"设置维护状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统维护状态
     */
    @GetMapping("/maintenance-status")
    public Result<Integer> getMaintenanceStatus() {
        try {
            Integer status = systemParamService.getMaintenanceStatus();
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取系统维护状态失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR,"获取维护状态失败");
        }
    }
}
