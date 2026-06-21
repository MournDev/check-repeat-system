package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.teacher.dto.BatchReviewDTO;
import com.abin.checkrepeatsystem.teacher.dto.SendMessageDTO;
import com.abin.checkrepeatsystem.teacher.service.TeacherDashboardService;
import com.abin.checkrepeatsystem.teacher.service.TeacherReviewWorkflowService;
import com.abin.checkrepeatsystem.teacher.service.TeacherStudentManagementService;
import com.abin.checkrepeatsystem.teacher.vo.StudentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师控制台仪表盘控制器
 * 提供教师工作台所需的所有接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teacher")
@PreAuthorize("hasAuthority('TEACHER')")
@RequiredArgsConstructor
@Tag(name = "教师控制台接口", description = "教师工作台相关接口")
public class TeacherDashboardController {

    private final TeacherDashboardService teacherDashboardService;
    
    private final TeacherStudentManagementService studentManagementService;

    private final TeacherReviewWorkflowService reviewWorkflowService;

    /**
     * 1. 仪表盘统计数据接口（带教师ID参数）
     * GET /api/teacher/dashboard/stats/{teacherId}
     */
    @GetMapping("/dashboard/stats/{teacherId}")
    @Operation(summary = "获取教师仪表盘统计数据", description = "获取指导学生总数、待审核论文数量、已审核论文数量、审核通过率等统计信息")
    public Result<Map<String, Object>> getDashboardStats(
            @Parameter(description = "教师ID") @PathVariable Long teacherId) {
        log.info("教师{}请求获取仪表盘统计数据", teacherId);
        return teacherDashboardService.getDashboardStats(teacherId);
    }

    /**
     * 1.1 仪表盘统计数据接口（不带参数，使用当前用户）
     * GET /api/teacher/dashboard/stats
     */
    @GetMapping("/dashboard/stats")
    @Operation(summary = "获取当前教师仪表盘统计数据", description = "获取当前教师的仪表盘统计数据")
    public Result<Map<String, Object>> getCurrentTeacherDashboardStats() {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("教师{}请求获取仪表盘统计数据", teacherId);
        return teacherDashboardService.getDashboardStats(teacherId);
    }

    /**
     * 2. 待审核论文列表接口（兼容需求路径）
     * GET /api/teacher/dashboard/pending-papers
     */
    @GetMapping("/dashboard/pending-papers")
    @Operation(summary = "获取待审核论文列表", description = "获取需要审核的论文列表，包含论文基本信息")
    public Result<Object> getPendingPapersForReviews(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId) {
        // 如果没有传入teacherId，则从当前用户获取
        if (teacherId == null) {
            teacherId = UserBusinessInfoUtils.getCurrentUserId();
        }
        log.info("教师{}请求获取待审核论文列表: pageNum={}, pageSize={}", teacherId, pageNum, pageSize);
        return teacherDashboardService.getPendingPapers(teacherId, pageNum, pageSize);
    }

    /**
     * 3. 学生状态统计接口（带教师ID参数）
     * GET /api/teacher/students/stats/{teacherId}
     */
    @GetMapping("/students/stats/{teacherId}")
    @Operation(summary = "获取指导学生状态统计", description = "统计指导学生的论文提交和审核状态")
    public Result<Map<String, Object>> getStudentStats(
            @Parameter(description = "教师ID") @PathVariable Long teacherId) {
        log.info("教师{}请求获取学生状态统计", teacherId);
        return teacherDashboardService.getStudentStats(teacherId);
    }

    /**
     * 3.1 学生状态统计接口（不带参数，使用当前用户）
     * GET /api/teacher/students/stats
     */
    @GetMapping("/students/stats")
    @Operation(summary = "获取当前教师指导学生状态统计", description = "统计当前教师指导学生的论文提交和审核状态")
    public Result<Map<String, Object>> getCurrentTeacherStudentStats() {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("教师{}请求获取学生状态统计", teacherId);
        return teacherDashboardService.getStudentStats(teacherId);
    }

    /**
     * 4. 论文审核接口
     * POST /api/teacher/papers/review
     */
    @PostMapping("/papers/review")
    @Operation(summary = "批量论文审核", description = "提交论文审核结果")
    @OperationLog(type = "teacher_batch_review", description = "教师批量审核论文", recordResult = true)
    public Result<String> batchReviewPapers(@RequestBody @Valid BatchReviewDTO reviewDTO) {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("教师{}批量审核论文: paperIds={}, status={}", teacherId, reviewDTO.getPaperIds(), reviewDTO.getReviewStatus());
        return teacherDashboardService.batchReviewPapers(teacherId, reviewDTO);
    }

    /**
     * 5. 论文审核操作接口（保持原有单个审核接口）
     * POST /api/teacher/papers/{paperId}/review
     */
    @PostMapping("/papers/{paperId}/review")
    @Operation(summary = "审核论文", description = "对指定论文进行审核操作（通过/驳回）")
    @OperationLog(type = "teacher_paper_review", description = "教师审核论文", recordResult = true)
    public Result<String> reviewPaper(
            @Parameter(description = "论文ID") @PathVariable Long paperId,
            @Parameter(description = "审核结果") @RequestParam String reviewResult,
            @Parameter(description = "审核意见") @RequestParam(required = false) String reviewComment) {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("教师{}对论文{}进行审核: result={}, comment={}", teacherId, paperId, reviewResult, reviewComment);
        return teacherDashboardService.reviewPaper(teacherId, paperId, reviewResult, reviewComment);
    }

    /**
     * 5. 消息发送接口
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
     * 6. 审核进度统计接口
     * GET /api/teacher/review/statistics
     */
    @GetMapping("/review/statistics")
    @Operation(summary = "获取审核进度统计", description = "获取论文状态分布、各专业审核情况等统计图表数据")
    public Result<Map<String, Object>> getReviewStatistics() {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("教师{}请求获取审核进度统计", teacherId);
        return teacherDashboardService.getReviewStatistics(teacherId);
    }

    /**
     * 6. 教师数据导出接口
     * GET /api/teacher/export/data
     */
    @GetMapping("/export/data")
    @Operation(summary = "导出教师相关统计数据", description = "导出教师相关统计数据为Excel文件")
    public Result<String> exportTeacherData(
            @Parameter(description = "教师ID") @RequestParam Long teacherId,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate) {
        log.info("教师{}请求导出数据: startDate={}, endDate={}", teacherId, startDate, endDate);
        return teacherDashboardService.exportTeacherData(teacherId, startDate, endDate);
    }

    /**
     * 7. 近期活动记录接口
     * GET /api/teacher/activities/recent
     */
    @GetMapping("/activities/recent")
    @Operation(summary = "获取近期活动记录", description = "获取教师的审核操作记录、学生提交记录等")
    public Result<Object> getRecentActivities(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("教师{}请求获取近期活动记录: page={}, size={}", teacherId, page, size);
        return teacherDashboardService.getRecentActivities(teacherId, page, size);
    }

    /**
     * 8. 数据导出接口
     * POST /api/teacher/export/data
     */
    @PostMapping("/export/data")
    @Operation(summary = "导出审核数据报表", description = "导出审核数据为Excel或PDF格式")
    public Result<String> exportData(
            @Parameter(description = "导出格式") @RequestParam(defaultValue = "excel") String format,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("教师{}请求导出数据: format={}, startTime={}, endTime={}", teacherId, format, startTime, endTime);
        return teacherDashboardService.exportData(teacherId, format, startTime, endTime);
    }

    /**
     * 9. 学生管理跳转支持接口
     * GET /api/teacher/students/list
     */
    @GetMapping("/students/list")
    @Operation(summary = "获取指导学生列表", description = "获取教师指导的所有学生列表")
    public Result<Object> getStudentList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size) {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("教师{}请求获取指导学生列表: page={}, size={}", teacherId, page, size);
        return teacherDashboardService.getStudentList(teacherId, page, size);
    }

    /**
     * 10. 实时数据刷新接口
     * GET /api/teacher/dashboard/refresh
     */
    @GetMapping("/dashboard/refresh")
    @Operation(summary = "刷新仪表盘实时数据", description = "刷新仪表盘的实时统计数据")
    public Result<Map<String, Object>> refreshDashboard() {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("教师{}请求刷新仪表盘数据", teacherId);
        return teacherDashboardService.refreshDashboard(teacherId);
    }

    /**
     * 获取教师列表（合并自 TeacherListController）
     */
    @GetMapping("/list")
    @Operation(summary = "获取教师列表", description = "获取所有教师列表")
    public Result<List<StudentVO>> getTeachers() {
        return reviewWorkflowService.getTeachers();
    }
}