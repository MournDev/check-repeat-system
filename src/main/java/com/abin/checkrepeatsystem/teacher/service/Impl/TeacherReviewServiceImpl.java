package com.abin.checkrepeatsystem.teacher.service.Impl;

import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.enums.PaperStatusEnum;
import com.abin.checkrepeatsystem.common.enums.ReviewStatusEnum;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.enums.UserTypeEnum;
import com.abin.checkrepeatsystem.common.service.PaperStatusTransitionService;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.*;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.teacher.dto.*;
import com.abin.checkrepeatsystem.teacher.mapper.ReviewRecordMapper;
import com.abin.checkrepeatsystem.teacher.service.TeacherReviewService;
import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.common.utils.ReviewAttachUtils;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.monitor.service.ApplicationMonitorService;
import com.abin.checkrepeatsystem.user.mapper.PaperStatusLogMapper;
import com.abin.checkrepeatsystem.user.service.StudentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.abin.checkrepeatsystem.common.utils.FileMimeTypeUtils;

/**
 * 教师审核服务实现类
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TeacherReviewServiceImpl extends ServiceImpl<ReviewRecordMapper, ReviewRecord> implements TeacherReviewService {

    private final PaperInfoMapper paperInfoMapper;

    private final CheckTaskMapper checkTaskMapper;

    private final CheckReportMapper checkReportMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    private final SysUserMapper sysUserMapper;

    private final ReviewAttachUtils reviewAttachUtils;

    private final PaperStatusLogMapper paperStatusLogMapper;

    private final StudentInfoService studentInfoService;

    private final ApplicationMonitorService monitorService;

    private final PaperStatusTransitionService paperStatusTransitionService;

    @Value("${app.host:localhost}")
    private String serverHost;

    private final FileService fileService;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${kkfileview.base-url:}")
    private String kkfileviewUrl;

    @Value("${server.servlet.context-path:}")
    private String appContext;

    // 批量审核最大数量（从配置文件获取）
    @Value("${review.batch.max-count}")
    private Integer batchMaxCount;

    @Override
    public Result<Page<ReviewResultDTO>> getPendingReviewList(ReviewQueryReq queryReq) {
        Long currentTeacherId = UserBusinessInfoUtils.getCurrentUserId();
        Integer currentPage = queryReq.getCurrentPage();
        Integer pageSize = queryReq.getPageSize();

        // 1. 构建分页查询条件
        Page<PaperInfo> paperPage = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
        // 仅查询自己指导的、待审核的、未删除的论文
        paperWrapper.eq(PaperInfo::getTeacherId, currentTeacherId)
                .eq(PaperInfo::getPaperStatus, PaperStatusEnum.AUDITING.getValue()) // 待审核状态
                .eq(PaperInfo::getIsDeleted, 0);

        // 2. 模糊查询条件（学生姓名、论文标题）
        if (org.springframework.util.StringUtils.hasText(queryReq.getStudentName())) {
            List<Long> matchedStudentIds = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                    .like(SysUser::getRealName, queryReq.getStudentName())
                    .eq(SysUser::getIsDeleted, 0)
                    .select(SysUser::getId)
            ).stream().map(SysUser::getId).collect(Collectors.toList());
            if (!matchedStudentIds.isEmpty()) {
                paperWrapper.in(PaperInfo::getStudentId, matchedStudentIds);
            } else {
                paperWrapper.eq(PaperInfo::getStudentId, -1L);
            }
        }
        if (org.springframework.util.StringUtils.hasText(queryReq.getPaperTitle())) {
            paperWrapper.like(PaperInfo::getPaperTitle, queryReq.getPaperTitle());
        }

        // 3. 执行分页查询
        IPage<PaperInfo> paperIPage = paperInfoMapper.selectPage(paperPage, paperWrapper);
        List<PaperInfo> paperList = paperIPage.getRecords();
        if (CollectionUtils.isEmpty(paperList)) {
            return Result.success("待审核论文列表查询成功", new Page<>());
        }

        // 4. 转换为DTO（批量查询关联数据，减少SQL次数）
        List<ReviewResultDTO> resultDTOList = convertToReviewResultDTOList(paperList);

        // 5. 构建分页结果
        Page<ReviewResultDTO> pageInfo = new Page<>();
        pageInfo.setRecords(resultDTOList);
        pageInfo.setCurrent(paperIPage.getCurrent());
        pageInfo.setSize(paperIPage.getSize());
        pageInfo.setTotal(paperIPage.getTotal());
        pageInfo.setPages(paperIPage.getPages());
        return Result.success("待审核论文列表查询成功", pageInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Timed(value = "review.do", description = "执行审核操作耗时")
    public Result<Map<String, Object>> doReview(ReviewOperateReq operateReq) {
        Long currentTeacherId = UserBusinessInfoUtils.getCurrentUserId();
        List<Long> paperIds = operateReq.getPaperIds();
        String reviewStatus = operateReq.getReviewStatus();
        String reviewOpinion = operateReq.getReviewOpinion();
        MultipartFile reviewAttach = operateReq.getReviewAttach();
        String suggestedModifications = operateReq.getSuggestedModifications(); // 获取建议修改点

        // 校验批量数量
        if (paperIds.size() > batchMaxCount) {
            return Result.error(ResultCode.PARAM_ERROR,
                    String.format("单次审核最多选择%s篇论文，当前选择%s篇", batchMaxCount, paperIds.size()));
        }
        // 校验审核状态
        if (!ReviewStatusEnum.isValid(reviewStatus)) {
            return Result.error(ResultCode.PARAM_ERROR, "审核状态无效（仅支持通过、不通过、需要修改）");
        }
        // 清洗审核意见（防XSS）
        String cleanedOpinion = reviewAttachUtils.cleanReviewOpinion(reviewOpinion);

        // 2. 上传审核附件（若有）
        ReviewAttachUtils.AttachInfo attachInfo = null;
        if (reviewAttach != null && !reviewAttach.isEmpty()) {
            attachInfo = reviewAttachUtils.uploadReviewAttach(reviewAttach, currentTeacherId);
        }

        // 3. 批量处理审核（统计成功/失败数量）
        int successCount = 0;
        List<String> failReasons = new ArrayList<>();
        for (Long paperId : paperIds) {
            try {
                // 3.1 校验论文合法性与状态
                PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
                if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
                    failReasons.add(String.format("论文ID：%s，原因：论文不存在或已删除", paperId));
                    continue;
                }
                // 校验是否为当前教师指导的论文，管理员可以审核任何论文
                String currentRole = UserBusinessInfoUtils.getCurrentUserRoleCode();
                if (!UserTypeEnum.ROLE_ADMIN.equals(currentRole) && !paperInfo.getTeacherId().equals(currentTeacherId)) {
                    failReasons.add(String.format("论文ID：%s，原因：无权限审核他人指导的论文", paperId));
                    continue;
                }
                // 校验论文状态（仅待审核状态可审核）
                if (!paperInfo.getPaperStatus().equals(PaperStatusEnum.AUDITING.getValue())) {
                    failReasons.add(String.format("论文ID：%s，原因：当前状态（%s）不允许审核（仅待审核可审核）",
                            paperId, paperInfo.getPaperStatus()));
                    continue;
                }

                // 3.2 查询关联的查重任务（取最新的成功任务）
                Page<CheckTask> checkTaskPage = new Page<>(0, 1);
                CheckTask checkTask = checkTaskMapper.selectPage(checkTaskPage,
                        new LambdaQueryWrapper<CheckTask>()
                                .eq(CheckTask::getPaperId, paperId)
                                .eq(CheckTask::getCheckStatus, DictConstants.CheckStatus.COMPLETED)
                                .eq(CheckTask::getIsDeleted, 0)
                                .orderByDesc(CheckTask::getCreateTime)
                ).getRecords().stream().findFirst().orElse(null);
                if (checkTask == null) {
                    failReasons.add(String.format("论文ID：%s，原因：未找到有效的查重任务结果", paperId));
                    continue;
                }

                // 3.3 创建审核记录
                ReviewRecord reviewRecord = new ReviewRecord();
                reviewRecord.setPaperId(paperId);
                reviewRecord.setTaskId(checkTask.getId());
                reviewRecord.setTeacherId(currentTeacherId);
                reviewRecord.setReviewStatus(reviewStatus);
                reviewRecord.setReviewOpinion(cleanedOpinion);
                reviewRecord.setSuggestedModifications(suggestedModifications);
                // 填充附件信息（若有）
                if (attachInfo != null) {
                    reviewRecord.setReviewAttach(attachInfo.getAttachPath());
                }
                reviewRecord.setReviewTime(LocalDateTime.now());
                UserBusinessInfoUtils.setAuditField(reviewRecord, true); // 填充审计字段
                save(reviewRecord);

                // 3.4 更新论文状态（通过状态机服务）
                PaperStatusEnum targetStatus;
                String transitionReason;
                if (reviewStatus.equals(ReviewStatusEnum.PASS.getValue())) {
                    targetStatus = PaperStatusEnum.COMPLETED;
                    transitionReason = "导师审核通过";
                } else if (reviewStatus.equals(ReviewStatusEnum.REVISION_NEEDED.getValue())) {
                    targetStatus = PaperStatusEnum.REVISION_NEEDED;
                    transitionReason = "导师要求修改: " + cleanedOpinion;
                } else if (reviewStatus.equals(ReviewStatusEnum.REJECT.getValue())) {
                    targetStatus = PaperStatusEnum.REJECTED;
                    transitionReason = "导师审核不通过: " + cleanedOpinion;
                } else {
                    failReasons.add(String.format("论文ID：%s，原因：未知的审核状态", paperId));
                    continue;
                }
                paperStatusTransitionService.transition(paperId, targetStatus, currentTeacherId, transitionReason);

                successCount++;
                log.info("论文审核成功：论文 ID={}，审核结果={}，审核记录 ID={}",
                        paperId, ReviewStatusEnum.getByValue(reviewStatus).getDescription(), reviewRecord.getId());

                // 3.5 状态变更日志和通知已由 PaperStatusTransitionService 自动处理，无需重复发送

            } catch (Exception e) {
                log.error("论文审核失败（论文ID：{}）：", paperId, e);
                failReasons.add(String.format("论文ID：%s，原因：%s", paperId, e.getMessage()));
            }
        }

        // 4. 构建返回结果
        Map<String, Object> resultMap = new HashMap<>(4);
        resultMap.put("totalCount", paperIds.size());
        resultMap.put("successCount", successCount);
        resultMap.put("failCount", paperIds.size() - successCount);
        resultMap.put("failReasons", failReasons);

        // 记录审核业务指标
        if (successCount > 0) {
            String resultLabel = ReviewStatusEnum.PASS.getValue().equals(reviewStatus) ? "pass" : "reject";
            monitorService.recordBusinessEvent("paper_review", resultLabel, successCount);
        }
        int failCount = paperIds.size() - successCount;
        if (failCount > 0) {
            monitorService.recordBusinessEvent("paper_review", "failure", failCount);
        }

        return Result.success("审核操作完成", resultMap);
    }

    @Override
    public Result<Page<ReviewResultDTO>> getReviewedList(ReviewQueryReq queryReq) {
        Long currentTeacherId = UserBusinessInfoUtils.getCurrentUserId();
        Integer currentPage = queryReq.getCurrentPage();
        Integer pageSize = queryReq.getPageSize();

        // 1. 先查询有审核记录的论文ID
        List<Long> reviewedPaperIds = reviewRecordMapper.selectList(
                new LambdaQueryWrapper<ReviewRecord>()
                        .eq(ReviewRecord::getTeacherId, currentTeacherId)
                        .eq(ReviewRecord::getIsDeleted, 0)
                        .select(ReviewRecord::getPaperId)
        ).stream()
                .map(ReviewRecord::getPaperId)
                .distinct()
                .collect(Collectors.toList());

        // 如果没有审核记录，直接返回空列表
        if (CollectionUtils.isEmpty(reviewedPaperIds)) {
            Page<ReviewResultDTO> emptyPage = new Page<>();
            emptyPage.setRecords(new ArrayList<>());
            emptyPage.setCurrent(currentPage);
            emptyPage.setSize(pageSize);
            emptyPage.setTotal(0);
            return Result.success("已审核论文列表查询成功", emptyPage);
        }

        // 2. 构建分页查询条件（已审核状态且存在审核记录）
        Page<PaperInfo> paperPage = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
        paperWrapper.in(PaperInfo::getId, reviewedPaperIds)
                .eq(PaperInfo::getTeacherId, currentTeacherId)
                .eq(PaperInfo::getIsDeleted, 0);

        // 审核状态筛选
        if (StringUtils.hasText(queryReq.getStatus())) {
            paperWrapper.eq(PaperInfo::getPaperStatus, queryReq.getStatus());
        } else {
            paperWrapper.in(PaperInfo::getPaperStatus,
                    PaperStatusEnum.COMPLETED.getValue(),
                    PaperStatusEnum.REJECTED.getValue(),
                    PaperStatusEnum.REVISION_NEEDED.getValue());
        }

        // 审核时间范围筛选
        if (StringUtils.hasText(queryReq.getStartTime())) {
            paperWrapper.ge(PaperInfo::getCheckTime, queryReq.getStartTime());
        }
        if (StringUtils.hasText(queryReq.getEndTime())) {
            paperWrapper.le(PaperInfo::getCheckTime, queryReq.getEndTime());
        }

        // 相似度范围筛选
        if (StringUtils.hasText(queryReq.getSimilarityRange())) {
            String[] range = queryReq.getSimilarityRange().split("-");
            if (range.length == 2) {
                paperWrapper.ge(PaperInfo::getSimilarityRate, new BigDecimal(range[0]))
                           .le(PaperInfo::getSimilarityRate, new BigDecimal(range[1]));
            }
        }

        // 2. 模糊查询条件（与待审核列表一致）
        if (StringUtils.hasText(queryReq.getStudentName())) {
            List<SysUser> matchedUsers = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, queryReq.getStudentName())
                            .eq(SysUser::getIsDeleted, 0)
                            .select(SysUser::getId)
            );
            if (!matchedUsers.isEmpty()) {
                List<Long> studentIds = matchedUsers.stream()
                        .map(SysUser::getId)
                        .collect(Collectors.toList());
                paperWrapper.in(PaperInfo::getStudentId, studentIds);
            } else {
                paperWrapper.eq(PaperInfo::getStudentId, -1);
            }
        }
        if (StringUtils.hasText(queryReq.getPaperTitle())) {
            paperWrapper.like(PaperInfo::getPaperTitle, queryReq.getPaperTitle());
        }

        // 3. 执行分页查询
        IPage<PaperInfo> paperIPage = paperInfoMapper.selectPage(paperPage, paperWrapper);
        List<PaperInfo> paperList = paperIPage.getRecords();
        List<ReviewResultDTO> resultDTOList = CollectionUtils.isEmpty(paperList)
                ? new ArrayList<>()
                : convertToReviewResultDTOList(paperList);

        // 4. 构建分页结果
        Page<ReviewResultDTO> pageInfo = new Page<>();
        pageInfo.setRecords(resultDTOList);
        pageInfo.setCurrent(paperIPage.getCurrent());
        pageInfo.setSize(paperIPage.getSize());
        pageInfo.setTotal(paperIPage.getTotal());
        pageInfo.setPages(paperIPage.getPages());

        return Result.success("已审核论文列表查询成功", pageInfo);
    }


    @Override
    public Result<ReviewResultDTO> getReviewDetail(@RequestParam("paperId") Long paperId) {
        Long currentTeacherId = UserBusinessInfoUtils.getCurrentUserId();

        // 1. 校验论文合法性与权限
        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "论文不存在或已删除");
        }
        if (!paperInfo.getTeacherId().equals(currentTeacherId) && !UserBusinessInfoUtils.isAdmin()) {
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限查看他人指导论文的审核详情");
        }

        // 2. 转换为DTO
        List<ReviewResultDTO> dtoList = convertToReviewResultDTOList(Collections.singletonList(paperInfo));
        if (CollectionUtils.isEmpty(dtoList)) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "未找到该论文的审核记录");
        }

        return Result.success("审核详情查询成功", dtoList.get(0));
    }

    @Override
    public void downloadReviewAttach(@RequestParam("attachPath") String attachPath, HttpServletResponse response) {
        // 1. 校验附件路径合法性
        if (attachPath == null || attachPath.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_EMPTY, "附件路径不能为空");
        }
        // 校验路径是否为审核附件目录（防路径遍历攻击）
        if (!attachPath.startsWith("/data/review-attach/")) {
            throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR, "附件路径不合法");
        }

        // 2. 读取附件文件
        java.nio.file.Path filePath = java.nio.file.Paths.get(attachPath);
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "审核附件不存在或已删除");
        }

        // 3. 设置响应头（触发下载）
        try {
            // 获取附件原文件名（从审核记录中查询，此处简化为从路径提取）
            String fileName = "审核附件." + attachPath.substring(attachPath.lastIndexOf(".") + 1);
            // 处理中文文件名编码
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());

            // 设置响应头
            response.setContentType(Files.probeContentType(filePath));
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setHeader("Content-Length", String.valueOf(Files.size(filePath)));

            // 4. 输出文件流
            try (InputStream in = Files.newInputStream(filePath);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
            log.info("审核附件下载成功：{}", attachPath);

        } catch (IOException e) {
            log.error("审核附件下载失败（路径：{}）：", attachPath, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "附件下载异常，请重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> reInitiateReview(@RequestParam("paperId") Long paperId) {
        Long currentTeacherId = UserBusinessInfoUtils.getCurrentUserId();

        // 1. 校验论文合法性与状态
        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "论文不存在或已删除");
        }
        // 仅审核不通过的论文可重新发起审核
        if (!"rejected".equals(paperInfo.getPaperStatus())) {
            String currentStatusDesc = getPaperStatusDesc(paperInfo.getPaperStatus());
            return Result.error(ResultCode.PERMISSION_NOT_STATUS,
                    String.format("仅审核不通过（状态）的论文可重新发起审核，当前状态：%s", currentStatusDesc));
        }
        // 校验权限（仅指导教师或管理员可操作）
        if (!paperInfo.getTeacherId().equals(currentTeacherId) && !UserBusinessInfoUtils.isAdmin()) {
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限重新发起他人指导论文的审核");
        }

        // 2. 更新论文状态为"待审核"（通过状态机服务）
        paperStatusTransitionService.transition(
                paperId, PaperStatusEnum.AUDITING, currentTeacherId, "教师重新发起审核");

        log.info("论文重新发起审核成功：论文ID={}，指导教师ID={}", paperId, currentTeacherId);
        return Result.success("重新发起审核成功，论文已进入待审核队列");
    }

    @Override
    public Result<PaperContentDTO> getPaperContent(Long teacherId, Long paperId) {
        try {
            log.info("教师{}获取论文内容: paperId={}", teacherId, paperId);

            // 1. 验证论文权限
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                return Result.error(ResultCode.PARAM_ERROR, "论文不存在");
            }

            if (!paperInfo.getTeacherId().equals(teacherId)) {
                return Result.error(ResultCode.PARAM_ERROR, "无权限访问此论文");
            }

            // 2. 构建返回数据
            PaperContentDTO dto = new PaperContentDTO();
            dto.setPaperId(paperId);
            dto.setPaperTitle(paperInfo.getPaperTitle());
            dto.setStudentId(paperInfo.getStudentId());

            // 获取学生信息
            SysUser student = sysUserMapper.selectById(paperInfo.getStudentId());
            if (student != null) {
                dto.setStudentName(student.getRealName());
            }

            // 获取文件信息
            if (paperInfo.getFileId() != null) {
                FileInfo fileInfo = fileService.getById(paperInfo.getFileId());
                if (fileInfo != null) {
                    dto.setFileName(fileInfo.getOriginalFilename());
                    dto.setFileSizeDesc(fileInfo.getFileSizeDesc());
                    dto.setWordCount(fileInfo.getWordCount());
                    dto.setPageCount(fileInfo.getPageCount());
                }
            }

            return Result.success("获取论文内容成功", dto);

        } catch (Exception e) {
            log.error("获取论文内容失败: teacherId={}, paperId={}", teacherId, paperId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取论文内容失败: " + e.getMessage());
        }
    }

    @Override
    public Result<PaperPreviewUrlDTO> getPaperPreviewUrl(Long teacherId, Long paperId) {
        try {
            log.info("教师{}获取论文预览URL: paperId={}", teacherId, paperId);

            // 1. 验证论文权限
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                return Result.error(ResultCode.PARAM_ERROR, "论文不存在");
            }

            if (!paperInfo.getTeacherId().equals(teacherId)) {
                return Result.error(ResultCode.PARAM_ERROR, "无权限访问此论文");
            }

            // 2. 检查文件是否存在
            if (paperInfo.getFileId() == null) {
                return Result.error(ResultCode.PARAM_ERROR, "论文文件不存在");
            }

            // 3. 构建预览URL
            PaperPreviewUrlDTO dto = new PaperPreviewUrlDTO();
            dto.setPaperId(paperId);

            FileInfo fileInfo = fileService.getById(paperInfo.getFileId());
            if (fileInfo != null) {
                dto.setFileName(fileInfo.getOriginalFilename());
                dto.setFileType(FileMimeTypeUtils.getFileExtension(fileInfo.getOriginalFilename()));

                // 构建文件访问URL
                String fileUrl = String.format("http://%s:%s%s/api/file/download/%s/%s",
                        serverHost,
                        serverPort,
                        appContext,
                        paperInfo.getFileId(),
                        URLEncoder.encode(fileInfo.getOriginalFilename(), StandardCharsets.UTF_8));

                // Base64编码URL
                String encodedUrl = Base64.getUrlEncoder().encodeToString(fileUrl.getBytes(StandardCharsets.UTF_8));

                // 构建KKFileView预览URL
                String previewUrl = String.format("%s/onlinePreview?url=%s", kkfileviewUrl, encodedUrl);

                dto.setPreviewUrl(previewUrl);
                dto.setKkFileViewServer(kkfileviewUrl);
            }

            log.info("成功生成论文预览URL: paperId={}, previewUrl={}", paperId, dto.getPreviewUrl());
            return Result.success("获取预览URL成功", dto);

        } catch (Exception e) {
            log.error("获取论文预览URL失败: teacherId={}, paperId={}", teacherId, paperId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取论文预览URL失败: " + e.getMessage());
        }
    }


    // ------------------------------ 私有辅助方法 ------------------------------

    /**
     * 获取论文状态描述
     */
    private String getPaperStatusDesc(String paperStatus) {
        if (paperStatus == null) {
            return "未知状态";
        }
        try {
            PaperStatusEnum statusEnum = PaperStatusEnum.fromCode(paperStatus);
            return statusEnum != null ? statusEnum.getDescription() : paperStatus;
        } catch (Exception e) {
            return paperStatus;
        }
    }

    /**
     * 记录论文状态变更日志
     */
    private void recordPaperStatusLog(Long paperId, String oldStatus, String newStatus, String reason) {
        try {
            com.abin.checkrepeatsystem.pojo.entity.PaperStatusLog statusLog =
                    new com.abin.checkrepeatsystem.pojo.entity.PaperStatusLog();
            statusLog.setPaperId(paperId);
            statusLog.setOldStatus(oldStatus);
            statusLog.setNewStatus(newStatus);
            statusLog.setStatusReason(reason);
            UserBusinessInfoUtils.setAuditField(statusLog, true);
            paperStatusLogMapper.insert(statusLog);
            log.info("论文状态日志记录成功 - 论文 ID: {}, {} -> {}, 原因：{}", paperId, oldStatus, newStatus, reason);
        } catch (Exception e) {
            log.error("论文状态日志记录失败 - 论文 ID: {}", paperId, e);
            // 日志记录失败不影响主流程，仅警告
        }
    }

    /**
     * 批量转换 PaperInfo 列表为 ReviewResultDTO 列表（减少 SQL 查询次数）
     */
    private List<ReviewResultDTO> convertToReviewResultDTOList(List<PaperInfo> paperList) {
        // 1. 批量查询关联数据（学生、任务、报告、审核记录）
        // 1.1 提取论文ID、学生ID列表
        List<Long> paperIds = paperList.stream().map(PaperInfo::getId).collect(Collectors.toList());
        List<Long> studentIds = paperList.stream().map(PaperInfo::getStudentId).distinct().collect(Collectors.toList());

        // 1.2 批量查询学生信息（Map<学生ID, 学生实体>）
        Map<Long, SysUser> studentMap = sysUserMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, student -> student));

        // 1.3 批量查询查重任务（Map<论文ID, 最新成功任务>）
        Map<Long, CheckTask> taskMap = new HashMap<>();
        List<CheckTask> taskList = checkTaskMapper.selectList(
                new LambdaQueryWrapper<CheckTask>()
                        .in(CheckTask::getPaperId, paperIds)
                        .eq(CheckTask::getCheckStatus, DictConstants.CheckStatus.COMPLETED) // 仅成功任务
                        .eq(CheckTask::getIsDeleted, 0)
        );
        // 按论文ID分组，取最新任务
        Map<Long, List<CheckTask>> taskGroupByPaperId = taskList.stream()
                .collect(Collectors.groupingBy(CheckTask::getPaperId));
        for (Map.Entry<Long, List<CheckTask>> entry : taskGroupByPaperId.entrySet()) {
            // 按创建时间倒序，取第一个
            CheckTask latestTask = entry.getValue().stream()
                    .sorted((t1, t2) -> t2.getCreateTime().compareTo(t1.getCreateTime()))
                    .findFirst()
                    .orElse(null);
            taskMap.put(entry.getKey(), latestTask);
        }

        // 1.4 批量查询审核记录（Map<论文ID, 最新审核记录>）
        Map<Long, ReviewRecord> reviewMap = new HashMap<>();
        List<ReviewRecord> reviewList = list(
                new LambdaQueryWrapper<ReviewRecord>()
                        .in(ReviewRecord::getPaperId, paperIds)
                        .eq(ReviewRecord::getIsDeleted, 0)
        );
        Map<Long, List<ReviewRecord>> reviewGroupByPaperId = reviewList.stream()
                .collect(Collectors.groupingBy(ReviewRecord::getPaperId));
        for (Map.Entry<Long, List<ReviewRecord>> entry : reviewGroupByPaperId.entrySet()) {
            ReviewRecord latestReview = entry.getValue().stream()
                    .sorted((r1, r2) -> r2.getReviewTime().compareTo(r1.getReviewTime()))
                    .findFirst()
                    .orElse(null);
            reviewMap.put(entry.getKey(), latestReview);
        }

        // 1.5 批量查询学生详细信息（避免循环内调用 getByUserId）
        Map<Long, StudentInfo> studentInfoMap = studentIds.isEmpty() ? new HashMap<>() :
                studentInfoService.listByUserIds(studentIds).stream()
                        .collect(Collectors.toMap(StudentInfo::getUserId, s -> s, (a, b) -> a));

        // 1.6 批量查询报告信息（避免循环内调用 checkReportMapper.selectById）
        List<Long> reportIds = taskMap.values().stream()
                .filter(t -> t != null && t.getReportId() != null)
                .map(CheckTask::getReportId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, CheckReport> reportMap = reportIds.isEmpty() ? new HashMap<>() :
                checkReportMapper.selectBatchIds(reportIds).stream()
                        .collect(Collectors.toMap(CheckReport::getId, r -> r, (a, b) -> a));

        // 1.7 批量查询审核教师信息（避免循环内调用 sysUserMapper.selectById）
        List<Long> reviewerIds = reviewMap.values().stream()
                .filter(r -> r != null && r.getTeacherId() != null)
                .map(ReviewRecord::getTeacherId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SysUser> reviewerMap = reviewerIds.isEmpty() ? new HashMap<>() :
                sysUserMapper.selectBatchIds(reviewerIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        // 2. 转换为DTO
        return paperList.stream().map(paper -> {
            ReviewResultDTO dto = new ReviewResultDTO();

            // 2.1 填充论文基础信息
            ReviewResultDTO.PaperBaseInfoDTO paperBaseInfo = new ReviewResultDTO.PaperBaseInfoDTO();
            paperBaseInfo.setPaperId(paper.getId());
            paperBaseInfo.setFileId(paper.getFileId());
            paperBaseInfo.setPaperTitle(paper.getPaperTitle());
            SysUser student = studentMap.get(paper.getStudentId());
            if (student != null) {
                paperBaseInfo.setStudentName(student.getRealName());
                paperBaseInfo.setStudentNo(student.getUsername()); // 学生学号=username
                paperBaseInfo.setEmail(student.getEmail());

                // 从StudentInfo表获取学生的学院信息
                StudentInfo studentInfo = studentInfoMap.get(student.getId());
                if (studentInfo != null) {
                    paperBaseInfo.setCollege(studentInfo.getCollegeName());
                    paperBaseInfo.setMajor(studentInfo.getMajor());
                }
            }

            paperBaseInfo.setSubmitTime(paper.getSubmitTime());
            paperBaseInfo.setPaperStatus(paper.getPaperStatus());
            dto.setPaperBaseInfo(paperBaseInfo);

            // 2.1.1 填充字数和页数信息
            dto.setWordCount(paper.getWordCount());
            Integer pageCount = paper.getPageCount();
            dto.setPageCount(pageCount != null && pageCount > 0 ? pageCount :
                (paper.getWordCount() != null ? paper.getWordCount() / 500 : 0));

            // 2.2 填充查重任务信息
            CheckTask task = taskMap.get(paper.getId());
            if (task != null) {
                ReviewResultDTO.CheckTaskBaseDTO taskBaseInfo = new ReviewResultDTO.CheckTaskBaseDTO();
                taskBaseInfo.setTaskId(task.getId());
                taskBaseInfo.setTaskNo(task.getTaskNo());
                taskBaseInfo.setCheckRate(task.getCheckRate() != null ? task.getCheckRate().doubleValue() : 0.0);
                taskBaseInfo.setCheckEndTime(task.getEndTime());
                // 查询报告编号
                CheckReport report = reportMap.get(task.getReportId());
                if (report != null) {
                    taskBaseInfo.setReportNo(report.getReportNo());
                }
                dto.setTaskBaseInfo(taskBaseInfo);
            }

            // 2.3 填充审核操作信息
            ReviewRecord review = reviewMap.get(paper.getId());
            if (review != null) {
                ReviewResultDTO.ReviewOperateInfoDTO reviewOperateInfo = new ReviewResultDTO.ReviewOperateInfoDTO();
                reviewOperateInfo.setReviewStatus(review.getReviewStatus());
                reviewOperateInfo.setReviewStatusDesc(review.getReviewStatus());
                reviewOperateInfo.setReviewOpinion(review.getReviewOpinion());
                // 查询审核教师姓名
                SysUser teacher = reviewerMap.get(review.getTeacherId());
                if (teacher != null) {
                    reviewOperateInfo.setReviewerName(teacher.getRealName());
                }
                reviewOperateInfo.setReviewTime(review.getReviewTime());
                dto.setReviewOperateInfo(reviewOperateInfo);

                // 2.4 填充审核附件信息（若有）
                String attachPath = review.getReviewAttach();
                if (org.springframework.util.StringUtils.hasText(attachPath) && Files.exists(Paths.get(attachPath))) {
                    ReviewResultDTO.ReviewAttachDTO attachDTO = new ReviewResultDTO.ReviewAttachDTO();
                    // 提取附件原文件名（从路径提取，简化处理；实际应存储原文件名）
                    attachDTO.setAttachName("审核附件." + attachPath.substring(attachPath.lastIndexOf(".") + 1));
                    attachDTO.setAttachPath(attachPath);
                    try {
                        attachDTO.setAttachSize(Files.size(Paths.get(attachPath)));
                    } catch (IOException e) {
                        attachDTO.setAttachSize(0L);
                    }
                    attachDTO.setAttachType(attachPath.substring(attachPath.lastIndexOf(".") + 1));
                    // 构建下载URL（前端拼接域名）
                    attachDTO.setDownloadUrl("/api/v1/teacher/reviews/download-attach?attachPath=" + URLEncoder.encode(attachPath, StandardCharsets.UTF_8));
                    dto.setReviewAttach(attachDTO);
                }

                // 2.5 填充审核记录ID
                dto.setReviewId(review.getId());
            }

            // 2.6 填充顶层字段（方便前端直接使用）
            // 基础信息
            dto.setPaperId(paper.getId());
            dto.setPaperTitle(paper.getPaperTitle());
            dto.setStudentId(paper.getStudentId());
            dto.setSubmitTime(paper.getSubmitTime());
            dto.setPaperStatus(paper.getPaperStatus());
            
            // 学生信息
            if (student != null) {
                dto.setStudentName(student.getRealName());
                dto.setStudentNo(student.getUsername());
                dto.setEmail(student.getEmail());
                StudentInfo si = studentInfoMap.get(student.getId());
                if (si != null) {
                    dto.setCollege(si.getCollegeName());
                    dto.setMajor(si.getMajor());
                }
            }
            
            // 相似度
            if (task != null) {
                dto.setSimilarity(task.getCheckRate() != null ? task.getCheckRate().doubleValue() : 0.0);
            } else {
                dto.setSimilarity(0.0);
            }
            
            // 等待时间（天）
            if (paper.getSubmitTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                long days = java.time.Duration.between(paper.getSubmitTime(), now).toDays();
                dto.setWaitingTime((int) days);
            } else {
                dto.setWaitingTime(0);
            }
            
            // 审核时长（分钟）
            if (paper.getSubmitTime() != null && review != null && review.getReviewTime() != null) {
                long minutes = java.time.Duration.between(paper.getSubmitTime(), review.getReviewTime()).toMinutes();
                dto.setReviewDuration((int) minutes);
            } else {
                dto.setReviewDuration(0);
            }
            
            // 优先级
            Integer waitingDays = dto.getWaitingTime();
            if (waitingDays >= 14) {
                dto.setPriority("urgent");
            } else if (waitingDays >= 7) {
                dto.setPriority("high");
            } else {
                dto.setPriority("normal");
            }
            
            // 截止时间（默认7天后）
            if (paper.getSubmitTime() != null) {
                dto.setDeadline(paper.getSubmitTime().plusDays(7));
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public String exportReviewedList(ReviewQueryReq queryReq) {
        Long currentTeacherId = UserBusinessInfoUtils.getCurrentUserId();

        // 1. 构建查询条件（已审核状态）
        LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
        paperWrapper.eq(PaperInfo::getTeacherId, currentTeacherId)
                .in(PaperInfo::getPaperStatus,
                        PaperStatusEnum.COMPLETED.getValue(),
                        PaperStatusEnum.REJECTED.getValue(),
                        PaperStatusEnum.REVISION_NEEDED.getValue())
                .eq(PaperInfo::getIsDeleted, 0);

        // 2. 模糊查询条件
        if (org.springframework.util.StringUtils.hasText(queryReq.getStudentName())) {
            List<SysUser> matchedUsers = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, queryReq.getStudentName())
                            .eq(SysUser::getIsDeleted, 0)
                            .select(SysUser::getId)
            );
            if (!matchedUsers.isEmpty()) {
                List<Long> studentIds = matchedUsers.stream()
                        .map(SysUser::getId)
                        .collect(Collectors.toList());
                paperWrapper.in(PaperInfo::getStudentId, studentIds);
            } else {
                paperWrapper.eq(PaperInfo::getStudentId, -1);
            }
        }
        if (org.springframework.util.StringUtils.hasText(queryReq.getPaperTitle())) {
            paperWrapper.like(PaperInfo::getPaperTitle, queryReq.getPaperTitle());
        }

        // 3. 查询所有匹配的记录（不分页，最大10000条）
        paperWrapper.orderByDesc(PaperInfo::getUpdateTime);
        List<PaperInfo> paperList = paperInfoMapper.selectList(paperWrapper);

        // 4. 转换为DTO列表
        List<ReviewResultDTO> resultDTOList = CollectionUtils.isEmpty(paperList)
                ? new ArrayList<>()
                : convertToReviewResultDTOList(paperList);

        // 5. 准备导出数据
        List<Map<String, Object>> exportData = resultDTOList.stream().map(dto -> {
            Map<String, Object> row = new HashMap<>();
            row.put("学号", dto.getStudentNo());
            row.put("学生姓名", dto.getStudentName());
            row.put("学院", dto.getCollege());
            row.put("论文标题", dto.getPaperTitle());
            row.put("提交时间", dto.getSubmitTime() != null ? dto.getSubmitTime().toString() : "");
            row.put("相似度", dto.getSimilarity() != null ? dto.getSimilarity() + "%" : "");
            row.put("审核状态", dto.getPaperStatus());
            row.put("审核意见", dto.getReviewOperateInfo() != null ? dto.getReviewOperateInfo().getReviewOpinion() : "");
            row.put("审核时间", dto.getReviewOperateInfo() != null && dto.getReviewOperateInfo().getReviewTime() != null
                    ? dto.getReviewOperateInfo().getReviewTime().toString() : "");
            return row;
        }).collect(Collectors.toList());

        // 6. 生成Excel文件
        String fileName = "审核记录_" + System.currentTimeMillis() + ".xlsx";
        String filePath = exportToExcel(exportData, fileName);

        log.info("导出审核记录成功: teacherId={}, count={}", currentTeacherId, resultDTOList.size());
        return filePath;
    }

    @Override
    public void exportReviewedList(ReviewQueryReq queryReq, HttpServletResponse response) {
        Long currentTeacherId = UserBusinessInfoUtils.getCurrentUserId();

        // 1. 构建查询条件（已审核状态）
        LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
        paperWrapper.eq(PaperInfo::getTeacherId, currentTeacherId)
                .in(PaperInfo::getPaperStatus,
                        PaperStatusEnum.COMPLETED.getValue(),
                        PaperStatusEnum.REJECTED.getValue(),
                        PaperStatusEnum.REVISION_NEEDED.getValue())
                .eq(PaperInfo::getIsDeleted, 0);

        // 2. 模糊查询条件
        if (org.springframework.util.StringUtils.hasText(queryReq.getStudentName())) {
            List<SysUser> matchedUsers = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .like(SysUser::getRealName, queryReq.getStudentName())
                            .eq(SysUser::getIsDeleted, 0)
                            .select(SysUser::getId)
            );
            if (!matchedUsers.isEmpty()) {
                List<Long> studentIds = matchedUsers.stream()
                        .map(SysUser::getId)
                        .collect(Collectors.toList());
                paperWrapper.in(PaperInfo::getStudentId, studentIds);
            } else {
                paperWrapper.eq(PaperInfo::getStudentId, -1);
            }
        }
        if (org.springframework.util.StringUtils.hasText(queryReq.getPaperTitle())) {
            paperWrapper.like(PaperInfo::getPaperTitle, queryReq.getPaperTitle());
        }

        // 3. 查询所有匹配的记录（不分页，最大10000条）
        paperWrapper.orderByDesc(PaperInfo::getUpdateTime);
        List<PaperInfo> paperList = paperInfoMapper.selectList(paperWrapper);

        // 4. 转换为DTO列表
        List<ReviewResultDTO> resultDTOList = CollectionUtils.isEmpty(paperList)
                ? new ArrayList<>()
                : convertToReviewResultDTOList(paperList);

        // 5. 准备导出数据
        List<Map<String, Object>> exportData = resultDTOList.stream().map(dto -> {
            Map<String, Object> row = new HashMap<>();
            row.put("学号", dto.getStudentNo());
            row.put("学生姓名", dto.getStudentName());
            row.put("学院", dto.getCollege());
            row.put("论文标题", dto.getPaperTitle());
            row.put("提交时间", dto.getSubmitTime() != null ? dto.getSubmitTime().toString() : "");
            row.put("相似度", dto.getSimilarity() != null ? dto.getSimilarity() + "%" : "");
            row.put("审核状态", dto.getPaperStatus());
            row.put("审核意见", dto.getReviewOperateInfo() != null ? dto.getReviewOperateInfo().getReviewOpinion() : "");
            row.put("审核时间", dto.getReviewOperateInfo() != null && dto.getReviewOperateInfo().getReviewTime() != null
                    ? dto.getReviewOperateInfo().getReviewTime().toString() : "");
            return row;
        }).collect(Collectors.toList());

        // 6. 直接写入响应流
        String fileName = "审核记录_" + System.currentTimeMillis() + ".xlsx";
        exportToExcel(exportData, fileName, response);

        log.info("导出审核记录成功: teacherId={}, count={}", currentTeacherId, resultDTOList.size());
    }

    private String exportToExcel(List<Map<String, Object>> data, String fileName) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("审核记录");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"学号", "学生姓名", "学院", "论文标题", "提交时间", "相似度", "审核状态", "审核意见", "审核时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 填充数据
            int rowNum = 1;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue((String) rowData.get("学号"));
                row.createCell(1).setCellValue((String) rowData.get("学生姓名"));
                row.createCell(2).setCellValue((String) rowData.get("学院"));
                row.createCell(3).setCellValue((String) rowData.get("论文标题"));
                row.createCell(4).setCellValue((String) rowData.get("提交时间"));
                row.createCell(5).setCellValue((String) rowData.get("相似度"));
                row.createCell(6).setCellValue((String) rowData.get("审核状态"));
                row.createCell(7).setCellValue((String) rowData.get("审核意见"));
                row.createCell(8).setCellValue((String) rowData.get("审核时间"));
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 保存文件
            String filePath = System.getProperty("java.io.tmpdir") + "/" + fileName;
            try (java.io.FileOutputStream fileOut = new java.io.FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            return filePath;
        } catch (Exception e) {
            log.error("生成Excel文件失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "生成Excel文件失败: " + e.getMessage());
        }
    }

    private void exportToExcel(List<Map<String, Object>> data, String fileName, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("审核记录");

            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"学号", "学生姓名", "学院", "论文标题", "提交时间", "相似度", "审核状态", "审核意见", "审核时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 填充数据
            int rowNum = 1;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue((String) rowData.get("学号"));
                row.createCell(1).setCellValue((String) rowData.get("学生姓名"));
                row.createCell(2).setCellValue((String) rowData.get("学院"));
                row.createCell(3).setCellValue((String) rowData.get("论文标题"));
                row.createCell(4).setCellValue((String) rowData.get("提交时间"));
                row.createCell(5).setCellValue((String) rowData.get("相似度"));
                row.createCell(6).setCellValue((String) rowData.get("审核状态"));
                row.createCell(7).setCellValue((String) rowData.get("审核意见"));
                row.createCell(8).setCellValue((String) rowData.get("审核时间"));
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 设置响应头
            String encodedFilename = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");

            // 写入响应流
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("生成Excel文件失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "生成Excel文件失败: " + e.getMessage());
        }
    }
}
