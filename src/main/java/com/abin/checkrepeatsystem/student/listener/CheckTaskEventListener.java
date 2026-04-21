package com.abin.checkrepeatsystem.student.listener;

import com.abin.checkrepeatsystem.admin.mapper.CheckResultMapper;
import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.engine.CheckEngineManager;
import com.abin.checkrepeatsystem.common.enums.CheckEngineTypeEnum;
import com.abin.checkrepeatsystem.common.enums.CheckTaskStatusEnum;
import com.abin.checkrepeatsystem.common.enums.PaperStatusEnum;
import com.abin.checkrepeatsystem.common.statemachine.CheckTaskStateMachine;
import com.abin.checkrepeatsystem.common.utils.PdfReportGenerator;
import com.abin.checkrepeatsystem.common.utils.SpringContextUtil;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.common.utils.UserContextHolder;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.pojo.vo.CheckResult;
import com.abin.checkrepeatsystem.student.dto.ReportPreviewDTO;
import com.abin.checkrepeatsystem.student.event.CheckProgressEvent;
import com.abin.checkrepeatsystem.student.event.CheckTaskCreatedEvent;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.common.websocket.handler.CheckProgressWebSocketHandler;
import jakarta.annotation.Resource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 查重任务事件监听器
 * 异步处理查重任务的执行
 */
@Component
@Slf4j
public class CheckTaskEventListener {

    @Resource
    private CheckTaskMapper checkTaskMapper;

    @Resource
    private PaperInfoMapper paperInfoMapper;

    @Resource
    private CheckReportMapper checkReportMapper;

    @Resource
    private CheckTaskStateMachine stateMachine;

    @Resource
    private CheckEngineManager checkEngineManager;

    @Resource
    private PdfReportGenerator pdfReportGenerator;



    @Value("${file.upload.base-path}")
    private String basePath;
    
    /**
     * 初始化上传路径，确保在Windows环境下使用正确的路径
     */
    @PostConstruct
    private void init() {
        try {
            // 如果是 Windows 环境且路径为 Unix 风格，转换为 Windows 路径
            if (System.getProperty("os.name").toLowerCase().contains("win") && 
                (basePath.startsWith("/") || basePath.startsWith("data"))) {
                // 使用用户主目录下的临时上传目录
                String userHome = System.getProperty("user.home");
                basePath = java.nio.file.Paths.get(userHome, "check-repeat-system", "upload").toString();
                log.info("检测到 Windows 环境，使用上传路径：{}", basePath);
            }
        } catch (Exception e) {
            log.error("初始化上传路径失败", e);
        }
    }

    @Value("${admin.check-rule.default-threshold}")
    private BigDecimal defaultThreshold;

    @Value("${check.task.timeout:3600000}")
    private long taskTimeout;

    private final Tika tika = new Tika();

    /**
     * 处理任务创建事件
     */
    @Async
    @EventListener
    @Transactional
    public void handleCheckTaskCreatedEvent(CheckTaskCreatedEvent event) {
        try {
            Long taskId = event.getTaskId();
            CheckTask checkTask = checkTaskMapper.selectById(taskId);
            if (checkTask == null) {
                log.error("查重任务不存在：{}", taskId);
                return;
            }
            
            // 1. 更新任务状态为进行中
            stateMachine.transitionStatus(taskId, CheckTaskStatusEnum.CHECKING, "开始查重");
            
            // 2. 发送开始查重通知
            sendProgressMessage(taskId, 0, "开始查重，请稍候...");
            
            // 3. 执行查重
            long startTime = System.currentTimeMillis();
            CheckTask checkTaskInDB = checkTaskMapper.selectById(taskId);
            if (checkTaskInDB == null) {
                log.error("查重任务不存在：{}", taskId);
                return;
            }
            
            // 3.1 获取论文信息
            PaperInfo paperInfo = paperInfoMapper.selectById(checkTaskInDB.getPaperId());
            if (paperInfo == null) {
                log.error("论文信息不存在：{}", checkTaskInDB.getPaperId());
                stateMachine.transitionStatus(taskId, CheckTaskStatusEnum.FAILURE, "论文信息不存在");
                sendProgressMessage(taskId, 100, "论文信息不存在，查重失败");
                return;
            }
            
            // 3.2 提取论文内容
            sendProgressMessage(taskId, 10, "正在提取论文内容...");
            String paperContent = extractTextFromFile(paperInfo.getFilePath());
            if (paperContent == null || paperContent.trim().isEmpty()) {
                log.error("论文内容为空：{}", paperInfo.getId());
                stateMachine.transitionStatus(taskId, CheckTaskStatusEnum.FAILURE, "论文内容为空");
                sendProgressMessage(taskId, 100, "论文内容为空，查重失败");
                return;
            }
            
            // 3.3 执行查重
            sendProgressMessage(taskId, 30, "正在执行查重...");
            CheckResult checkResult = null;
            try {
                checkResult = checkEngineManager.executeCheck(paperContent, paperInfo.getPaperTitle(), java.util.Arrays.asList(com.abin.checkrepeatsystem.common.enums.CheckEngineTypeEnum.LOCAL));
            } catch (Exception e) {
                log.error("执行查重失败：{}", e.getMessage(), e);
                stateMachine.transitionStatus(taskId, CheckTaskStatusEnum.FAILURE, "执行查重失败：" + e.getMessage());
                sendProgressMessage(taskId, 100, "执行查重失败：" + e.getMessage());
                return;
            }
            
            // 3.4 解析查重结果
            sendProgressMessage(taskId, 70, "正在生成查重报告...");
            if (!checkResult.isSuccess()) {
                log.error("查重失败：{}", checkResult.getFailReason());
                stateMachine.transitionStatus(taskId, CheckTaskStatusEnum.FAILURE, "查重失败：" + checkResult.getFailReason());
                sendProgressMessage(taskId, 100, "查重失败：" + checkResult.getFailReason());
                return;
            }
            
            // 4. 生成查重报告
            BigDecimal checkRate = checkResult.getSimilarity();
            List<Map<String, Object>> repeatDetails = parseRepeatDetailsFromCheckResult(checkResult);
            CheckReport checkReport = generateCheckReport(checkTaskInDB, checkRate, repeatDetails);
            
            // 5. 更新任务状态为完成
            stateMachine.transitionStatus(taskId, CheckTaskStatusEnum.COMPLETED, "查重完成");
            checkTaskInDB.setCheckRate(checkRate);
            checkTaskMapper.updateById(checkTaskInDB);
            
            // 6. 更新论文状态
            updatePaperStatus(paperInfo, checkRate);
            
            // 7. 发送完成通知
            sendProgressMessage(taskId, 100, "查重完成，相似度：" + checkRate + "%");
            
            long endTime = System.currentTimeMillis();
            log.info("查重任务完成 - 任务ID: {}, 耗时: {}秒, 相似度: {}", 
                     taskId, (endTime - startTime) / 1000, checkRate);
            
        } catch (Exception e) {
            log.error("处理查重任务失败", e);
        }
    }

    /**
     * 发送进度消息
     */
    private void sendProgressMessage(Long taskId, int progress, String message) {
        try {
            Map<String, Object> progressMessage = new HashMap<>();
            progressMessage.put("taskId", taskId);
            progressMessage.put("progress", progress);
            progressMessage.put("message", message);
            progressMessage.put("timestamp", System.currentTimeMillis());
            
            String messageJson = com.alibaba.fastjson.JSON.toJSONString(progressMessage);
            
            // 发送消息
            CheckProgressWebSocketHandler.sendProgressMessage(taskId.toString(), messageJson);
        } catch (Exception e) {
            log.warn("WebSocket 进度推送失败 - 任务 ID: {}", taskId, e);
        }
    }

    /**
     * 从文件中提取文本内容
     */
    private String extractTextFromFile(String filePath) throws Exception {
        // 确保basePath以斜杠结尾
        String normalizedBasePath = basePath.endsWith("/") ? basePath : basePath + "/";
        // 处理文件路径中的反斜杠，统一转换为斜杠
        String normalizedFilePath = filePath.replace("\\", "/");
        String fullPath = normalizedBasePath + normalizedFilePath;
        
        // 打印完整文件路径，方便调试
        log.info("尝试读取论文文件：{}", fullPath);
        
        File file = new File(fullPath);
        if (!file.exists() || !file.isFile()) {
            // 检查目录是否存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                log.warn("论文文件目录不存在：{}", parentDir.getAbsolutePath());
            }
            throw new RuntimeException("论文文件不存在：" + fullPath);
        }
        
        try (InputStream inputStream = new FileInputStream(file)) {
            return tika.parseToString(inputStream);
        }
    }

    /**
     * 解析查重结果详情
     */
    private List<Map<String, Object>> parseRepeatDetailsFromCheckResult(
            CheckResult checkResult) {
        List<Map<String, Object>> details = new ArrayList<>();
        
        try {
            // 从 CheckResult 的 extraInfo 或报告中提取详细信息
            String extraInfo = checkResult.getExtraInfo();
            if (extraInfo != null && !extraInfo.trim().isEmpty()) {
                // 处理相似论文列表
                List<CheckResult.SimilarPaper> similarPapers = checkResult.getSimilarPapers();
                if (similarPapers != null && !similarPapers.isEmpty()) {
                    for (CheckResult.SimilarPaper similarPaper : similarPapers) {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("similarity", similarPaper.getSimilarity().doubleValue());
                        detail.put("source", "论文信息库（SimHash+ 余弦相似度）");
                        detail.put("reportUrl", checkResult.getReportUrl());
                        detail.put("extraInfo", extraInfo);
                        detail.put("sourceId", similarPaper.getPaperId()); // 添加相似论文ID作为sourceId
                        detail.put("sourceName", similarPaper.getPaperTitle()); // 添加相似论文标题
                        details.add(detail);
                    }
                } else {
                    // 如果没有相似论文列表，创建默认详情
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("similarity", checkResult.getSimilarity().doubleValue());
                    detail.put("source", "论文信息库（SimHash+ 余弦相似度）");
                    detail.put("reportUrl", checkResult.getReportUrl());
                    detail.put("extraInfo", extraInfo);
                    details.add(detail);
                }
            }
        } catch (Exception e) {
            log.error("解析查重结果详情失败", e);
        }
        
        return details;
    }

    /**
     * 生成查重报告
     */
    private CheckReport generateCheckReport(CheckTask checkTask,
                                           BigDecimal checkRate, List<Map<String, Object>> repeatDetails) {
        // 使用 CheckTaskServiceImpl 中的方法
        try {
            // 反射调用或重新实现
            return createCheckReport(checkTask, checkRate, repeatDetails);
        } catch (Exception e) {
            log.error("生成查重报告失败", e);
            throw new RuntimeException("生成查重报告失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建查重报告（简化版）
     */
    private CheckReport createCheckReport(CheckTask checkTask, BigDecimal checkRate, List<Map<String, Object>> repeatDetails) {
        // 1. 生成报告编号
        String reportNo = "REPORT" + System.currentTimeMillis();
        
        // 2. 生成报告路径
        String reportPath = "D:/data/report/" + LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/" + reportNo + ".pdf";
        
        // 3. 构建报告实体
        CheckReport checkReport = new CheckReport();
        checkReport.setTaskId(checkTask.getId());
        checkReport.setPaperId(checkTask.getPaperId());
        checkReport.setReportNo(reportNo);
        checkReport.setTotalSimilarity(checkRate);
        checkReport.setRepeatDetails(com.alibaba.fastjson.JSON.toJSONString(repeatDetails));
        checkReport.setReportPath(reportPath);
        checkReport.setReportType("pdf");
        checkReport.setReportGenerateTime(LocalDateTime.now());
        
        // 使用真实的用户信息进行审计操作，确保责任溯源
        UserBusinessInfoUtils.setAuditField(checkReport, true);
        checkReportMapper.insert(checkReport);
        
        // 4. 关键：实际生成PDF文件
        generateCheckReportPdf(checkTask, checkReport, checkRate, repeatDetails);
        
        return checkReport;
    }
    
    /**
     * 生成PDF报告文件
     */
    private void generateCheckReportPdf(CheckTask checkTask, CheckReport checkReport,
                                        BigDecimal checkRate, List<Map<String, Object>> repeatDetails) {
        try {
            log.info("开始生成查重报告PDF - 任务ID: {}, 报告ID: {}", checkTask.getId(), checkReport.getId());

            // 构建报告预览数据
            ReportPreviewDTO previewDTO = buildReportPreviewDTO(checkTask, checkReport, checkRate, repeatDetails);

            // 确保报告目录存在
            File reportFile = new File(checkReport.getReportPath());
            File parentDir = reportFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.mkdirs()) {
                    log.info("报告目录创建成功: {}", parentDir.getAbsolutePath());
                } else {
                    log.error("报告目录创建失败: {}", parentDir.getAbsolutePath());
                    throw new RuntimeException("报告目录创建失败");
                }
            }

            // 调用PDF生成服务
            pdfReportGenerator.generatePdfToFile(previewDTO, checkReport.getReportPath());

            log.info("查重报告PDF生成成功 - 路径: {}", checkReport.getReportPath());

        } catch (Exception e) {
            log.error("查重报告PDF生成失败 - 任务ID: {}, 错误: ", checkTask.getId(), e);
            // 抛出异常让事务回滚
            throw new RuntimeException("查重报告生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建报告预览DTO
     */
    private ReportPreviewDTO buildReportPreviewDTO(CheckTask checkTask, CheckReport checkReport,
                                                  BigDecimal checkRate, List<Map<String, Object>> repeatDetails) {
        ReportPreviewDTO dto = new ReportPreviewDTO();
        ReportPreviewDTO.ReportBaseInfoDTO baseInfo = new ReportPreviewDTO.ReportBaseInfoDTO();
        // 设置查重任务信息
        baseInfo.setTaskId(checkTask.getId());
        baseInfo.setTaskNo(checkTask.getTaskNo());
        baseInfo.setReportId(checkReport.getId());
        baseInfo.setReportNo(checkReport.getReportNo());

        // 设置查重结果
        baseInfo.setSimilarityRate((checkRate));
        baseInfo.setCheckTime(LocalDateTime.now());
        baseInfo.setReportDetails(checkReport.getRepeatDetails());

        // 设置论文信息
        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
        if (paperInfo != null) {
            baseInfo.setPaperTitle(paperInfo.getPaperTitle());
            baseInfo.setAuthor(paperInfo.getAuthor());
            baseInfo.setStudentId(paperInfo.getStudentId());
            // 设置学生姓名
            baseInfo.setStudentName(paperInfo.getAuthor());
            // 设置指导教师
            baseInfo.setTeacherName(paperInfo.getTeacherName());
        }

        // 设置用户信息
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        if (currentUser != null) {
            baseInfo.setUserName(currentUser.getUsername());
            baseInfo.setRealName(currentUser.getRealName());
        }
        
        // 设置生成时间
        baseInfo.setGenerateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        // 将 baseInfo 设置到 dto
        dto.setBaseInfo(baseInfo);
        
        // 设置段落详情
        List<ReportPreviewDTO.ReportParagraphDTO> paragraphs = new ArrayList<>();
        if (paperInfo != null) {
            try {
                // 提取论文文本内容
                String paperText = extractTextFromFile(paperInfo.getFilePath());
                if (paperText != null && !paperText.trim().isEmpty()) {
                    // 简单分割段落（按换行符）
                    String[] textParagraphs = paperText.split("\\n\\s*\\n");
                    int paraNo = 1;
                    for (String para : textParagraphs) {
                        if (para.trim().isEmpty()) continue;
                        
                        ReportPreviewDTO.ReportParagraphDTO paragraphDTO = new ReportPreviewDTO.ReportParagraphDTO();
                        paragraphDTO.setParagraphNo(paraNo++);
                        // 设置相似度（如果有相似来源，设置为最高相似度）
                        paragraphDTO.setSimilarity(checkRate);
                        // 设置内容（如果有重复，添加标红标记）
                        if (checkRate.compareTo(BigDecimal.ZERO) > 0) {
                            // 简单标红处理：在内容前添加标红标记
                            paragraphDTO.setContent("<span style=\"color:red\">" + para.trim() + "</span>");
                        } else {
                            paragraphDTO.setContent(para.trim());
                        }
                        paragraphs.add(paragraphDTO);
                    }
                }
            } catch (Exception e) {
                log.warn("提取论文段落失败", e);
            }
        }
        dto.setParagraphs(paragraphs);
        
        // 设置相似来源
        List<ReportPreviewDTO.ReportSimilarSourceDTO> similarSources = new ArrayList<>();
        if (!repeatDetails.isEmpty()) {
            int sourceNo = 1;
            for (Map<String, Object> detail : repeatDetails) {
                ReportPreviewDTO.ReportSimilarSourceDTO sourceDTO = new ReportPreviewDTO.ReportSimilarSourceDTO();
                sourceDTO.setSourceName(detail.get("sourceName") != null ? detail.get("sourceName").toString() : "未知来源");
                sourceDTO.setSourceType("论文库");
                sourceDTO.setMaxSimilarity(BigDecimal.valueOf((Double) detail.get("similarity")));
                similarSources.add(sourceDTO);
            }
        } else if (checkRate.compareTo(BigDecimal.ZERO) > 0) {
            // 如果没有详细信息但有相似度，添加默认相似来源
            ReportPreviewDTO.ReportSimilarSourceDTO sourceDTO = new ReportPreviewDTO.ReportSimilarSourceDTO();
            sourceDTO.setSourceName("论文信息库");
            sourceDTO.setSourceType("本地库");
            sourceDTO.setMaxSimilarity(checkRate);
            similarSources.add(sourceDTO);
        }
        dto.setSimilarSources(similarSources);
        
        return dto;
    }

    /**
     * 更新论文状态为成功
     */
    private void updatePaperSuccess(PaperInfo paperInfo, BigDecimal similarity) {
        PaperInfo updatePaper = new PaperInfo();
        updatePaper.setId(paperInfo.getId());
        updatePaper.setCheckCompleted(1);
        updatePaper.setCheckEngineType("local");
        updatePaper.setCheckSource("school");
        updatePaper.setSimilarityRate(similarity);
        updatePaper.setPaperStatus(PaperStatusEnum.AUDITING.getValue());
        updatePaper.setUpdateTime(LocalDateTime.now());
        paperInfoMapper.updateById(updatePaper);
    }

    /**
     * 更新论文状态
     */
    private void updatePaperStatus(PaperInfo paperInfo, BigDecimal similarity) {
        try {
            updatePaperSuccess(paperInfo, similarity);
        } catch (Exception e) {
            log.error("更新论文状态失败", e);
        }
    }
}