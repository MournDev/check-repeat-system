package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.admin.service.ReportService;
import com.abin.checkrepeatsystem.common.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    @Resource
    private ReportService reportService;

    @GetMapping("/list")
    public Result<Map<String, Object>> getReportList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String paperTitle,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String checkStatus,
            @RequestParam(required = false) Double minSimilarity,
            @RequestParam(required = false) Double maxSimilarity) {
        return reportService.getReportList(pageNum, pageSize, paperTitle, studentName, checkStatus, minSimilarity, maxSimilarity);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getReportStats() {
        return reportService.getReportStats();
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> getReportDetail(@RequestParam Long reportId) {
        return reportService.getReportDetail(reportId);
    }

    @PostMapping("/batch-export")
    public void batchExportReports(@RequestBody Map<String, List<Long>> body, HttpServletResponse response) {
        reportService.batchExportReports(body.get("ids"), response);
    }
}
