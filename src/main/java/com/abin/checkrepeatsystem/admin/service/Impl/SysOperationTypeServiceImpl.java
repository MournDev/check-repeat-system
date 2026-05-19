package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.mapper.SysOperationTypeMapper;
import com.abin.checkrepeatsystem.admin.service.SysOperationTypeService;
import com.abin.checkrepeatsystem.pojo.entity.SysOperationType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作类型服务实现
 */
@Service
@Slf4j
public class SysOperationTypeServiceImpl extends ServiceImpl<SysOperationTypeMapper, SysOperationType> implements SysOperationTypeService {

    @Resource
    private SysOperationTypeMapper sysOperationTypeMapper;

    @Override
    public Page<SysOperationType> getOperationTypePage(int page, int size, String module, String keyword) {
        Page<SysOperationType> operationTypePage = new Page<>(page, size);
        LambdaQueryWrapper<SysOperationType> wrapper = new LambdaQueryWrapper<>();

        // 模块筛选
        if (module != null && !module.isEmpty()) {
            wrapper.eq(SysOperationType::getModule, module);
        }

        // 关键字搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysOperationType::getName, keyword)
                    .or()
                    .like(SysOperationType::getType, keyword)
                    .or()
                    .like(SysOperationType::getDescription, keyword));
        }

        // 按创建时间倒序
        wrapper.orderByDesc(SysOperationType::getCreateTime);

        return sysOperationTypeMapper.selectPage(operationTypePage, wrapper);
    }

    @Override
    public SysOperationType getOperationTypeByType(String type) {
        LambdaQueryWrapper<SysOperationType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOperationType::getType, type);
        return sysOperationTypeMapper.selectOne(wrapper);
    }

    @Override
    public List<SysOperationType> getAllOperationTypes() {
        LambdaQueryWrapper<SysOperationType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysOperationType::getModule)
                .orderByAsc(SysOperationType::getType);
        return sysOperationTypeMapper.selectList(wrapper);
    }

    @Override
    public Map<String, List<SysOperationType>> getOperationTypesByModule() {
        List<SysOperationType> operationTypes = getAllOperationTypes();
        return operationTypes.stream()
                .collect(Collectors.groupingBy(SysOperationType::getModule));
    }

    @Override
    public boolean createOperationType(SysOperationType operationType) {
        try {
            // 检查类型是否已存在
            SysOperationType existing = getOperationTypeByType(operationType.getType());
            if (existing != null) {
                log.warn("操作类型已存在: {}", operationType.getType());
                return false;
            }

            boolean result = save(operationType);
            log.info("创建操作类型成功: {}", operationType.getType());
            return result;
        } catch (Exception e) {
            log.error("创建操作类型失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateOperationType(SysOperationType operationType) {
        try {
            boolean result = updateById(operationType);
            log.info("更新操作类型成功: {}", operationType.getType());
            return result;
        } catch (Exception e) {
            log.error("更新操作类型失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteOperationType(String id) {
        try {
            boolean result = removeById(id);
            log.info("删除操作类型成功: {}", id);
            return result;
        } catch (Exception e) {
            log.error("删除操作类型失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean batchDeleteOperationTypes(List<String> ids) {
        try {
            boolean result = removeByIds(ids);
            log.info("批量删除操作类型成功: 数量={}", ids.size());
            return result;
        } catch (Exception e) {
            log.error("批量删除操作类型失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateOperationTypeStatus(String id, Integer status) {
        try {
            SysOperationType operationType = new SysOperationType();
            operationType.setId(id);
            operationType.setStatus(status);
            boolean result = updateById(operationType);
            log.info("更新操作类型状态成功: id={}, status={}", id, status);
            return result;
        } catch (Exception e) {
            log.error("更新操作类型状态失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
