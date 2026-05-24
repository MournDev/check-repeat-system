package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.teacher.dto.*;
import com.abin.checkrepeatsystem.teacher.service.TeacherStudentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 教师学生管理控制器
 * 提供学生列表、统计、分配、消息等完整功能
 * 
 * 接口路径说明：
 * - 学生列表：GET /api/teacher/students
 * - 删除学生：DELETE /api/teacher/students/{studentId}
 * - 学生统计：GET /api/teacher/students/statistics (注意：不是/stats以避免与仪表盘冲突)
 * - 分配导师：POST /api/teacher/students/{studentId}/assign-advisor
 * - 发送消息：POST /api/teacher/students/messages/send
 * - 批量分配：POST /api/teacher/students/batch-assign-advisor
 * - 批量发送：POST /api/teacher/students/messages/batch-send
 * - 批量删除：DELETE /api/teacher/students/batch-delete
 * - 添加学生：POST /api/teacher/students/add
 * - 数据导出：GET /api/teacher/students/export
 * - 数据导入：POST /api/teacher/students/import
 * - 学生论文信息：GET /api/teacher/students/{studentId}/paper (新增)
 */
@RestController
@RequestMapping("/api/v1/teacher/students")
@PreAuthorize("hasAuthority('TEACHER')")
@RequiredArgsConstructor
@Tag(name = "教师学生管理", description = "教师对学生进行管理的相关接口")
@Slf4j
public class TeacherStudentManagementController {

    private final TeacherStudentManagementService studentManagementService;

    /**
     * 1. 获取学生列表接口
     * GET /api/teacher/students
     */
    @GetMapping
    @Operation(summary = "获取学生列表", description = "获取教师指导的学生列表，支持分页和筛选")
    public Result<Object> getStudentList(
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search,
            @Parameter(description = "论文状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "学院筛选") @RequestParam(required = false) String college) {
        
        // 如果没有传入teacherId，则使用当前登录用户的ID
        if (teacherId == null) {
            teacherId = UserBusinessInfoUtils.getCurrentUserId();
        }

        log.info("获取学生列表: teacherId={}, page={}, pageSize={}, search={}, status={}, college={}", 
                teacherId, page, pageSize, search, status, college);

        StudentListRequestDTO requestDTO = new StudentListRequestDTO();
        requestDTO.setTeacherId(teacherId);
        requestDTO.setPage(page);
        requestDTO.setPageSize(pageSize);
        requestDTO.setSearch(search);
        requestDTO.setStatus(status);
        requestDTO.setCollege(college);

        return studentManagementService.getStudentList(requestDTO);
    }

    /**
     * 2. 删除学生接口
     * DELETE /api/teacher/students/{studentId}
     */
    @DeleteMapping("/{studentId}")
    @Operation(summary = "删除学生", description = "删除指定学生")
    @OperationLog(type = "delete_student", description = "删除学生")
    public Result<String> deleteStudent(
            @Parameter(description = "学生ID") @PathVariable Long studentId) {
        
        log.info("删除学生: studentId={}", studentId);
        boolean result = studentManagementService.deleteStudent(studentId);
        if (result) {
            return Result.success("删除成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "删除失败");
        }
    }

    /**
     * 3. 获取统计信息接口
     * GET /api/teacher/students/statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取学生统计信息", description = "获取教师指导学生的各项统计数据")
    public Result<Object> getStudentStats(
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId) {
        
        if (teacherId == null) {
            teacherId = UserBusinessInfoUtils.getCurrentUserId();
        }

        log.info("获取学生统计信息: teacherId={}", teacherId);
        return studentManagementService.getStudentStats(teacherId);
    }

    /**
     * 4. 分配导师接口
     * POST /api/teacher/students/{studentId}/assign-advisor
     */
    @PostMapping("/{studentId}/assign-advisor")
    @Operation(summary = "分配导师", description = "为学生分配指导老师")
    @OperationLog(type = "assign_advisor", description = "分配导师")
    public Result<String> assignAdvisor(
            @Parameter(description = "学生ID") @PathVariable Long studentId,
            @RequestBody AssignAdvisorDTO assignAdvisorDTO) {
        
        log.info("分配导师: studentId={}, advisorId={}", studentId, assignAdvisorDTO.getAdvisorId());
        boolean result = studentManagementService.assignAdvisor(studentId, assignAdvisorDTO);
        if (result) {
            return Result.success("导师分配成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "导师分配失败");
        }
    }

    /**
     * 5. 发送消息接口
     * POST /api/teacher/messages/send
     */
    @PostMapping("/messages/send")
    @Operation(summary = "发送消息", description = "向学生发送消息")
    @OperationLog(type = "send_message", description = "发送消息给学生")
    public Result<String> sendMessage(@RequestBody SendMessageDTO sendMessageDTO) {
        
        log.info("发送消息: receiverId={}, title={}", sendMessageDTO.getReceiverId(), sendMessageDTO.getTitle());
        boolean result = studentManagementService.sendMessage(sendMessageDTO);
        if (result) {
            return Result.success("消息发送成功");
        } else {
            return Result.error(ResultCode.SYSTEM_ERROR, "消息发送失败");
        }
    }

    /**
     * 6.1 批量分配导师接口
     * POST /api/teacher/students/batch-assign-advisor
     */
    @PostMapping("/batch-assign-advisor")
    @Operation(summary = "批量分配导师", description = "为多个学生批量分配同一指导老师")
    @OperationLog(type = "batch_assign_advisor", description = "批量分配导师")
    public Result<Object> batchAssignAdvisor(@RequestBody BatchAssignAdvisorDTO batchAssignDTO) {
        
        log.info("批量分配导师: studentIds={}, advisorId={}", 
                batchAssignDTO.getStudentIds(), batchAssignDTO.getAdvisorId());
        BatchOperationResultDTO result = studentManagementService.batchAssignAdvisor(batchAssignDTO);
        return Result.<Object>success("批量分配完成", result);
    }

    /**
     * 6.2 批量发送消息接口
     * POST /api/teacher/messages/batch-send
     */
    @PostMapping("/messages/batch-send")
    @Operation(summary = "批量发送消息", description = "向多个学生批量发送相同消息")
    @OperationLog(type = "batch_send_message", description = "批量发送消息")
    public Result<Object> batchSendMessage(@RequestBody BatchSendMessageDTO batchSendDTO) {
        
        log.info("批量发送消息: receiverIds={}, title={}", 
                batchSendDTO.getReceiverIds(), batchSendDTO.getTitle());
        BatchOperationResultDTO result = studentManagementService.batchSendMessage(batchSendDTO);
        return Result.<Object>success("批量发送完成", result);
    }

    /**
     * 6.3 批量删除学生接口
     * DELETE /api/teacher/students/batch-delete
     */
    @DeleteMapping("/batch-delete")
    @Operation(summary = "批量删除学生", description = "批量删除多个学生")
    @OperationLog(type = "batch_delete_student", description = "批量删除学生")
    public Result<Object> batchDeleteStudents(@RequestBody BatchDeleteDTO batchDeleteDTO) {
        
        log.info("批量删除学生: studentIds={}", batchDeleteDTO.getStudentIds());
        BatchOperationResultDTO result = studentManagementService.batchDeleteStudents(batchDeleteDTO);
        return Result.<Object>success("批量删除完成", result);
    }

    /**
     * 7. 数据导出接口
     * GET /api/teacher/students/export
     */
    @GetMapping("/export")
    @Operation(summary = "导出学生数据", description = "导出学生列表数据为Excel文件")
    @OperationLog(type = "export_students", description = "导出学生数据")
    public Result<String> exportStudents(
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "导出格式") @RequestParam(defaultValue = "excel") String format,
            @Parameter(description = "搜索条件") @RequestParam(required = false) String search,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "学院筛选") @RequestParam(required = false) String college) {
        
        if (teacherId == null) {
            teacherId = UserBusinessInfoUtils.getCurrentUserId();
        }

        log.info("导出学生数据: teacherId={}, format={}", teacherId, format);

        ExportRequestDTO exportRequest = new ExportRequestDTO();
        exportRequest.setTeacherId(teacherId);
        exportRequest.setFormat(format);
        exportRequest.setSearch(search);
        exportRequest.setStatus(status);
        exportRequest.setCollege(college);

        String filePath = studentManagementService.exportStudents(exportRequest);
        return Result.success("导出成功", filePath);
    }

    /**
     * 9. 添加学生接口
     * POST /api/teacher/students/add
     */
    @PostMapping("/add")
    @Operation(summary = "添加学生", description = "添加新的学生用户")
    @OperationLog(type = "add_student", description = "添加学生")
    public Result<Object> addStudent(@RequestBody AddStudentDTO addStudentDTO) {
        
        log.info("添加学生: username={}, studentName={}", 
                addStudentDTO.getUsername(), addStudentDTO.getStudentName());
        return studentManagementService.addStudent(addStudentDTO);
    }
    
    /**
     * 8. 数据导入接口
     * POST /api/teacher/students/import
     */
    @PostMapping("/import")
    @Operation(summary = "导入学生数据", description = "从Excel文件导入学生数据")
    @OperationLog(type = "import_students", description = "导入学生数据")
    public Result<Object> importStudents(@RequestParam("file") MultipartFile file) {
        
        log.info("导入学生数据: fileName={}", file.getOriginalFilename());
        ImportResultDTO result = studentManagementService.importStudents(file);
        return Result.success("导入完成", result);
    }
    
    /**
     * 10. 获取学生论文信息接口（新增）
     * GET /api/teacher/students/{studentId}/paper
     */
    @GetMapping("/{studentId}/paper")
    @Operation(summary = "获取学生论文信息", description = "获取指定学生的最新论文信息")
    public Result<StudentPaperInfoDTO> getStudentPaperInfo(
            @Parameter(description = "学生ID") @PathVariable Long studentId) {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("获取学生论文信息: teacherId={}, studentId={}", teacherId, studentId);
        return studentManagementService.getStudentPaperInfo(teacherId, studentId);
    }
    
    /**
     * 11. 获取学生所有论文接口（新增）
     * GET /api/teacher/students/{studentId}/papers
     */
    @GetMapping("/{studentId}/papers")
    @Operation(summary = "获取学生所有论文", description = "获取指定学生的所有论文信息")
    public Result<List<StudentPaperInfoDTO>> getStudentAllPapers(
            @Parameter(description = "学生ID") @PathVariable Long studentId) {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("获取学生所有论文: teacherId={}, studentId={}", teacherId, studentId);
        return studentManagementService.getStudentAllPapers(teacherId, studentId);
    }
}