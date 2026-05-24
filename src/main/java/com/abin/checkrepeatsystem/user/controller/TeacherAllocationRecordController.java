package com.abin.checkrepeatsystem.user.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.TeacherAllocationRecord;
import com.abin.checkrepeatsystem.user.service.TeacherAllocationRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/teacher-allocations")
public class TeacherAllocationRecordController {

    private final TeacherAllocationRecordService teacherAllocationRecordService;

    /**
     * 获取论文的分配记录
     */
    @GetMapping("/paper/{paperId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<TeacherAllocationRecord>> getByPaperId(@PathVariable Long paperId) {
        List<TeacherAllocationRecord> records = teacherAllocationRecordService.getByPaperId(paperId);
        return Result.success(records);
    }

    /**
     * 获取教师的分配记录
     */
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<TeacherAllocationRecord>> getByTeacherId(@PathVariable Long teacherId) {
        List<TeacherAllocationRecord> records = teacherAllocationRecordService.getByTeacherId(teacherId);
        return Result.success(records);
    }

    /**
     * 获取学生的分配记录
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<TeacherAllocationRecord>> getByStudentId(@PathVariable Long studentId) {
        List<TeacherAllocationRecord> records = teacherAllocationRecordService.getByStudentId(studentId);
        return Result.success(records);
    }

    /**
     * 获取论文的当前有效分配
     */
    @GetMapping("/current/{paperId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<TeacherAllocationRecord> getCurrentAllocation(@PathVariable Long paperId) {
        TeacherAllocationRecord record = teacherAllocationRecordService.getCurrentAllocation(paperId);
        if (record == null) {
            return Result.error(ResultCode.SYSTEM_ERROR, "当前无有效分配");
        }
        return Result.success(record);
    }

    /**
     * 创建分配记录
     */
    @PostMapping("/create")
    @OperationLog(type = "teacher_allocation_create", description = "创建教师分配记录", recordResult = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<TeacherAllocationRecord> createRecord(@RequestBody TeacherAllocationRecord record) {
        boolean success = teacherAllocationRecordService.createRecord(record);
        if (success) {
            return Result.success("创建分配记录成功", record);
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "创建分配记录失败");
        }
    }

    /**
     * 撤销分配记录
     */
    @PutMapping("/revoke/{id}")
    @OperationLog(type = "teacher_allocation_revoke", description = "撤销教师分配记录", recordResult = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<String> revokeRecord(@PathVariable Long id, @RequestParam String reason) {
        boolean success = teacherAllocationRecordService.revokeRecord(id, reason);
        if (success) {
            return Result.success("撤销分配记录成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "撤销分配记录失败");
        }
    }

    /**
     * 批量创建分配记录
     */
    @PostMapping("/batch-create")
    @OperationLog(type = "teacher_allocation_batch_create", description = "批量创建教师分配记录", recordResult = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchCreateRecords(@RequestBody List<TeacherAllocationRecord> records) {
        boolean success = teacherAllocationRecordService.batchCreateRecords(records);
        if (success) {
            return Result.success("批量创建分配记录成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "批量创建分配记录失败");
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
        Map<String, Object> stats = teacherAllocationRecordService.getAllocationStats(teacherId, startDate, endDate);
        return Result.success(stats);
    }
}
