package com.abin.checkrepeatsystem.student.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.student.service.CheckReportService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 查重报告通用控制器：支持学生、教师、管理员三种角色访问
 */
@RestController
@RequestMapping("/api/student")
public class CheckReportController {

    @Resource
    private CheckReportService checkReportService;

    /**
     * 根据论文ID获取查重报告
     * 学生只能查看自己论文的报告，教师可以查看自己指导学生的报告，管理员可以查看全部
     * @param paperId 论文ID
     */
    @GetMapping("/check-report/{paperId}")
    public Result<CheckReport> getReportByPaperId(@PathVariable Long paperId) {
        return checkReportService.getReportByPaperId(paperId);
    }
}