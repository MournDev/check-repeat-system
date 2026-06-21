package com.abin.checkrepeatsystem.teacher.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.PaperStatusEnum;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.pojo.entity.StudentInfo;
import com.abin.checkrepeatsystem.teacher.dto.BatchReviewDTO;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.teacher.mapper.TeacherDashboardMapper;
import com.abin.checkrepeatsystem.teacher.service.TeacherDashboardService;
import com.abin.checkrepeatsystem.user.service.StudentInfoService;
import com.abin.checkrepeatsystem.user.service.SysUserService;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 教师控制台服务实现类
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class TeacherDashboardServiceImpl implements TeacherDashboardService {

    private final TeacherDashboardMapper teacherDashboardMapper;

    private final PaperInfoMapper paperInfoMapper;

    private final SysUserService sysUserService;

    private final StudentInfoService studentInfoService;

    @Value("${file.upload.base-path:./uploads}")
    private String uploadBasePath;

    @Override
    public Result<Map<String, Object>> getDashboardStats(Long teacherId) {
        Map<String, Object> stats = new HashMap<>();

        // 1. 指导学生总数
        Long totalStudents = teacherDashboardMapper.countTotalStudents(teacherId);

        // 2. 待审核论文数量
        Long pendingPapers = teacherDashboardMapper.countPendingPapers(teacherId);

        // 3. 已审核论文数量
        Long reviewedPapers = teacherDashboardMapper.countReviewedPapers(teacherId);

        // 4. 审核通过率
        BigDecimal passRate = BigDecimal.ZERO;
        if (reviewedPapers > 0) {
            Long passedPapers = teacherDashboardMapper.countPassedPapers(teacherId);
            passRate = new BigDecimal(passedPapers)
                .divide(new BigDecimal(reviewedPapers), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }

        stats.put("totalStudents", totalStudents);
        stats.put("pendingPapers", pendingPapers);
        stats.put("reviewedPapers", reviewedPapers);
        stats.put("passRate", passRate);

        // 5. 添加待办事项提醒
        List<Map<String, Object>> todoItems = getTodoItems(teacherId);
        stats.put("todoItems", todoItems);

        // 6. 添加快速操作入口
        List<Map<String, Object>> quickActions = getQuickActions(teacherId);
        stats.put("quickActions", quickActions);

        // 7. 添加今日统计
        Map<String, Object> todayStats = getTodayStatistics(teacherId);
        stats.put("todayStats", todayStats);

        log.debug("教师{}仪表盘统计: 学生{}人, 待审核{}篇, 已审核{}篇, 通过率{}%",
                 teacherId, totalStudents, pendingPapers, reviewedPapers, passRate);

        return Result.success("获取统计数据成功", stats);
    }

    @Override
    public Result<Object> getPendingPapers(Long teacherId, Integer pageNum, Integer pageSize) {
        Page<PaperInfo> paperPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PaperInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperInfo::getTeacherId, teacherId)
               .eq(PaperInfo::getPaperStatus, PaperStatusEnum.AUDITING.getCode()) // 待审核状态
               .orderByDesc(PaperInfo::getSubmitTime);

        Page<PaperInfo> resultPage = paperInfoMapper.selectPage(paperPage, wrapper);

        // 构造符合需求的数据结构
        Map<String, Object> responseData = new HashMap<>();
        List<Map<String, Object>> records = new ArrayList<>();

        for (PaperInfo paper : resultPage.getRecords()) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", paper.getId());

            // 顶层字段（方便前端直接使用）
            record.put("paperId", paper.getId());
            record.put("paperTitle", paper.getPaperTitle());
            record.put("fileId", paper.getFileId());
            record.put("submitTime", paper.getSubmitTime());
            record.put("version", paper.getFileId() != null ? 1 : 0);

            // 计算等待时间（天）
            if (paper.getSubmitTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                long days = java.time.Duration.between(paper.getSubmitTime(), now).toDays();
                record.put("waitingTime", (int) days);
            } else {
                record.put("waitingTime", 0);
            }

            // 计算优先级
            Integer waitingDays = (Integer) record.get("waitingTime");
            if (waitingDays >= 14) {
                record.put("priority", "urgent");
            } else if (waitingDays >= 7) {
                record.put("priority", "high");
            } else {
                record.put("priority", "normal");
            }

            // 计算截止时间（默认7天后）
            if (paper.getSubmitTime() != null) {
                record.put("deadline", paper.getSubmitTime().plusDays(7));
            }

            // paperBaseInfo 对象
            Map<String, Object> paperBaseInfo = new HashMap<>();
            paperBaseInfo.put("paperTitle", paper.getPaperTitle());

            // 获取学生信息
            SysUser student = sysUserService.getById(paper.getStudentId());
            if (student != null) {
                record.put("studentName", student.getRealName());
                record.put("studentId", student.getId());
                record.put("studentNo", student.getUsername());
                record.put("email", student.getEmail());

                paperBaseInfo.put("studentName", student.getRealName());
                paperBaseInfo.put("studentId", student.getId());
                paperBaseInfo.put("studentNo", student.getUsername());
                paperBaseInfo.put("email", student.getEmail());

                // 从StudentInfo表获取学生的学院信息
                StudentInfo studentInfo = studentInfoService.getByUserId(student.getId());
                if (studentInfo != null) {
                    record.put("college", studentInfo.getCollegeName());
                    record.put("major", studentInfo.getMajor());
                    paperBaseInfo.put("college", studentInfo.getCollegeName());
                    paperBaseInfo.put("major", studentInfo.getMajor());
                }
            }

            record.put("paperBaseInfo", paperBaseInfo);

            // taskBaseInfo 对象
            Map<String, Object> taskBaseInfo = new HashMap<>();
            taskBaseInfo.put("checkEndTime", paper.getCheckTime());
            taskBaseInfo.put("checkRate", paper.getSimilarityRate() != null ? paper.getSimilarityRate().doubleValue() : 0.0);
            record.put("taskBaseInfo", taskBaseInfo);

            // 相似度
            record.put("similarity", paper.getSimilarityRate() != null ? paper.getSimilarityRate().doubleValue() : 0.0);

            // 字数和页数（）
            record.put("wordCount", paper.getWordCount());
            record.put("pageCount", paper.getPageCount());

            records.add(record);
        }

        responseData.put("records", records);
        responseData.put("total", resultPage.getTotal());

        return Result.success("获取待审核论文列表成功", responseData);
    }

    @Override
    public Result<Map<String, Object>> getStudentStats(Long teacherId) {
        Map<String, Object> stats = new HashMap<>();

        // 总学生数
        Long totalStudents = teacherDashboardMapper.countTotalStudents(teacherId);
        // 已提交论文数（对应前端 submittedStudents）
        Long submittedPapers = teacherDashboardMapper.countSubmittedPapers(teacherId);
        // 已分配导师数（paper_status = 'assigned'）
        Long assignedPapers = teacherDashboardMapper.countAssignedPapers(teacherId);
        // 已完成审核数（paper_status = 'completed'，对应前端 completedStudents）
        Long passedPapers = teacherDashboardMapper.countPassedPapers(teacherId);

        stats.put("totalStudents", totalStudents);
        stats.put("submittedStudents", submittedPapers);
        stats.put("assignedStudents", assignedPapers);
        stats.put("completedStudents", passedPapers);

        log.debug("教师{}学生统计: 总{}人, 已提交{}人, 已分配{}人, 已完成{}人",
                 teacherId, totalStudents, submittedPapers, assignedPapers, passedPapers);

        return Result.success("获取学生状态统计成功", stats);
    }

    @Override
    public Result<String> batchReviewPapers(Long teacherId, BatchReviewDTO reviewDTO) {
        List<Long> paperIds = reviewDTO.getPaperIds();
        String reviewStatus = reviewDTO.getReviewStatus();
        String reviewOpinion = reviewDTO.getReviewOpinion();

        if (paperIds == null || paperIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "论文ID列表不能为空");
        }

        // 验证审核状态
        String newStatus;
        if ("APPROVED".equalsIgnoreCase(reviewStatus)) {
            newStatus = PaperStatusEnum.COMPLETED.getCode();
        } else if ("REJECTED".equalsIgnoreCase(reviewStatus)) {
            newStatus = PaperStatusEnum.REJECTED.getCode();
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审核状态参数无效，应为APPROVED或REJECTED");
        }

        int successCount = 0;
        int failCount = 0;

        // 批量处理审核
        for (Long paperId : paperIds) {
            try {
                // 验证论文是否存在且属于该教师
                PaperInfo paper = paperInfoMapper.selectById(paperId);
                if (paper == null) {
                    log.warn("论文不存在: paperId={}", paperId);
                    failCount++;
                    continue;
                }

                if (!paper.getTeacherId().equals(teacherId)) {
                    log.warn("无权限审核论文: teacherId={}, paperId={}", teacherId, paperId);
                    failCount++;
                    continue;
                }

                // 验证论文状态必须为待审核
                if (!PaperStatusEnum.AUDITING.getValue().equals(paper.getPaperStatus())) {
                    log.warn("论文状态不允许审核: paperId={}, status={}", paperId, paper.getPaperStatus());
                    failCount++;
                    continue;
                }

                // 更新论文状态
                paper.setPaperStatus(newStatus);
                paper.setCheckResult(reviewOpinion);
                paper.setCheckTime(LocalDateTime.now());

                paperInfoMapper.updateById(paper);
                successCount++;

                log.info("教师{}审核论文{}成功: 状态={}", teacherId, paperId, reviewStatus);
            } catch (Exception e) {
                log.error("审核论文失败: teacherId={}, paperId={}", teacherId, paperId, e);
                failCount++;
            }
        }

        String message = String.format("批量审核完成: 成功%d篇, 失败%d篇", successCount, failCount);
        log.info("教师{}批量审核完成: {}", teacherId, message);

        return Result.success(message);
    }

    @Override
    public Result<String> reviewPaper(Long teacherId, Long paperId, String reviewResult, String reviewComment) {
        // 验证论文是否存在且属于该教师
        PaperInfo paper = paperInfoMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "论文不存在");
        }

        if (!paper.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限审核此论文");
        }

        // 验证论文状态必须为待审核
        if (!PaperStatusEnum.AUDITING.getValue().equals(paper.getPaperStatus())) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_STATUS, "论文当前状态不允许审核：" + paper.getPaperStatus());
        }

        // 更新论文状态
        String newStatus;
        if ("pass".equalsIgnoreCase(reviewResult)) {
            newStatus = PaperStatusEnum.COMPLETED.getCode();
        } else if ("reject".equalsIgnoreCase(reviewResult)) {
            newStatus = PaperStatusEnum.REJECTED.getCode();
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审核结果参数无效");
        }

        paper.setPaperStatus(newStatus);
        paper.setCheckResult(reviewComment);
        paper.setCheckTime(LocalDateTime.now());

        paperInfoMapper.updateById(paper);

        log.info("教师{}审核论文{}结果: {}, 意见: {}", teacherId, paperId, reviewResult, reviewComment);
        return Result.success("论文审核成功");
    }

    @Override
    public Result<String> downloadPaper(Long teacherId, Long paperId) {
        // 验证论文是否存在且属于该教师
        PaperInfo paper = paperInfoMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "论文不存在");
        }

        if (!paper.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限下载此论文");
        }

        // 生成下载链接（这里返回文件ID，前端根据文件ID下载）
        String downloadUrl = "/api/v1/file/download/" + paper.getFileId();

        log.info("教师{}下载论文{}, 文件ID: {}", teacherId, paperId, paper.getFileId());
        return Result.success("获取下载链接成功", downloadUrl);
    }

    @Override
    public Result<Map<String, Object>> getReviewStatistics(Long teacherId) {
        Map<String, Object> statistics = new HashMap<>();

        // 论文状态分布数据
        List<Map<String, Object>> statusDistribution = teacherDashboardMapper.getPaperStatusDistribution(teacherId);

        // 构建图表数据格式，与前端期望一致
        Map<String, Object> chartData = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        List<String> colors = new ArrayList<>();

        // 状态颜色映射
        Map<String, String> statusColorMap = new HashMap<>();
        statusColorMap.put("已通过", "#67c23a");
        statusColorMap.put("需修改", "#f56c6c");
        statusColorMap.put("审核中", "#e6a23c");
        statusColorMap.put("未提交", "#909399");

        if (statusDistribution != null) {
            for (Map<String, Object> status : statusDistribution) {
                if (status != null) {
                    String statusName = (String) status.get("statusName");
                    Object countObj = status.get("count");
                    Integer count = (countObj != null) ? ((Number) countObj).intValue() : 0;
                    labels.add(statusName);
                    values.add(count);
                    colors.add(statusColorMap.getOrDefault(statusName, "#909399"));
                }
            }
        }

        chartData.put("labels", labels);
        chartData.put("values", values);
        chartData.put("colors", colors);

        // 各专业审核情况
        List<Map<String, Object>> majorReviewStats = teacherDashboardMapper.getMajorReviewStatistics(teacherId);

        // 转换为前端期望的格式
        List<Map<String, Object>> collegeDistribution = new ArrayList<>();
        if (majorReviewStats != null) {
            for (Map<String, Object> major : majorReviewStats) {
                if (major != null) {
                    Map<String, Object> collegeItem = new HashMap<>();
                    String majorName = (String) major.get("majorName");
                    Object countObj = major.get("totalPapers");
                    Integer count = (countObj != null) ? ((Number) countObj).intValue() : 0;
                    collegeItem.put("label", majorName != null ? majorName : "未知专业");
                    collegeItem.put("value", count);
                    collegeDistribution.add(collegeItem);
                }
            }
        }

        // 时间趋势数据（近30天）
        List<Map<String, Object>> timeTrend = teacherDashboardMapper.getTimeTrendStatistics(teacherId);

        statistics.put("chartData", chartData);
        statistics.put("collegeDistribution", collegeDistribution);
        statistics.put("timeTrend", timeTrend);
        statistics.put("statusDistribution", statusDistribution);

        log.debug("教师{}获取审核统计成功", teacherId);
        return Result.success("获取审核进度统计成功", statistics);
    }

    @Override
    public Result<Object> getRecentActivities(Long teacherId, Integer page, Integer size) {
        Page<Map<String, Object>> activityPage = new Page<>(page, size);
        Page<Map<String, Object>> result = teacherDashboardMapper.getRecentReviewActivities(teacherId, activityPage);

        log.debug("教师{}获取近期活动记录: {}条, 总数: {}", teacherId, result.getRecords().size(), result.getTotal());
        return Result.success("获取近期活动记录成功", result);
    }

    @Override
    public Result<String> exportTeacherData(Long teacherId, String startDate, String endDate) {
        try {
            String fileName = String.format("teacher_%d_data_%s.xlsx",
                teacherId, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            String filePath = uploadBasePath + File.separator + "exports" + File.separator + fileName;

            File dir = new File(uploadBasePath + File.separator + "exports");
            if (!dir.exists()) dir.mkdirs();

            // 查询论文状态分布
            List<Map<String, Object>> statusDist = teacherDashboardMapper.getPaperStatusDistribution(teacherId);
            // 查询专业审核统计
            List<Map<String, Object>> majorStats = teacherDashboardMapper.getMajorReviewStatistics(teacherId);
            // 查询时间趋势
            List<Map<String, Object>> timeTrend = teacherDashboardMapper.getTimeTrendStatistics(teacherId);

            EasyExcel.write(filePath)
                    .sheet("论文状态分布")
                    .head(List.of(List.of("状态"), List.of("数量")))
                    .doWrite(statusDist != null ? statusDist : Collections.emptyList());

            EasyExcel.write(filePath)
                    .sheet("专业审核统计")
                    .head(majorStats != null && !majorStats.isEmpty()
                            ? new ArrayList<>(majorStats.get(0).keySet()).stream().map(k -> List.of((String) k)).toList()
                            : List.of(List.of("无数据")))
                    .doWrite(majorStats != null ? majorStats : Collections.emptyList());

            EasyExcel.write(filePath)
                    .sheet("时间趋势")
                    .head(timeTrend != null && !timeTrend.isEmpty()
                            ? new ArrayList<>(timeTrend.get(0).keySet()).stream().map(k -> List.of((String) k)).toList()
                            : List.of(List.of("无数据")))
                    .doWrite(timeTrend != null ? timeTrend : Collections.emptyList());

            log.info("教师{}导出数据完成: file={}", teacherId, filePath);
            return Result.success("数据导出成功", fileName);
        } catch (Exception e) {
            log.error("教师数据导出失败: teacherId={}", teacherId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "数据导出失败: " + e.getMessage());
        }
    }

    @Override
    public Result<String> exportData(Long teacherId, String format, String startTime, String endTime) {
        try {
            String ext = "csv".equalsIgnoreCase(format) ? "csv" : "xlsx";
            String exportFileName = String.format("teacher_%d_export_%s.%s",
                teacherId, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")), ext);
            String filePath = uploadBasePath + File.separator + "exports" + File.separator + exportFileName;

            File dir = new File(uploadBasePath + File.separator + "exports");
            if (!dir.exists()) dir.mkdirs();

            // 查询近期审核活动
            Page<Map<String, Object>> activityPage = new Page<>(1, 1000);
            Page<Map<String, Object>> activities = teacherDashboardMapper.getRecentReviewActivities(teacherId, activityPage);
            List<Map<String, Object>> records = activities.getRecords();

            if ("csv".equalsIgnoreCase(format)) {
                // CSV格式导出
                try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
                    if (records != null && !records.isEmpty()) {
                        // 写表头
                        writer.write(String.join(",", records.get(0).keySet()) + "\n");
                        // 写数据
                        for (Map<String, Object> row : records) {
                            writer.write(row.values().stream()
                                    .map(v -> v == null ? "" : v.toString())
                                    .reduce((a, b) -> a + "," + b).orElse("") + "\n");
                        }
                    }
                }
            } else {
                EasyExcel.write(filePath)
                        .sheet("审核活动")
                        .head(records != null && !records.isEmpty()
                                ? new ArrayList<>(records.get(0).keySet()).stream().map(k -> List.of((String) k)).toList()
                                : List.of(List.of("无数据")))
                        .doWrite(records != null ? records : Collections.emptyList());
            }

            log.info("教师{}导出数据完成: format={}, file={}", teacherId, format, filePath);
            return Result.success("数据导出成功", exportFileName);
        } catch (IOException e) {
            log.error("数据导出失败: teacherId={}", teacherId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "数据导出失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Object> getStudentList(Long teacherId, Integer page, Integer size) {
        Page<SysUser> studentPage = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserType, 1) // 学生类型
               .eq(SysUser::getStatus, 1)   // 启用状态
               .eq(SysUser::getIsDeleted, 0);

        // 通过论文信息关联查找该教师指导的学生
        List<Long> studentIds = teacherDashboardMapper.getStudentIdsByTeacher(teacherId);
        if (!studentIds.isEmpty()) {
            wrapper.in(SysUser::getId, studentIds);
        } else {
            wrapper.eq(SysUser::getId, -1L); // 无结果
        }

        Page<SysUser> resultPage = sysUserService.page(studentPage, wrapper);

        // 转换为前端需要的格式
        Page<Map<String, Object>> responsePage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();

        for (SysUser student : resultPage.getRecords()) {
            Map<String, Object> record = new HashMap<>();
            record.put("studentId", student.getUsername());
            record.put("studentName", student.getRealName());
            record.put("username", student.getUsername());
            record.put("email", student.getEmail());
            record.put("phone", student.getPhone());

            // 从StudentInfo表获取学生年级、专业、学院信息
            StudentInfo studentInfo = studentInfoService.getByUserId(student.getId());
            if (studentInfo != null) {
                record.put("grade", studentInfo.getGrade());
                record.put("major", studentInfo.getMajor());
                record.put("college", studentInfo.getCollegeName());
            }

            records.add(record);
        }

        responsePage.setRecords(records);
        return Result.success("获取指导学生列表成功", responsePage);
    }

    @Override
    public Result<Map<String, Object>> refreshDashboard(Long teacherId) {
        // 直接调用获取统计数据的方法
        return getDashboardStats(teacherId);
    }
    
    /**
     * 获取待办事项列表
     */
    private List<Map<String, Object>> getTodoItems(Long teacherId) {
        List<Map<String, Object>> todoItems = new ArrayList<>();
        
        try {
            // 1. 待审核论文提醒
            Long pendingCount = teacherDashboardMapper.countPendingPapers(teacherId);
            if (pendingCount > 0) {
                Map<String, Object> pendingItem = new HashMap<>();
                pendingItem.put("type", "PENDING_REVIEW");
                pendingItem.put("title", "有待审核的论文");
                pendingItem.put("count", pendingCount);
                pendingItem.put("priority", "HIGH");
                pendingItem.put("description", String.format("您有%d篇论文等待审核", pendingCount));
                pendingItem.put("actionUrl", "/teacher/papers/pending");
                todoItems.add(pendingItem);
            }
            
            // 2. 新提交论文提醒（24小时内）
            Long newSubmissions = teacherDashboardMapper.countNewSubmissions(teacherId, 24);
            if (newSubmissions > 0) {
                Map<String, Object> newItem = new HashMap<>();
                newItem.put("type", "NEW_SUBMISSION");
                newItem.put("title", "新论文提交");
                newItem.put("count", newSubmissions);
                newItem.put("priority", "MEDIUM");
                newItem.put("description", String.format("过去24小时有%d篇新论文提交", newSubmissions));
                newItem.put("actionUrl", "/teacher/papers/new");
                todoItems.add(newItem);
            }
            
            // 3. 需要关注的学生提醒（长时间未提交论文）
            Long inactiveStudents = teacherDashboardMapper.countInactiveStudents(teacherId, 7);
            if (inactiveStudents > 0) {
                Map<String, Object> inactiveItem = new HashMap<>();
                inactiveItem.put("type", "INACTIVE_STUDENTS");
                inactiveItem.put("title", "需关注的学生");
                inactiveItem.put("count", inactiveStudents);
                inactiveItem.put("priority", "LOW");
                inactiveItem.put("description", String.format("%d名学生超过7天未提交论文", inactiveStudents));
                inactiveItem.put("actionUrl", "/teacher/students/inactive");
                todoItems.add(inactiveItem);
            }
            
        } catch (Exception e) {
            log.warn("获取待办事项失败: teacherId={}", teacherId, e);
        }
        
        return todoItems;
    }
    
    /**
     * 获取快速操作入口
     */
    private List<Map<String, Object>> getQuickActions(Long teacherId) {
        List<Map<String, Object>> quickActions = new ArrayList<>();
        
        // 1. 快速审核
        Map<String, Object> reviewAction = new HashMap<>();
        reviewAction.put("id", "quick_review");
        reviewAction.put("title", "快速审核");
        reviewAction.put("icon", "edit-document");
        reviewAction.put("description", "快速处理待审核论文");
        reviewAction.put("actionUrl", "/teacher/papers/pending");
        reviewAction.put("permission", "REVIEW_PAPER");
        quickActions.add(reviewAction);
        
        // 2. 查看学生列表
        Map<String, Object> studentListAction = new HashMap<>();
        studentListAction.put("id", "student_list");
        studentListAction.put("title", "学生管理");
        studentListAction.put("icon", "user-group");
        studentListAction.put("description", "查看和管理指导学生");
        studentListAction.put("actionUrl", "/teacher/students");
        studentListAction.put("permission", "VIEW_STUDENTS");
        quickActions.add(studentListAction);
        
        // 3. 数据统计
        Map<String, Object> statsAction = new HashMap<>();
        statsAction.put("id", "review_stats");
        statsAction.put("title", "审核统计");
        statsAction.put("icon", "bar-chart");
        statsAction.put("description", "查看审核工作量统计");
        statsAction.put("actionUrl", "/teacher/review/statistics");
        statsAction.put("permission", "VIEW_STATISTICS");
        quickActions.add(statsAction);
        
        // 4. 消息中心
        Map<String, Object> messageAction = new HashMap<>();
        messageAction.put("id", "message_center");
        messageAction.put("title", "消息中心");
        messageAction.put("icon", "message");
        messageAction.put("description", "查看系统消息和学生咨询");
        messageAction.put("actionUrl", "/teacher/messages");
        messageAction.put("permission", "VIEW_MESSAGES");
        quickActions.add(messageAction);
        
        // 5. 系统设置
        Map<String, Object> settingAction = new HashMap<>();
        settingAction.put("id", "system_setting");
        settingAction.put("title", "个人设置");
        settingAction.put("icon", "setting");
        settingAction.put("description", "修改个人信息和偏好设置");
        settingAction.put("actionUrl", "/teacher/settings");
        settingAction.put("permission", "VIEW_SETTINGS");
        quickActions.add(settingAction);
        
        return quickActions;
    }
    
    /**
     * 获取今日统计
     */
    private Map<String, Object> getTodayStatistics(Long teacherId) {
        Map<String, Object> todayStats = new HashMap<>();
        
        try {
            // 今日审核数
            Long todayReviewed = teacherDashboardMapper.countTodayReviews(teacherId);
            todayStats.put("todayReviewed", todayReviewed);
            
            // 今日通过数
            Long todayPassed = teacherDashboardMapper.countTodayPasses(teacherId);
            todayStats.put("todayPassed", todayPassed);
            
            // 今日驳回数
            Long todayRejected = todayReviewed - todayPassed;
            todayStats.put("todayRejected", todayRejected >= 0 ? todayRejected : 0);
            
            // 今日新提交
            Long todayNewSubmissions = teacherDashboardMapper.countNewSubmissions(teacherId, 24);
            todayStats.put("todayNewSubmissions", todayNewSubmissions);
            
            // 平均审核耗时（分钟）
            Double avgReviewTime = teacherDashboardMapper.getAverageReviewTime(teacherId);
            todayStats.put("averageReviewTime", avgReviewTime != null ? avgReviewTime.intValue() : 0);
            
        } catch (Exception e) {
            log.warn("获取今日统计失败: teacherId={}", teacherId, e);
            // 返回默认值
            todayStats.put("todayReviewed", 0);
            todayStats.put("todayPassed", 0);
            todayStats.put("todayRejected", 0);
            todayStats.put("todayNewSubmissions", 0);
            todayStats.put("averageReviewTime", 0);
        }
        
        return todayStats;
    }
}