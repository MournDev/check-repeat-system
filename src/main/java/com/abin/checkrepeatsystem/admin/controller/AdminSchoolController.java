package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.admin.service.AdminSchoolService;
import com.abin.checkrepeatsystem.admin.service.DataStatService;
import com.abin.checkrepeatsystem.admin.vo.StatQueryReq;
import com.abin.checkrepeatsystem.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/admin/school")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminSchoolController {

    private static final Logger log = LoggerFactory.getLogger(AdminSchoolController.class);

    private final AdminSchoolService adminSchoolService;

    private final DataStatService dataStatService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getSchoolOverview() {
        return Result.success("学校概览数据获取成功", adminSchoolService.getSchoolOverview());
    }

    @GetMapping("/college-distribution")
    public Result<Map<String, Object>> getCollegeDistribution() {
        try {
            Map<String, Object> stats = adminSchoolService.getCollegeDistribution();
            return Result.success("学院分布数据获取成功", stats);
        } catch (Exception e) {
            log.error("获取学院分布数据失败", e);
            return Result.success("学院分布数据获取成功", adminSchoolService.getCollegeDistribution());
        }
    }

    @GetMapping("/monthly-trend")
    public Result<List<Map<String, Object>>> getMonthlyTrend() {
        return Result.success("月度趋势数据获取成功", adminSchoolService.getMonthlyTrend());
    }

    @GetMapping("/similarity-distribution")
    public Result<Map<String, Object>> getSimilarityDistribution() {
        return Result.success("查重结果分布获取成功", adminSchoolService.getSimilarityDistribution());
    }

    @GetMapping("/realtime-stats")
    public Result<Map<String, Object>> getRealtimeStats() {
        return Result.success("实时统计数据获取成功", adminSchoolService.getRealtimeStats());
    }

    @GetMapping("/export-report")
    public void exportSchoolReport(HttpServletResponse response,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate) throws IOException {
        StatQueryReq queryReq = new StatQueryReq();
        if (startDate != null && !startDate.isEmpty()) {
            queryReq.setStartDate(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            queryReq.setEndDate(endDate);
        }
        dataStatService.exportStatResult(queryReq, "CHECK", response);
    }
}
