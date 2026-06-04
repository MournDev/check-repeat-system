package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.admin.mapper.PaperSubmitMapper;
import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.constant.PaperNoticeConstants;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.common.utils.FileMimeTypeUtils;
import com.abin.checkrepeatsystem.common.utils.UserContextHolder;
import com.abin.checkrepeatsystem.mapper.FileInfoMapper;
import com.abin.checkrepeatsystem.mapper.PaperAttachmentMapper;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.*;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.service.CheckTaskService;
import com.abin.checkrepeatsystem.student.service.PaperInfoService;
import com.abin.checkrepeatsystem.student.vo.PaperQueryRequest;
import com.abin.checkrepeatsystem.student.dto.*;
import com.abin.checkrepeatsystem.student.vo.PaperSubmitRequest;
import com.abin.checkrepeatsystem.user.service.AdvisorAssignService;
import com.abin.checkrepeatsystem.user.service.Impl.InternalMessageNotificationService;
import com.abin.checkrepeatsystem.user.service.Impl.NotificationFacadeService;
import com.abin.checkrepeatsystem.user.service.MessageService;
import com.abin.checkrepeatsystem.user.service.PaperStatusLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * 论文信息服务实现类
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class PaperInfoServiceImpl extends ServiceImpl<PaperInfoMapper, PaperInfo> implements PaperInfoService {

    private final FileService fileService;
    
    @Value("${file.upload.base-path}")
    private String uploadBasePath;

    private final AdvisorAssignService advisorAssignService;

    private final CheckTaskService checkTaskService;

    private final PaperInfoMapper paperInfoMapper;

    private final FileInfoMapper fileInfoMapper;

    private final PaperSubmitMapper paperSubmitMapper;

    private final SysUserMapper sysUserMapper;

    private final InternalMessageNotificationService internalMessageNotificationService;
    
    private final NotificationFacadeService notificationFacadeService;
    
    private final CheckTaskMapper checkTaskMapper;
    
    private final CheckReportMapper checkReportMapper;

    private final PaperAttachmentMapper paperAttachmentMapper;

    private final MessageService messageService;

    private final PaperStatusLogService paperStatusLogService;

    private final com.abin.checkrepeatsystem.student.mapper.MajorMapper majorMapper;

    private final com.abin.checkrepeatsystem.detection.service.PaperContentExtractor paperContentExtractor;

    private final Executor asyncExecutor;
    private final PaperVersionServiceImpl paperVersionService;
    private final PaperCheckHistoryServiceImpl paperCheckHistoryService;
    private final PaperAttachmentServiceImpl paperAttachmentService;






    @Override
    public Page<PaperInfo> getStudentPaperPage(PaperQueryRequest request) {
        try {
            // 参数校验
            if (request.getStudentId() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "学生ID不能为空");
            }
            // 构建查询条件
            LambdaQueryWrapper<PaperInfo> queryWrapper = buildQueryWrapper(request);
            // 分页查询
            Page<PaperInfo> page = new Page<>(request.getPageNum(), request.getPageSize());
            // 返回分页结果
            return page(page,queryWrapper);
        }  catch (Exception e) {
            log.error("论文列表分页查询失败，学生ID：{}", request.getStudentId(), e);
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "论文列表查询失败");
        }
    }
    /**
     * 构建查询条件包装器
     */
    private LambdaQueryWrapper<PaperInfo> buildQueryWrapper(PaperQueryRequest request) {
        LambdaQueryWrapper<PaperInfo> queryWrapper = new LambdaQueryWrapper<>();

        // 基础条件：学生ID和未删除
        queryWrapper.eq(PaperInfo::getStudentId, request.getStudentId())
                .eq(PaperInfo::getIsDeleted, 0);

        // 论文状态条件（支持查询所有状态）
        if (StringUtils.hasText(request.getPaperStatus())) {
            queryWrapper.eq(PaperInfo::getPaperStatus, request.getPaperStatus());
        }

        // 论文名称搜索（模糊查询）
        if (StringUtils.hasText(request.getPaperTitle())) {
            queryWrapper.like(PaperInfo::getPaperTitle, request.getPaperTitle());
        }

        // 时间范围搜索
        if (request.getStartTime() != null) {
            queryWrapper.ge(PaperInfo::getSubmitTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            queryWrapper.le(PaperInfo::getSubmitTime, request.getEndTime());
        }

        // 按提交时间倒序
        queryWrapper.orderByDesc(PaperInfo::getSubmitTime);

        return queryWrapper;
    }
    /**
     * 软删除论文（标记删除而不是物理删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePaper(Long paperId, Long studentId) {
        try {
            log.info("开始删除论文 - 论文ID: {}, 操作人ID: {}", paperId, studentId);

            // 1. 验证论文信息
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                log.warn("论文不存在 - 论文ID: {}", paperId);
                return false;
            }

            // 2. 验证论文状态
            String paperStatus = paperInfo.getPaperStatus();
            if (!DictConstants.PaperStatus.PENDING.equals(paperStatus) && !DictConstants.PaperStatus.WITHDRAWN.equals(paperStatus)) {
                log.warn("论文状态不允许删除 - 论文ID: {}, 状态: {}", paperId, paperStatus);
                return false;
            }

            // 3. 更新论文状态为已撤回,软删除
            String oldStatus = paperInfo.getPaperStatus();
            String newStatus = DictConstants.PaperStatus.WITHDRAWN;
            
            PaperInfo updateInfo = new PaperInfo();
            updateInfo.setId(paperId);
            updateInfo.setPaperStatus(newStatus);
            updateInfo.setUpdateTime(LocalDateTime.now());
            updateInfo.setIsDeleted(1);

            int result = paperInfoMapper.updateById(updateInfo);
            boolean success = result > 0;
            
            if (success) {
                // 记录状态变更日志
                paperStatusLogService.recordStatusLog(
                    paperId,
                    getStatusValue(oldStatus),
                    getStatusValue(newStatus),
                    "学生删除论文",
                    studentId,
                    null
                );
            }

            if (success) {
                // 4. 更新提交记录状态
                PaperSubmit submitUpdate = new PaperSubmit();
                paperSubmitMapper.update(submitUpdate,
                        new LambdaQueryWrapper<PaperSubmit>()
                                .eq(PaperSubmit::getPaperId, paperId)
                                .orderByDesc(PaperSubmit::getSubmitVersion)
                                .last("LIMIT 1"));
            }

            log.info("论文软删除完成 - 论文ID: {}, 结果: {}", paperId, success);
            return success;

        } catch (Exception e) {
            log.error("软删除论文失败 - 论文ID: {}", paperId, e);
            throw new RuntimeException("删除论文失败", e);
        }
    }

    /**
     * 完整的论文提交流程（包含文件上传和信息录入）
     */
    @Override
    public PaperInfo submitPaper(MultipartFile multipartFile, String subjectCode, String paperTitle,
                                 String paperAbstract, Long collegeId, Long majorId, String paperType,
                                 Long studentId) {
        
        log.info("开始完整论文提交流程 - 学生ID: {}, 论文标题: {}", studentId, paperTitle);
        
        try {
            // 1. 文件上传处理
            FileInfo uploadedFile = uploadPaperFile(multipartFile, studentId);
            
            // 2. 调用文件ID方式提交 - 修复参数类型
            PaperInfo paperInfo = submitPaperByFileId(subjectCode, paperTitle, paperAbstract, collegeId, majorId, 
                                     paperType, uploadedFile.getId(), uploadedFile.getMd5(), studentId);
            
            // 3. 异步处理论文后续流程：分配指导老师和触发查重
            // 注意：submitPaperByFileId方法已经会调用asyncProcessPaperAfterSubmit，这里不需要重复调用
            
            return paperInfo;
            
        } catch (Exception e) {
            log.error("完整论文提交流程失败 - 学生ID: {}, 论文标题: {}", studentId, paperTitle, e);
            // 发送提交失败通知
            sendPaperSubmitFailedNotification(paperTitle, studentId, e.getMessage());
            throw new RuntimeException("论文提交失败: " + e.getMessage());
        }
    }
    
    /**
     * 文件上传处理
     */
    private FileInfo uploadPaperFile(MultipartFile multipartFile, Long studentId) {
        try {
            // 调用文件服务上传文件，获取文件 ID
            Long fileId = fileService.uploadFile(multipartFile, studentId);
                
            // 通过文件 ID 查询文件信息
            FileInfo fileInfo = fileInfoMapper.selectById(fileId);
            
            if (fileInfo == null) {
                throw new RuntimeException("文件上传后查询失败，文件ID: " + fileId);
            }
            
            log.info("文件上传成功 - 文件ID: {}, 文件名: {}", fileInfo.getId(), fileInfo.getOriginalFilename());
            return fileInfo;
        } catch (Exception e) {
            log.error("文件上传失败 - 学生ID: {}", studentId, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 文件ID方式提交论文
     * 根据文件ID提交论文，不涉及文件上传
     */
    public PaperInfo submitPaperByFileId(String subjectCode,String paperTitle, String paperAbstract,
                                         Long collegeId, Long majorId, String paperType,
                                         Long fileId, String fileMd5, Long studentId) {

        log.info("开始文件ID方式提交论文 - 学生ID: {}, 论文标题: {}, 文件ID: {}",
                studentId, paperTitle, fileId);

        try {
            // 事务内处理核心业务
            PaperInfo paperInfo = doSubmitPaperByFileId(subjectCode, paperTitle, paperAbstract,
                    collegeId, majorId, paperType, fileId, fileMd5, studentId);
            
            // 事务外调用异步处理
            asyncProcessPaperAfterSubmit(paperInfo.getId(), studentId);

            log.info("文件ID方式提交论文成功 - 论文ID: {}, 文件ID: {}", paperInfo.getId(), fileId);
            return paperInfo;

        } catch (Exception e) {
            log.error("文件ID方式提交论文失败 - 学生ID: {}, 论文标题: {}", studentId, paperTitle, e);
            throw new RuntimeException("论文提交失败: " + e.getMessage());
        }
    }
    
    /**
     * 实际执行文件ID方式提交论文的核心逻辑
     */
    @Transactional(rollbackFor = Exception.class)
    protected PaperInfo doSubmitPaperByFileId(String subjectCode, String paperTitle, String paperAbstract,
                                              Long collegeId, Long majorId, String paperType,
                                              Long fileId, String fileMd5, Long studentId) {
        // 1. 验证文件信息
        validateFileInfo(fileId, fileMd5);

        // 2. 查找是否已存在该学生的论文（根据标题判断）
        PaperInfo existingPaper = findExistingPaper(studentId, paperTitle);

        PaperInfo paperInfo;
        boolean isNewPaper = false;//是否为新论文
        if (existingPaper != null) {
            // 更新现有论文信息
            paperInfo = updatePaperInfo(existingPaper, subjectCode, paperTitle, paperAbstract,
                    collegeId, majorId, paperType, fileId, fileMd5, studentId);
            log.info("更新现有论文 - 论文ID: {}", paperInfo.getId());
        } else {
            // 创建新论文信息
            paperInfo = createPaperInfo(subjectCode, paperTitle, paperAbstract, collegeId,
                    majorId, paperType, fileId, fileMd5, studentId);
            log.info("创建新论文 - 论文ID: {}", paperInfo.getId());
            isNewPaper = true;
        }

        // 3. 创建提交记录
        createPaperSubmitRecord(paperInfo, fileId, fileMd5, studentId);

        // 4. 发送论文提交成功通知
        sendPaperSubmitSuccessNotification(paperInfo, studentId, isNewPaper);

        return paperInfo;
    }
    /**
     * 异步处理论文后续流程：分配指导老师和触发查重
     */
    @Async("asyncExecutor")
    public void asyncProcessPaperAfterSubmit(Long paperSubmitId, Long studentId) {
        try {
            log.info("开始异步处理论文后续流程 - 论文ID: {}, 学生ID: {}", paperSubmitId, studentId);

            // 存储学生信息到UserContextHolder，供异步线程使用
            com.abin.checkrepeatsystem.pojo.entity.SysUser student = sysUserMapper.selectById(studentId);
            if (student != null) {
                UserContextHolder.setUser(student);
            }

            // 并行处理：分配指导老师、提取内容（查重延迟到教师确认后由 TeacherAssignmentService 触发）
            CompletableFuture<Void> allocateTask = CompletableFuture.runAsync(() -> {
                try {
                    if (student != null) {

                        UserContextHolder.setUser(student);
                    }

                    // 1. 分配指导老师
                    Result<Boolean> result = advisorAssignService.autoAssignAdvisor(paperSubmitId);
                    boolean allocationSuccess = result.isSuccess();
                    if (allocationSuccess) {
                        sendAdvisorAllocatedNotification(paperSubmitId, studentId);
                        log.info("指导老师分配成功 - 论文ID: {}", paperSubmitId);
                    } else {
                        log.warn("指导老师分配失败 - 论文ID: {}", paperSubmitId);
                        String errorMsg = result.getMessage();
                        sendAdvisorAllocateFailedNotification(paperSubmitId, studentId, errorMsg);
                    }
                } catch (Exception e) {
                    log.error("分配指导老师异常 - 论文ID: {}", paperSubmitId, e);
                } finally {
                    UserContextHolder.removeUser();
                }
            }, asyncExecutor);

            CompletableFuture<Void> extractContentTask = CompletableFuture.runAsync(() -> {
                try {
                    if (student != null) {
                        UserContextHolder.setUser(student);
                    }

                    // 2. 提取论文内容并存储到Minio（为后续查重做准备）
                    log.info("开始提取论文内容 - 论文ID: {}", paperSubmitId);
                    paperContentExtractor.extractRawContent(paperSubmitId);
                    log.info("论文内容提取成功 - 论文ID: {}", paperSubmitId);
                } catch (Exception e) {
                    log.error("提取论文内容异常 - 论文ID: {}", paperSubmitId, e);
                } finally {
                    UserContextHolder.removeUser();
                }
            }, asyncExecutor);

            // 等待两个任务完成（查重将在教师确认分配后触发）
            CompletableFuture.allOf(allocateTask, extractContentTask).join();
            log.info("论文后续流程处理完成 - 论文ID: {}", paperSubmitId);

        } catch (Exception e) {
            log.error("异步处理论文后续流程系统异常 - 论文ID: {}", paperSubmitId, e);
            // 系统异常可以继续捕获，不影响主流程
        } finally {
            // 清理UserContextHolder
            UserContextHolder.removeUser();
        }
    }

    /**
     * 获取论文状态标签
     */
    public String getPaperStatusLabel(String statusValue) {
        if (!StringUtils.hasText(statusValue)) {
            return "未知状态";
        }

        if (DictConstants.PaperStatus.PENDING.equals(statusValue)) return "待分配指导老师";
        if (DictConstants.PaperStatus.CHECKING.equals(statusValue)) return "待重中";
        if (DictConstants.PaperStatus.AUDITING.equals(statusValue)) return "待审核";
        if (DictConstants.PaperStatus.COMPLETED.equals(statusValue)) return "已完成";
        if (DictConstants.PaperStatus.REJECTED.equals(statusValue)) return "已拒绝";
        if (DictConstants.PaperStatus.WITHDRAWN.equals(statusValue)) return "已撤回";
        return "未知状态";
    }

    /**
     * 获取状态值
     */
    private String getStatusValue(String status) {
        return status;
    }

    /**
     * 验证文件信息
     */
    private void validateFileInfo(Long fileId, String fileMd5) {
        if (fileId == null) {
            throw new RuntimeException("文件 ID 不能为空");
        }
    
        // 验证文件是否存在
        FileInfo fileInfo = fileService.getById(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在，文件 ID: " + fileId);
        }

        // 验证MD5（如果提供了MD5）
        if (StringUtils.hasText(fileMd5) && StringUtils.hasText(fileInfo.getMd5())) {
            if (!fileMd5.equals(fileInfo.getMd5())) {
                throw new RuntimeException("文件MD5校验失败，文件可能已被修改");
            }
        }

        log.debug("文件验证通过 - 文件ID: {}, 文件名: {}", fileId, fileInfo.getOriginalFilename());
    }

    /**
     * 查找已存在的论文
     */
    private PaperInfo findExistingPaper(Long studentId, String paperTitle) {
        return paperInfoMapper.selectOne(new LambdaQueryWrapper<PaperInfo>()
                .eq(PaperInfo::getStudentId, studentId)
                .eq(PaperInfo::getPaperTitle, paperTitle)
                .orderByDesc(PaperInfo::getCreateTime)
                .last("LIMIT 1"));
    }

    /**
     * 创建新论文信息
     */
    private PaperInfo createPaperInfo(String subjectCode,String paperTitle, String paperAbstract,
                                      Long collegeId, Long majorId, String paperType,
                                      Long fileId, String fileMd5, Long studentId) {
        PaperInfo paperInfo = new PaperInfo();
        // 设置作者姓名
        SysUser student = sysUserMapper.selectById(studentId);
        if (student != null) {
            paperInfo.setAuthor(student.getRealName()); // 或者使用 username，根据需求
        } else {
            log.warn("未找到学生信息，学生ID: {}", studentId);
            paperInfo.setAuthor("未知作者"); // 设置默认值
        }
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        paperInfo.setSubjectCode(subjectCode);
        paperInfo.setPaperTitle(paperTitle);
        paperInfo.setPaperAbstract(paperAbstract);
        paperInfo.setCollegeId(collegeId);
        paperInfo.setMajorId(majorId);
        paperInfo.setPaperType(paperType);
        paperInfo.setFileId(fileId);
        paperInfo.setFileMd5(fileMd5);
        paperInfo.setFilePath(fileInfo.getStoragePath());
        paperInfo.setWordCount(fileInfo.getWordCount());
        paperInfo.setPageCount(fileInfo.getPageCount());//论文页数
        paperInfo.setStudentId(studentId);
        paperInfo.setPaperStatus(DictConstants.PaperStatus.PENDING);
        paperInfo.setSimilarityRate(BigDecimal.ZERO);
        paperInfo.setSubmitTime(LocalDateTime.now());
        paperInfo.setCreateTime(LocalDateTime.now());
        paperInfo.setUpdateTime(LocalDateTime.now());

        int result = paperInfoMapper.insert(paperInfo);
        if (result <= 0) {
            throw new RuntimeException("创建论文信息失败");
        }

        return paperInfo;
    }

    /**
     * 更新现有论文信息
     */
    private PaperInfo updatePaperInfo(PaperInfo existingPaper, String  subjectCode,String paperTitle, String paperAbstract,
                                      Long collegeId, Long majorId, String paperType,
                                      Long fileId, String fileMd5, Long studentId) {

        String oldStatus = existingPaper.getPaperStatus();
        String newStatus = DictConstants.PaperStatus.PENDING; // 重置状态
        
        existingPaper.setSubjectCode(subjectCode);
        existingPaper.setPaperTitle(paperTitle);
        existingPaper.setPaperAbstract(paperAbstract);
        existingPaper.setCollegeId(collegeId);
        existingPaper.setMajorId(majorId);
        existingPaper.setPaperType(paperType);
        existingPaper.setFileId(fileId);
        existingPaper.setFileMd5(fileMd5);
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        existingPaper.setFilePath(fileInfo.getStoragePath());
        existingPaper.setPaperStatus(newStatus);
        existingPaper.setWordCount(fileInfo.getWordCount());//论文字数
        existingPaper.setPageCount(fileInfo.getPageCount());//论文页数
        existingPaper.setSimilarityRate(BigDecimal.ZERO); // 重置相似度
        existingPaper.setSubmitTime(LocalDateTime.now());
        existingPaper.setUpdateTime(LocalDateTime.now());

        int result = paperInfoMapper.updateById(existingPaper);
        if (result <= 0) {
            throw new RuntimeException("更新论文信息失败");
        }
        
        // 记录状态变更日志
        paperStatusLogService.recordStatusLog(
            existingPaper.getId(),
            getStatusValue(oldStatus),
            getStatusValue(newStatus),
            "学生重新提交论文",
            studentId,
            null
        );

        return existingPaper;
    }

    /**
     * 创建提交记录
     */
    private void createPaperSubmitRecord(PaperInfo paperInfo, Long fileId, String fileMd5, Long studentId) {
        // 获取当前版本号
        Integer currentVersion = getCurrentVersion(paperInfo.getId());
        Integer newVersion = currentVersion + 1;

        PaperSubmit paperSubmit = new PaperSubmit();
        paperSubmit.setPaperId(paperInfo.getId());
        paperSubmit.setStudentId(studentId);
        paperSubmit.setSubmitVersion(newVersion);
        paperSubmit.setFileId(fileId);
        paperSubmit.setFileMd5(fileMd5);
        paperSubmit.setSubmitTime(LocalDateTime.now());
        // 备注字段可用于记录提交说明
        paperSubmit.setRemark("第" + newVersion + "次提交");
        paperSubmit.setCreateTime(LocalDateTime.now());

        int result = paperSubmitMapper.insert(paperSubmit);
        if (result <= 0) {
            throw new RuntimeException("创建提交记录失败");
        }

        log.debug("创建提交记录成功 - 论文ID: {}, 版本: {}", paperInfo.getId(), newVersion);
    }

    /**
     * 获取当前版本号
     */
    private Integer getCurrentVersion(Long paperId) {
        PaperSubmit lastSubmit = paperSubmitMapper.selectOne(
                new LambdaQueryWrapper<PaperSubmit>()
                        .eq(PaperSubmit::getPaperId, paperId)
                        .orderByDesc(PaperSubmit::getSubmitVersion)
                        .last("LIMIT 1")
        );
        return lastSubmit != null ? lastSubmit.getSubmitVersion() : 0;
    }

    
    /**
     * 发送论文提交成功通知
     */
    private void sendPaperSubmitSuccessNotification(PaperInfo paperInfo, Long studentId, boolean isNewPaper) {
        try {
            String title = isNewPaper ? "论文提交成功" : "论文重新提交成功";
            String content = String.format("您的论文《%s》已成功%s，系统将自动分配指导老师并进行查重检测。",
                    paperInfo.getPaperTitle(), isNewPaper ? "提交" : "重新提交");

            internalMessageNotificationService.sendSystemNotice(
                    studentId,
                    title,
                    content,
                    PaperNoticeConstants.NOTICE_TYPE_PAPER_SUBMIT_SUCCESS,
                    paperInfo.getId().toString()
            );
            log.info("论文提交成功通知发送成功 - 论文ID: {}, 学生ID: {}", paperInfo.getId(), studentId);
        } catch (Exception e) {
            log.error("发送论文提交成功通知失败 - 论文ID: {}, 学生ID: {}", paperInfo.getId(), studentId, e);
        }
    }

    /**
     * 发送论文提交失败通知
     */
    private void sendPaperSubmitFailedNotification(String paperTitle, Long studentId, String errorMsg) {
        try {
            String title = "论文提交失败";
            String content = String.format("您的论文《%s》提交失败。%s",
                    paperTitle,
                    StringUtils.hasText(errorMsg) ? "原因：" + errorMsg : "请稍后重试或联系管理员。");

            internalMessageNotificationService.sendSystemNotice(
                    studentId,
                    title,
                    content,
                    PaperNoticeConstants.NOTICE_TYPE_PAPER_SUBMIT_FAILED,
                    null
            );
            log.info("论文提交失败通知发送成功 - 学生ID: {}", studentId);
        } catch (Exception e) {
            log.error("发送论文提交失败通知失败 - 学生ID: {}", studentId, e);
        }
    }

// ========== 指导老师分配相关通知 ==========

    /**
     * 发送指导老师分配成功通知
     */
    private void sendAdvisorAllocatedNotification(Long paperId, Long studentId) {
        try {
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) return;

            String title = "指导老师分配成功";
            String content = String.format("您的论文《%s》已成功分配指导老师，系统将开始进行查重检测。",
                    paperInfo.getPaperTitle());

            internalMessageNotificationService.sendSystemNotice(
                    studentId,
                    title,
                    content,
                    PaperNoticeConstants.NOTICE_TYPE_ADVISOR_ASSIGN_SUCCESS,
                    paperId.toString()
            );
            log.info("指导老师分配成功通知发送成功 - 论文ID: {}", paperId);
        } catch (Exception e) {
            log.error("发送指导老师分配成功通知失败 - 论文ID: {}", paperId, e);
        }
    }

    /**
     * 发送指导老师分配失败通知
     */
    private void sendAdvisorAllocateFailedNotification(Long paperId, Long studentId, String errorMsg) {
        try {
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) return;

            String title = "指导老师分配失败";
            String content = String.format("您的论文《%s》指导老师分配失败。%s",
                    paperInfo.getPaperTitle(),
                    StringUtils.hasText(errorMsg) ? "原因：" + errorMsg : "请稍后重试或联系管理员。");

            internalMessageNotificationService.sendSystemNotice(
                    studentId,
                    title,
                    content,
                    PaperNoticeConstants.NOTICE_TYPE_ADVISOR_ASSIGN_FAILED,
                    paperId.toString()
            );
            log.info("指导老师分配失败通知发送成功 - 论文ID: {}", paperId);
        } catch (Exception e) {
            log.error("发送指导老师分配失败通知失败 - 论文ID: {}", paperId, e);
        }
    }
    
    // ==================== 新增接口实现 ====================
    
    /**
     * 论文撤回接口实现
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawPaper(Long paperId, Long studentId, String reason) {
        try {
            log.info("开始撤回论文 - 论文 ID: {}, 学生 ID: {}, 原因：{}", paperId, studentId, reason);
                
            // 1. 验证论文信息
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                log.warn("论文不存在 - 论文 ID: {}", paperId);
                return false;
            }
                
            // 2. 验证论文归属
            if (!paperInfo.getStudentId().equals(studentId)) {
                log.warn("无权限撤回他人论文 - 论文 ID: {}, 学生 ID: {}", paperId, studentId);
                return false;
            }
                
            // 3. 验证论文状态：待分配、已分配、查重中、待审核状态的论文可以撤回
            if (!(DictConstants.PaperStatus.PENDING.equals(paperInfo.getPaperStatus()) ||
                  DictConstants.PaperStatus.ASSIGNED.equals(paperInfo.getPaperStatus()) ||
                  DictConstants.PaperStatus.CHECKING.equals(paperInfo.getPaperStatus()) ||
                  DictConstants.PaperStatus.AUDITING.equals(paperInfo.getPaperStatus()))) {
                String statusLabel = getPaperStatusLabel(paperInfo.getPaperStatus());
                log.warn("论文状态不允许撤回 - 论文 ID: {}, 状态：{}", paperId, statusLabel);
                return false;
            }
                
            // 【新增】4. 检查撤回次数限制
            Long withdrawCount = paperSubmitMapper.selectCount(
                new LambdaQueryWrapper<PaperSubmit>()
                    .eq(PaperSubmit::getPaperId, paperId)
                    .like(PaperSubmit::getRemark, "%撤回%")
            );
                
            int maxWithdrawCount = 3; // 最多撤回 3 次
            if (withdrawCount >= maxWithdrawCount) {
                log.warn("论文撤回次数已达上限 - 论文 ID: {}, 已撤回：{}次", paperId, withdrawCount);
                throw new BusinessException(ResultCode.BUSINESS_NO_SAFE, 
                    String.format("该论文撤回次数已达上限（最多%d次），请联系管理员", maxWithdrawCount));
            }
                
            // 5. 更新论文状态为已撤回
            String oldStatus = paperInfo.getPaperStatus();
            String newStatus = DictConstants.PaperStatus.WITHDRAWN;
            
            PaperInfo updateInfo = new PaperInfo();
            updateInfo.setId(paperId);
            updateInfo.setPaperStatus(newStatus);
            updateInfo.setUpdateTime(LocalDateTime.now());
            
            int result = paperInfoMapper.updateById(updateInfo);
            boolean success = result > 0;
            
            if (success) {
                // 记录状态变更日志
                paperStatusLogService.recordStatusLog(
                    paperId,
                    getStatusValue(oldStatus),
                    getStatusValue(newStatus),
                    "学生撤回论文: " + reason,
                    studentId,
                    null
                );
                
                // 5. 发送撤回成功通知
                sendPaperWithdrawSuccessNotification(paperInfo, studentId, reason);
                log.info("论文撤回成功 - 论文ID: {}", paperId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("论文撤回失败 - 论文ID: {}", paperId, e);
            throw new RuntimeException("论文撤回失败: " + e.getMessage());
        }
    }
    
    /**
     * 撤回后重新提交论文
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperInfo resubmitAfterWithdraw(Long paperId, PaperReSubmitAfterWithdrawRequest request, Long studentId) {
        try {
            PaperInfo updatedPaper = doResubmitAfterWithdraw(paperId, request, studentId);

            // 事务提交后再执行异步处理（避免自调用导致 @Transactional 失效）
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        asyncProcessPaperAfterSubmit(paperId, studentId);
                    }
                }
            );

            return updatedPaper;

        } catch (BusinessException e) {
            log.error("撤回后重新提交失败 - 论文 ID: {}", paperId, e);
            throw e;
        } catch (Exception e) {
            log.error("撤回后重新提交失败 - 论文 ID: {}", paperId, e);
            throw new RuntimeException("重新提交失败：" + e.getMessage());
        }
    }

    /**
     * 实际执行撤回后重新提交的核心逻辑（无事务注解，由调用方管理事务）
     */
    private PaperInfo doResubmitAfterWithdraw(Long paperId, PaperReSubmitAfterWithdrawRequest request, Long studentId) {
        log.info("开始撤回后重新提交 - 论文 ID: {}, 学生 ID: {}", paperId, studentId);
        
        // 1. 验证文件信息
        FileInfo fileInfo = fileService.getById(request.getFileId());
        if (fileInfo == null) {
            throw new BusinessException(ResultCode.BUSINESS_NO_SAFE, "文件不存在或已被删除");
        }
        
        // 2. 更新论文基本信息
        PaperInfo updatePaper = new PaperInfo();
        updatePaper.setId(paperId);
        updatePaper.setPaperTitle(request.getPaperTitle());
        updatePaper.setPaperAbstract(request.getPaperAbstract());
        updatePaper.setFileId(request.getFileId());
        updatePaper.setFileMd5(request.getFileMd5());

        // 3. 获取当前版本号
        Integer currentVersion = getCurrentVersion(paperId);
        Integer newVersion = currentVersion+1;
        
        // 4. 重置状态为已分配（论文已有教师，跳过待分配阶段）
        String oldStatus = "WITHDRAWN";
        String newStatus = DictConstants.PaperStatus.ASSIGNED;
        updatePaper.setPaperStatus(newStatus);
        updatePaper.setUpdateTime(LocalDateTime.now());
        
        int updateResult = paperInfoMapper.updateById(updatePaper);
        if (updateResult == 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR,"更新论文信息失败");
        }
        
        // 记录状态变更日志
        paperStatusLogService.recordStatusLog(
            paperId,
            getStatusValue(oldStatus),
            getStatusValue(newStatus),
            "学生撤回后重新提交论文",
            studentId,
            null
        );
        
        // 5. 创建新的提交记录
        PaperSubmit submitRecord = new PaperSubmit();
        submitRecord.setPaperId(paperId);
        submitRecord.setStudentId(studentId);
        submitRecord.setSubmitVersion(newVersion);
        submitRecord.setFileId(request.getFileId());
        submitRecord.setFileMd5(request.getFileMd5());
        submitRecord.setSubmitTime(LocalDateTime.now());
        submitRecord.setRemark("撤回后重新提交");
        int submitResult = paperSubmitMapper.insert(submitRecord);
        if (submitResult == 0) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR,"创建提交记录失败");
        }
        
        PaperInfo updatedPaper = paperInfoMapper.selectById(paperId);
        log.info("撤回后重新提交成功 - 论文 ID: {}, 新版本号：{}", paperId, newVersion);
        
        return updatedPaper;
    }
    
    /**
     * 申请修改已通过论文接口实现
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean requestPaperModification(Long paperId, Long studentId, String reason) {
        try {
            log.info("开始申请修改论文 - 论文ID: {}, 学生ID: {}, 原因: {}", paperId, studentId, reason);
            
            // 1. 验证论文信息
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                log.warn("论文不存在 - 论文ID: {}", paperId);
                return false;
            }
            
            // 2. 验证论文归属
            if (!paperInfo.getStudentId().equals(studentId)) {
                log.warn("无权限申请修改他人论文 - 论文ID: {}, 学生ID: {}", paperId, studentId);
                return false;
            }
            
            // 3. 验证论文状态：只有已通过的论文才能申请修改
            if (!DictConstants.PaperStatus.COMPLETED.equals(paperInfo.getPaperStatus())) {
                String statusLabel = getPaperStatusLabel(paperInfo.getPaperStatus());
                log.warn("论文状态不允许申请修改 - 论文ID: {}, 状态: {}", paperId, statusLabel);
                return false;
            }
            
            // 4. 更新论文状态为待审核（申请修改状态）
            String oldStatus = paperInfo.getPaperStatus();
            String newStatus = DictConstants.PaperStatus.AUDITING; // 或者自定义一个申请修改状态
            
            PaperInfo updateInfo = new PaperInfo();
            updateInfo.setId(paperId);
            updateInfo.setPaperStatus(newStatus);
            updateInfo.setUpdateTime(LocalDateTime.now());
            
            int result = paperInfoMapper.updateById(updateInfo);
            boolean success = result > 0;
            
            if (success) {
                // 记录状态变更日志
                paperStatusLogService.recordStatusLog(
                    paperId,
                    getStatusValue(oldStatus),
                    getStatusValue(newStatus),
                    "学生申请修改论文: " + reason,
                    studentId,
                    null
                );
                
                // 5. 发送申请修改通知
                sendPaperModifyRequestNotification(paperInfo, studentId, reason);
                log.info("申请修改论文成功 - 论文ID: {}", paperId);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("申请修改论文失败 - 论文ID: {}", paperId, e);
            throw new RuntimeException("申请修改失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量下载论文接口实现
     */
    @Override
    public void batchDownloadPapers(List<Long> paperIds, Long studentId, HttpServletResponse response) {
        try {
            log.info("开始批量下载论文 - 学生ID: {}, 论文数量: {}", studentId, paperIds.size());
            
            // 1. 验证论文权限
            List<PaperInfo> papers = validateAndFilterPapers(paperIds, studentId);
            if (papers.isEmpty()) {
                throw new RuntimeException("没有可下载的论文");
            }
            
            // 2. 设置响应头
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=papers_" + 
                System.currentTimeMillis() + ".zip");
            
            // 3. 创建ZIP输出流
            try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
                for (PaperInfo paper : papers) {
                    FileInfo fileInfo = fileInfoMapper.selectById(paper.getFileId());
                    if (fileInfo != null && StringUtils.hasText(fileInfo.getStoragePath())) {
                        // 添加文件到ZIP
                        ZipEntry zipEntry = new ZipEntry(paper.getPaperTitle() + ".pdf");
                        zipOut.putNextEntry(zipEntry);
                        
                        // 从文件系统读取文件内容并写入ZIP
                        String fullPath = Paths.get(uploadBasePath, fileInfo.getStoragePath()).toString();
                        File file = new File(fullPath);
                        if (file.exists()) {
                            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = fis.read(buffer)) > 0) {
                                    zipOut.write(buffer, 0, len);
                                }
                            }
                        } else {
                            // 文件不存在时写入占位符
                            zipOut.write("文件不存在".getBytes(StandardCharsets.UTF_8));
                        }
                        zipOut.closeEntry();
                    }
                }
            }
            
            log.info("批量下载论文完成 - 学生ID: {}, 成功下载: {}篇", studentId, papers.size());
            
        } catch (Exception e) {
            log.error("批量下载论文失败 - 学生ID: {}", studentId, e);
            throw new RuntimeException("批量下载失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除论文接口实现
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchDeletePapers(List<Long> paperIds, Long studentId) {
        Map<String, Object> result = new HashMap<>();
        List<Long> deletedIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        
        try {
            log.info("开始批量删除论文 - 学生ID: {}, 论文数量: {}", studentId, paperIds.size());
            
            for (Long paperId : paperIds) {
                try {
                    if (deletePaper(paperId, studentId)) {
                        deletedIds.add(paperId);
                    } else {
                        failedIds.add(paperId);
                    }
                } catch (Exception e) {
                    log.error("删除论文失败 - 论文ID: {}", paperId, e);
                    failedIds.add(paperId);
                }
            }
            
            result.put("deletedCount", deletedIds.size());
            result.put("failedIds", failedIds);
            result.put("success", failedIds.isEmpty());
            
            log.info("批量删除论文完成 - 学生ID: {}, 成功: {}, 失败: {}", 
                studentId, deletedIds.size(), failedIds.size());
            
        } catch (Exception e) {
            log.error("批量删除论文失败 - 学生ID: {}", studentId, e);
            throw new RuntimeException("批量删除失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取论文所有提交版本列表
     */
    @Override
    public List<PaperSubmitDTO> getPaperVersions(Long paperId, Long studentId) {
        return paperVersionService.getPaperVersions(paperId, studentId);
    }

    @Override
    public PaperVersionDTO getPaperVersion(Long paperId, Long versionId, Long studentId) {
        return paperVersionService.getPaperVersion(paperId, versionId, studentId);
    }

    @Override
    public VersionCompareResult comparePaperVersions(Long paperId, List<Long> versionIds, Long studentId) {
        return paperVersionService.comparePaperVersions(paperId, versionIds, studentId);
    }

    @Override
    public void downloadVersionCompareReport(Long paperId, List<Long> versionIds, Long studentId, HttpServletResponse response) {
        paperVersionService.downloadVersionCompareReport(paperId, versionIds, studentId, response);
    }

    @Override
    public void downloadPaperVersion(Long versionId, Long studentId, HttpServletResponse response) {
        paperVersionService.downloadPaperVersion(versionId, studentId, response);
    }
    
    /**
     * 下载附件接口实现
     */
    @Override
    public void downloadAttachment(String attachmentId, Long studentId, HttpServletResponse response) {
        try {
            log.info("开始下载附件 - 附件ID: {}, 学生ID: {}", attachmentId, studentId);
            
            // 1. 验证附件存在且属于该学生
            PaperAttachment attachment = paperAttachmentMapper.selectById(attachmentId);
            if (attachment == null) {
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "附件不存在");
            }
            if (!attachment.getStudentId().equals(studentId)) {
                throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限访问该附件");
            }
                        
            // 2. 下载文件 - 直接操作 response
            String fullPath = Paths.get(uploadBasePath, attachment.getStoragePath()).toString();
            File file = new File(fullPath);
            
            if (!file.exists()) {
                throw new RuntimeException("附件文件不存在");
            }
            
            // 设置响应头
            String fileName = attachment.getOriginalFilename() != null ? 
                attachment.getOriginalFilename() : "attachment_" + attachmentId;
            response.setContentType(FileMimeTypeUtils.getContentType(fileName));
            response.setHeader("Content-Disposition", 
                "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
            response.setContentLengthLong(file.length());
            
            // 写入文件内容
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    response.getOutputStream().write(buffer, 0, len);
                }
                response.getOutputStream().flush();
            }
            
            log.info("附件下载完成 - 附件ID: {}", attachmentId);
            
        } catch (Exception e) {
            log.error("下载附件失败 - 附件ID: {}", attachmentId, e);
            throw new RuntimeException("下载附件失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载论文接口实现
     */
    @Override
    public void downloadPaper(Long paperId, Long studentId, HttpServletResponse response) {
        try {
            log.info("开始下载论文 - 论文ID: {}, 学生ID: {}", paperId, studentId);
            
            // 1. 查询论文信息
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                throw new RuntimeException("论文不存在");
            }
            
            // 2. 验证论文归属
            if (!paperInfo.getStudentId().equals(studentId)) {
                throw new RuntimeException("无权限下载此论文");
            }
            
            // 3. 检查论文状态（只有通过审核的论文才能下载）
            if (!"completed".equals(paperInfo.getPaperStatus())) {
                throw new RuntimeException("论文未通过审核，无法下载");
            }
            
            // 4. 下载文件 - 直接操作response
            FileInfo fileInfo = fileService.getById(paperInfo.getFileId());
            if (fileInfo != null && StringUtils.hasText(fileInfo.getStoragePath())) {
                String fullPath = Paths.get(uploadBasePath, fileInfo.getStoragePath()).toString();
                File file = new File(fullPath);
                
                if (file.exists()) {
                    // 设置响应头
                    String fileName = fileInfo.getOriginalFilename() != null ? 
                        fileInfo.getOriginalFilename() : "paper_" + paperId + ".pdf";
                    response.setContentType(FileMimeTypeUtils.getContentType(fileName));
                    response.setHeader("Content-Disposition", 
                        "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
                    response.setContentLengthLong(file.length());
                    
                    // 写入文件内容
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            response.getOutputStream().write(buffer, 0, len);
                        }
                        response.getOutputStream().flush();
                    }
                    
                    log.info("论文下载完成 - 论文ID: {}, 文件名: {}", paperId, fileName);
                } else {
                    throw new RuntimeException("论文文件不存在");
                }
            } else {
                throw new RuntimeException("论文文件信息不存在");
            }
            
        } catch (Exception e) {
            log.error("下载论文失败 - 论文ID: {}", paperId, e);
            throw new RuntimeException("下载论文失败: " + e.getMessage());
        }
    }
        
    /**
     * 验证并过滤论文列表
     */
    private List<PaperInfo> validateAndFilterPapers(List<Long> paperIds, Long studentId) {
        if (paperIds == null || paperIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // 批量查询避免 N+1
        List<PaperInfo> papers = paperInfoMapper.selectBatchIds(paperIds);
        return papers.stream()
                .filter(p -> p != null && p.getStudentId().equals(studentId) && p.getIsDeleted() == 0)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 发送论文撤回成功通知
     */
    private void sendPaperWithdrawSuccessNotification(PaperInfo paperInfo, Long studentId, String reason) {
        try {
            String title = "论文撤回成功";
            String content = String.format("您的论文《%s》已成功撤回。%s",
                paperInfo.getPaperTitle(),
                StringUtils.hasText(reason) ? "撤回原因：" + reason : "");
            
            internalMessageNotificationService.sendSystemNotice(
                studentId,
                title,
                content,
                PaperNoticeConstants.NOTICE_TYPE_PAPER_WITHDRAW_SUCCESS,
                paperInfo.getId().toString()
            );
        } catch (Exception e) {
            log.error("发送论文撤回成功通知失败", e);
        }
    }
    
    /**
     * 发送申请修改论文通知
     */
    private void sendPaperModifyRequestNotification(PaperInfo paperInfo, Long studentId, String reason) {
        try {
            String title = "修改申请已提交";
            String content = String.format("您对论文《%s》的修改申请已提交，等待导师审核。%s",
                paperInfo.getPaperTitle(),
                StringUtils.hasText(reason) ? "申请原因：" + reason : "");
            
            internalMessageNotificationService.sendSystemNotice(
                studentId,
                title,
                content,
                PaperNoticeConstants.NOTICE_TYPE_PAPER_MODIFY_REQUEST,
                paperInfo.getId().toString()
            );
        } catch (Exception e) {
            log.error("发送申请修改论文通知失败", e);
        }
    }
    
    // ==================== 查重历史相关方法 ====================
    
    @Override
    public CheckHistoryResponseDTO getCheckHistory(Long paperId, Long studentId) {
        return paperCheckHistoryService.getCheckHistory(paperId, studentId);
    }

    @Override
    public SimilarityTrendDTO getSimilarityTrend(Long paperId, Long studentId, Integer period) {
        return paperCheckHistoryService.getSimilarityTrend(paperId, studentId, period);
    }

    @Override
    public VersionCompareResponseDTO compareVersions(Long paperId, Long studentId, VersionCompareRequestDTO request) {
        return paperCheckHistoryService.compareVersions(paperId, studentId, request);
    }

    @Override
    public StatisticsDTO getPaperStatistics(Long paperId, Long studentId) {
        return paperCheckHistoryService.getPaperStatistics(paperId, studentId);
    }
    
    /**
     * 上传附件接口实现
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperAttachment uploadAttachment(Long paperId, MultipartFile file, String attachmentType, Long studentId) {
        return paperAttachmentService.uploadAttachment(paperId, file, attachmentType, studentId);
    }

    @Override
    public List<PaperAttachment> getPaperAttachments(Long paperId, Long studentId) {
        return paperAttachmentService.getPaperAttachments(paperId, studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAttachment(Long attachmentId, Long studentId) {
        return paperAttachmentService.deleteAttachment(attachmentId, studentId);
    }

    /**
     * 获取专业列表接口实现
     */
    @Override
    public List<Major> getMajorList() {
        try {
            log.info("获取专业列表");
            // 从数据库中查询所有专业
            List<Major> majorList = majorMapper.selectList(
                new LambdaQueryWrapper<Major>()
                    .eq(Major::getIsDeleted, 0)
                    .orderByAsc(Major::getMajorName)
            );
            log.info("获取专业列表成功，共 {} 个专业", majorList.size());
            return majorList;
        } catch (Exception e) {
            log.error("获取专业列表失败", e);
            throw new RuntimeException("获取专业列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新论文信息接口实现
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperInfo updatePaper(Long paperId, PaperSubmitRequest request, Long studentId) {
        try {
            PaperInfo paperInfo = doUpdatePaper(paperId, request, studentId);

            // 事务提交后再执行异步处理
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        asyncProcessPaperAfterSubmit(paperId, studentId);
                    }
                }
            );

            log.info("论文更新成功 - 论文ID: {}", paperId);
            return paperInfo;

        } catch (Exception e) {
            log.error("更新论文失败 - 论文ID: {}", paperId, e);
            throw new RuntimeException("更新论文失败: " + e.getMessage());
        }
    }

    /**
     * 实际执行更新论文的核心逻辑（无事务注解，由调用方管理事务）
     */
    private PaperInfo doUpdatePaper(Long paperId, PaperSubmitRequest request, Long studentId) {
        log.info("开始更新论文 - 论文ID: {}, 学生ID: {}, 请求参数: {}", paperId, studentId, request);

        // 1. 验证论文信息
        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null) {
            throw new RuntimeException("论文不存在");
        }

        // 2. 验证论文归属
        if (!paperInfo.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权限更新他人论文");
        }

        // 3. 验证论文状态：只有待处理状态的论文可以更新
        if (!DictConstants.PaperStatus.PENDING.equals(paperInfo.getPaperStatus())) {
            String statusLabel = getPaperStatusLabel(paperInfo.getPaperStatus());
            throw new RuntimeException("当前论文状态为【" + statusLabel + "】，不允许更新");
        }

        // 4. 验证文件信息
        validateFileInfo(request.getFileId(), request.getFileMd5());

        // 5. 更新论文信息
        String oldStatus = paperInfo.getPaperStatus();
        String newStatus = DictConstants.PaperStatus.PENDING; // 保持待处理状态

        paperInfo.setSubjectCode(request.getSubjectCode());
        paperInfo.setPaperTitle(request.getPaperTitle());
        paperInfo.setPaperAbstract(request.getPaperAbstract());
        paperInfo.setCollegeId(request.getCollegeId());
        paperInfo.setMajorId(request.getMajorId());
        paperInfo.setPaperType(request.getPaperType());
        FileInfo fileInfo = fileInfoMapper.selectById(request.getFileId());
        paperInfo.setFileId(request.getFileId());
        paperInfo.setFileMd5(request.getFileMd5());
        paperInfo.setFilePath(fileInfo.getStoragePath());
        paperInfo.setWordCount(fileInfo.getWordCount());
        paperInfo.setPageCount(fileInfo.getPageCount());//论文页数
        paperInfo.setUpdateTime(LocalDateTime.now());

        int result = paperInfoMapper.updateById(paperInfo);
        if (result <= 0) {
            throw new RuntimeException("更新论文信息失败");
        }

        // 6. 记录状态变更日志
        paperStatusLogService.recordStatusLog(
            paperId,
            getStatusValue(oldStatus),
            getStatusValue(newStatus),
            "学生更新论文信息",
            studentId,
            null
        );

        // 7. 创建新的提交记录
        createPaperSubmitRecord(paperInfo, request.getFileId(), request.getFileMd5(), studentId);

        return paperInfo;
    }
}