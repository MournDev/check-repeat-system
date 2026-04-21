package com.abin.checkrepeatsystem.user.service;

import com.abin.checkrepeatsystem.pojo.entity.TeacherAllocationRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TeacherAllocationRecordService extends IService<TeacherAllocationRecord> {

    /**
     * 获取论文的分配记录
     * @param paperId 论文ID
     * @return 分配记录列表
     */
    List<TeacherAllocationRecord> getByPaperId(Long paperId);

    /**
     * 获取教师的分配记录
     * @param teacherId 教师ID
     * @return 分配记录列表
     */
    List<TeacherAllocationRecord> getByTeacherId(Long teacherId);

    /**
     * 获取学生的分配记录
     * @param studentId 学生ID
     * @return 分配记录列表
     */
    List<TeacherAllocationRecord> getByStudentId(Long studentId);

    /**
     * 创建分配记录
     * @param record 分配记录
     * @return 创建结果
     */
    boolean createRecord(TeacherAllocationRecord record);

    /**
     * 撤销分配记录
     * @param id 记录ID
     * @param reason 撤销原因
     * @return 撤销结果
     */
    boolean revokeRecord(Long id, String reason);

    /**
     * 获取论文的当前有效分配
     * @param paperId 论文ID
     * @return 当前有效分配记录
     */
    TeacherAllocationRecord getCurrentAllocation(Long paperId);

    /**
     * 批量创建分配记录
     * @param records 分配记录列表
     * @return 创建结果
     */
    boolean batchCreateRecords(List<TeacherAllocationRecord> records);

    /**
     * 获取分配统计信息
     * @param teacherId 教师ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 统计信息
     */
    java.util.Map<String, Object> getAllocationStats(Long teacherId, String startDate, String endDate);
}
