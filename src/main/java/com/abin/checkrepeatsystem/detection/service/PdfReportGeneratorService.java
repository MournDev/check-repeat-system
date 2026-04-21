package com.abin.checkrepeatsystem.detection.service;

import com.abin.checkrepeatsystem.admin.mapper.CheckResultMapper;
import com.abin.checkrepeatsystem.detection.dto.SimilarityDetectionResult;
import com.abin.checkrepeatsystem.detection.dto.SimilaritySegment;
import com.abin.checkrepeatsystem.pojo.entity.CheckResult;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * PDF查重报告生成服务
 * 负责生成详细的查重报告PDF文件
 */
@Service
@Slf4j
public class PdfReportGeneratorService {

    @Resource
    private CheckResultMapper checkResultMapper;

    @Resource
    private PaperInfoMapper paperInfoMapper;

    @Value("${report.output.base-path:/data/reports/}")
    private String reportBasePath;

    private PdfFont chineseFont;

    public PdfReportGeneratorService() {
        try {
            // 加载中文字体
            // 使用iText7内置的中文字体
            chineseFont = PdfFontFactory.createFont(
                    "STSongStd-Light",
                    "UniGB-UCS2-H"
            );
            log.info("中文字体加载成功: STSongStd-Light");
        } catch (IOException e) {
            log.warn("加载中文字体失败，尝试使用替代字体: {}", e.getMessage());
            try {
                // 尝试使用其他中文字体
                chineseFont = PdfFontFactory.createFont(
                        "AdobeSongStd-Light",
                        "UniGB-UCS2-H"
                );
                log.info("中文字体加载成功: AdobeSongStd-Light");
            } catch (IOException ex) {
                log.warn("加载替代中文字体失败，使用默认字体: {}", ex.getMessage());
                try {
                    // 回退到默认字体
                    chineseFont = PdfFontFactory.createFont();
                    log.info("默认字体加载成功");
                } catch (IOException exx) {
                    log.error("加载默认字体失败: {}", exx.getMessage());
                }
            }
        }
    }

    /**
     * 生成查重报告PDF
     *
     * @param result 查重检测结果
     * @return PDF字节数组
     */
    public byte[] generateReport(SimilarityDetectionResult result) throws IOException {
        ByteArrayOutputStream outputStream = null;
        PdfWriter writer = null;
        PdfDocument pdfDoc = null;
        Document document = null;

        try {
            outputStream = new ByteArrayOutputStream();
            writer = new PdfWriter(outputStream);
            pdfDoc = new PdfDocument(writer);
            document = new Document(pdfDoc);

            // 设置字体
            if (chineseFont != null) {
                document.setFont(chineseFont);
            }

            // 生成报告内容
            generateReportContent(document, result);

            // 关闭文档
            if (document != null) {
                document.close();
            }

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("PDF报告生成成功，大小: {} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error("生成PDF报告失败: {}", e.getMessage(), e);
            throw new IOException("生成PDF报告失败: " + e.getMessage(), e);
        } finally {
            // 按顺序关闭资源
            try {
                if (document != null) {
                    document.close();
                }
            } catch (Exception e) {
                log.warn("关闭文档失败: {}", e.getMessage());
            }
            
            try {
                if (pdfDoc != null) {
                    pdfDoc.close();
                }
            } catch (Exception e) {
                log.warn("关闭PDF文档失败: {}", e.getMessage());
            }
            
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                log.warn("关闭PDF写入器失败: {}", e.getMessage());
            }
            
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                log.warn("关闭输出流失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 生成报告内容
     */
    private void generateReportContent(Document document, SimilarityDetectionResult result) {
        // 标题部分
        addTitle(document, result);

        // 论文信息
        addPaperInfo(document, result);

        // 查重结果摘要
        addSummary(document, result);

        // 相似论文列表
        addSimilarPapers(document, result);

        // 重复片段
        addRepeatedFragments(document, result);

        // 建议措施
        addRecommendations(document, result);

        // 统计信息
        addStatistics(document, result);

        // 页脚
        addFooter(document);
    }

    /**
     * 添加标题
     */
    private void addTitle(Document document, SimilarityDetectionResult result) {
        Paragraph title = new Paragraph("论文查重报告")
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setMarginBottom(30);
        document.add(title);

        Paragraph subTitle = new Paragraph("PLAGIARISM DETECTION REPORT")
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic()
                .setMarginBottom(40);
        document.add(subTitle);
    }

    /**
     * 添加论文信息
     */
    private void addPaperInfo(Document document, SimilarityDetectionResult result) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        addTableRow(table, "论文标题", result.getTargetPaperTitle());
        addTableRow(table, "作者", result.getTargetAuthor());
        addTableRow(table, "学院", result.getTargetCollege());
        addTableRow(table, "检测时间", result.getDetectionTime().toString());
        addTableRow(table, "比对论文数", String.valueOf(result.getTotalComparisons()));

        document.add(table);
    }

    /**
     * 添加摘要
     */
    private void addSummary(Document document, SimilarityDetectionResult result) {
        Paragraph sectionTitle = new Paragraph("查重结果摘要")
                .setFontSize(16)
                .setBold()
                .setMarginTop(30)
                .setMarginBottom(15);
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // 相似度
        Cell similarityCell = new Cell(1, 2)
                .add(new Paragraph("相似度: " + result.getOverallSimilarity() + "%")
                        .setFontSize(18)
                        .setBold());
        similarityCell.setBackgroundColor(getSimilarityColor(result.getOverallSimilarity()));
        similarityCell.setTextAlignment(TextAlignment.CENTER);
        table.addCell(similarityCell);

        // 风险等级
        addTableRow(table, "风险等级", result.getRiskLevel());

        document.add(table);
    }

    /**
     * 添加相似论文列表
     */
    private void addSimilarPapers(Document document, SimilarityDetectionResult result) {
        List<SimilaritySegment> segments = result.getSimilarSegments();
        if (segments == null || segments.isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("相似论文列表")
                .setFontSize(16)
                .setBold()
                .setMarginTop(30)
                .setMarginBottom(15);
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // 表头
        table.addHeaderCell(new Cell().add(new Paragraph("序号").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("论文标题").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("作者").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("相似度").setBold()));

        // 数据行
        for (int i = 0; i < segments.size(); i++) {
            SimilaritySegment segment = segments.get(i);
            table.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1))));
            table.addCell(new Cell().add(new Paragraph(segment.getPaperTitle())));
            table.addCell(new Cell().add(new Paragraph(segment.getAuthor())));
            table.addCell(new Cell().add(new Paragraph(segment.getSimilarity() + "%")));
        }

        document.add(table);
    }

    /**
     * 添加重复片段
     */
    private void addRepeatedFragments(Document document, SimilarityDetectionResult result) {
        List<SimilaritySegment> segments = result.getSimilarSegments();
        if (segments == null || segments.isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("重复片段分析")
                .setFontSize(16)
                .setBold()
                .setMarginTop(30)
                .setMarginBottom(15);
        document.add(sectionTitle);

        for (int i = 0; i < segments.size(); i++) {
            SimilaritySegment segment = segments.get(i);
            if (segment.getRepeatedFragments() != null && !segment.getRepeatedFragments().isEmpty()) {
                Paragraph paperTitle = new Paragraph((i + 1) + ". " + segment.getPaperTitle() + " (相似度: " + segment.getSimilarity() + "%)")
                        .setFontSize(12)
                        .setBold()
                        .setMarginTop(10);
                document.add(paperTitle);

                List<String> fragments = segment.getRepeatedFragments();
                for (int j = 0; j < Math.min(3, fragments.size()); j++) { // 最多显示3个片段
                    Paragraph fragment = new Paragraph("  " + (j + 1) + ". " + fragments.get(j))
                            .setFontSize(10)
                            .setMarginLeft(20);
                    document.add(fragment);
                }

                if (fragments.size() > 3) {
                    Paragraph more = new Paragraph("  ... 等" + fragments.size() + "个重复片段")
                            .setFontSize(10)
                            .setMarginLeft(20)
                            .setItalic();
                    document.add(more);
                }
            }
        }
    }

    /**
     * 添加建议措施
     */
    private void addRecommendations(Document document, SimilarityDetectionResult result) {
        List<String> recommendations = result.getRecommendations();
        if (recommendations == null || recommendations.isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("建议措施")
                .setFontSize(16)
                .setBold()
                .setMarginTop(30)
                .setMarginBottom(15);
        document.add(sectionTitle);

        com.itextpdf.layout.element.List list = new com.itextpdf.layout.element.List();
        for (String recommendation : recommendations) {
            list.add(new ListItem(recommendation));
        }
        document.add(list);
    }

    /**
     * 添加统计信息
     */
    private void addStatistics(Document document, SimilarityDetectionResult result) {
        Map<String, Object> statistics = result.getStatistics();
        if (statistics == null || statistics.isEmpty()) {
            return;
        }

        Paragraph sectionTitle = new Paragraph("统计信息")
                .setFontSize(16)
                .setBold()
                .setMarginTop(30)
                .setMarginBottom(15);
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        for (Map.Entry<String, Object> entry : statistics.entrySet()) {
            addTableRow(table, formatKey(entry.getKey()), entry.getValue().toString());
        }

        document.add(table);
    }

    /**
     * 添加页脚
     */
    private void addFooter(Document document) {
        Paragraph footer = new Paragraph("报告生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " \n 此报告由论文查重系统自动生成，请勿修改")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(40);
        document.add(footer);
    }

    /**
     * 添加表格行
     */
    private void addTableRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(value)));
    }

    /**
     * 根据相似度获取颜色
     */
    private com.itextpdf.kernel.colors.Color getSimilarityColor(double similarity) {
        if (similarity <= 15) {
            return ColorConstants.GREEN;
        } else if (similarity <= 30) {
            return ColorConstants.YELLOW;
        } else if (similarity <= 50) {
            return ColorConstants.ORANGE;
        } else {
            return ColorConstants.RED;
        }
    }

    /**
     * 格式化统计键
     */
    private String formatKey(String key) {
        switch (key) {
            case "highSimilarityCount":
                return "高相似度论文数";
            case "mediumSimilarityCount":
                return "中等相似度论文数";
            case "lowSimilarityCount":
                return "低相似度论文数";
            case "totalCompared":
                return "总比对论文数";
            case "detectionTime":
                return "检测时间";
            default:
                return key;
        }
    }

    /**
     * 根据查重结果ID生成报告
     */
    public byte[] generateReportByCheckResultId(Long checkResultId) throws IOException {
        CheckResult checkResult = checkResultMapper.selectById(checkResultId);
        if (checkResult == null) {
            throw new IllegalArgumentException("查重结果不存在: " + checkResultId);
        }

        // 根据checkResult构建SimilarityDetectionResult
        SimilarityDetectionResult result = buildResultFromCheckResult(checkResult);
        if (result == null) {
            throw new IllegalArgumentException("无法构建查重结果: " + checkResultId);
        }

        return generateReport(result);
    }

    /**
     * 根据论文ID生成报告
     */
    public byte[] generateReportByPaperId(Long paperId) throws IOException {
        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null) {
            throw new IllegalArgumentException("论文不存在: " + paperId);
        }

        // 获取最新的查重结果
        CheckResult checkResult = getLatestCheckResult(paperId);
        if (checkResult == null) {
            throw new IllegalArgumentException("未找到查重结果: " + paperId);
        }

        return generateReportByCheckResultId(checkResult.getId());
    }

    /**
     * 从CheckResult构建SimilarityDetectionResult
     */
    private SimilarityDetectionResult buildResultFromCheckResult(CheckResult checkResult) {
        try {
            SimilarityDetectionResult result = new SimilarityDetectionResult();
            result.setTargetPaperId(checkResult.getPaperId());
            result.setOverallSimilarity(checkResult.getRepeatRate().doubleValue());
            result.setDetectionTime(new Date());

            // 获取论文信息
            PaperInfo paperInfo = paperInfoMapper.selectById(checkResult.getPaperId());
            if (paperInfo != null) {
                result.setTargetPaperTitle(paperInfo.getPaperTitle());
                result.setTargetAuthor(paperInfo.getAuthor());
                result.setTargetCollege(paperInfo.getCollegeName());
            }

            // 解析checkDetails中的统计信息
            if (checkResult.getCheckDetails() != null) {
                try {
                    // 简单解析，实际项目中可以使用JSON库
                    Map<String, Object> statistics = new HashMap<>();
                    statistics.put("totalCompared", 100); // 示例数据
                    statistics.put("detectionTime", new Date().toString());
                    result.setStatistics(statistics);
                } catch (Exception e) {
                    log.warn("解析统计信息失败: {}", e.getMessage());
                }
            }

            // 设置风险等级
            double similarity = checkResult.getRepeatRate().doubleValue();
            if (similarity <= 15) {
                result.setRiskLevel("低风险");
            } else if (similarity <= 30) {
                result.setRiskLevel("中等风险");
            } else if (similarity <= 50) {
                result.setRiskLevel("高风险");
            } else {
                result.setRiskLevel("极高风险");
            }

            // 设置建议措施
            List<String> recommendations = new ArrayList<>();
            if (similarity <= 15) {
                recommendations.add("论文原创性良好，请继续保持");
                recommendations.add("建议检查引用格式是否规范");
            } else if (similarity <= 30) {
                recommendations.add("存在一定程度的重复内容，需要适当修改");
                recommendations.add("重点检查高相似度段落的表述方式");
            } else {
                recommendations.add("重复率较高，建议大幅修改相关内容");
                recommendations.add("重新组织高相似度段落的内容结构");
            }
            result.setRecommendations(recommendations);

            return result;
        } catch (Exception e) {
            log.error("构建查重结果失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取论文的最新查重结果
     */
    private CheckResult getLatestCheckResult(Long paperId) {
        // 这里可以根据实际的查询逻辑获取最新的查重结果
        // 暂时返回第一个结果
        try {
            List<CheckResult> results = checkResultMapper.selectList(
                    new LambdaQueryWrapper<CheckResult>()
                            .eq(CheckResult::getPaperId, paperId)
                            .eq(CheckResult::getIsDeleted, 0)
                            .orderByDesc(CheckResult::getCheckTime)
                            .last("LIMIT 1")
            );
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("获取查重结果失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 生成测试PDF报告
     * 用于验证PDF生成功能是否正常
     */
    public byte[] generateTestReport() throws IOException {
        // 创建测试用的查重结果
        SimilarityDetectionResult result = new SimilarityDetectionResult();
        result.setTargetPaperId(1L);
        result.setTargetPaperTitle("测试论文标题");
        result.setTargetAuthor("测试作者");
        result.setTargetCollege("测试学院");
        result.setOverallSimilarity(25.5);
        result.setRiskLevel("中等风险");
        result.setDetectionTime(new Date());
        result.setTotalComparisons(100);

        // 设置统计信息
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCompared", 100);
        statistics.put("highSimilarityCount", 2);
        statistics.put("mediumSimilarityCount", 5);
        statistics.put("lowSimilarityCount", 93);
        statistics.put("detectionTime", "10秒");
        result.setStatistics(statistics);

        // 设置建议措施
        List<String> recommendations = new ArrayList<>();
        recommendations.add("存在一定程度的重复内容，需要适当修改");
        recommendations.add("重点检查高相似度段落的表述方式");
        recommendations.add("建议使用不同的表达方法");
        result.setRecommendations(recommendations);

        // 设置相似论文
        List<SimilaritySegment> segments = new ArrayList<>();
        SimilaritySegment segment1 = new SimilaritySegment();
        segment1.setPaperTitle("相似论文1");
        segment1.setAuthor("作者1");
        segment1.setSimilarity(45.0);
        List<String> fragments1 = new ArrayList<>();
        fragments1.add("这是一段重复的内容，需要修改");
        fragments1.add("这是另一段重复的内容");
        segment1.setRepeatedFragments(fragments1);
        segments.add(segment1);

        SimilaritySegment segment2 = new SimilaritySegment();
        segment2.setPaperTitle("相似论文2");
        segment2.setAuthor("作者2");
        segment2.setSimilarity(30.5);
        List<String> fragments2 = new ArrayList<>();
        fragments2.add("这是重复的内容");
        segment2.setRepeatedFragments(fragments2);
        segments.add(segment2);

        result.setSimilarSegments(segments);

        return generateReport(result);
    }
}