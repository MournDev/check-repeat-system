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
}
