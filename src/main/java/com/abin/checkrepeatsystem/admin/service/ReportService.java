package com.abin.checkrepeatsystem.admin.service;

import com.abin.checkrepeatsystem.common.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface ReportService {

    Result<Map<String, Object>> getReportList(Integer pageNum, Integer pageSize, String paperTitle,
                                              String studentName, String checkStatus,
                                              Double minSimilarity, Double maxSimilarity);

    Result<Map<String, Object>> getReportStats();

    Result<Map<String, Object>> getReportDetail(Long reportId);

    void batchExportReports(List<Long> ids, jakarta.servlet.http.HttpServletResponse response);
}
