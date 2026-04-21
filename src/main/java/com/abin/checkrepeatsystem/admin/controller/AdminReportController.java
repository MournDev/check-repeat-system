package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    @Resource
    private CheckReportMapper checkReportMapper;

    @Resource
    private PaperInfoMapper paperInfoMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @GetMapping("/list")
    public Result<Map<String, Object>> getReportList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String paperTitle,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String checkStatus,
            @RequestParam(required = false) Double minSimilarity,
            @RequestParam(required = false) Double maxSimilarity) {

        Page<CheckReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CheckReport> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(CheckReport::getIsDeleted, 0);
        queryWrapper.orderByDesc(CheckReport::getCreateTime);

        Page<CheckReport> resultPage = checkReportMapper.selectPage(page, queryWrapper);
        List<CheckReport> reports = resultPage.getRecords();

        List<Map<String, Object>> reportList = reports.stream().map(report -> {
            Map<String, Object> reportMap = new HashMap<>();
            reportMap.put("id", report.getId());
            reportMap.put("reportNo", report.getReportNo());
            reportMap.put("reportPath", report.getReportPath());
            reportMap.put("totalSimilarity", report.getTotalSimilarity());
            reportMap.put("checkTime", report.getCreateTime());
            reportMap.put("status", "已完成");

            Long paperId = report.getPaperId();
            if (paperId != null) {
                PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
                if (paperInfo != null) {
                    reportMap.put("paperTitle", paperInfo.getPaperTitle());
                    reportMap.put("paperId", paperInfo.getId());
                    reportMap.put("studentId", paperInfo.getStudentId());
                    reportMap.put("teacherName", paperInfo.getTeacherName());
                    reportMap.put("similarityRate", paperInfo.getSimilarityRate());
                    reportMap.put("studentName", paperInfo.getAuthor());
                }
            }

            if (report.getTotalSimilarity() != null) {
                reportMap.put("similarity", report.getTotalSimilarity().doubleValue());
            }

            return reportMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", reportList);
        result.put("total", resultPage.getTotal());

        return Result.success("获取报告列表成功", result);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> getReportStats() {
        LambdaQueryWrapper<CheckReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CheckReport::getIsDeleted, 0);

        long totalReports = checkReportMapper.selectCount(queryWrapper);

        LambdaQueryWrapper<PaperInfo> paperQueryWrapper = new LambdaQueryWrapper<>();
        paperQueryWrapper.eq(PaperInfo::getCheckCompleted, 1);
        long completedReports = paperInfoMapper.selectCount(paperQueryWrapper);

        paperQueryWrapper = new LambdaQueryWrapper<>();
        paperQueryWrapper.eq(PaperInfo::getCheckCompleted, 1);
        paperQueryWrapper.ge(PaperInfo::getSimilarityRate, new BigDecimal("30"));
        long highSimilarity = paperInfoMapper.selectCount(paperQueryWrapper);

        paperQueryWrapper = new LambdaQueryWrapper<>();
        paperQueryWrapper.eq(PaperInfo::getCheckCompleted, 1);
        List<PaperInfo> checkedPapers = paperInfoMapper.selectList(paperQueryWrapper);
        double avgSimilarity = 0;
        if (!checkedPapers.isEmpty()) {
            double sum = checkedPapers.stream()
                    .filter(p -> p.getSimilarityRate() != null)
                    .mapToDouble(p -> p.getSimilarityRate().doubleValue())
                    .sum();
            avgSimilarity = sum / checkedPapers.size();
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPapers", totalReports);
        stats.put("checkedPapers", completedReports);
        stats.put("highSimilarity", highSimilarity);
        stats.put("avgSimilarity", Math.round(avgSimilarity * 100.0) / 100.0);

        return Result.success("获取统计信息成功", stats);
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> getReportDetail(@RequestParam Long reportId) {
        CheckReport report = checkReportMapper.selectById(reportId);
        if (report == null) {
            return Result.error(404, "报告不存在", null);
        }

        Map<String, Object> reportMap = new HashMap<>();
        reportMap.put("id", report.getId());
        reportMap.put("reportNo", report.getReportNo());
        reportMap.put("reportPath", report.getReportPath());
        reportMap.put("totalSimilarity", report.getTotalSimilarity());
        reportMap.put("checkTime", report.getCreateTime());
        reportMap.put("status", "已完成");
        reportMap.put("repeatDetails", report.getRepeatDetails());

        Long paperId = report.getPaperId();
        if (paperId != null) {
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo != null) {
                reportMap.put("paperTitle", paperInfo.getPaperTitle());
                reportMap.put("paperId", paperInfo.getId());
                reportMap.put("studentId", paperInfo.getStudentId());
                reportMap.put("teacherName", paperInfo.getTeacherName());
                reportMap.put("similarityRate", paperInfo.getSimilarityRate());
                reportMap.put("wordCount", paperInfo.getWordCount());
                reportMap.put("studentName", paperInfo.getAuthor());
            }
        }

        if (report.getTotalSimilarity() != null) {
            reportMap.put("similarity", report.getTotalSimilarity().doubleValue());
        }

        return Result.success("获取报告详情成功", reportMap);
    }
}
