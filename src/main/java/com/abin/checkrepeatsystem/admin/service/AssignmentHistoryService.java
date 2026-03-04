package com.abin.checkrepeatsystem.admin.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.admin.dto.AssignmentHistoryStatsDTO;
import com.abin.checkrepeatsystem.admin.dto.AssignmentRecordDTO;
import com.abin.checkrepeatsystem.admin.dto.DeleteAssignmentRecordsDTO;
import com.abin.checkrepeatsystem.admin.dto.RevokeAssignmentDTO;
import com.abin.checkrepeatsystem.admin.dto.ReassignTeacherDTO;
import com.abin.checkrepeatsystem.admin.dto.AvailableTeacherDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 分配记录管理服务接口
 */
public interface AssignmentHistoryService {
    
    /**
     * 获取分配记录统计信息
     */
    Result<AssignmentHistoryStatsDTO> getAssignmentHistoryStats();
    
    /**
     * 获取分配记录列表
     */
    Result<Page<AssignmentRecordDTO>> getAssignmentRecordList(
            String startDate,
            String endDate,
            String assignmentType,
            String major,
            String status,
            String keyword,
            Integer page,
            Integer size);
    
    /**
     * 删除分配记录
     */
    Result<String> deleteAssignmentRecords(DeleteAssignmentRecordsDTO request);
    
    /**
     * 撤销指导老师分配
     */
    Result<String> revokeAssignment(RevokeAssignmentDTO request);
    
    /**
     * 重新分配指导老师
     */
    Result<String> reassignTeacher(ReassignTeacherDTO request);
    
    /**
     * 获取可用于重新分配的教师列表
     */
    Result<List<AvailableTeacherDTO>> getAvailableTeachers();
    
    /**
     * 导出分配记录
     */
    void exportAssignmentRecords(
            String startDate,
            String endDate,
            String assignmentType,
            String major,
            String status,
            String keyword,
            HttpServletResponse response);
    
    /**
     * 刷新分配记录数据
     */
    Result<String> refreshAssignmentData();
}