package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.admin.service.AssignmentHistoryService;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.admin.dto.AssignmentHistoryStatsDTO;
import com.abin.checkrepeatsystem.admin.dto.AssignmentRecordDTO;
import com.abin.checkrepeatsystem.admin.dto.DeleteAssignmentRecordsDTO;
import com.abin.checkrepeatsystem.admin.dto.RevokeAssignmentDTO;
import com.abin.checkrepeatsystem.admin.dto.ReassignTeacherDTO;
import com.abin.checkrepeatsystem.admin.dto.AvailableTeacherDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 分配记录管理控制器
 */
@RestController
@RequestMapping("/api/v1/admin/assignment/history")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AssignmentHistoryController {

    private final AssignmentHistoryService assignmentHistoryService;

    /**
     * 1. 获取分配记录统计信息
     */
    @GetMapping("/stats")
    public Result<AssignmentHistoryStatsDTO> getAssignmentHistoryStats() {
        log.info("接收获取分配记录统计信息请求");
        return assignmentHistoryService.getAssignmentHistoryStats();
    }

    /**
     * 2. 获取分配记录列表
     */
    @GetMapping("/list")
    public Result<Page<AssignmentRecordDTO>> getAssignmentRecordList(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) String assignmentType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        log.info("接收获取分配记录列表请求: startDate={}, endDate={}, collegeId={}, assignmentType={}, status={}, keyword={}, page={}, size={}",
                startDate, endDate, collegeId, assignmentType, status, keyword, page, size);

        return assignmentHistoryService.getAssignmentRecordList(
                startDate, endDate, collegeId, assignmentType, status, keyword, page, size);
    }

    /**
     * 3. 删除分配记录
     */
    @DeleteMapping
    public Result<String> deleteAssignmentRecords(@RequestBody DeleteAssignmentRecordsDTO request) {
        log.info("接收删除分配记录请求: ids={}", request.getIds());
        return assignmentHistoryService.deleteAssignmentRecords(request);
    }

    /**
     * 4. 撤销指导老师分配
     */
    @PostMapping("/revoke")
    public Result<String> revokeAssignment(@RequestBody RevokeAssignmentDTO request) {
        log.info("接收撤销指导老师分配请求: recordId={}, reason={}", request.getRecordId(), request.getReason());
        return assignmentHistoryService.revokeAssignment(request);
    }

    /**
     * 5. 重新分配指导老师
     */
    @PostMapping("/reassign")
    public Result<String> reassignTeacher(@RequestBody ReassignTeacherDTO request) {
        log.info("接收重新分配指导老师请求: recordId={}, newTeacherId={}, reason={}", 
                request.getRecordId(), request.getNewTeacherId(), request.getReason());
        return assignmentHistoryService.reassignTeacher(request);
    }

    /**
     * 6. 获取可用于重新分配的教师列表
     */
    @GetMapping("/available-teachers")
    public Result<List<AvailableTeacherDTO>> getAvailableTeachers() {
        log.info("接收获取可用教师列表请求");
        return assignmentHistoryService.getAvailableTeachers();
    }

    /**
     * 7. 导出分配记录
     */
    @GetMapping("/export")
    public void exportAssignmentRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String assignmentType,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            HttpServletResponse response) {
            
        log.info("接收导出分配记录请求: startDate={}, endDate={}, assignmentType={}, major={}, status={}, keyword={}"
                , startDate, endDate, assignmentType, major, status, keyword);
            
        try {
            assignmentHistoryService.exportAssignmentRecords(
                    startDate, endDate, assignmentType, major, status, keyword, response);
        } catch (Exception e) {
            log.error("导出分配记录失败: {}", e.getMessage(), e);
            try {
                response.reset();
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败：" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("设置错误响应失败", ex);
            }
        }
    }

    /**
     * 8. 刷新分配记录数据
     */
    @PostMapping("/refresh")
    public Result<String> refreshAssignmentData() {
        log.info("接收刷新分配记录数据请求");
        return assignmentHistoryService.refreshAssignmentData();
    }
}