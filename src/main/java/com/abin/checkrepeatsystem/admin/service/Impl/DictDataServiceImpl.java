package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.pojo.entity.DictData;
import com.abin.checkrepeatsystem.admin.mapper.DictDataMapper;
import com.abin.checkrepeatsystem.admin.service.DictDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class DictDataServiceImpl extends ServiceImpl<DictDataMapper, DictData> implements DictDataService {

    @Resource
    private DictDataMapper dictDataMapper;

    @Override
    public List<DictData> getDictDataList(int page, int size, String dictType, Integer status) {
        try {
            LambdaQueryWrapper<DictData> queryWrapper = new LambdaQueryWrapper<DictData>()
                .eq(DictData::getIsDeleted, 0);
            
            if (dictType != null && !dictType.isEmpty()) {
                queryWrapper.eq(DictData::getDictType, dictType);
            }
            
            if (status != null) {
                queryWrapper.eq(DictData::getStatus, status);
            }
            
            queryWrapper.orderByAsc(DictData::getDictType).orderByAsc(DictData::getSort);
            
            int offset = (page - 1) * size;
            queryWrapper.last("LIMIT " + offset + ", " + size);
            
            List<DictData> dictDataList = dictDataMapper.selectList(queryWrapper);
            
            // 设置状态文本
            for (DictData dictData : dictDataList) {
                dictData.setStatusText(dictData.getStatus() == 1 ? "启用" : "禁用");
            }
            
            return dictDataList;
        } catch (Exception e) {
            log.error("获取字典数据列表失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<DictData> getDictDataByType(String dictType) {
        try {
            List<DictData> dictDataList = dictDataMapper.selectList(
                new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getDictType, dictType)
                    .eq(DictData::getStatus, 1)
                    .eq(DictData::getIsDeleted, 0)
                    .orderByAsc(DictData::getSort)
            );
            
            // 设置状态文本
            for (DictData dictData : dictDataList) {
                dictData.setStatusText(dictData.getStatus() == 1 ? "启用" : "禁用");
            }
            
            return dictDataList;
        } catch (Exception e) {
            log.error("根据字典类型获取字典数据失败 - 类型: {}", dictType, e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getDictLabel(String dictType, String dictValue) {
        try {
            DictData dictData = dictDataMapper.selectOne(
                new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getDictType, dictType)
                    .eq(DictData::getDictValue, dictValue)
                    .eq(DictData::getStatus, 1)
                    .eq(DictData::getIsDeleted, 0)
            );
            return dictData != null ? dictData.getDictLabel() : dictValue;
        } catch (Exception e) {
            log.error("根据字典类型和值获取字典标签失败 - 类型: {}, 值: {}", dictType, dictValue, e);
            return dictValue;
        }
    }

    @Override
    public boolean createDictData(DictData dictData) {
        try {
            // 检查字典类型和值是否已存在
            DictData existing = dictDataMapper.selectOne(
                new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getDictType, dictData.getDictType())
                    .eq(DictData::getDictValue, dictData.getDictValue())
                    .eq(DictData::getIsDeleted, 0)
            );
            
            if (existing != null) {
                log.warn("字典数据已存在 - 类型: {}, 值: {}", dictData.getDictType(), dictData.getDictValue());
                return false;
            }
            
            // 设置默认值
            if (dictData.getSort() == null) {
                dictData.setSort(0);
            }
            if (dictData.getStatus() == null) {
                dictData.setStatus(1);
            }
            
            dictData.setCreateTime(LocalDateTime.now());
            dictData.setUpdateTime(LocalDateTime.now());
            
            int result = dictDataMapper.insert(dictData);
            return result > 0;
        } catch (Exception e) {
            log.error("创建字典数据失败", e);
            return false;
        }
    }

    @Override
    public boolean updateDictData(DictData dictData) {
        try {
            // 检查字典类型和值是否已存在（排除当前记录）
            DictData existing = dictDataMapper.selectOne(
                new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getDictType, dictData.getDictType())
                    .eq(DictData::getDictValue, dictData.getDictValue())
                    .ne(DictData::getId, dictData.getId())
                    .eq(DictData::getIsDeleted, 0)
            );
            
            if (existing != null) {
                log.warn("字典数据已存在 - 类型: {}, 值: {}", dictData.getDictType(), dictData.getDictValue());
                return false;
            }
            
            dictData.setUpdateTime(LocalDateTime.now());
            
            int result = dictDataMapper.updateById(dictData);
            return result > 0;
        } catch (Exception e) {
            log.error("更新字典数据失败 - ID: {}", dictData.getId(), e);
            return false;
        }
    }

    @Override
    public boolean deleteDictData(Long id) {
        try {
            DictData dictData = new DictData();
            dictData.setId(id);
            dictData.setIsDeleted(1);
            dictData.setUpdateTime(LocalDateTime.now());
            
            int result = dictDataMapper.updateById(dictData);
            return result > 0;
        } catch (Exception e) {
            log.error("删除字典数据失败 - ID: {}", id, e);
            return false;
        }
    }

    @Override
    public boolean batchDeleteDictData(List<Long> ids) {
        try {
            for (Long id : ids) {
                DictData dictData = new DictData();
                dictData.setId(id);
                dictData.setIsDeleted(1);
                dictData.setUpdateTime(LocalDateTime.now());
                dictDataMapper.updateById(dictData);
            }
            return true;
        } catch (Exception e) {
            log.error("批量删除字典数据失败", e);
            return false;
        }
    }

    @Override
    public List<String> getDictTypes() {
        try {
            List<DictData> dictDataList = dictDataMapper.selectList(
                new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getIsDeleted, 0)
                    .groupBy(DictData::getDictType)
            );
            
            List<String> dictTypes = new ArrayList<>();
            for (DictData dictData : dictDataList) {
                dictTypes.add(dictData.getDictType());
            }
            
            return dictTypes;
        } catch (Exception e) {
            log.error("获取所有字典类型失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getDictStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 统计总字典数据数
            long totalCount = dictDataMapper.selectCount(
                new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getIsDeleted, 0)
            );
            stats.put("totalCount", totalCount);
            
            // 统计启用的字典数据数
            long enabledCount = dictDataMapper.selectCount(
                new LambdaQueryWrapper<DictData>()
                    .eq(DictData::getStatus, 1)
                    .eq(DictData::getIsDeleted, 0)
            );
            stats.put("enabledCount", enabledCount);
            
            // 统计字典类型数
            List<String> dictTypes = getDictTypes();
            stats.put("typeCount", dictTypes.size());
            
            // 统计各类型字典数据数
            Map<String, Long> typeCountMap = new HashMap<>();
            for (String dictType : dictTypes) {
                long count = dictDataMapper.selectCount(
                    new LambdaQueryWrapper<DictData>()
                        .eq(DictData::getDictType, dictType)
                        .eq(DictData::getIsDeleted, 0)
                );
                typeCountMap.put(dictType, count);
            }
            stats.put("typeCountMap", typeCountMap);
            
            return stats;
        } catch (Exception e) {
            log.error("获取字典统计信息失败", e);
            return Collections.emptyMap();
        }
    }
}
