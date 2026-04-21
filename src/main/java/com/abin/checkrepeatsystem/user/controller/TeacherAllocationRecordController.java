package com.abin.checkrepeatsystem.user.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.TeacherAllocationRecord;
import com.abin.checkrepeatsystem.user.service.TeacherAllocationRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/teacher-allocations")
public class TeacherAllocationRecordController {

    @Resource
    private TeacherAllocationRecordService teacherAllocationRecordService;

    /**
     * 获取论文的分配记录
     */
    @GetMapping("/paper/{paperId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<TeacherAllocationRecord>> getByPaperId(@PathVariable Long paperId) {
        try {
            List<TeacherAllocationRecord> records = teacherAllocationRecordService.getByPaperId(paperId);
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取论文分配记录失败 - 论文ID: {}", paperId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取分配记录失败");
        }
    }

    /**
     * 获取教师的分配记录
     */
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<TeacherAllocationRecord>> getByTeacherId(@PathVariable Long teacherId) {
        try {
            List<TeacherAllocationRecord> records = teacherAllocationRecordService.getByTeacherId(teacherId);
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取教师分配记录失败 - 教师ID: {}", teacherId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取分配记录失败");
        }
    }

    /**
     * 获取学生的分配记录
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<TeacherAllocationRecord>> getByStudentId(@PathVariable Long studentId) {
        try {
            List<TeacherAllocationRecord> records = teacherAllocationRecordService.getByStudentId(studentId);
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取学生分配记录失败 - 学生ID: {}", studentId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取分配记录失败");
        }
    }

    /**
     * 获取论文的当前有效分配
     */
    @GetMapping("/current/{paperId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<TeacherAllocationRecord> getCurrentAllocation(@PathVariable Long paperId) {
        try {
            TeacherAllocationRecord record = teacherAllocationRecordService.getCurrentAllocation(paperId);
            if (record == null) {
                return Result.error(ResultCode.SYSTEM_ERROR, "当前无有效分配");
            }
            return Result.success(record);
        } catch (Exception e) {
            log.error("获取当前有效分配失败 - 论文ID: {}", paperId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取当前分配失败" + e.getMessage());
        }
    }

    /**
     * 创建分配记录
     */
    @PostMapping("/create")
    @OperationLog(type = "teacher_allocation_create", description = "创建教师分配记录", recordResult = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<TeacherAllocationRecord> createRecord(@RequestBody TeacherAllocationRecord record) {
        try {
            boolean success = teacherAllocationRecordService.createRecord(record);
            if (success) {
                return Result.success("创建分配记录成功", record);
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "创建分配记录失败");
            }
        } catch (Exception e) {
            log.error("创建分配记录失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "创建分配记录失败: " + e.getMessage());
        }
    }

    /**
     * 撤销分配记录
     */
    @PutMapping("/revoke/{id}")
    @OperationLog(type = "teacher_allocation_revoke", description = "撤销教师分配记录", recordResult = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<String> revokeRecord(@PathVariable Long id, @RequestParam String reason) {
        try {
            boolean success = teacherAllocationRecordService.revokeRecord(id, reason);
            if (success) {
                return Result.success("撤销分配记录成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "撤销分配记录失败");
            }
        } catch (Exception e) {
            log.error("撤销分配记录失败 - 记录ID: {}", id, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "撤销分配记录失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建分配记录
     */
    @PostMapping("/batch-create")
    @OperationLog(type = "teacher_allocation_batch_create", description = "批量创建教师分配记录", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchCreateRecords(@RequestBody List<TeacherAllocationRecord> records) {
        try {
            boolean success = teacherAllocationRecordService.batchCreateRecords(records);
            if (success) {
                return Result.success("批量创建分配记录成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "批量创建分配记录失败");
            }
        } catch (Exception e) {
            log.error("批量创建分配记录失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量创建分配记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取分配统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getAllocationStats(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> stats = teacherAllocationRecordService.getAllocationStats(teacherId, startDate, endDate);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取分配统计信息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取统计信息失败");
        }
    }
}
