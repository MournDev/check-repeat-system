package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.DictData;
import com.abin.checkrepeatsystem.admin.service.DictDataService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dict-data")
public class DictDataController {

    @Resource
    private DictDataService dictDataService;

    /**
     * 获取字典数据列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<DictData>> getDictDataList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) Integer status) {
        try {
            List<DictData> dictDataList = dictDataService.getDictDataList(page, size, dictType, status);
            return Result.success(dictDataList);
        } catch (Exception e) {
            log.error("获取字典数据列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取字典数据列表失败");
        }
    }

    /**
     * 根据字典类型获取字典数据
     */
    @GetMapping("/by-type/{dictType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public Result<List<DictData>> getDictDataByType(@PathVariable String dictType) {
        try {
            List<DictData> dictDataList = dictDataService.getDictDataByType(dictType);
            return Result.success(dictDataList);
        } catch (Exception e) {
            log.error("根据字典类型获取字典数据失败 - 类型: {}", dictType, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取字典数据失败");
        }
    }

    /**
     * 根据字典类型和值获取字典标签
     */
    @GetMapping("/label")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public Result<String> getDictLabel(@RequestParam String dictType, @RequestParam String dictValue) {
        try {
            String dictLabel = dictDataService.getDictLabel(dictType, dictValue);
            return Result.success(dictLabel);
        } catch (Exception e) {
            log.error("根据字典类型和值获取字典标签失败 - 类型: {}, 值: {}", dictType, dictValue, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取字典标签失败");
        }
    }

    /**
     * 创建字典数据
     */
    @PostMapping("/create")
    @OperationLog(type = "dict_data_create", description = "创建字典数据", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DictData> createDictData(@RequestBody DictData dictData) {
        try {
            boolean success = dictDataService.createDictData(dictData);
            if (success) {
                return Result.success("创建字典数据成功", dictData);
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "创建字典数据失败，数据已存在");
            }
        } catch (Exception e) {
            log.error("创建字典数据失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "创建字典数据失败: " + e.getMessage());
        }
    }

    /**
     * 更新字典数据
     */
    @PutMapping("/update")
    @OperationLog(type = "dict_data_update", description = "更新字典数据", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DictData> updateDictData(@RequestBody DictData dictData) {
        try {
            boolean success = dictDataService.updateDictData(dictData);
            if (success) {
                return Result.success("更新字典数据成功", dictData);
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "更新字典数据失败，数据已存在");
            }
        } catch (Exception e) {
            log.error("更新字典数据失败 - ID: {}", dictData.getId(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "更新字典数据失败: " + e.getMessage());
        }
    }

    /**
     * 删除字典数据
     */
    @DeleteMapping("/delete/{id}")
    @OperationLog(type = "dict_data_delete", description = "删除字典数据", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteDictData(@PathVariable Long id) {
        try {
            boolean success = dictDataService.deleteDictData(id);
            if (success) {
                return Result.success("删除字典数据成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "删除字典数据失败");
            }
        } catch (Exception e) {
            log.error("删除字典数据失败 - ID: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "删除字典数据失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除字典数据
     */
    @DeleteMapping("/batch-delete")
    @OperationLog(type = "dict_data_batch_delete", description = "批量删除字典数据", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchDeleteDictData(@RequestBody List<Long> ids) {
        try {
            boolean success = dictDataService.batchDeleteDictData(ids);
            if (success) {
                return Result.success("批量删除字典数据成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "批量删除字典数据失败");
            }
        } catch (Exception e) {
            log.error("批量删除字典数据失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量删除字典数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有字典类型
     */
    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<String>> getDictTypes() {
        try {
            List<String> dictTypes = dictDataService.getDictTypes();
            return Result.success(dictTypes);
        } catch (Exception e) {
            log.error("获取所有字典类型失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取字典类型失败");
        }
    }

    /**
     * 获取字典统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getDictStats() {
        try {
            Map<String, Object> stats = dictDataService.getDictStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取字典统计信息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取字典统计信息失败");
        }
    }
}
