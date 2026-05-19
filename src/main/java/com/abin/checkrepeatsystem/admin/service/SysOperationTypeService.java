package com.abin.checkrepeatsystem.admin.service;

import com.abin.checkrepeatsystem.pojo.entity.SysOperationType;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 操作类型服务接口
 */
public interface SysOperationTypeService extends IService<SysOperationType> {

    /**
     * 分页查询操作类型
     */
    Page<SysOperationType> getOperationTypePage(int page, int size, String module, String keyword);

    /**
     * 根据类型获取操作类型信息
     */
    SysOperationType getOperationTypeByType(String type);

    /**
     * 获取所有操作类型
     */
    List<SysOperationType> getAllOperationTypes();

    /**
     * 按模块分组获取操作类型
     */
    Map<String, List<SysOperationType>> getOperationTypesByModule();

    /**
     * 创建操作类型
     */
    boolean createOperationType(SysOperationType operationType);

    /**
     * 更新操作类型
     */
    boolean updateOperationType(SysOperationType operationType);

    /**
     * 删除操作类型
     */
    boolean deleteOperationType(String id);

    /**
     * 批量删除操作类型
     */
    boolean batchDeleteOperationTypes(List<String> ids);

    /**
     * 启用/禁用操作类型
     */
    boolean updateOperationTypeStatus(String id, Integer status);
}
