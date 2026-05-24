package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.service.ReportService;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private final CheckReportMapper checkReportMapper;

    private final PaperInfoMapper paperInfoMapper;

    private final SysUserMapper sysUserMapper;

    @Override
    public Result<Map<String, Object>> getReportList(Integer pageNum, Integer pageSize, String paperTitle,
                                                     String studentName, String checkStatus,
                                                     Double minSimilarity, Double maxSimilarity) {
        log.info("接收获取报告列表请求: pageNum={}, pageSize={}, paperTitle={}, studentName={}, checkStatus={}, minSimilarity={}, maxSimilarity={}",
                pageNum, pageSize, paperTitle, studentName, checkStatus, minSimilarity, maxSimilarity);

        Page<CheckReport> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CheckReport> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(CheckReport::getIsDeleted, 0);

        // 论文标题搜索（需要关联 paper_info 表）
        if (paperTitle != null && !paperTitle.isEmpty()) {
            List<Long> paperIds = getMatchedPaperIdsByTitle(paperTitle);
            if (!paperIds.isEmpty()) {
                queryWrapper.in(CheckReport::getPaperId, paperIds);
            } else {
                queryWrapper.eq(CheckReport::getId, -1L);
            }
        }

        // 学生姓名搜索（需要关联 paper_info 表获取 student_id，再关联 sys_user 表）
        if (studentName != null && !studentName.isEmpty()) {
            List<Long> paperIds = getMatchedPaperIdsByStudentName(studentName);
            if (!paperIds.isEmpty()) {
                queryWrapper.in(CheckReport::getPaperId, paperIds);
            } else {
                queryWrapper.eq(CheckReport::getId, -1L);
            }
        }

        // 相似度范围筛选（需要关联 paper_info 表）
        if (minSimilarity != null || maxSimilarity != null) {
            List<Long> paperIds = getMatchedPaperIdsBySimilarity(minSimilarity, maxSimilarity);
            if (!paperIds.isEmpty()) {
                queryWrapper.in(CheckReport::getPaperId, paperIds);
            } else {
                queryWrapper.eq(CheckReport::getId, -1L);
            }
        }

        queryWrapper.orderByDesc(CheckReport::getCreateTime);

        Page<CheckReport> resultPage = checkReportMapper.selectPage(page, queryWrapper);
        List<CheckReport> reports = resultPage.getRecords();

        List<Map<String, Object>> reportList = reports.stream().map(this::convertToReportMap).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", reportList);
        result.put("total", resultPage.getTotal());

        log.info("获取报告列表成功: 共{}条记录", resultPage.getTotal());
        return Result.success("获取报告列表成功", result);
    }

    @Override
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

    @Override
    public Result<Map<String, Object>> getReportDetail(Long reportId) {
        CheckReport report = checkReportMapper.selectById(reportId);
        if (report == null) {
            return Result.error(404, "报告不存在", null);
        }

        Map<String, Object> reportMap = convertToReportMap(report);
        reportMap.put("repeatDetails", report.getRepeatDetails());

        return Result.success("获取报告详情成功", reportMap);
    }

    @Override
    public void batchExportReports(List<Long> ids, HttpServletResponse response) {
        try {
            if (ids == null || ids.isEmpty()) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":400,\"message\":\"请选择要导出的报告\"}");
                return;
            }

            List<CheckReport> reports = checkReportMapper.selectBatchIds(ids);
            if (reports.isEmpty()) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":404,\"message\":\"未找到报告\"}");
                return;
            }

            if (reports.size() == 1) {
                CheckReport report = reports.get(0);
                String reportPath = report.getReportPath();
                if (reportPath == null || !Files.exists(Paths.get(reportPath))) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":404,\"message\":\"报告文件不存在\"}");
                    return;
                }
                String fileName = (report.getReportNo() != null ? report.getReportNo() : "report") + ".pdf";
                String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
                Files.copy(Paths.get(reportPath), response.getOutputStream());
            } else {
                String zipName = "reports_" + LocalDate.now() + ".zip";
                String encodedZipName = URLEncoder.encode(zipName, StandardCharsets.UTF_8);
                response.setContentType("application/zip");
                response.setHeader("Content-Disposition",
                        "attachment; filename=\"" + encodedZipName + "\"; filename*=UTF-8''" + encodedZipName);

                try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                    for (CheckReport report : reports) {
                        String reportPath = report.getReportPath();
                        if (reportPath == null || !Files.exists(Paths.get(reportPath))) {
                            continue;
                        }
                        String entryName = (report.getReportNo() != null ? report.getReportNo() : "report_" + report.getId()) + ".pdf";
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(Paths.get(reportPath), zos);
                        zos.closeEntry();
                    }
                    zos.finish();
                    zos.flush();
                }
            }
        } catch (IOException e) {
            log.error("批量导出报告失败 - ids: {}", ids, e);
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败: " + e.getMessage() + "\"}");
            } catch (IOException writeEx) {
                log.error("写入错误响应失败", writeEx);
            }
        }
    }

    private List<Long> getMatchedPaperIdsByTitle(String paperTitle) {
        LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
        paperWrapper.like(PaperInfo::getPaperTitle, paperTitle);
        paperWrapper.eq(PaperInfo::getIsDeleted, 0);
        List<PaperInfo> matchedPapers = paperInfoMapper.selectList(paperWrapper);
        return matchedPapers.stream().map(PaperInfo::getId).collect(Collectors.toList());
    }

    private List<Long> getMatchedPaperIdsByStudentName(String studentName) {
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.like(SysUser::getRealName, studentName);
        userWrapper.eq(SysUser::getIsDeleted, 0);
        List<SysUser> matchedUsers = sysUserMapper.selectList(userWrapper);
        if (matchedUsers.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = matchedUsers.stream().map(SysUser::getId).collect(Collectors.toList());

        LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
        paperWrapper.in(PaperInfo::getStudentId, userIds);
        paperWrapper.eq(PaperInfo::getIsDeleted, 0);
        List<PaperInfo> matchedPapers = paperInfoMapper.selectList(paperWrapper);
        return matchedPapers.stream().map(PaperInfo::getId).collect(Collectors.toList());
    }

    private List<Long> getMatchedPaperIdsBySimilarity(Double minSimilarity, Double maxSimilarity) {
        LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
        paperWrapper.eq(PaperInfo::getIsDeleted, 0);
        if (minSimilarity != null) {
            paperWrapper.ge(PaperInfo::getSimilarityRate, minSimilarity);
        }
        if (maxSimilarity != null) {
            paperWrapper.le(PaperInfo::getSimilarityRate, maxSimilarity);
        }
        List<PaperInfo> matchedPapers = paperInfoMapper.selectList(paperWrapper);
        return matchedPapers.stream().map(PaperInfo::getId).collect(Collectors.toList());
    }

    private Map<String, Object> convertToReportMap(CheckReport report) {
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
    }
}
