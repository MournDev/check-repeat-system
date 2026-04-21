package com.abin.checkrepeatsystem.admin.service;

import com.abin.checkrepeatsystem.pojo.entity.DictData;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface DictDataService extends IService<DictData> {

    /**
     * 获取字典数据列表
     * @param page 页码
     * @param size 每页大小
     * @param dictType 字典类型（可选）
     * @param status 状态（可选）
     * @return 字典数据列表
     */
    List<DictData> getDictDataList(int page, int size, String dictType, Integer status);

    /**
     * 根据字典类型获取字典数据
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    List<DictData> getDictDataByType(String dictType);

    /**
     * 根据字典类型和值获取字典标签
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    String getDictLabel(String dictType, String dictValue);

    /**
     * 创建字典数据
     * @param dictData 字典数据
     * @return 创建结果
     */
    boolean createDictData(DictData dictData);

    /**
     * 更新字典数据
     * @param dictData 字典数据
     * @return 更新结果
     */
    boolean updateDictData(DictData dictData);

    /**
     * 删除字典数据
     * @param id 字典数据ID
     * @return 删除结果
     */
    boolean deleteDictData(Long id);

    /**
     * 批量删除字典数据
     * @param ids 字典数据ID列表
     * @return 删除结果
     */
    boolean batchDeleteDictData(List<Long> ids);

    /**
     * 获取所有字典类型
     * @return 字典类型列表
     */
    List<String> getDictTypes();

    /**
     * 获取字典统计信息
     * @return 统计信息
     */
    Map<String, Object> getDictStats();
}
