package com.abin.checkrepeatsystem.student.service.Impl;


import cn.hutool.core.date.DateTime;
import com.abin.checkrepeatsystem.common.Exception.BusinessException;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.enums.CheckTaskStatusEnum;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.statemachine.CheckTaskStateMachine;
import com.abin.checkrepeatsystem.common.utils.PdfReportGenerator;
import com.abin.checkrepeatsystem.common.utils.TextSimilarityUtils;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.pojo.entity.*;
import com.abin.checkrepeatsystem.student.dto.CheckTaskResultDTO;
import com.abin.checkrepeatsystem.student.dto.ReportPreviewDTO;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.student.service.CheckTaskService;
import com.abin.checkrepeatsystem.user.vo.CheckResultVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 查重任务服务实现类
 */
@Slf4j
@Service
public class CheckTaskServiceImpl extends ServiceImpl<CheckTaskMapper, CheckTask> implements CheckTaskService {

    @Resource
    private PaperInfoMapper paperInfoMapper;

    @Resource
    private CheckReportMapper checkReportMapper;

    @Resource
    private PdfReportGenerator pdfReportGenerator;

    @Resource
    private TextSimilarityUtils textSimilarityUtils;

    @Resource
    private CheckTaskStateMachine stateMachine;

    @Value("${file.upload.base-path}")
    private String basePath;

    @Value("${admin.check-rule.default-max-count}")
    private int defaultMaxCount;

    @Value("${admin.check-rule.default-interval}")
    private Long checkInterval;

    // 最大并发任务数（从配置文件获取）
    @Value("${check.task.max-concurrent}")
    private int maxConcurrentTasks;

    // 任务超时时间（从配置文件获取）
    @Value("${check.task.timeout}")
    private long taskTimeout;

    @Value("${admin.check-rule.default-threshold}")
    private BigDecimal defaultThreshold;

    // Apache Tika：提取文件文本内容
    private final Tika tika = new Tika();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<CheckResultVO> createCheckTask(Long paperId) {
        Long currentUserId = UserBusinessInfoUtils.getCurrentUserId();
        // 1. 校验论文合法性与权限
        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "论文不存在或已删除");
        }
        // 仅论文所属学生可发起查重
//        if (!paperInfo.getStudentId().equals(currentUserId)) {
//            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限为他人论文发起查重");
//        }

        // 3. 校验查重次数与间隔（防止频繁查重）
//        LambdaQueryWrapper<CheckTask> taskWrapper = new LambdaQueryWrapper<>();
//        taskWrapper.eq(CheckTask::getPaperId, paperId)
//                .eq(CheckTask::getIsDeleted, 0)
//                .orderByDesc(CheckTask::getCreateTime);
//        List<CheckTask> taskList = list(taskWrapper);
//
//        // 校验最大查重次数
//        if (taskList.size() >= defaultMaxCount) {
//            return Result.error(ResultCode.BUSINESS_NO_COUNT,
//                    String.format("该论文已达到最大查重次数（%d次），无法继续发起", defaultMaxCount));
//        }

        // 校验二次查重间隔（仅当存在历史任务时校验）
//        if (!taskList.isEmpty()) {
//            CheckTask lastTask = taskList.get(0);
//            LocalDateTime lastCreateTime = lastTask.getCreateTime();
//            LocalDateTime currentTime = LocalDateTime.now();
//            // 计算时间差（秒）
//            long interval = Duration.between(lastCreateTime, currentTime).getSeconds();
//            if (interval < checkInterval) {
//                long remainingSeconds = checkInterval - interval;
//                return Result.error(ResultCode.SYSTEM_ERROR,
//                        String.format("二次查重需间隔%d秒，剩余%d秒后可发起", checkInterval, remainingSeconds));
//            }
//        }

        // 4. 校验当前并发任务数（避免服务器过载）
//        long runningTaskCount = count(
//                new LambdaQueryWrapper<CheckTask>()
//                        .eq(CheckTask::getCheckStatus, 1) // 1-执行中
//                        .eq(CheckTask::getIsDeleted, 0)
//        );
//        if (runningTaskCount >= maxConcurrentTasks) {
//            return Result.error(ResultCode.SYSTEM_ERROR, "当前系统查重任务繁忙，请稍后再试");
//        }

        // 5. 创建查重任务
        CheckTask checkTask = new CheckTask();
        checkTask.setPaperId(paperId);
        checkTask.setTaskNo(generateTaskNo()); // 生成唯一任务编号
        checkTask.setCheckStatus(DictConstants.CheckStatus.PENDING); // 0-待执行
        UserBusinessInfoUtils.setAuditField(checkTask, true); // 填充审计字段
        save(checkTask);

        // 6. 更新论文状态为“查重中”
        paperInfo.setPaperStatus(DictConstants.PaperStatus.CHECKING);// 1-查重中
        paperInfo.setCheckTime(LocalDateTime.now());// 查重时间
        paperInfoMapper.updateById(paperInfo);

        // 7. 异步触发任务执行（避免前端等待）
        executeCheckTaskAsync(checkTask.getId());

        CheckResultVO checkResultVO = new CheckResultVO();
        checkResultVO.setTaskId(checkTask.getId());
        checkResultVO.setPaperId(checkTask.getPaperId());
        checkResultVO.setCheckStatus(checkTask.getCheckStatus());
        checkResultVO.setCreateTime(checkTask.getCreateTime());

        return Result.success("查重任务创建成功，正在执行查重", checkResultVO);
    }

    @Override
    public Result<List<CheckTaskResultDTO>> getMyCheckTaskList(Long paperId, Integer checkStatus) {
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        LambdaQueryWrapper<CheckTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CheckTask::getIsDeleted, 0)
                .orderByDesc(CheckTask::getCreateTime);

        // 1. 按角色过滤数据
        if (UserBusinessInfoUtils.isStudent()) {
            // 学生：仅查询自己论文的任务（需关联论文表的student_id）
            queryWrapper.inSql(CheckTask::getPaperId,
                    "SELECT id FROM paper_info WHERE student_id = " + currentUser.getId() + " AND is_deleted = 0");
        } else if (UserBusinessInfoUtils.isTeacher()) {
            // 教师：仅查询自己指导论文的任务（需关联论文表的teacher_id）
            queryWrapper.inSql(CheckTask::getPaperId,
                    "SELECT id FROM paper_info WHERE teacher_id = " + currentUser.getId() + " AND is_deleted = 0");
        }
        // 管理员：无过滤

        // 2. 按论文ID过滤（可选）
        if (paperId != null) {
            queryWrapper.eq(CheckTask::getPaperId, paperId);
        }

        // 3. 按任务状态过滤（可选）
        if (checkStatus != null && (checkStatus >= 0 && checkStatus <= 3)) {
            queryWrapper.eq(CheckTask::getCheckStatus, checkStatus);
        }

        // 4. 查询任务并转换为DTO（含报告摘要）
        List<CheckTask> taskList = list(queryWrapper);
        List<CheckTaskResultDTO> resultDTOList = taskList.stream()
                .map(this::convertToTaskResultDTO)
                .collect(Collectors.toList());

        return Result.success("查重任务列表查询成功", resultDTOList);
    }

    @Override
    public Result<CheckTaskResultDTO> getCheckTaskDetail(Long paperId) {
        // 1. 查询任务信息
        LambdaQueryWrapper<CheckTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CheckTask::getPaperId, paperId)
                .eq(CheckTask::getIsDeleted, 0)
                .orderByDesc(CheckTask::getCreateTime);
        List<CheckTask> checkTasks = list(queryWrapper);
        CheckTask checkTask = checkTasks.isEmpty() ? null : checkTasks.get(0); // 替代 getFirst()
        if (checkTask == null || checkTask.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "查重任务不存在或已删除");
        }

        // 2. 权限校验（学生查自己的，教师查指导的，管理员查所有）
        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        boolean isStudentOwner = paperInfo.getStudentId().equals(currentUser.getId());
        boolean isAdmin = UserBusinessInfoUtils.isAdmin();
        if (!isStudentOwner && !isAdmin) {
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限查看该查重任务");
        }
        // 3. 转换为DTO（含完整报告信息）
        CheckTaskResultDTO resultDTO = convertToTaskResultDTO(checkTask, true);
        return Result.success("查重任务详情查询成功", resultDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> cancelCheckTask(Long taskId) {
        // 1. 查询任务信息
        CheckTask checkTask = getById(taskId);
        if (checkTask == null || checkTask.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "查重任务不存在或已删除");
        }

        // 2. 状态校验（仅“待执行”状态可取消）
        if (!checkTask.getCheckStatus().equals(DictConstants.CheckStatus.PENDING)) {
            return Result.error(ResultCode.PERMISSION_NOT_STATUS, "仅“待执行”状态的任务可取消");
        }

        // 3. 权限校验
        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        boolean isStudentOwner = paperInfo.getStudentId().equals(currentUser.getId());
        boolean isAdmin = UserBusinessInfoUtils.isAdmin();
        if (!isStudentOwner && !isAdmin) {
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限取消该任务");
        }

        // 4. 更新任务状态为“已取消”（自定义状态：4-已取消，需在枚举中补充）
        checkTask.setCheckStatus(DictConstants.CheckStatus.CANCELLED);
        checkTask.setFailReason("用户主动取消任务");
        UserBusinessInfoUtils.setAuditField(checkTask, false);
        updateById(checkTask);

        // 5. 恢复论文状态为“待查重”
        PaperInfo updatePaper = new PaperInfo();
        updatePaper.setId(paperInfo.getId());
        updatePaper.setPaperStatus(DictConstants.PaperStatus.CHECKING); // 0-待查重
        paperInfoMapper.updateById(updatePaper);

        return Result.success("查重任务取消成功");
    }

    @Override
    public CheckResultVO getCheckResult(Long paperId) {
        // 1. Find the latest completed check task for the given paper
        LambdaQueryWrapper<CheckTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CheckTask::getPaperId, paperId)
                .eq(CheckTask::getCheckStatus, DictConstants.CheckStatus.COMPLETED)
                .eq(CheckTask::getIsDeleted, 0)
                .orderByDesc(CheckTask::getCreateTime)
                .last("LIMIT 1");

        CheckTask checkTask = getOne(queryWrapper);

        if (checkTask == null) {
            return null; // No completed check task found
        }

        // 2. Convert CheckTask to CheckResultVO
        CheckResultVO resultVO = new CheckResultVO();
        resultVO.setTaskId(checkTask.getId());
        resultVO.setPaperId(checkTask.getPaperId());
        resultVO.setCheckStatus(checkTask.getCheckStatus());
        resultVO.setCreateTime(checkTask.getCreateTime());

        // 3. Add report information if available
        if (checkTask.getReportId() != null) {
            CheckReport checkReport = checkReportMapper.selectById(checkTask.getReportId());
            if (checkReport != null) {
                resultVO.setReportId(checkReport.getId());
                resultVO.setReportNo(checkReport.getReportNo());
                resultVO.setReportPath(checkReport.getReportPath());
            }
        }

        return resultVO;
    }


    /**
     * 异步执行查重任务（@Async需在启动类添加@EnableAsync）
     */
    @Async
    public void executeCheckTaskAsync(Long taskId) {
        LocalDateTime startTime = LocalDateTime.now();
        CheckTask checkTask = getById(taskId);
        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
    
        try {
            // 1. 使用状态机更新任务状态为"执行中"
            boolean transitionSuccess = stateMachine.transitionStatus(
                taskId, 
                CheckTaskStatusEnum.CHECKING,
                "开始执行查重任务"
            );
                
            if (!transitionSuccess) {
                log.warn("状态转换失败，任务ID: {}", taskId);
                return;
            }
    
            // 2. 校验任务超时（防止任务无限执行）
            if (Duration.between(startTime, LocalDateTime.now()).toMillis() > taskTimeout) {
                throw new BusinessException(ResultCode.SYSTEM_TIMEOUT,"查重任务执行超时（超过" + taskTimeout / 1000 + "秒）");
            }
            if (paperInfo.getFilePath() == null || paperInfo.getFilePath().trim().isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "论文文件路径未设置");
            }
                
            // 3. 提取论文文本内容（从文件路径读取）
            String paperText = extractTextFromFile(paperInfo.getFilePath());
            if (paperText.trim().isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_TYPE_ERROR,"论文文本提取失败，文件可能为空或格式不支持");
            }
    
            // 4. 模拟比对库查询（实际项目需对接校内论文库/第三方库，此处简化为模拟数据）
            List<String> compareTextList = mockCompareLibraryData(); // 模拟比对文本列表
            double maxSimilarity = 0.0;
            List<Map<String, Object>> repeatDetails = new ArrayList<>();
    
            // 5. 逐篇比对计算相似度
            for (String compareText : compareTextList) {
                double similarity = textSimilarityUtils.calculateComprehensiveSimilarity(paperText, compareText);
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                }
                // 记录重复率≥5%的段落（可自定义阈值）
                if (similarity >= 10.0) {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("similarity", similarity);
                    detail.put("source", "模拟比对库文档_" + UUID.randomUUID().toString().substring(0, 8));
                    detail.put("repeatParagraph", "..." + compareText.substring(0, 100) + "..."); // 截取前100字符作为示例
                    repeatDetails.add(detail);
                }
            }
    
            // 6. 生成查重报告（结构化数据+PDF文件）
            CheckReport checkReport = generateCheckReport(checkTask, maxSimilarity, repeatDetails);
    
            // 7. 使用状态机更新任务状态为"执行成功"
            stateMachine.transitionStatus(
                taskId, 
                CheckTaskStatusEnum.COMPLETED, 
                "查重任务执行成功"
            );
    
            // 8. 更新任务相关信息
            checkTask.setCheckRate(BigDecimal.valueOf(maxSimilarity));
            checkTask.setEndTime(LocalDateTime.now());
            checkTask.setReportId(checkReport.getId());
            updateById(checkTask);
    
            // 9. 更新论文状态（根据重复率判断：≤阈值→待审核，>阈值→审核不通过）
            PaperInfo updatePaper = new PaperInfo();
            updatePaper.setId(paperInfo.getId());
            updatePaper.setPaperStatus(maxSimilarity <= defaultThreshold.doubleValue()
                    ? DictConstants.PaperStatus.AUDITING
                    : DictConstants.PaperStatus.PENDING);
            paperInfoMapper.updateById(updatePaper);
    
        } catch (Exception e) {
            log.error("查重任务执行失败（任务ID：{}）", taskId,e);
                
            // 10. 异常处理：使用状态机更新任务状态为"执行失败"
            stateMachine.transitionStatus(
                taskId, 
                CheckTaskStatusEnum.FAILURE, 
                e.getMessage().length() > 500 ? e.getMessage().substring(0, 500) : e.getMessage()
            );
    
            // 11. 恢复论文状态为"待查重"
            PaperInfo updatePaper = new PaperInfo();
            updatePaper.setId(paperInfo.getId());
            updatePaper.setPaperStatus(DictConstants.PaperStatus.PENDING);
            updatePaper.setCheckTime(null);
            paperInfoMapper.updateById(updatePaper);
        }
    }

    // ------------------------------ 私有辅助方法 ------------------------------
    /**
     * 生成唯一任务编号（格式：CHECK+年月日+3位序号，如CHECK20251108001）
     */
    private String generateTaskNo() {
        String datePrefix = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seqPrefix = "CHECK" + datePrefix;
        // 查询当日最大序号
        LambdaQueryWrapper<CheckTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CheckTask::getTaskNo, seqPrefix)
                .eq(CheckTask::getIsDeleted, 0)
                .select(CheckTask::getTaskNo)
                .orderByDesc(CheckTask::getTaskNo)
                .last("LIMIT 1");
        CheckTask lastTask = getOne(wrapper);

        int seq = 1;
        if (lastTask != null) {
            String lastNo = lastTask.getTaskNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 3)) + 1;
        }
        // 序号补0（3位）
        return seqPrefix + String.format("%03d", seq);
    }

    /**
     * 从文件中提取文本内容（支持doc/docx/pdf）
     */
    private String extractTextFromFile(String filePath) throws IOException, TikaException, SAXException {
        String fullPath = basePath + filePath;
        File file = new File(fullPath);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(ResultCode.PARAM_ERROR,"论文文件不存在：" + filePath);
        }
        // 使用Tika提取文本（自动识别文件类型）
        try (InputStream inputStream = new FileInputStream(file)) {
            return tika.parseToString(inputStream);
        }
    }

    /**
     * 生成查重报告（含结构化数据与PDF文件）
     */
    private CheckReport generateCheckReport(CheckTask checkTask, double checkRate, List<Map<String, Object>> repeatDetails) {
        // 1. 生成报告编号（格式：REPORT+任务编号后缀，如REPORT20251108001）
        String reportNo = "REPORT" + checkTask.getTaskNo().substring(5);

        // 2. 生成PDF报告文件（后续开发：调用ReportGenerateUtils生成，此处简化为模拟路径）
        String reportPath = "D:/data/report" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/" + reportNo + ".pdf";

        // 3. 构建报告实体
        CheckReport checkReport = new CheckReport();
        checkReport.setTaskId(checkTask.getId());
        checkReport.setReportNo(reportNo);
        checkReport.setRepeatDetails(JSON.toJSONString(repeatDetails)); // 重复详情转JSON字符串
        checkReport.setReportPath(reportPath);
        checkReport.setReportType("pdf");
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
                                        double checkRate, List<Map<String, Object>> repeatDetails) {
        try {
            log.info("开始生成查重报告PDF - 任务ID: {}, 报告ID: {}", checkTask.getId(), checkReport.getId());

            // 构建报告预览数据
            ReportPreviewDTO previewDTO = buildReportPreviewDTO(checkTask, checkReport, checkRate, repeatDetails);

            // 调用PDF生成服务
            pdfReportGenerator.generatePdfToFile(previewDTO, checkReport.getReportPath());

            log.info("查重报告PDF生成成功 - 路径: {}", checkReport.getReportPath());

        } catch (Exception e) {
            log.error("查重报告PDF生成失败 - 任务ID: {}, 错误: ", checkTask.getId(), e);
            // 可以设置报告状态为生成失败，但不影响整体查重流程
            // 或者抛出异常让事务回滚
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "查重报告生成失败");
        }
    }
    /**
     * 构建报告预览DTO
     */
    private ReportPreviewDTO buildReportPreviewDTO(CheckTask checkTask, CheckReport checkReport,
                                                   double checkRate, List<Map<String, Object>> repeatDetails) {
        ReportPreviewDTO dto = new ReportPreviewDTO();
        ReportPreviewDTO.ReportBaseInfoDTO baseInfo = new ReportPreviewDTO.ReportBaseInfoDTO();
        // 设置查重任务信息
        baseInfo.setTaskId(checkTask.getId());
        baseInfo.setTaskNo(checkTask.getTaskNo());
        baseInfo.setReportId(checkReport.getId());
        baseInfo.setReportNo(checkReport.getReportNo());

        // 设置查重结果
        baseInfo.setSimilarityRate(BigDecimal.valueOf(checkRate));
        baseInfo.setCheckTime(LocalDateTime.now());
        baseInfo.setReportDetails(checkReport.getRepeatDetails());

        // 设置论文信息
        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
        if (paperInfo != null) {
            baseInfo.setPaperTitle(paperInfo.getPaperTitle());
            baseInfo.setAuthor(paperInfo.getAuthor());
            baseInfo.setStudentId(paperInfo.getStudentId());
        }

        // 设置用户信息
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        if (currentUser != null) {
            baseInfo.setUserName(currentUser.getUsername());
            baseInfo.setRealName(currentUser.getRealName());
        }
        // 将 baseInfo 设置到 dto
        dto.setBaseInfo(baseInfo);
        return dto;
    }

    /**
     * 转换CheckTask为CheckTaskResultDTO（含报告摘要）
     */
    private CheckTaskResultDTO convertToTaskResultDTO(CheckTask checkTask) {
        return convertToTaskResultDTO(checkTask, false);
    }

    /**
     * 转换CheckTask为CheckTaskResultDTO（支持是否返回完整报告）
     * @param withFullReport 是否返回完整报告信息
     */
    private CheckTaskResultDTO convertToTaskResultDTO(CheckTask checkTask, boolean withFullReport) {
        CheckTaskResultDTO dto = new CheckTaskResultDTO();
        dto.setTaskId(checkTask.getId());
        dto.setTaskNo(checkTask.getTaskNo());
        dto.setPaperId(checkTask.getPaperId());
        dto.setCheckStatus(checkTask.getCheckStatus());
        dto.setCheckRate(checkTask.getCheckRate() != null ? checkTask.getCheckRate().doubleValue() : null);
        dto.setStartTime(checkTask.getStartTime());
        dto.setEndTime(checkTask.getEndTime());
        dto.setFailReason(checkTask.getFailReason());

        // 补充论文标题
        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
        if (paperInfo != null) {
            dto.setPaperTitle(paperInfo.getPaperTitle());
        }

        // 补充报告摘要（仅任务成功且需要时）
        if (checkTask.getCheckStatus().equals(DictConstants.CheckStatus.COMPLETED) && checkTask.getReportId() != null && (withFullReport || true)) {
            CheckReport checkReport = checkReportMapper.selectById(checkTask.getReportId());
            if (checkReport != null) {
                CheckTaskResultDTO.CheckReportSummaryDTO reportSummary = new CheckTaskResultDTO.CheckReportSummaryDTO();
                reportSummary.setReportId(checkReport.getId());
                reportSummary.setReportNo(checkReport.getReportNo());
                // 解析重复段落数量（从JSON字符串中提取）
                List<Map<String, Object>> repeatDetails = JSON.parseObject(checkReport.getRepeatDetails(), new TypeReference<List<Map<String, Object>>>(){});
                reportSummary.setRepeatParagraphCount(repeatDetails.size());
                // 构建报告下载URL（前端拼接域名，如：/api/v1/student/reports/download?reportId=xxx）
                reportSummary.setReportDownloadUrl("/api/student/reports/download?reportId=" + checkReport.getId());
                dto.setReportSummary(reportSummary);
            }
        }

        return dto;
    }

    /**
     * 模拟比对库数据（实际项目需对接真实数据库，此处仅用于测试）
     */
    private List<String> mockCompareLibraryData() {
        List<String> data = new ArrayList<>();
        // 模拟3篇比对文本（含重复内容）
        data.add("Spring Boot是由Pivotal团队提供的全新框架，其设计目的是用来简化新Spring应用的初始搭建以及开发过程。该框架使用了特定的方式来进行配置，从而使开发人员不再需要定义样板化的配置。");
        data.add("基于Spring Boot的论文查重管理系统，通常采用前后端分离架构，后端使用Spring Boot框架实现RESTful API，前端使用Vue.js框架开发响应式界面。");
        data.add("文本相似度计算是论文查重系统的核心技术，常用的算法包括SimHash、余弦相似度、Jaccard相似度等，其中SimHash适合大规模文本快速去重，余弦相似度适合精细比对。");
        return data;
    }
}
