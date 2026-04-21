package com.abin.checkrepeatsystem.teacher.controller;


import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.student.dto.ReportPreviewDTO;
import com.abin.checkrepeatsystem.student.service.CheckReportService;
import com.abin.checkrepeatsystem.student.vo.ReportDownloadReq;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 教师端查重报告控制器：仅教师角色可访问
 */
@RestController
@RequestMapping("/api/teacher/reports")
@PreAuthorize("hasAuthority('TEACHER')")
public class TeacherReportController {

    @Resource
    private CheckReportService checkReportService;

    /**
     * 1. 教师预览指导学生的报告详情
     * @param reportId 报告ID
     */
    @GetMapping("/preview")
    public Result<ReportPreviewDTO> previewReport(@RequestParam Long reportId) {
        return checkReportService.previewReport(reportId);
    }

    /**
     * 2. 教师下载指导学生的报告文件
     * @param downloadReq 下载请求参数
     * @param response HTTP响应
     */
    @PostMapping("/download")
    public void downloadReport(
            @Valid @RequestBody ReportDownloadReq downloadReq,
            HttpServletResponse response) {
        checkReportService.downloadReport(downloadReq, response);
    }

    /**
     * 3. 教师查询指导学生的历史报告列表
     * @param paperId 论文ID（可选）
     */
    @GetMapping("/list")
    public Result<List<CheckReport>> getGuideReportList(
            @RequestParam(required = false) Long paperId) {
        return checkReportService.getMyReportList(paperId);
    }
    
    /**
     * 4. 详细对比接口：根据报告ID和来源ID，返回原文与相似内容的详细对比数据
     * @param reportId 报告ID
     * @param sourceId 来源ID
     */
    @GetMapping("/compare")
    public Result<Map<String, Object>> compareReport(
            @RequestParam Long reportId,
            @RequestParam Long sourceId) {
        return checkReportService.compareReport(reportId, sourceId);
    }
    
    /**
     * 5. 审核操作接口：处理教师对论文的审核操作（通过）
     * @param reportId 报告ID
     * @param comment 审核意见
     */
    @PostMapping("/approve")
    public Result<String> approveReport(
            @RequestParam Long reportId,
            @RequestParam String comment) {
        return checkReportService.approveReport(reportId, comment);
    }
    
    /**
     * 6. 审核操作接口：处理教师对论文的审核操作（要求修改）
     * @param reportId 报告ID
     * @param comment 审核意见
     */
    @PostMapping("/revision")
    public Result<String> requestRevision(
            @RequestParam Long reportId,
            @RequestParam String comment) {
        return checkReportService.requestRevision(reportId, comment);
    }
    
    /**
     * 7. 联系学生接口：教师向学生发送消息
     * @param reportId 报告ID
     * @param content 消息内容
     */
    @PostMapping("/contact")
    public Result<String> contactStudent(
            @RequestParam Long reportId,
            @RequestParam String content) {
        return checkReportService.contactStudent(reportId, content);
    }
    
    /**
     * 8. 相似来源详情接口：获取相似来源的详细信息
     * @param sourceId 来源ID
     */
    @GetMapping("/source/detail")
    public Result<Map<String, Object>> getSourceDetail(
            @RequestParam Long sourceId) {
        return checkReportService.getSourceDetail(sourceId);
    }
    
    /**
     * 9. 历史报告列表接口：获取论文的历史查重报告列表
     * @param paperId 论文ID
     */
    @GetMapping("/history")
    public Result<List<CheckReport>> getHistoryReportList(
            @RequestParam Long paperId) {
        return checkReportService.getHistoryReportList(paperId);
    }
}
