package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.common.Exception.BusinessException;
import com.abin.checkrepeatsystem.common.Exception.PermissionDeniedException;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.service.AuthService;
import com.abin.checkrepeatsystem.common.service.PaperContentMinioService;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.student.dto.ReportDataDTO;
import com.abin.checkrepeatsystem.student.dto.ReportPreviewDTO;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.student.service.CheckReportService;
import com.abin.checkrepeatsystem.student.vo.ReportDownloadReq;
import com.abin.checkrepeatsystem.common.utils.PdfReportGenerator;
import com.abin.checkrepeatsystem.common.utils.ReportContentProcessor;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.user.service.MessageService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 查重报告服务实现类
 */
@Service
@Slf4j
public class CheckReportServiceImpl extends ServiceImpl<CheckReportMapper, CheckReport> implements CheckReportService {

    @Resource
    private CheckTaskMapper checkTaskMapper;

    @Resource
    private PaperInfoMapper paperInfoMapper;

    @Resource
    private AuthService authService;

    @Resource
    private MessageService messageService;

    @Resource
    private ReportContentProcessor reportContentProcessor;

    @Resource
    private PdfReportGenerator pdfReportGenerator;

    @Resource
    private PaperContentMinioService paperContentMinioService;

    @Resource
    private SysUserMapper userMapper;

    // 报告存储根路径（从配置文件获取）
    @Value("${report.storage.base-path}")
    private String reportBasePath;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckReport generateReport(Long taskId, Double checkRate, String repeatDetails) {
        CheckTask checkTask = checkTaskMapper.selectById(taskId);
        if (checkTask == null || checkTask.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "关联的查重任务不存在");
        }

        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
        if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "关联的论文不存在");
        }

        // 1. 先生成PDF文件
        String taskNo = checkTask.getTaskNo();
        String reportNo = "REPORT" + taskNo.substring(5);
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String reportFileName = reportNo + ".pdf";
        String reportPath = reportBasePath + dateDir + "/" + reportFileName;

        // 调试：打印路径信息
        log.info("开始生成PDF报告，路径: {}", reportPath);

        try {
            // 先创建目录
            File reportDir = new File(reportBasePath + dateDir);
            if (!reportDir.exists()) {
                boolean created = reportDir.mkdirs();
                log.info("创建报告目录: {}, 成功: {}", reportDir.getAbsolutePath(), created);
            }

            // 临时创建报告对象用于生成预览
            CheckReport tempReport = new CheckReport();
            tempReport.setReportNo(reportNo);
            tempReport.setRepeatDetails(repeatDetails);

            SysUser student = userMapper.selectById(paperInfo.getStudentId());
            SysUser teacher = userMapper.selectById(paperInfo.getTeacherId());

            ReportPreviewDTO previewDTO = reportContentProcessor.buildReportPreviewDTO(checkTask, tempReport, paperInfo, student, teacher);

            // 生成PDF文件
            pdfReportGenerator.generatePdfToFile(previewDTO, reportPath);

            // 验证文件是否生成成功
            File generatedFile = new File(reportPath);
            if (!generatedFile.exists() || generatedFile.length() == 0) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "PDF文件生成失败，文件不存在或为空");
            }

            log.info("PDF文件生成成功，大小: {} bytes", generatedFile.length());

        } catch (Exception e) {
            log.error("PDF报告生成失败（任务ID：{}）：", taskId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "报告文件生成失败: " + e.getMessage());
        }

        // 2. 文件生成成功后再保存数据库记录
        CheckReport checkReport = new CheckReport();
        checkReport.setTaskId(taskId);
        checkReport.setPaperId(checkTask.getPaperId());
        checkReport.setReportNo(reportNo);
        checkReport.setRepeatDetails(repeatDetails);
        checkReport.setReportPath(reportPath);
        checkReport.setReportType("pdf");
        UserBusinessInfoUtils.setAuditField(checkReport, true);
        save(checkReport);

        log.info("查重报告生成完成 - 报告ID: {}, 文件路径: {}", checkReport.getId(), reportPath);

        return checkReport;
    }
    @Override
    public Result<ReportPreviewDTO> previewReport(Long reportId) {
        // 1. 查询报告信息
        CheckReport checkReport = getById(reportId);
        if (checkReport == null || checkReport.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "查重报告不存在或已删除");
        }

        // 2. 查询关联任务
        CheckTask checkTask = checkTaskMapper.selectById(checkReport.getTaskId());
        if (checkTask == null || checkTask.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "报告关联的查重任务不存在");
        }

        // 3. 权限校验（学生查自己的，教师查指导的，管理员查所有）
        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        log.info("权限检查 - 当前用户ID: {}, userType: {}, roleId: {}", currentUser.getId(), currentUser.getUserType(), currentUser.getRoleId());
        log.info("权限检查 - 论文ID: {}, 学生ID: {}, 教师ID: {}", paperInfo.getId(), paperInfo.getStudentId(), paperInfo.getTeacherId());
        
        // 使用与getMyReportList相同的角色判断逻辑
        boolean isStudent = UserBusinessInfoUtils.isStudent();
        boolean isTeacher = UserBusinessInfoUtils.isTeacher();
        boolean isAdmin = UserBusinessInfoUtils.isAdmin();
        log.info("权限检查 - isStudent: {}, isTeacher: {}, isAdmin: {}", isStudent, isTeacher, isAdmin);
        
        boolean isStudentOwner = paperInfo.getStudentId().equals(currentUser.getId());
        boolean isTeacherOwner = paperInfo.getTeacherId() != null && paperInfo.getTeacherId().equals(currentUser.getId());
        log.info("权限检查 - isStudentOwner: {}, isTeacherOwner: {}", isStudentOwner, isTeacherOwner);
        
        // 权限判断逻辑
        if (isAdmin) {
            log.info("权限检查通过 - 管理员角色");
        } else if (isStudent && isStudentOwner) {
            log.info("权限检查通过 - 学生查看自己的论文");
        } else if (isTeacher && isTeacherOwner) {
            log.info("权限检查通过 - 教师查看指导的论文");
        } else {
            log.warn("权限检查失败 - 用户ID: {} 无权限预览报告ID: {}", currentUser.getId(), reportId);
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限预览该报告");
        }

        // 4. 构建预览DTO
        // 获取学生和教师信息
        SysUser student = userMapper.selectById(paperInfo.getStudentId());
        SysUser teacher = userMapper.selectById(paperInfo.getTeacherId());

        ReportPreviewDTO previewDTO = reportContentProcessor.buildReportPreviewDTO(checkTask, checkReport, paperInfo, student, teacher);
        return Result.success("报告预览成功", previewDTO);
    }

    @Override
    public void downloadReport(ReportDownloadReq downloadReq, HttpServletResponse response) {
        Long reportId = downloadReq.getReportId();
        String format = downloadReq.getFormat().toLowerCase();

        // 1. 校验报告存在性与权限
        CheckReport checkReport = getById(reportId);
        if (checkReport == null || checkReport.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "查重报告不存在或已删除");
        }
        // 权限校验（学生查自己的，教师查指导的，管理员查所有）
        CheckTask checkTask = checkTaskMapper.selectById(checkReport.getTaskId());
        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();

        // 使用与previewReport相同的角色判断逻辑
        boolean isStudent = UserBusinessInfoUtils.isStudent();
        boolean isTeacher = UserBusinessInfoUtils.isTeacher();
        boolean isAdmin = UserBusinessInfoUtils.isAdmin();

        boolean isStudentOwner = paperInfo.getStudentId().equals(currentUser.getId());
        boolean isTeacherOwner = paperInfo.getTeacherId() != null && paperInfo.getTeacherId().equals(currentUser.getId());

        boolean hasPermission = isAdmin || (isStudent && isStudentOwner) || (isTeacher && isTeacherOwner);
        if (!hasPermission) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限下载该报告");
        }

        // 2. 校验格式（仅支持pdf/html）
        if (!"pdf".equals(format) && !"html".equals(format)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仅支持pdf和html格式的报告下载");
        }

        // 3. 设置响应头（触发浏览器下载）
        try {
            String reportNo = checkReport.getReportNo();
            String fileName = reportNo + "." + format;
            // 处理中文文件名编码（避免乱码）
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
            // 设置响应头
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            // 4. 生成并输出报告文件
            if ("pdf".equals(format)) {
                // 若已存在PDF文件，直接读取；否则重新生成
                if (java.nio.file.Files.exists(java.nio.file.Paths.get(checkReport.getReportPath()))) {
                    // 直接读取文件输出（简化逻辑，实际需用流读取）
                    java.nio.file.Files.copy(
                            java.nio.file.Paths.get(checkReport.getReportPath()),
                            response.getOutputStream()
                    );
                } else {
                    // 重新生成PDF并输出
                    CheckTask task = checkTaskMapper.selectById(checkReport.getTaskId());
                    if (paperInfo == null) {
                        throw new BusinessException(ResultCode.PARAM_ERROR, "论文信息不存在");
                    }
                    // 获取学生和教师信息
                    SysUser student = userMapper.selectById(paperInfo.getStudentId());
                    SysUser teacher = userMapper.selectById(paperInfo.getTeacherId());

                    ReportPreviewDTO previewDTO = reportContentProcessor.buildReportPreviewDTO(task, checkReport, paperInfo, student, teacher);
                    pdfReportGenerator.generatePdf(previewDTO, response.getOutputStream());
                }
            } else if ("html".equals(format)) {
                // HTML格式（后续扩展，此处简化为返回提示）
                response.setContentType("text/html; charset=UTF-8");
                response.getWriter().write("<h1>HTML格式报告下载功能待扩展</h1>");
            }

            // 5. 刷新输出流
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("报告下载失败（报告ID：" + reportId + "）：" , e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "报告下载异常，文件输出失败");
        }
    }

    @Override
    public Result<List<CheckReport>> getMyReportList(Long paperId) {
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();

        // 构建报告查询条件
        LambdaQueryWrapper<CheckReport> reportQueryWrapper = new LambdaQueryWrapper<>();
        reportQueryWrapper.eq(CheckReport::getIsDeleted, 0)
                .orderByDesc(CheckReport::getCreateTime);

        // 按角色过滤
        if (UserBusinessInfoUtils.isStudent()) {
            // 学生：查询自己论文的报告
            LambdaQueryWrapper<PaperInfo> paperQueryWrapper = new LambdaQueryWrapper<>();
            paperQueryWrapper.eq(PaperInfo::getStudentId, currentUser.getId())
                    .eq(PaperInfo::getIsDeleted, 0);
            List<PaperInfo> studentPapers = paperInfoMapper.selectList(paperQueryWrapper);
            if (!studentPapers.isEmpty()) {
                List<Long> paperIds = studentPapers.stream().map(PaperInfo::getId).collect(Collectors.toList());
                reportQueryWrapper.in(CheckReport::getPaperId, paperIds);
            } else {
                return Result.success("历史报告列表查询成功", new ArrayList<>());
            }
        } else if (UserBusinessInfoUtils.isTeacher()) {
            // 教师：查询自己指导论文的报告
            LambdaQueryWrapper<PaperInfo> paperQueryWrapper = new LambdaQueryWrapper<>();
            paperQueryWrapper.eq(PaperInfo::getTeacherId, currentUser.getId())
                    .eq(PaperInfo::getIsDeleted, 0);
            List<PaperInfo> teacherPapers = paperInfoMapper.selectList(paperQueryWrapper);
            if (!teacherPapers.isEmpty()) {
                List<Long> paperIds = teacherPapers.stream().map(PaperInfo::getId).collect(Collectors.toList());
                reportQueryWrapper.in(CheckReport::getPaperId, paperIds);
            } else {
                return Result.success("历史报告列表查询成功", new ArrayList<>());
            }
        }

        // 按论文ID过滤（可选）
        if (paperId != null) {
            reportQueryWrapper.eq(CheckReport::getPaperId, paperId);
        }

        List<CheckReport> reportList = list(reportQueryWrapper);
        return Result.success("历史报告列表查询成功", reportList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteReport(Long reportId) {
        // 1. 查询报告与关联任务
        CheckReport checkReport = getById(reportId);
        if (checkReport == null || checkReport.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "查重报告不存在或已删除");
        }
        CheckTask checkTask = checkTaskMapper.selectById(checkReport.getTaskId());
        if (checkTask == null || checkTask.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "报告关联的查重任务不存在");
        }
        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());

        // 2. 权限校验（仅学生本人或管理员可删除）
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        boolean isStudentOwner = paperInfo.getStudentId().equals(currentUser.getId());
        boolean isAdmin = UserBusinessInfoUtils.isAdmin();
        if (!isStudentOwner && !isAdmin) {
            return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限删除该报告");
        }

        // 3. 状态校验（仅关联任务未审核的报告可删除）
        if (!Objects.equals(paperInfo.getPaperStatus(), DictConstants.PaperStatus.PENDING)) { // 2-待审核，3-审核通过，4-审核不通过
            return Result.error(ResultCode.PERMISSION_NOT_STATUS, "已进入审核流程的报告不允许删除");
        }

        // 4. 软删除报告
        removeById(reportId);

        // 5. 物理删除报告文件（可选）
        try {
            java.nio.file.Path filePath = java.nio.file.Paths.get(checkReport.getReportPath());
            if (java.nio.file.Files.exists(filePath)) {
                java.nio.file.Files.delete(filePath);
                log.info("报告文件删除成功：{}", checkReport.getReportPath());
            }
        } catch (IOException e) {
            log.error("报告文件删除失败：", e);
            // 文件删除失败不影响数据库删除结果
        }

        // 6. 更新任务的reportId为null
        checkTask.setReportId(null);
        checkTaskMapper.updateById(checkTask);

        return Result.success("报告删除成功");
    }

    @Override
    public Result<ReportDataDTO> getReportData(Long reportId) {
        // 1. 查询报告信息
        CheckReport checkReport = getById(reportId);
        if (checkReport == null || checkReport.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "查重报告不存在或已删除");
        }

        // 2. 查询关联任务
        CheckTask checkTask = checkTaskMapper.selectById(checkReport.getTaskId());
        if (checkTask == null || checkTask.getIsDeleted() == 1) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "报告关联的查重任务不存在");
        }

        // 3. 权限校验
        PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
        try {
            authService.checkReportAccess(checkReport);
        } catch (com.abin.checkrepeatsystem.common.Exception.PermissionDeniedException e) {
            return Result.error(e.getResultCode(), e.getMessage());
        }

        // 4. 构建报告数据DTO
        ReportDataDTO reportData = new ReportDataDTO();
        
        // 基本信息
        reportData.setPaperTitle(paperInfo.getPaperTitle());
        reportData.setAuthor(paperInfo.getAuthor());
        reportData.setStudentId(paperInfo.getStudentId());
        reportData.setSubmitTime(paperInfo.getCreateTime());
        reportData.setCheckTime(checkTask.getStartTime());
        
        // 查重结果
        reportData.setTotalSimilarity(checkTask.getCheckRate() != null ? checkTask.getCheckRate().doubleValue() : null);
        reportData.setWordCount(paperInfo.getWordCount());
        
        // 从report的repeatDetails中解析真实数据
        Map<String, ReportDataDTO.SectionInfo> sections = new HashMap<>();
        List<ReportDataDTO.SimilarSource> similarSourceList = new ArrayList<>();
        int citationCount = 0;
        int similarSourceCount = 0;
        
        try {
            // 解析重复详情JSON
            if (checkReport.getRepeatDetails() != null && !checkReport.getRepeatDetails().isEmpty()) {
                JSONArray repeatDetailsArray = JSON.parseArray(checkReport.getRepeatDetails());
                if (repeatDetailsArray != null && repeatDetailsArray.size() > 0) {
                    similarSourceCount = repeatDetailsArray.size();
                    
                    // 构建相似来源
                    for (int i = 0; i < repeatDetailsArray.size(); i++) {
                        JSONObject detail = repeatDetailsArray.getJSONObject(i);
                        double similarity = detail.getDoubleValue("similarity");
                        
                        // 只有当相似度大于0时，才添加相似来源信息
                        // 注意：如果相似度为0，说明论文是100%原创，不应该添加相似来源
                        if (similarity > 0) {
                            ReportDataDTO.SimilarSource similarSource = new ReportDataDTO.SimilarSource();
                            similarSource.setSourceId("src_" + String.format("%03d", i + 1));
                            similarSource.setTitle(detail.getString("source"));
                            similarSource.setAuthor(paperInfo.getAuthor());
                            similarSource.setSimilarity(similarity);
                            
                            // 构建匹配段落
                            List<ReportDataDTO.SimilarSource.MatchedParagraph> matchedParagraphs = new ArrayList<>();
                            
                            // 从Minio读取论文内容，提取匹配段落
                            String paperText = extractTextFromMinio(paperInfo.getId());
                            if (paperText != null && !paperText.trim().isEmpty()) {
                                // 简单分割段落
                                String[] paragraphs = paperText.split("\\n\\s*\\n");
                                // 取前3个段落作为匹配段落示例
                                int paraCount = 0;
                                for (String para : paragraphs) {
                                    if (para.trim().isEmpty()) continue;
                                    if (paraCount >= 3) break;
                                    
                                    ReportDataDTO.SimilarSource.MatchedParagraph matchedParagraph = new ReportDataDTO.SimilarSource.MatchedParagraph();
                                    matchedParagraph.setSourceText(detail.getString("source"));
                                    matchedParagraph.setPaperText(para.trim());
                                    matchedParagraph.setSimilarity(similarity);
                                    matchedParagraphs.add(matchedParagraph);
                                    paraCount++;
                                }
                            }
                            
                            // 如果没有匹配段落，添加默认段落
                            if (matchedParagraphs.isEmpty()) {
                                ReportDataDTO.SimilarSource.MatchedParagraph matchedParagraph = new ReportDataDTO.SimilarSource.MatchedParagraph();
                                matchedParagraph.setSourceText(detail.getString("source"));
                                matchedParagraph.setPaperText(paperInfo.getPaperTitle());
                                matchedParagraph.setSimilarity(similarity);
                                matchedParagraphs.add(matchedParagraph);
                            }
                            
                            similarSource.setMatchedParagraphs(matchedParagraphs);
                            similarSourceList.add(similarSource);
                        }
                    }
                }
            }
            
            // 从论文内容中提取章节信息
            sections = extractSectionsFromPaper(paperInfo);
        } catch (Exception e) {
            log.warn("解析报告数据失败: {}", e.getMessage());
            // 如果解析失败，返回空数据
            sections = new HashMap<>();
            similarSourceList = new ArrayList<>();
            citationCount = 0;
            similarSourceCount = 0;
        }
        
        reportData.setCitationCount(citationCount);
        reportData.setSimilarSources(similarSourceCount);
        reportData.setSimilarSourceCount(similarSourceCount);
        reportData.setCheckEngines(Arrays.asList("本地查重引擎"));
        reportData.setSections(sections);
        reportData.setSimilarSourceList(similarSourceList);
        
        return Result.success("报告数据获取成功", reportData);
    }
    
    /**
     * 从论文内容中提取章节信息
     */
    private Map<String, ReportDataDTO.SectionInfo> extractSectionsFromPaper(PaperInfo paperInfo) throws Exception {
        Map<String, ReportDataDTO.SectionInfo> sections = new HashMap<>();
        
        // 从Minio提取论文文本内容
        String paperText = extractTextFromMinio(paperInfo.getId());
        if (paperText != null && !paperText.trim().isEmpty()) {
            // 简单的章节识别逻辑
            String[] sectionPatterns = {
                "1\s*引言", "1\s*Introduction",
                "2\s*文献综述", "2\s*Literature\s*Review",
                "3\s*研究方法", "3\s*Methodology",
                "4\s*实验结果", "4\s*Results",
                "5\s*结论", "5\s*Conclusion"
            };
            
            // 章节名称映射
            java.util.Map<String, String> sectionNameMap = new java.util.HashMap<>();
            sectionNameMap.put("1\s*引言", "introduction");
            sectionNameMap.put("1\s*Introduction", "introduction");
            sectionNameMap.put("2\s*文献综述", "literature_review");
            sectionNameMap.put("2\s*Literature\s*Review", "literature_review");
            sectionNameMap.put("3\s*研究方法", "methodology");
            sectionNameMap.put("3\s*Methodology", "methodology");
            sectionNameMap.put("4\s*实验结果", "results");
            sectionNameMap.put("4\s*Results", "results");
            sectionNameMap.put("5\s*结论", "conclusion");
            sectionNameMap.put("5\s*Conclusion", "conclusion");
            
            // 初始化所有章节
            for (String sectionKey : sectionNameMap.values()) {
                ReportDataDTO.SectionInfo sectionInfo = new ReportDataDTO.SectionInfo();
                sectionInfo.setSimilarity(0.0);
                sectionInfo.setWordCount(0);
                sections.put(sectionKey, sectionInfo);
            }
            
            // 简单统计每个章节的字数
            for (String pattern : sectionPatterns) {
                if (sectionNameMap.containsKey(pattern)) {
                    java.util.regex.Pattern sectionPattern = java.util.regex.Pattern.compile(pattern);
                    java.util.regex.Matcher matcher = sectionPattern.matcher(paperText);
                    if (matcher.find()) {
                        String sectionKey = sectionNameMap.get(pattern);
                        ReportDataDTO.SectionInfo sectionInfo = sections.get(sectionKey);
                        if (sectionInfo != null) {
                            // 简单估算章节字数
                            int startIndex = matcher.start();
                            int endIndex = paperText.length();
                            // 查找下一个章节的开始
                            for (String nextPattern : sectionPatterns) {
                                if (!nextPattern.equals(pattern)) {
                                    java.util.regex.Matcher nextMatcher = java.util.regex.Pattern.compile(nextPattern).matcher(paperText);
                                    if (nextMatcher.find(startIndex + 1)) {
                                        endIndex = nextMatcher.start();
                                        break;
                                    }
                                }
                            }
                            String sectionText = paperText.substring(startIndex, endIndex);
                            sectionInfo.setWordCount(sectionText.length());
                            sections.put(sectionKey, sectionInfo);
                        }
                    }
                }
            }
        }
        
        return sections;
    }
    
    /**
     * 从Minio读取论文内容
     */
    private String extractTextFromMinio(Long paperId) throws Exception {
        // 1. 获取论文信息
        com.abin.checkrepeatsystem.pojo.entity.PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null) {
            throw new RuntimeException("论文不存在：" + paperId);
        }
        
        // 2. 优先从MinIO读取已提取的内容
        if (paperInfo.getContentPath() != null && !paperInfo.getContentPath().isEmpty()) {
            try {
                String content = paperContentMinioService.readPaperContent(paperInfo.getContentPath());
                log.info("从MinIO读取论文内容成功: paperId={}", paperId);
                return content;
            } catch (Exception e) {
                log.warn("从MinIO读取论文内容失败: paperId={}, error={}", paperId, e.getMessage());
            }
        }
        
        // 3. 如果MinIO中没有，返回空字符串
        log.warn("MinIO中没有论文内容: paperId={}", paperId);
        return "";
    }
    
    /**
     * 构建报告预览DTO（复用PDF报告生成逻辑）
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
        List<ReportPreviewDTO.ReportParagraphDTO> paragraphs = new java.util.ArrayList<>();
        if (paperInfo != null) {
            try {
                // 从Minio提取论文文本内容
                String paperText = extractTextFromMinio(paperInfo.getId());
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
                sourceDTO.setSourceName(detail.get("source") != null ? detail.get("source").toString() : "未知来源");
                sourceDTO.setSourceType("论文库");
                sourceDTO.setMaxSimilarity(checkRate);
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

    @Override
    public Result<Map<String, Object>> compareReport(Long reportId, Long sourceId) {
        try {
            CheckReport checkReport = this.getById(reportId);
            if (checkReport == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "查重报告不存在");
            }

            // 权限检查
            try {
                if (!authService.checkReportAccess(checkReport)) {
                    return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限访问此报告");
                }
            } catch (PermissionDeniedException e) {
                return Result.error(ResultCode.PERMISSION_NO_ACCESS, e.getMessage());
            }

            CheckTask checkTask = checkTaskMapper.selectById(checkReport.getTaskId());
            if (checkTask == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "关联的查重任务不存在");
            }

            PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
            if (paperInfo == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "关联的论文不存在");
            }

            // 提取论文文本
            String paperText = extractTextFromMinio(paperInfo.getId());
            List<String> paragraphs = new ArrayList<>();
            if (paperText != null && !paperText.isEmpty()) {
                paragraphs = Arrays.asList(paperText.split("\n\s*\n"));
            }

            // 解析重复详情
            List<Map<String, Object>> repeatDetails = new ArrayList<>();
            if (checkReport.getRepeatDetails() != null && !checkReport.getRepeatDetails().isEmpty()) {
                repeatDetails = JSON.parseObject(
                        checkReport.getRepeatDetails(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            }

            // 过滤出与指定来源相关的重复详情
            List<Map<String, Object>> sourceDetails = repeatDetails.stream()
                    .filter(detail -> detail.get("sourceId") != null && detail.get("sourceId").equals(sourceId))
                    .collect(Collectors.toList());

            // 构建对比数据
            Map<String, Object> comparisonData = new HashMap<>();
            List<Map<String, Object>> originalSegments = new ArrayList<>();
            List<Map<String, Object>> sourceSegments = new ArrayList<>();

            // 提取原文段落和相似内容
            for (Map<String, Object> detail : sourceDetails) {
                Integer paragraphNo = (Integer) detail.get("paragraphNo");
                if (paragraphNo != null && paragraphNo > 0 && paragraphNo <= paragraphs.size()) {
                    Map<String, Object> originalSegment = new HashMap<>();
                    originalSegment.put("text", paragraphs.get(paragraphNo - 1));
                    originalSegment.put("isSimilar", true);
                    originalSegments.add(originalSegment);

                    Map<String, Object> sourceSegment = new HashMap<>();
                    sourceSegment.put("text", detail.get("similarContent") != null ? detail.get("similarContent") : "");
                    sourceSegment.put("isSimilar", true);
                    sourceSegments.add(sourceSegment);
                }
            }

            comparisonData.put("originalSegments", originalSegments);
            comparisonData.put("sourceSegments", sourceSegments);

            return Result.success(comparisonData);
        } catch (Exception e) {
            log.error("对比报告失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "对比报告失败");
        }
    }

    @Override
    public Result<String> approveReport(Long reportId, String comment) {
        try {
            CheckReport checkReport = this.getById(reportId);
            if (checkReport == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "查重报告不存在");
            }

            // 权限检查
            try {
                if (!authService.checkReportAccess(checkReport)) {
                    return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限操作此报告");
                }
            } catch (PermissionDeniedException e) {
                return Result.error(ResultCode.PERMISSION_NO_ACCESS, e.getMessage());
            }

            CheckTask checkTask = checkTaskMapper.selectById(checkReport.getTaskId());
            if (checkTask == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "关联的查重任务不存在");
            }

            // 更新任务状态为已通过
            checkTask.setCheckStatus("completed");
            checkTaskMapper.updateById(checkTask);

            // 记录审核意见到重复详情中
            if (checkReport.getRepeatDetails() != null && !checkReport.getRepeatDetails().isEmpty()) {
                try {
                    List<Map<String, Object>> repeatDetails = JSON.parseObject(
                            checkReport.getRepeatDetails(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );
                    
                    // 添加审核信息
                    Map<String, Object> reviewInfo = new HashMap<>();
                    reviewInfo.put("reviewStatus", "approved");
                    reviewInfo.put("reviewComments", comment);
                    reviewInfo.put("reviewTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    
                    repeatDetails.add(reviewInfo);
                    checkReport.setRepeatDetails(JSON.toJSONString(repeatDetails));
                    this.updateById(checkReport);
                } catch (Exception e) {
                    log.error("更新审核意见失败: {}", e.getMessage());
                    // 解析失败不影响审核操作
                }
            }

            return Result.success("论文已通过审核");
        } catch (Exception e) {
            log.error("审核通过失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "审核通过失败");
        }
    }

    @Override
    public Result<String> requestRevision(Long reportId, String comment) {
        try {
            CheckReport checkReport = this.getById(reportId);
            if (checkReport == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "查重报告不存在");
            }

            // 权限检查
            try {
                if (!authService.checkReportAccess(checkReport)) {
                    return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限操作此报告");
                }
            } catch (PermissionDeniedException e) {
                return Result.error(ResultCode.PERMISSION_NO_ACCESS, e.getMessage());
            }

            CheckTask checkTask = checkTaskMapper.selectById(checkReport.getTaskId());
            if (checkTask == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "关联的查重任务不存在");
            }

            // 更新任务状态为需要修改
            checkTask.setCheckStatus("failure"); // 使用failure状态表示需要修改
            checkTask.setFailReason("论文需要修改");
            checkTaskMapper.updateById(checkTask);

            // 记录审核意见到重复详情中
            if (checkReport.getRepeatDetails() != null && !checkReport.getRepeatDetails().isEmpty()) {
                try {
                    List<Map<String, Object>> repeatDetails = JSON.parseObject(
                            checkReport.getRepeatDetails(),
                            new TypeReference<List<Map<String, Object>>>() {}
                    );
                    
                    // 添加审核信息
                    Map<String, Object> reviewInfo = new HashMap<>();
                    reviewInfo.put("reviewStatus", "revision");
                    reviewInfo.put("reviewComments", comment);
                    reviewInfo.put("reviewTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    
                    repeatDetails.add(reviewInfo);
                    checkReport.setRepeatDetails(JSON.toJSONString(repeatDetails));
                    this.updateById(checkReport);
                } catch (Exception e) {
                    log.error("更新审核意见失败: {}", e.getMessage());
                    // 解析失败不影响审核操作
                }
            }

            return Result.success("已要求学生修改论文");
        } catch (Exception e) {
            log.error("要求修改失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "要求修改失败");
        }
    }

    @Override
    public Result<String> contactStudent(Long reportId, String content) {
        try {
            CheckReport checkReport = this.getById(reportId);
            if (checkReport == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "查重报告不存在");
            }

            // 权限检查
            try {
                if (!authService.checkReportAccess(checkReport)) {
                    return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限操作此报告");
                }
            } catch (PermissionDeniedException e) {
                return Result.error(ResultCode.PERMISSION_NO_ACCESS, e.getMessage());
            }

            CheckTask checkTask = checkTaskMapper.selectById(checkReport.getTaskId());
            if (checkTask == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "关联的查重任务不存在");
            }

            PaperInfo paperInfo = paperInfoMapper.selectById(checkTask.getPaperId());
            if (paperInfo == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "关联的论文不存在");
            }

            // 获取当前教师用户信息
            SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
            if (currentUser == null) {
                return Result.error(ResultCode.NOT_LOGIN, "用户未登录");
            }

            // 发送系统内消息
            String title = "论文查重报告反馈"; 
            String messageContent = String.format("尊敬的%s同学：\n\n%s\n\n发送人：%s\n发送时间：%s", 
                paperInfo.getAuthor(), 
                content, 
                currentUser.getRealName(), 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            Result<Boolean> sendResult = messageService.sendBusinessMessage(
                "PRIVATE", // 消息类型：私信
                paperInfo.getStudentId(), // 接收者ID：学生ID
                paperInfo.getId(), // 关联ID：论文ID
                "paper", // 关联类型：论文
                title, // 消息标题
                messageContent // 消息内容
            );

            if (sendResult != null && sendResult.isSuccess() && sendResult.getData()) {
                log.info("向学生 {} (ID: {}) 发送消息成功", paperInfo.getAuthor(), paperInfo.getStudentId());
                return Result.success("消息已发送");
            } else {
                log.error("向学生 {} 发送消息失败: {}", paperInfo.getAuthor(), sendResult != null ? sendResult.getMessage() : "未知错误");
                return Result.error(ResultCode.SYSTEM_ERROR, "消息发送失败");
            }
        } catch (Exception e) {
            log.error("联系学生失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "联系学生失败");
        }
    }

    @Override
    public Result<Map<String, Object>> getSourceDetail(Long sourceId) {
        try {
            // 从所有查重报告中查找包含该sourceId的重复详情
            List<CheckReport> reports = this.list(
                    new LambdaQueryWrapper<CheckReport>()
                            .eq(CheckReport::getIsDeleted, 0)
            );

            for (CheckReport report : reports) {
                if (report.getRepeatDetails() != null && !report.getRepeatDetails().isEmpty()) {
                    try {
                        List<Map<String, Object>> repeatDetails = JSON.parseObject(
                                report.getRepeatDetails(),
                                new TypeReference<List<Map<String, Object>>>() {}
                        );

                        for (Map<String, Object> detail : repeatDetails) {
                            Object detailSourceId = detail.get("sourceId");
                            if (detailSourceId != null) {
                                // 处理不同类型的sourceId
                                Long detailSourceIdLong = null;
                                if (detailSourceId instanceof Long) {
                                    detailSourceIdLong = (Long) detailSourceId;
                                } else if (detailSourceId instanceof Integer) {
                                    detailSourceIdLong = ((Integer) detailSourceId).longValue();
                                } else if (detailSourceId instanceof String) {
                                    try {
                                        detailSourceIdLong = Long.parseLong((String) detailSourceId);
                                    } catch (NumberFormatException e) {
                                        // 忽略无法转换的sourceId
                                        continue;
                                    }
                                }

                                if (detailSourceIdLong != null && detailSourceIdLong.equals(sourceId)) {
                                    // 构建相似来源详情
                                    Map<String, Object> sourceDetail = new HashMap<>();
                                    sourceDetail.put("sourceId", sourceId);
                                    sourceDetail.put("sourceName", detail.get("sourceName") != null ? detail.get("sourceName") : "未知来源");
                                    sourceDetail.put("sourceType", detail.get("sourceType") != null ? detail.get("sourceType") : "未知类型");
                                    sourceDetail.put("content", detail.get("similarContent") != null ? detail.get("similarContent") : "");
                                    sourceDetail.put("author", detail.get("author") != null ? detail.get("author") : "未知作者");
                                    sourceDetail.put("publishDate", detail.get("publishDate") != null ? detail.get("publishDate") : "未知日期");
                                    sourceDetail.put("sourceUrl", detail.get("sourceUrl") != null ? detail.get("sourceUrl") : "");
                                    sourceDetail.put("similarity", detail.get("similarity") != null ? detail.get("similarity") : 0);
                                    sourceDetail.put("matchedParagraphs", detail.get("matchedParagraphs") != null ? detail.get("matchedParagraphs") : "");

                                    return Result.success(sourceDetail);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("解析重复详情失败: {}", e.getMessage());
                        // 解析失败不影响其他报告的处理
                        continue;
                    }
                }
            }

            // 如果没有找到相关的相似来源
            Map<String, Object> sourceDetail = new HashMap<>();
            sourceDetail.put("sourceId", sourceId);
            sourceDetail.put("sourceName", "未找到相似来源");
            sourceDetail.put("sourceType", "未知类型");
            sourceDetail.put("content", "");
            sourceDetail.put("author", "未知作者");
            sourceDetail.put("publishDate", "未知日期");
            sourceDetail.put("sourceUrl", "");

            return Result.success(sourceDetail);
        } catch (Exception e) {
            log.error("获取相似来源详情失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取相似来源详情失败");
        }
    }

    @Override
    public Result<List<CheckReport>> getHistoryReportList(Long paperId) {
        try {
            if (paperId == null) {
                return Result.error(ResultCode.PARAM_ERROR, "论文ID不能为空");
            }

            // 权限检查
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "论文不存在");
            }
            try {
                if (!authService.checkPaperAccess(paperInfo)) {
                    return Result.error(ResultCode.PERMISSION_NO_ACCESS, "无权限访问此论文的报告");
                }
            } catch (PermissionDeniedException e) {
                return Result.error(ResultCode.PERMISSION_NO_ACCESS, e.getMessage());
            }

            // 查询该论文的所有历史报告
            List<CheckReport> reports = this.list(
                    new LambdaQueryWrapper<CheckReport>()
                            .eq(CheckReport::getPaperId, paperId)
                            .eq(CheckReport::getIsDeleted, 0)
                            .orderByDesc(CheckReport::getCreateTime)
            );

            return Result.success(reports);
        } catch (Exception e) {
            log.error("获取历史报告列表失败: {}", e.getMessage(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取历史报告列表失败");
        }
    }
}
