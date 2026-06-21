package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.vo.CheckResult;
import com.abin.checkrepeatsystem.common.engine.CheckEngineManager;
import com.abin.checkrepeatsystem.common.enums.CheckEngineTypeEnum;
import com.abin.checkrepeatsystem.common.enums.CheckTaskStatusEnum;
import com.abin.checkrepeatsystem.common.enums.PaperStatusEnum;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.service.PaperStatusTransitionService;
import com.abin.checkrepeatsystem.common.utils.PdfReportGenerator;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.notification.service.NotificationService;
import com.abin.checkrepeatsystem.pojo.entity.*;
import com.abin.checkrepeatsystem.student.dto.CheckTaskResultDTO;
import com.abin.checkrepeatsystem.student.dto.ReportPreviewDTO;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.student.service.CheckRuleService;
import com.abin.checkrepeatsystem.student.service.EnhancedCheckTaskService;
import com.abin.checkrepeatsystem.user.service.SysUserService;
import com.abin.checkrepeatsystem.user.vo.CheckResultVO;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强版查重任务服务实现
 * 提供完整的任务生命周期管理
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class EnhancedCheckTaskServiceImpl extends ServiceImpl<CheckTaskMapper, CheckTask> 
        implements EnhancedCheckTaskService {
    
    private final PaperInfoMapper paperInfoMapper;

    private final CheckReportMapper checkReportMapper;

    private final CheckEngineManager checkEngineManager;

    private final PdfReportGenerator pdfReportGenerator;

    private final SysUserService sysUserService;

    private final PaperStatusTransitionService paperStatusTransitionService;

    private final CheckRuleService checkRuleService;

    private final NotificationService notificationService;
    
    @Value("${file.upload.base-path}")
    private String basePath;
    
    @Value("${check.task.timeout:3600000}")
    private long taskTimeout;
    
    @Value("${admin.check-rule.default-threshold:20.00}")
    private BigDecimal defaultThreshold;
    
    private final Tika tika = new Tika();
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<CheckResultVO> createEnhancedCheckTask(Long paperId, List<String> engineTypes) {
        Long currentUserId = UserBusinessInfoUtils.getCurrentUserId();

        // 1. 校验论文
        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "论文不存在或已删除");
        }

        // 2. 读取查重规则（默认规则）
        CheckRule checkRule = checkRuleService.getDefaultRule();
        if (checkRule == null) {
            return Result.error(ResultCode.SYSTEM_ERROR, "未配置查重规则，请联系管理员");
        }

        // 3. 校验查重次数限制
        if (checkRule.getMaxCheckCount() != null && checkRule.getMaxCheckCount() > 0) {
            Long checkCount = count(new LambdaQueryWrapper<CheckTask>()
                    .eq(CheckTask::getPaperId, paperId)
                    .eq(CheckTask::getIsDeleted, 0)
                    .ne(CheckTask::getCheckStatus, CheckTaskStatusEnum.CANCELLED.getCode()));
            if (checkCount >= checkRule.getMaxCheckCount()) {
                return Result.error(ResultCode.PERMISSION_NOT_STATUS,
                        String.format("该论文查重次数已达上限（最多%d次）", checkRule.getMaxCheckCount()));
            }
        }

        // 4. 校验查重间隔
        if (checkRule.getCheckInterval() != null && checkRule.getCheckInterval() > 0) {
            CheckTask lastTask = getOne(new LambdaQueryWrapper<CheckTask>()
                    .eq(CheckTask::getPaperId, paperId)
                    .eq(CheckTask::getIsDeleted, 0)
                    .orderByDesc(CheckTask::getCreateTime)
                    .last("LIMIT 1"));
            if (lastTask != null && lastTask.getEndTime() != null) {
                long secondsSinceLastCheck = java.time.Duration.between(
                        lastTask.getEndTime(), LocalDateTime.now()).getSeconds();
                if (secondsSinceLastCheck < checkRule.getCheckInterval()) {
                    long waitSeconds = checkRule.getCheckInterval() - secondsSinceLastCheck;
                    return Result.error(ResultCode.PERMISSION_NOT_STATUS,
                            String.format("距上次查重间隔不足，请等待%d秒后重试", waitSeconds));
                }
            }
        }

        // 5. 解析引擎类型
        List<CheckEngineTypeEnum> engines = parseEngineTypes(engineTypes);

        // 6. 创建任务（关联查重规则）
        CheckTask checkTask = new CheckTask();
        checkTask.setPaperId(paperId);
        checkTask.setTaskNo(generateTaskNo());
        checkTask.setCheckStatus(CheckTaskStatusEnum.PENDING.getCode());
        checkTask.setCheckRuleId(checkRule.getId());
        UserBusinessInfoUtils.setAuditField(checkTask, true);
        save(checkTask);

        // 7. 更新论文状态为查重中（通过状态机服务）
        paperStatusTransitionService.transitionSilently(
                paperId, PaperStatusEnum.CHECKING, currentUserId, "发起查重任务");
        paperInfo.setCheckTime(LocalDateTime.now());
        paperInfoMapper.updateById(paperInfo);

        // 8. 异步执行查重（传入规则阈值）
        executeCheckTaskAsync(checkTask.getId(), engines);

        // 9. 返回结果
        CheckResultVO resultVO = buildCheckResultVO(checkTask);
        return Result.success("查重任务创建成功", resultVO);
    }
    
    @Override
    public Result<List<CheckTaskResultDTO>> getCheckTaskList(Long paperId, String status, 
                                                           Integer pageNum, Integer pageSize) {
        Page<CheckTask> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<CheckTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckTask::getIsDeleted, 0)
               .orderByDesc(CheckTask::getCreateTime);
        
        // 权限过滤
        applyPermissionFilter(wrapper);
        
        // 条件过滤
        if (paperId != null) {
            wrapper.eq(CheckTask::getPaperId, paperId);
        }
        if (status != null) {
            wrapper.eq(CheckTask::getCheckStatus, status);
        }
        
        Page<CheckTask> resultPage = page(page, wrapper);
        List<CheckTaskResultDTO> dtos = resultPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return Result.success(dtos);
    }
    
    @Override
    public Result<CheckTaskResultDTO> getCheckTaskDetail(Long taskId) {
        CheckTask task = getById(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "任务不存在");
        }
        
        // 权限校验
        if (!hasTaskAccess(task)) {
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限访问该任务");
        }
        
        CheckTaskResultDTO dto = convertToDTO(task);
        return Result.success(dto);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> cancelCheckTask(Long taskId) {
        CheckTask task = getById(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "任务不存在");
        }
        
        CheckTaskStatusEnum currentStatus = CheckTaskStatusEnum.fromCode(task.getCheckStatus());
        if (!currentStatus.canBeCancelled()) {
            return Result.error(ResultCode.PERMISSION_NOT_STATUS, 
                              "当前状态不允许取消：" + currentStatus.getDescription());
        }
        
        if (!hasTaskAccess(task)) {
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限取消该任务");
        }
        
        boolean success = updateTaskStatus(taskId, CheckTaskStatusEnum.CANCELLED, "用户主动取消");
        return success ? Result.success("任务取消成功") : Result.error(ResultCode.SYSTEM_ERROR,"任务取消失败");
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> retryCheckTask(Long taskId) {
        CheckTask task = getById(taskId);
        if (task == null || task.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "任务不存在");
        }
        
        CheckTaskStatusEnum currentStatus = CheckTaskStatusEnum.fromCode(task.getCheckStatus());
        if (currentStatus != CheckTaskStatusEnum.FAILURE) {
            return Result.error(ResultCode.PERMISSION_NOT_STATUS, "只有失败的任务才能重试");
        }
        
        // 重置任务状态
        task.setCheckStatus(CheckTaskStatusEnum.PENDING.getCode());
        task.setFailReason(null);
        task.setStartTime(null);
        task.setEndTime(null);
        task.setCheckRate(null);
        updateById(task);
        
        // 异步重新执行 - 使用Collections.singletonList
        executeCheckTaskAsync(taskId, Collections.singletonList(CheckEngineTypeEnum.LOCAL));
        
        return Result.success("任务重试已启动");
    }
    
    @Override
    public Result<Object> getTaskStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 各状态任务数量统计
        Map<String, Long> statusCount = new HashMap<>();
        for (CheckTaskStatusEnum status : CheckTaskStatusEnum.values()) {
            LambdaQueryWrapper<CheckTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CheckTask::getCheckStatus, status.getCode())
                   .eq(CheckTask::getIsDeleted, 0);
            applyPermissionFilter(wrapper);
            statusCount.put(status.getDescription(), count(wrapper));
        }
        stats.put("statusCount", statusCount);
        
        // 今日任务统计
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LambdaQueryWrapper<CheckTask> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(CheckTask::getCreateTime, todayStart)
                   .eq(CheckTask::getIsDeleted, 0);
        applyPermissionFilter(todayWrapper);
        stats.put("todayTaskCount", count(todayWrapper));
        
        return Result.success(stats);
    }
    
    @Override
    public boolean updateTaskStatus(Long taskId, CheckTaskStatusEnum newStatus, String reason) {
        CheckTask task = getById(taskId);
        if (task == null) return false;
        
        CheckTaskStatusEnum currentStatus = CheckTaskStatusEnum.fromCode(task.getCheckStatus());
        if (!currentStatus.canTransitionTo(newStatus)) {
            log.warn("非法状态流转：{} -> {}", currentStatus.getDescription(), newStatus.getDescription());
            return false;
        }
        
        task.setCheckStatus(newStatus.getCode());
        if (newStatus.isFinalStatus()) {
            task.setEndTime(LocalDateTime.now());
        }
        updateById(task);
        
        log.info("任务状态更新：ID={}, {} -> {}, 原因={}", 
                taskId, currentStatus.getDescription(), newStatus.getDescription(), reason);
        return true;
    }
    
    @Override
    public Result<Object> getTaskProgress(Long taskId) {
        CheckTask task = getById(taskId);
        if (task == null) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "任务不存在");
        }
        
        Map<String, Object> progress = new HashMap<>();
        progress.put("taskId", taskId);
        progress.put("status", task.getCheckStatus());
        progress.put("statusDescription", CheckTaskStatusEnum.fromCode(task.getCheckStatus()).getDescription());
        
        if (task.getStartTime() != null && task.getEndTime() != null) {
            long duration = Duration.between(task.getStartTime(), task.getEndTime()).getSeconds();
            progress.put("duration", duration + "秒");
        }
        
        return Result.success(progress);
    }
    
    // ------------------------------ 异步执行方法 ------------------------------
    
    @Async("checkTaskExecutor")
    public void executeCheckTaskAsync(Long taskId, List<CheckEngineTypeEnum> engineTypes) {
        CheckTask task = getById(taskId);
        if (task == null) return;
        
        // 更新状态为执行中
        if (!updateTaskStatus(taskId, CheckTaskStatusEnum.CHECKING, "开始执行查重")) {
            return;
        }
        
        try {
            PaperInfo paperInfo = paperInfoMapper.selectById(task.getPaperId());
            if (paperInfo == null || paperInfo.getFilePath() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "论文文件不存在");
            }
            
            // 提取文本
            String paperText = extractTextFromFile(paperInfo.getFilePath());
            task.setStartTime(LocalDateTime.now());
            updateById(task);
            
            // 执行查重
            CheckResult checkResult =
                    checkEngineManager.executeCheck(paperText, paperInfo.getPaperTitle(), engineTypes);
            
            // 生成报告
            CheckReport report = generateCheckReport(task, checkResult);
            
            // 更新任务状态
            task.setCheckRate(checkResult.getSimilarity());
            task.setReportId(report.getId());
            task.setReportPath(report.getReportPath());
            
            CheckTaskStatusEnum finalStatus = checkResult.isSuccess() ? 
                    CheckTaskStatusEnum.COMPLETED : CheckTaskStatusEnum.FAILURE;
            updateTaskStatus(taskId, finalStatus, checkResult.getFailReason());
            
            // 更新论文状态
            updatePaperStatus(paperInfo, checkResult.getSimilarity(), task);

            // 发送查重完成通知给学生
            try {
                String riskLevel = checkResult.getSimilarity().compareTo(defaultThreshold) <= 0 ? "低风险" : "高风险";
                notificationService.sendSimilarityCheckResultNotification(
                        paperInfo.getStudentId(), paperInfo.getId(), paperInfo.getPaperTitle(),
                        checkResult.getSimilarity().doubleValue(), riskLevel);
            } catch (Exception notifyEx) {
                log.warn("发送查重结果通知失败: paperId={}", paperInfo.getId(), notifyEx);
            }
            
        } catch (Exception e) {
            log.error("查重任务执行失败：taskId={}", taskId, e);
            task.setFailReason(e.getMessage().length() > 500 ? 
                             e.getMessage().substring(0, 500) : e.getMessage());
            updateTaskStatus(taskId, CheckTaskStatusEnum.FAILURE, e.getMessage());
            
            // 恢复论文状态（通过状态机服务）
            PaperInfo paperInfo = paperInfoMapper.selectById(task.getPaperId());
            if (paperInfo != null) {
                paperStatusTransitionService.transitionSilently(
                        task.getPaperId(), PaperStatusEnum.ASSIGNED,
                        paperInfo.getStudentId(), "查重任务失败，恢复状态");
                paperInfo.setCheckTime(null);
                paperInfoMapper.updateById(paperInfo);
            }
        }
    }
    
    // ------------------------------ 私有辅助方法 ------------------------------
    
    private List<CheckEngineTypeEnum> parseEngineTypes(List<String> engineCodes) {
        if (engineCodes == null || engineCodes.isEmpty()) {
            return Arrays.asList(CheckEngineTypeEnum.LOCAL);
        }
        
        return engineCodes.stream()
                .map(CheckEngineTypeEnum::fromCode)
                .collect(Collectors.toList());
    }
    
    private String generateTaskNo() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seqPrefix = "CHECK" + datePrefix;
        
        LambdaQueryWrapper<CheckTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(CheckTask::getTaskNo, seqPrefix)
               .eq(CheckTask::getIsDeleted, 0)
               .select(CheckTask::getTaskNo)
               .orderByDesc(CheckTask::getTaskNo);
        Page<CheckTask> taskNoPage = new Page<>(0, 1);
        CheckTask lastTask = page(taskNoPage, wrapper).getRecords()
                .stream().findFirst().orElse(null);
        int seq = 1;
        if (lastTask != null) {
            String lastNo = lastTask.getTaskNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 3)) + 1;
        }
        
        return seqPrefix + String.format("%03d", seq);
    }
    
    private String extractTextFromFile(String filePath) throws IOException, TikaException {
        String fullPath = basePath + filePath;
        File file = new File(fullPath);
        if (!file.exists()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不存在：" + filePath);
        }
        
        try (InputStream inputStream = new FileInputStream(file)) {
            return tika.parseToString(inputStream);
        }
    }
    
    private CheckReport generateCheckReport(CheckTask task, CheckResult checkResult) {
        String reportNo = "REPORT" + task.getTaskNo().substring(5);
        String reportPath = "/reports/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                          + "/" + reportNo + ".pdf";
        
        CheckReport report = new CheckReport();
        report.setTaskId(task.getId());
        report.setReportNo(reportNo);
        report.setReportPath(reportPath);
        report.setReportType("pdf");
        UserBusinessInfoUtils.setAuditField(report, true);
        checkReportMapper.insert(report);
        
        // 生成PDF报告
        generatePdfReport(task, report, checkResult);
        
        return report;
    }
    
    private void generatePdfReport(CheckTask task, CheckReport report, CheckResult checkResult) {
        try {
            ReportPreviewDTO previewDTO = buildReportPreviewDTO(task, report, checkResult);
            pdfReportGenerator.generatePdfToFile(previewDTO, report.getReportPath());
        } catch (Exception e) {
            log.error("PDF报告生成失败：taskId={}", task.getId(), e);
        }
    }
    
    private ReportPreviewDTO buildReportPreviewDTO(CheckTask task, CheckReport report, 
                                                 CheckResult checkResult) {
        ReportPreviewDTO dto = new ReportPreviewDTO();
        ReportPreviewDTO.ReportBaseInfoDTO baseInfo = new ReportPreviewDTO.ReportBaseInfoDTO();
        
        baseInfo.setTaskId(task.getId());
        baseInfo.setTaskNo(task.getTaskNo());
        baseInfo.setReportId(report.getId());
        baseInfo.setReportNo(report.getReportNo());
        baseInfo.setSimilarityRate(checkResult.getSimilarity());
        baseInfo.setCheckTime(LocalDateTime.now());
        
        PaperInfo paperInfo = paperInfoMapper.selectById(task.getPaperId());
        if (paperInfo != null) {
            baseInfo.setPaperTitle(paperInfo.getPaperTitle());
            baseInfo.setAuthor(paperInfo.getAuthor());
        }
        
        dto.setBaseInfo(baseInfo);
        return dto;
    }
    
    private void updatePaperStatus(PaperInfo paperInfo, BigDecimal similarity, CheckTask checkTask) {
        // 如果论文已处于终态（已通过/已驳回），不再更新状态，避免覆盖教师审核结果
        PaperStatusEnum currentStatus = PaperStatusEnum.fromCode(paperInfo.getPaperStatus());
        if (currentStatus != null && currentStatus.isTerminalStatus()) {
            log.info("论文已处于终态（{}），跳过状态更新: paperId={}",
                    currentStatus.getDescription(), paperInfo.getId());
            return;
        }

        // 从查重任务关联的规则中获取阈值，如果没有则使用默认阈值
        BigDecimal threshold = defaultThreshold;
        if (checkTask.getCheckRuleId() != null) {
            CheckRule rule = checkRuleService.getById(checkTask.getCheckRuleId());
            if (rule != null && rule.getPassThreshold() != null) {
                threshold = rule.getPassThreshold();
            }
        }

        PaperStatusEnum targetStatus = similarity.compareTo(threshold) <= 0 ?
                PaperStatusEnum.AUDITING :
                PaperStatusEnum.REJECTED;

        String reason = similarity.compareTo(threshold) <= 0 ?
                "查重完成，相似度" + similarity + "%，低于阈值" + threshold + "%，进入待审核" :
                "查重完成，相似度" + similarity + "%，超过阈值" + threshold + "%，审核不通过";

        Long operatorId = paperInfo.getStudentId();
        paperStatusTransitionService.transitionSilently(
                paperInfo.getId(), targetStatus, operatorId, reason);
    }
    
    private CheckResultVO buildCheckResultVO(CheckTask task) {
        CheckResultVO vo = new CheckResultVO();
        vo.setTaskId(task.getId());
        vo.setPaperId(task.getPaperId());
        vo.setCheckStatus(task.getCheckStatus());
        vo.setCreateTime(task.getCreateTime());
        return vo;
    }
    
    private CheckTaskResultDTO convertToDTO(CheckTask task) {
        CheckTaskResultDTO dto = new CheckTaskResultDTO();
        dto.setTaskId(task.getId());
        dto.setTaskNo(task.getTaskNo());
        dto.setPaperId(task.getPaperId());
        dto.setCheckStatus(task.getCheckStatus());
        dto.setCheckRate(task.getCheckRate() != null ? task.getCheckRate().doubleValue() : null);
        dto.setStartTime(task.getStartTime());
        dto.setEndTime(task.getEndTime());
        dto.setFailReason(task.getFailReason());
        
        // 补充论文信息
        PaperInfo paperInfo = paperInfoMapper.selectById(task.getPaperId());
        if (paperInfo != null) {
            dto.setPaperTitle(paperInfo.getPaperTitle());
        }
        
        return dto;
    }
    
    private void applyPermissionFilter(LambdaQueryWrapper<CheckTask> wrapper) {
        if (UserBusinessInfoUtils.isStudent()) {
            Long studentId = UserBusinessInfoUtils.getCurrentUserId();
            List<Long> paperIds = paperInfoMapper.selectList(
                new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getStudentId, studentId)
                    .eq(PaperInfo::getIsDeleted, 0)
                    .select(PaperInfo::getId)
            ).stream().map(PaperInfo::getId).collect(Collectors.toList());
            if (!paperIds.isEmpty()) {
                wrapper.in(CheckTask::getPaperId, paperIds);
            } else {
                wrapper.eq(CheckTask::getPaperId, -1L);
            }
        } else if (UserBusinessInfoUtils.isTeacher()) {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            List<Long> paperIds = paperInfoMapper.selectList(
                new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getTeacherId, teacherId)
                    .eq(PaperInfo::getIsDeleted, 0)
                    .select(PaperInfo::getId)
            ).stream().map(PaperInfo::getId).collect(Collectors.toList());
            if (!paperIds.isEmpty()) {
                wrapper.in(CheckTask::getPaperId, paperIds);
            } else {
                wrapper.eq(CheckTask::getPaperId, -1L);
            }
        }
        // 管理员无过滤
    }
    
    private boolean hasTaskAccess(CheckTask task) {
        if (UserBusinessInfoUtils.isAdmin()) return true;
        
        PaperInfo paperInfo = paperInfoMapper.selectById(task.getPaperId());
        if (paperInfo == null) return false;
        
        Long currentUserId = UserBusinessInfoUtils.getCurrentUserId();
        if (UserBusinessInfoUtils.isStudent()) {
            return paperInfo.getStudentId().equals(currentUserId);
        } else if (UserBusinessInfoUtils.isTeacher()) {
            return paperInfo.getTeacherId().equals(currentUserId);
        }
        
        return false;
    }
}