package com.abin.checkrepeatsystem.detection.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.detection.service.PdfReportGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * PDF查重报告控制器
 * 负责处理PDF报告的生成和下载
 */
@RestController
@RequestMapping("/api/detection/report")
@Slf4j
public class PdfReportController {

    @Resource
    private PdfReportGeneratorService reportGeneratorService;

    /**
     * 生成并下载PDF报告
     *
     * @param checkResultId 查重结果ID
     * @param response      HTTP响应
     */
    @GetMapping("/pdf/{checkResultId}")
    public void generateAndDownloadPdf(@PathVariable Long checkResultId, HttpServletResponse response) {
        try {
            // 生成PDF报告
            byte[] pdfBytes = reportGeneratorService.generateReportByCheckResultId(checkResultId);

            // 设置响应头
            response.setContentType("application/pdf");
            response.setContentLength(pdfBytes.length);
            response.setHeader("Content-Disposition", 
                    "attachment; filename=plagiarism_report_" + checkResultId + ".pdf");

            // 写入响应
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(pdfBytes);
            outputStream.flush();
            outputStream.close();

            log.info("PDF报告下载成功: checkResultId={}", checkResultId);

        } catch (Exception e) {
            log.error("生成PDF报告失败: checkResultId={}", checkResultId, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("生成PDF报告失败: " + e.getMessage());
            } catch (IOException ex) {
                log.warn("写入错误响应失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 根据论文ID生成PDF报告
     *
     * @param paperId  论文ID
     * @param response HTTP响应
     */
    @GetMapping("/pdf/paper/{paperId}")
    public void generatePdfByPaperId(@PathVariable Long paperId, HttpServletResponse response) {
        try {
            // 生成PDF报告
            byte[] pdfBytes = reportGeneratorService.generateReportByPaperId(paperId);

            // 设置响应头
            response.setContentType("application/pdf");
            response.setContentLength(pdfBytes.length);
            response.setHeader("Content-Disposition", 
                    "attachment; filename=plagiarism_report_paper_" + paperId + ".pdf");

            // 写入响应
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(pdfBytes);
            outputStream.flush();
            outputStream.close();

            log.info("论文PDF报告下载成功: paperId={}", paperId);

        } catch (Exception e) {
            log.error("生成论文PDF报告失败: paperId={}", paperId, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("生成PDF报告失败: " + e.getMessage());
            } catch (IOException ex) {
                log.warn("写入错误响应失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 测试PDF生成
     *
     * @param response HTTP响应
     */
    @GetMapping("/pdf/test")
    public void testPdfGeneration(HttpServletResponse response) {
        try {
            // 使用测试方法生成PDF
            byte[] pdfBytes = reportGeneratorService.generateTestReport();

            // 设置响应头
            response.setContentType("application/pdf");
            response.setContentLength(pdfBytes.length);
            response.setHeader("Content-Disposition", 
                    "attachment; filename=test_report_" + System.currentTimeMillis() + ".pdf");

            // 写入响应
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(pdfBytes);
            outputStream.flush();
            outputStream.close();

            log.info("测试PDF生成成功，大小: {} bytes", pdfBytes.length);

        } catch (Exception e) {
            log.error("测试PDF生成失败: {}", e.getMessage(), e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("测试PDF生成失败: " + e.getMessage());
            } catch (IOException ex) {
                log.warn("写入错误响应失败: {}", ex.getMessage());
            }
        }
    }
}