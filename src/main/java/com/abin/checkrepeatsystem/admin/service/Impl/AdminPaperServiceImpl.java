package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.service.AdminPaperService;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.enums.CheckStatusFilterEnum;
import com.abin.checkrepeatsystem.student.mapper.MajorMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.Major;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.pojo.entity.StudentInfo;
import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.pojo.entity.FileInfo;
import com.abin.checkrepeatsystem.detection.service.EnhancedSimilarityDetectionService;
import com.abin.checkrepeatsystem.detection.dto.SimilarityDetectionResult;
import com.abin.checkrepeatsystem.user.service.StudentInfoService;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.http.HttpResponse;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员论文管理服务实现类
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class AdminPaperServiceImpl implements AdminPaperService {

    private final PaperInfoMapper paperInfoMapper;

    private final SysUserMapper sysUserMapper;

    private final MajorMapper majorMapper;

    private final FileService fileService;
    
    private final EnhancedSimilarityDetectionService detectionService;
    
    private final StudentInfoService studentInfoService;

    private final ObjectMapper objectMapper;

    @Override
    public Result<Page<PaperInfo>> getPaperList(Integer page, Integer size, String paperStatus, 
                                              String paperType, String keyword, String startDate, String endDate,
                                              Long collegeId, Long majorId, String majorName, String grade, String checkStatus,
                                              Double minSimilarity, Double maxSimilarity) {
        try {
            Page<PaperInfo> paperPage = new Page<>(page, size);
            LambdaQueryWrapper<PaperInfo> wrapper = new LambdaQueryWrapper<>();
            
            // 【新增】默认排除已撤回的论文（除非明确指定要包含）
            wrapper.ne(PaperInfo::getPaperStatus, DictConstants.PaperStatus.WITHDRAWN);
            
            // 状态筛选
            if (paperStatus != null && !paperStatus.isEmpty()) {
                wrapper.eq(PaperInfo::getPaperStatus, paperStatus);
            }
            
            // 类型筛选
            if (paperType != null && !paperType.isEmpty()) {
                wrapper.eq(PaperInfo::getPaperType, paperType);
            }
            
            // 关键词搜索（标题、作者、论文关键词等）
            if (keyword != null && !keyword.isEmpty()) {
                List<Long> matchedUserIds = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                        .like(SysUser::getRealName, keyword)
                        .eq(SysUser::getIsDeleted, 0)
                        .select(SysUser::getId)
                ).stream().map(SysUser::getId).collect(Collectors.toList());

                wrapper.and(w -> {
                    w.like(PaperInfo::getPaperTitle, keyword)
                     .or()
                     .like(PaperInfo::getAuthor, keyword)
                     .or()
                     .like(PaperInfo::getPaperAbstract, keyword);
                    if (!matchedUserIds.isEmpty()) {
                        w.or().in(PaperInfo::getStudentId, matchedUserIds);
                    }
                });
            }
            
            // 时间范围筛选
            if (startDate != null && !startDate.isEmpty()) {
                wrapper.ge(PaperInfo::getCreateTime, LocalDateTime.parse(startDate));
            }
            if (endDate != null && !endDate.isEmpty()) {
                wrapper.le(PaperInfo::getCreateTime, LocalDateTime.parse(endDate));
            }
            
            // 学院筛选（直接使用PaperInfo表中的college_id字段）
            if (collegeId != null) {
                wrapper.eq(PaperInfo::getCollegeId, collegeId);
            }
            
            // 专业ID筛选（直接使用PaperInfo表中的major_id字段）
            if (majorId != null) {
                wrapper.eq(PaperInfo::getMajorId, majorId);
            }
            
            // 专业名称筛选（通过专业名称关联查询）
            if (majorName != null && !majorName.isEmpty()) {
                List<Long> matchedMajorIds = majorMapper.selectList(
                    new LambdaQueryWrapper<Major>()
                        .like(Major::getMajorName, majorName)
                        .eq(Major::getIsDeleted, 0)
                        .select(Major::getId)
                ).stream().map(Major::getId).collect(Collectors.toList());
                if (!matchedMajorIds.isEmpty()) {
                    wrapper.in(PaperInfo::getMajorId, matchedMajorIds);
                } else {
                    wrapper.eq(PaperInfo::getMajorId, -1L);
                }
            }
            
            // 年级筛选（通过StudentInfo表查询对应年级的学生ID）
            if (grade != null && !grade.isEmpty()) {
                List<StudentInfo> matchedStudents = studentInfoService.lambdaQuery()
                    .eq(StudentInfo::getGrade, grade)
                    .eq(StudentInfo::getIsDeleted, 0)
                    .select(StudentInfo::getUserId)
                    .list();
                List<Long> matchedStudentIds = matchedStudents.stream()
                    .map(StudentInfo::getUserId).collect(Collectors.toList());
                if (!matchedStudentIds.isEmpty()) {
                    wrapper.in(PaperInfo::getStudentId, matchedStudentIds);
                } else {
                    wrapper.eq(PaperInfo::getStudentId, -1L);
                }
            }
            
            // 查重状态筛选
            if (checkStatus != null && !checkStatus.isEmpty()) {
                addCheckStatusCondition(wrapper, checkStatus);
            }
            
            // 相似度范围筛选（数值范围）
            if (minSimilarity != null || maxSimilarity != null) {
                addSimilarityRangeByValue(wrapper, minSimilarity, maxSimilarity);
            }
            
            // 排除已删除的论文
            wrapper.eq(PaperInfo::getIsDeleted, 0);
            wrapper.orderByDesc(PaperInfo::getCreateTime);
            
            Page<PaperInfo> resultPage = paperInfoMapper.selectPage(paperPage, wrapper);
            
            // 补充关联信息
            enhancePaperList(resultPage.getRecords());
            
            log.info("管理员获取论文列表成功: 总数={}", resultPage.getTotal());
            return Result.success("论文列表获取成功", resultPage);
        } catch (Exception e) {
            log.error("获取论文列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取论文列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<PaperInfo> getPaperDetail(Long paperId) {
        try {
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
                return Result.error(ResultCode.PARAM_ERROR, "论文不存在");
            }
            
            // 补充详细信息
            enhancePaperDetail(paperInfo);
            
            log.info("获取论文详情成功: paperId={}", paperId);
            return Result.success("论文详情获取成功", paperInfo);
        } catch (Exception e) {
            log.error("获取论文详情失败: paperId={}", paperId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取论文详情失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> auditPaper(Long paperId, String auditResult, String auditComment) {
        try {
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
                return Result.error(ResultCode.PARAM_ERROR, "论文不存在");
            }
            
            // 更新审核状态
            if ("approved".equals(auditResult)) {
                paperInfo.setPaperStatus(DictConstants.PaperStatus.COMPLETED);
            } else if ("rejected".equals(auditResult)) {
                paperInfo.setPaperStatus(DictConstants.PaperStatus.REJECTED);
            } else {
                return Result.error(ResultCode.PARAM_ERROR, "无效的审核结果");
            }
            
            paperInfo.setCheckResult(auditComment);
            paperInfo.setUpdateTime(LocalDateTime.now());
            
            paperInfoMapper.updateById(paperInfo);
            
            log.info("论文审核成功: paperId={}, result={}", paperId, auditResult);
            return Result.success("论文审核成功");
        } catch (Exception e) {
            log.error("论文审核失败: paperId={}", paperId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "论文审核失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> batchAuditPapers(List<Long> paperIds, String auditResult, String auditComment) {
        try {
            int successCount = 0;
            int failCount = 0;
            
            for (Long paperId : paperIds) {
                try {
                    Result<String> result = auditPaper(paperId, auditResult, auditComment);
                    if (result.getCode() == 200) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    log.error("批量审核单个论文失败: paperId={}", paperId, e);
                    failCount++;
                }
            }
            
            String message = String.format("批量审核完成: 成功%d个, 失败%d个", successCount, failCount);
            log.info(message);
            
            if (successCount > 0) {
                return Result.success(message);
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "批量审核全部失败");
            }
        } catch (Exception e) {
            log.error("批量审核论文失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量审核论文失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> deletePaper(Long paperId) {
        try {
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
                return Result.error(ResultCode.PARAM_ERROR, "论文不存在");
            }
            
            // 软删除
            paperInfo.setIsDeleted(1);
            paperInfo.setUpdateTime(LocalDateTime.now());
            paperInfoMapper.updateById(paperInfo);
            
            log.info("论文删除成功: paperId={}", paperId);
            return Result.success("论文删除成功");
        } catch (Exception e) {
            log.error("论文删除失败: paperId={}", paperId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "论文删除失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> batchDeletePapers(List<Long> paperIds) {
        try {
            if (paperIds == null || paperIds.isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR, "论文ID列表不能为空");
            }
            int successCount = 0;
            for (Long paperId : paperIds) {
                PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
                if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
                    continue;
                }
                paperInfo.setIsDeleted(1);
                paperInfo.setUpdateTime(LocalDateTime.now());
                paperInfoMapper.updateById(paperInfo);
                successCount++;
            }
            log.info("批量论文删除成功: 请求{}篇, 成功{}篇", paperIds.size(), successCount);
            return Result.success(String.format("批量删除成功，共处理%d篇论文", successCount));
        } catch (Exception e) {
            log.error("批量论文删除失败: paperIds={}", paperIds, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量论文删除失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> getPaperStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 总论文数
            Long totalPapers = paperInfoMapper.selectCount(
                new LambdaQueryWrapper<PaperInfo>().eq(PaperInfo::getIsDeleted, 0));
            Long withdraw = paperInfoMapper.selectCount(
                    new LambdaQueryWrapper<PaperInfo>().eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.WITHDRAWN)
                            .eq(PaperInfo::getIsDeleted, 0));
            stats.put("totalPapers", totalPapers- withdraw);

            // 已查重
            Long checked = paperInfoMapper.selectCount(
                    new LambdaQueryWrapper<PaperInfo>().eq(
                            PaperInfo::getCheckCompleted, 1)
                            .ne(PaperInfo::getPaperStatus, DictConstants.PaperStatus.WITHDRAWN)
                    .eq(PaperInfo::getIsDeleted, 0)
            );
            stats.put("checked", checked);

            // 各状态论文数
            Map<String, Long> statusStats = new HashMap<>();
            statusStats.put("pending", paperInfoMapper.selectCount(
                new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.PENDING)
                    .eq(PaperInfo::getIsDeleted, 0)));
            statusStats.put("assigned", paperInfoMapper.selectCount(
                new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.ASSIGNED)
                    .eq(PaperInfo::getIsDeleted, 0)));
            statusStats.put("checking", paperInfoMapper.selectCount(
                new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.CHECKING)
                    .eq(PaperInfo::getIsDeleted, 0)));
            statusStats.put("auditing", paperInfoMapper.selectCount(
                new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.AUDITING)
                    .eq(PaperInfo::getIsDeleted, 0)));
            statusStats.put("completed", paperInfoMapper.selectCount(
                new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.COMPLETED)
                    .eq(PaperInfo::getIsDeleted, 0)));
            statusStats.put("rejected", paperInfoMapper.selectCount(
                new LambdaQueryWrapper<PaperInfo>()
                    .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.REJECTED)
                    .eq(PaperInfo::getIsDeleted, 0)));
            
            stats.put("statusStats", statusStats);
            
            // 今日新增论文数
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            Long todayPapers = paperInfoMapper.selectCount(
                new LambdaQueryWrapper<PaperInfo>()
                    .ge(PaperInfo::getCreateTime, todayStart)
                    .eq(PaperInfo::getIsDeleted, 0));
            stats.put("todayPapers", todayPapers);
            
            log.info("获取论文统计信息成功");
            return Result.success("论文统计信息获取成功", stats);
        } catch (Exception e) {
            log.error("获取论文统计信息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取论文统计信息失败: " + e.getMessage());
        }
    }

    @Override
    public void exportPaperList(Map<String, Object> params, jakarta.servlet.http.HttpServletResponse response) {
        log.info("开始导出论文列表: params={}", params);
        try {
            // 从参数中提取筛选条件
            Long collegeId = null;
            if (params.get("collegeId") != null && !"" .equals(params.get("collegeId").toString().trim())) {
                collegeId = Long.parseLong(params.get("collegeId").toString());
            }
            Long majorId = null;
            if (params.get("majorId") != null && !"" .equals(params.get("majorId").toString().trim())) {
                majorId = Long.parseLong(params.get("majorId").toString());
            }
            String grade = params.get("grade") != null ? params.get("grade").toString() : null;
            String checkStatus = params.get("checkStatus") != null ? params.get("checkStatus").toString() : null;
            String keyword = params.get("keyword") != null ? params.get("keyword").toString() : null;
            
            // 构建查询条件
            LambdaQueryWrapper<PaperInfo> wrapper = new LambdaQueryWrapper<>();
            
            // 默认排除已撤回的论文
            wrapper.ne(PaperInfo::getPaperStatus, DictConstants.PaperStatus.WITHDRAWN);
            
            // 学院筛选
            if (collegeId != null) {
                wrapper.eq(PaperInfo::getCollegeId, collegeId);
            }
            
            // 专业筛选
            if (majorId != null) {
                wrapper.eq(PaperInfo::getMajorId, majorId);
            }
            
            // 年级筛选
            if (grade != null && !grade.isEmpty()) {
                List<Long> gradeStudentIds = studentInfoService.lambdaQuery()
                    .eq(StudentInfo::getGrade, grade)
                    .eq(StudentInfo::getIsDeleted, 0)
                    .select(StudentInfo::getUserId)
                    .list().stream().map(StudentInfo::getUserId).collect(Collectors.toList());
                if (!gradeStudentIds.isEmpty()) {
                    wrapper.in(PaperInfo::getStudentId, gradeStudentIds);
                } else {
                    wrapper.eq(PaperInfo::getStudentId, -1L);
                }
            }

            // 查重状态筛选
            if (checkStatus != null && !checkStatus.isEmpty()) {
                addCheckStatusCondition(wrapper, checkStatus);
            }

            // 关键词搜索
            if (keyword != null && !keyword.isEmpty()) {
                List<Long> keywordMatchedUserIds = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                        .like(SysUser::getRealName, keyword)
                        .eq(SysUser::getIsDeleted, 0)
                        .select(SysUser::getId)
                ).stream().map(SysUser::getId).collect(Collectors.toList());

                wrapper.and(w -> {
                    w.like(PaperInfo::getPaperTitle, keyword)
                     .or()
                     .like(PaperInfo::getAuthor, keyword)
                     .or()
                     .like(PaperInfo::getPaperAbstract, keyword);
                    if (!keywordMatchedUserIds.isEmpty()) {
                        w.or().in(PaperInfo::getStudentId, keywordMatchedUserIds);
                    }
                });
            }
            
            // 排除已删除的论文
            wrapper.eq(PaperInfo::getIsDeleted, 0);
            wrapper.orderByDesc(PaperInfo::getCreateTime);
            
            // 查询论文数据
            List<PaperInfo> papers = paperInfoMapper.selectList(wrapper);
            
            // 补充关联信息
            enhancePaperList(papers);
            
            log.info("查询到论文数据: 数量={}", papers.size());
            
            // 准备Excel导出数据
            List<PaperExportVo> exportData = new ArrayList<>();
            for (PaperInfo paper : papers) {
                PaperExportVo vo = new PaperExportVo();
                vo.setPaperTitle(paper.getPaperTitle());
                vo.setAuthor(paper.getAuthor());
                vo.setStudentName(paper.getStudentName());
                vo.setStudentCollege(paper.getStudentCollege());
                vo.setStudentMajor(paper.getStudentMajor());
                vo.setStudentGrade(paper.getStudentGrade());
                vo.setPaperType(paper.getPaperType());
                vo.setPaperStatus(getPaperStatusText(paper.getPaperStatus()));
                vo.setSimilarityRate(paper.getSimilarityRate() != null ? paper.getSimilarityRate() + "%" : "未查重");
                vo.setCheckStatus(paper.getCheckCompleted() != null && paper.getCheckCompleted() == 1 ? "已完成" : "未完成");
                vo.setCreateTime(paper.getCreateTime() != null ? paper.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                vo.setCheckTime(paper.getCheckTime() != null ? paper.getCheckTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                exportData.add(vo);
            }
            
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String fileName = URLEncoder.encode("论文库导出_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName);
            
            // 生成Excel并输出
            EasyExcel.write(response.getOutputStream(), PaperExportVo.class)
                    .sheet("论文列表")
                    .doWrite(exportData);
            
            log.info("论文列表导出成功: 导出数量={}", exportData.size());
        } catch (Exception e) {
            log.error("导出论文列表失败: {}", e.getMessage(), e);
            try {
                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("导出失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("发送错误响应失败", ioException);
            }
        }
    }
    
    /**
     * 论文导出VO类
     */
    @Data
    private static class PaperExportVo {
        @ExcelProperty("论文标题")
        private String paperTitle;
        
        @ExcelProperty("作者")
        private String author;
        
        @ExcelProperty("学生姓名")
        private String studentName;
        
        @ExcelProperty("学院")
        private String studentCollege;
        
        @ExcelProperty("专业")
        private String studentMajor;
        
        @ExcelProperty("年级")
        private String studentGrade;
        
        @ExcelProperty("论文类型")
        private String paperType;
        
        @ExcelProperty("状态")
        private String paperStatus;
        
        @ExcelProperty("相似度")
        private String similarityRate;
        
        @ExcelProperty("查重状态")
        private String checkStatus;
        
        @ExcelProperty("提交时间")
        private String createTime;
        
        @ExcelProperty("查重时间")
        private String checkTime;
    }
    
    /**
     * 获取论文状态文本
     */
    private String getPaperStatusText(String status) {
        switch (status) {
            case DictConstants.PaperStatus.PENDING:
                return "待分配";
            case DictConstants.PaperStatus.ASSIGNED:
                return "已分配";
            case DictConstants.PaperStatus.CHECKING:
                return "查重中";
            case DictConstants.PaperStatus.AUDITING:
                return "审核中";
            case DictConstants.PaperStatus.COMPLETED:
                return "已完成";
            case DictConstants.PaperStatus.REJECTED:
                return "已拒绝";
            case DictConstants.PaperStatus.WITHDRAWN:
                return "已撤回";
            default:
                return status;
        }
    }
    
    @Override
    public Result<String> downloadPaper(Long paperId) {
        try {
            // 获取论文信息
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
                return Result.error(ResultCode.PARAM_ERROR, "论文不存在");
            }
            
            // 检查是否有文件
            if (paperInfo.getFileId() == null) {
                return Result.error(ResultCode.PARAM_ERROR, "论文文件不存在");
            }
            
            // 获取文件信息
            FileInfo fileInfo = fileService.getById(paperInfo.getFileId());
            if (fileInfo == null) {
                return Result.error(ResultCode.PARAM_ERROR, "文件信息不存在");
            }
            
            // 构造文件下载链接
            String downloadUrl = "/api/v1/file/download/" + paperInfo.getFileId();
            
            log.info("论文文件下载准备就绪: paperId={}, fileName={}", paperId, fileInfo.getOriginalFilename());
            return Result.success("论文文件下载链接生成成功", downloadUrl);
            
        } catch (Exception e) {
            log.error("论文文件下载失败: paperId={}", paperId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "论文文件下载失败: " + e.getMessage());
        }
    }
    
    @Override
    public Result<String> schoolInternalCheckPaper(Long paperId) {
        try {
            // 获取论文信息
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
                return Result.error(ResultCode.PARAM_ERROR, "论文不存在");
            }
            
            // 更新论文查重标记
            updatePaperCheckFlags(paperInfo, "school", "local");
            
            // 执行内部查重检测
            Result<SimilarityDetectionResult> detectionResult = detectionService.detectPaperSimilarity(paperId, null);
            
            if (detectionResult.getCode() == 200) {
                SimilarityDetectionResult result = detectionResult.getData();
                
                // 更新查重完成状态和相似度
                updatePaperCheckCompletion(paperInfo, result.getOverallSimilarity(), true);
                
                log.info("校内查重检测完成: paperId={}, similarity={}%, segments={}", 
                        paperId, result.getOverallSimilarity(), result.getSimilarSegments().size());
                return Result.success("校内查重检测完成", "查重相似度: " + result.getOverallSimilarity() + "%");
            } else {
                // 查重失败，更新失败状态
                updatePaperCheckCompletion(paperInfo, null, false);
                return Result.error(detectionResult.getCode(), "查重检测失败: " + detectionResult.getMessage());
            }
            
        } catch (Exception e) {
            log.error("校内查重检测失败：paperId={}", paperId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "校内查重检测失败：" + e.getMessage());
        }
    }
        
    @Override
    public Result<String> batchSchoolInternalCheckPaper(List<Long> paperIds) {
        try {
            if (paperIds == null || paperIds.isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR, "论文 ID 列表不能为空");
            }
                
            if (paperIds.size() > 20) {
                return Result.error(ResultCode.PARAM_ERROR, "单次批量查重最多支持 20 篇论文");
            }
                
            int successCount = 0;
            int failCount = 0;
            List<String> failReasons = new ArrayList<>();
                
            for (Long paperId : paperIds) {
                try {
                    PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
                    if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
                        failCount++;
                        failReasons.add("论文 ID " + paperId + " 不存在");
                        continue;
                    }
                        
                    // 更新论文查重标记
                    updatePaperCheckFlags(paperInfo, "school", "local");
                        
                    // 执行内部查重检测
                    Result<SimilarityDetectionResult> detectionResult = detectionService.detectPaperSimilarity(paperId, null);
                        
                    if (detectionResult.getCode() == 200) {
                        SimilarityDetectionResult result = detectionResult.getData();
                        updatePaperCheckCompletion(paperInfo, result.getOverallSimilarity(), true);
                        successCount++;
                        log.info("批量查重 - 论文 ID: {}, 相似度：{}%", paperId, result.getOverallSimilarity());
                    } else {
                        failCount++;
                        failReasons.add("论文 ID " + paperId + ": " + detectionResult.getMessage());
                        updatePaperCheckCompletion(paperInfo, null, false);
                    }
                } catch (Exception e) {
                    failCount++;
                    failReasons.add("论文 ID " + paperId + ": " + e.getMessage());
                    log.error("批量查重失败：paperId={}", paperId, e);
                }
            }
                
            String message = String.format("批量查重完成：成功%d篇，失败%d篇", successCount, failCount);
            if (!failReasons.isEmpty()) {
                message += "。失败详情：" + String.join("; ", failReasons);
            }
                
            return Result.success(message);
                
        } catch (Exception e) {
            log.error("批量校内查重失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量校内查重失败：" + e.getMessage());
        }
    }
        
    @Override
    public Result<String> batchThirdPartyCheckPaper(List<Long> paperIds) {
        try {
            if (paperIds == null || paperIds.isEmpty()) {
                return Result.error(ResultCode.PARAM_ERROR, "论文 ID 列表不能为空");
            }
                
            if (paperIds.size() > 20) {
                return Result.error(ResultCode.PARAM_ERROR, "单次批量查重最多支持 20 篇论文");
            }
                
            // TODO: 实现第三方查重逻辑（需要对接知网、维普等 API）
            // 当前返回提示错误
            return Result.error(ResultCode.SYSTEM_ERROR, "第三方查重功能暂未实现，请联系管理员配置");
                
        } catch (Exception e) {
            log.error("批量第三方查重失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量第三方查重失败：" + e.getMessage());
        }
    }

    /**
     * 批量补充论文列表信息（消除N+1查询）
     */
    private void enhancePaperList(List<PaperInfo> papers) {
        if (papers.isEmpty()) return;

        // 收集所有需要查询的学生ID和教师ID
        List<Long> studentIds = papers.stream()
                .map(PaperInfo::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<Long> teacherIds = papers.stream()
                .map(PaperInfo::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Set<Long> allUserIds = new HashSet<>();
        allUserIds.addAll(studentIds);
        allUserIds.addAll(teacherIds);

        // 批量查询用户信息
        Map<Long, SysUser> userMap = Collections.emptyMap();
        if (!allUserIds.isEmpty()) {
            userMap = sysUserMapper.selectBatchIds(allUserIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u));
        }

        // 批量查询学生详细信息
        Map<Long, StudentInfo> studentInfoMap = Collections.emptyMap();
        if (!studentIds.isEmpty()) {
            studentInfoMap = studentInfoService.lambdaQuery()
                    .in(StudentInfo::getUserId, studentIds)
                    .eq(StudentInfo::getIsDeleted, 0)
                    .list().stream()
                    .collect(Collectors.toMap(StudentInfo::getUserId, s -> s));
        }

        // 填充信息
        for (PaperInfo paper : papers) {
            // 补充学生信息
            if (paper.getStudentId() != null) {
                SysUser student = userMap.get(paper.getStudentId());
                if (student != null) {
                    paper.setStudentName(student.getRealName());
                    paper.setStudentUsername(student.getUsername());
                    StudentInfo studentInfo = studentInfoMap.get(student.getId());
                    if (studentInfo != null) {
                        paper.setStudentGrade(studentInfo.getGrade());
                        paper.setStudentMajor(studentInfo.getMajor());
                        paper.setStudentCollege(studentInfo.getCollegeName());
                    }
                }
            }

            // 补充导师信息
            if (paper.getTeacherId() != null) {
                SysUser teacher = userMap.get(paper.getTeacherId());
                if (teacher != null) {
                    paper.setTeacherRealName(teacher.getRealName());
                }
            }

            // 解析校内查重结果
            if (paper.getInternalCheckResult() != null && !paper.getInternalCheckResult().isEmpty()) {
                try {
                    paper.setInternalCheck(objectMapper.readValue(paper.getInternalCheckResult(), Map.class));
                } catch (Exception e) {
                    log.warn("解析校内查重结果失败: {}", e.getMessage());
                }
            }

            // 解析第三方查重结果
            if (paper.getThirdPartyCheckResult() != null && !paper.getThirdPartyCheckResult().isEmpty()) {
                try {
                    paper.setThirdPartyCheck(objectMapper.readValue(paper.getThirdPartyCheckResult(), Map.class));
                } catch (Exception e) {
                    log.warn("解析第三方查重结果失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 补充单篇论文详细信息（用于详情查询等单条场景）
     */
    private void enhancePaperDetail(PaperInfo paper) {
        enhancePaperList(Collections.singletonList(paper));
    }
    
    /**
     * 更新论文查重标记
     */
    private void updatePaperCheckFlags(PaperInfo paperInfo, String engineType, String source) {
        try {
            PaperInfo updateInfo = new PaperInfo();
            updateInfo.setId(paperInfo.getId());
            updateInfo.setCheckEngineType(engineType);
            updateInfo.setCheckSource(source);
            updateInfo.setCheckTime(LocalDateTime.now());
            // 初始化查重完成状态为 0（未开始），实际完成时由 updatePaperCheckCompletion 设置为 1
            if (paperInfo.getCheckCompleted() == null) {
                updateInfo.setCheckCompleted(0);
            }
            updateInfo.setUpdateTime(LocalDateTime.now());
                
            paperInfoMapper.updateById(updateInfo);
                
            log.info("论文查重标记更新成功：paperId={}, engineType={}, source={}", 
                    paperInfo.getId(), engineType, source);
        } catch (Exception e) {
            log.error("更新论文查重标记失败：paperId={}", paperInfo.getId(), e);
        }
    }
    
    /**
     * 更新论文查重完成状态
     */
    private void updatePaperCheckCompletion(PaperInfo paperInfo, Double similarity, boolean success) {
        try {
            PaperInfo updateInfo = new PaperInfo();
            updateInfo.setId(paperInfo.getId());
            updateInfo.setCheckCompleted(success ? 1 : 0);
                
            if (similarity != null) {
                updateInfo.setSimilarityRate(BigDecimal.valueOf(similarity));
                // 设置查重结果描述
                updateInfo.setCheckResult("查重完成，相似度：" + similarity + "%");
                
                // 构建查重结果JSON对象
                Map<String, Object> checkResult = new HashMap<>();
                checkResult.put("score", similarity);
                checkResult.put("time", LocalDateTime.now());
                
                // 根据查重来源存储到对应的字段
                String checkSource = paperInfo.getCheckSource();
                if ("local".equals(checkSource)) {
                    // 校内查重
                    updateInfo.setInternalCheckResult(objectMapper.writeValueAsString(checkResult));
                } else if ("third_party".equals(checkSource)) {
                    // 第三方查重
                    updateInfo.setThirdPartyCheckResult(objectMapper.writeValueAsString(checkResult));
                }
            } else {
                updateInfo.setCheckResult(success ? "查重完成" : "查重失败");
            }
                
            // 确保查重引擎类型和来源被正确设置
            if (paperInfo.getCheckEngineType() == null) {
                updateInfo.setCheckEngineType("LOCAL");
            }
            if (paperInfo.getCheckSource() == null) {
                updateInfo.setCheckSource("LOCAL");
            }
                
            // 设置查重时间（如果是第一次完成）
            if (success && paperInfo.getCheckTime() == null) {
                updateInfo.setCheckTime(LocalDateTime.now());
            }
                
            updateInfo.setUpdateTime(LocalDateTime.now());
                
            paperInfoMapper.updateById(updateInfo);
                
            log.info("论文查重完成状态更新：paperId={}, success={}, similarity={}", 
                    paperInfo.getId(), success, similarity);
        } catch (Exception e) {
            log.error("更新论文查重完成状态失败：paperId={}", paperInfo.getId(), e);
        }
    }
    
    /**
     * 添加相似度范围筛选条件
     */
    private void addSimilarityRangeCondition(LambdaQueryWrapper<PaperInfo> wrapper, String similarityRange) {
        switch (similarityRange.toLowerCase()) {
            case "<20%":
            case "lt20":
                wrapper.lt(PaperInfo::getSimilarityRate, 20);
                break;
            case "20%-50%":
            case "20to50":
                wrapper.between(PaperInfo::getSimilarityRate, 20, 50);
                break;
            case ">50%":
            case "gt50":
                wrapper.gt(PaperInfo::getSimilarityRate, 50);
                break;
            default:
                // 不支持的范围，不添加筛选条件
                log.warn("不支持的相似度范围: {}", similarityRange);
                break;
        }
    }
    
    /**
     * 添加查重状态筛选条件
     */
    private void addCheckStatusCondition(LambdaQueryWrapper<PaperInfo> wrapper, String checkStatus) {
        CheckStatusFilterEnum status = CheckStatusFilterEnum.fromCode(checkStatus);
        
        switch (status) {
            case NOT_CHECKED:
                // 未查重：check_completed = 0 或 check_engine_type IS NULL
                wrapper.and(w -> w.eq(PaperInfo::getCheckCompleted, 0)
                                .or()
                                .isNull(PaperInfo::getCheckEngineType));
                break;
                
            case SCHOOL_CHECK:
                // 校内查重：check_engine_type = 'school' 且 check_completed = 1
                wrapper.eq(PaperInfo::getCheckEngineType, "school")
                       .eq(PaperInfo::getCheckCompleted, 1);
                break;
                
            case THIRD_PARTY_CHECK:
                // 第三方查重：check_engine_type = 'third_party' 且 check_completed = 1
                wrapper.eq(PaperInfo::getCheckEngineType, "third_party")
                       .eq(PaperInfo::getCheckCompleted, 1);
                break;
                
            case COMPLETED:
                // 已完成：check_completed = 1（无论使用哪种引擎）
                wrapper.eq(PaperInfo::getCheckCompleted, 1);
                break;
                
            default:
                log.warn("不支持的查重状态筛选: {}", checkStatus);
                break;
        }
    }
    
    /**
     * 根据数值范围添加相似度筛选条件
     */
    private void addSimilarityRangeByValue(LambdaQueryWrapper<PaperInfo> wrapper, Double minSimilarity, Double maxSimilarity) {
        if (minSimilarity != null && maxSimilarity != null) {
            // 同时指定了最小值和最大值 - 范围查询
            wrapper.between(PaperInfo::getSimilarityRate, minSimilarity, maxSimilarity);
        } else if (minSimilarity != null) {
            // 只指定了最小值 - 大于等于
            wrapper.ge(PaperInfo::getSimilarityRate, minSimilarity);
        } else if (maxSimilarity != null) {
            // 只指定了最大值 - 小于等于
            wrapper.le(PaperInfo::getSimilarityRate, maxSimilarity);
        }
        
        log.debug("添加相似度范围筛选条件: min={}, max={}", minSimilarity, maxSimilarity);
    }

}