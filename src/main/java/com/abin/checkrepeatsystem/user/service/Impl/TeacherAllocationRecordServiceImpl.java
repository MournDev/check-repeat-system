package com.abin.checkrepeatsystem.user.service.Impl;

import com.abin.checkrepeatsystem.pojo.entity.TeacherAllocationRecord;
import com.abin.checkrepeatsystem.user.mapper.TeacherAllocationRecordMapper;
import com.abin.checkrepeatsystem.user.service.TeacherAllocationRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class TeacherAllocationRecordServiceImpl extends ServiceImpl<TeacherAllocationRecordMapper, TeacherAllocationRecord> implements TeacherAllocationRecordService {

    @Resource
    private TeacherAllocationRecordMapper teacherAllocationRecordMapper;

    @Override
    public List<TeacherAllocationRecord> getByPaperId(Long paperId) {
        try {
            return teacherAllocationRecordMapper.selectList(
                new LambdaQueryWrapper<TeacherAllocationRecord>()
                    .eq(TeacherAllocationRecord::getPaperId, paperId)
                    .eq(TeacherAllocationRecord::getIsDeleted, 0)
                    .orderByDesc(TeacherAllocationRecord::getAllocationTime)
            );
        } catch (Exception e) {
            log.error("获取论文分配记录失败 - 论文ID: {}", paperId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<TeacherAllocationRecord> getByTeacherId(Long teacherId) {
        try {
            return teacherAllocationRecordMapper.selectList(
                new LambdaQueryWrapper<TeacherAllocationRecord>()
                    .eq(TeacherAllocationRecord::getTeacherId, teacherId)
                    .eq(TeacherAllocationRecord::getIsDeleted, 0)
                    .orderByDesc(TeacherAllocationRecord::getAllocationTime)
            );
        } catch (Exception e) {
            log.error("获取教师分配记录失败 - 教师ID: {}", teacherId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<TeacherAllocationRecord> getByStudentId(Long studentId) {
        try {
            return teacherAllocationRecordMapper.selectList(
                new LambdaQueryWrapper<TeacherAllocationRecord>()
                    .eq(TeacherAllocationRecord::getStudentId, studentId)
                    .eq(TeacherAllocationRecord::getIsDeleted, 0)
                    .orderByDesc(TeacherAllocationRecord::getAllocationTime)
            );
        } catch (Exception e) {
            log.error("获取学生分配记录失败 - 学生ID: {}", studentId, e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRecord(TeacherAllocationRecord record) {
        try {
            // 先将该论文的其他分配记录设置为已撤销
            TeacherAllocationRecord updateRecord = new TeacherAllocationRecord();
            updateRecord.setAllocationStatus("revoked");
            updateRecord.setUpdateTime(LocalDateTime.now());
            teacherAllocationRecordMapper.update(updateRecord, 
                new LambdaQueryWrapper<TeacherAllocationRecord>()
                    .eq(TeacherAllocationRecord::getPaperId, record.getPaperId())
                    .eq(TeacherAllocationRecord::getAllocationStatus, "active")
                    .eq(TeacherAllocationRecord::getIsDeleted, 0)
            );
            
            // 设置分配时间和状态
            record.setAllocationTime(LocalDateTime.now());
            record.setAllocationStatus("active");
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            
            int result = teacherAllocationRecordMapper.insert(record);
            return result > 0;
        } catch (Exception e) {
            log.error("创建分配记录失败", e);
            return false;
        }
    }

    @Override
    public boolean revokeRecord(Long id, String reason) {
        try {
            TeacherAllocationRecord record = teacherAllocationRecordMapper.selectById(id);
            if (record == null || record.getIsDeleted() == 1) {
                log.warn("撤销分配记录失败：记录不存在或已删除 - 记录ID: {}", id);
                return false;
            }
            
            TeacherAllocationRecord updateRecord = new TeacherAllocationRecord();
            updateRecord.setId(id);
            updateRecord.setAllocationStatus("revoked");
            updateRecord.setAllocationReason(record.getAllocationReason() + " (已撤销：" + reason + ")");
            updateRecord.setUpdateTime(LocalDateTime.now());
            
            int result = teacherAllocationRecordMapper.updateById(updateRecord);
            return result > 0;
        } catch (Exception e) {
            log.error("撤销分配记录失败 - 记录ID: {}", id, e);
            return false;
        }
    }

    @Override
    public TeacherAllocationRecord getCurrentAllocation(Long paperId) {
        try {
            return teacherAllocationRecordMapper.selectOne(
                new LambdaQueryWrapper<TeacherAllocationRecord>()
                    .eq(TeacherAllocationRecord::getPaperId, paperId)
                    .eq(TeacherAllocationRecord::getAllocationStatus, "active")
                    .eq(TeacherAllocationRecord::getIsDeleted, 0)
                    .orderByDesc(TeacherAllocationRecord::getAllocationTime)
                    .last("LIMIT 1")
            );
        } catch (Exception e) {
            log.error("获取当前有效分配失败 - 论文ID: {}", paperId, e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchCreateRecords(List<TeacherAllocationRecord> records) {
        try {
            for (TeacherAllocationRecord record : records) {
                // 先将该论文的其他分配记录设置为已撤销
                TeacherAllocationRecord updateRecord = new TeacherAllocationRecord();
                updateRecord.setAllocationStatus("revoked");
                updateRecord.setUpdateTime(LocalDateTime.now());
                teacherAllocationRecordMapper.update(updateRecord, 
                    new LambdaQueryWrapper<TeacherAllocationRecord>()
                        .eq(TeacherAllocationRecord::getPaperId, record.getPaperId())
                        .eq(TeacherAllocationRecord::getAllocationStatus, "active")
                        .eq(TeacherAllocationRecord::getIsDeleted, 0)
                );
                
                // 设置分配时间和状态
                record.setAllocationTime(LocalDateTime.now());
                record.setAllocationStatus("active");
                record.setCreateTime(LocalDateTime.now());
                record.setUpdateTime(LocalDateTime.now());
            }
            
            return saveBatch(records);
        } catch (Exception e) {
            log.error("批量创建分配记录失败", e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getAllocationStats(Long teacherId, String startDate, String endDate) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            LambdaQueryWrapper<TeacherAllocationRecord> queryWrapper = new LambdaQueryWrapper<TeacherAllocationRecord>()
                .eq(TeacherAllocationRecord::getIsDeleted, 0);
            
            if (teacherId != null) {
                queryWrapper.eq(TeacherAllocationRecord::getTeacherId, teacherId);
            }
            
            // 统计总分配次数
            long totalCount = teacherAllocationRecordMapper.selectCount(queryWrapper);
            stats.put("totalCount", totalCount);
            
            // 统计当前有效分配数
            long activeCount = teacherAllocationRecordMapper.selectCount(
                queryWrapper.clone()
                    .eq(TeacherAllocationRecord::getAllocationStatus, "active")
            );
            stats.put("activeCount", activeCount);
            
            // 统计分配类型分布
            Map<String, Long> typeStats = new HashMap<>();
            List<TeacherAllocationRecord> records = teacherAllocationRecordMapper.selectList(queryWrapper);
            for (TeacherAllocationRecord record : records) {
                String type = record.getAllocationType();
                typeStats.put(type, typeStats.getOrDefault(type, 0L) + 1);
            }
            stats.put("typeStats", typeStats);
            
            return stats;
        } catch (Exception e) {
            log.error("获取分配统计信息失败", e);
            return Collections.emptyMap();
        }
    }
}
