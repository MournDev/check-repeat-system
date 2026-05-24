package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.admin.mapper.SystemConfigMapper;
import com.abin.checkrepeatsystem.common.enums.PaperStatusEnum;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.mapper.FileInfoMapper;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.FileInfo;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.pojo.entity.SystemConfig;
import com.abin.checkrepeatsystem.pojo.entity.TeacherInfo;
import com.abin.checkrepeatsystem.student.dto.*;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.user.service.TeacherInfoDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentDashboardService {
    
    private final PaperInfoMapper paperInfoMapper;
    
    private final FileInfoMapper fileInfoMapper;
    
    private final CheckTaskMapper checkTaskMapper;
    
    private final SysUserMapper sysUserMapper;
    
    private final SystemConfigMapper systemConfigMapper;

    private final TeacherInfoDataService teacherInfoService;

    public StudentDashboardStatsDTO getDashboardStats(Long studentId) {
        StudentDashboardStatsDTO stats = new StudentDashboardStatsDTO();
        
        // 1. 获取论文统计
        LambdaQueryWrapper<PaperInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getIsDeleted, 0);
        
        // 总提交数
        Long totalCount = paperInfoMapper.selectCount(wrapper);
        stats.setSubmittedCount(totalCount.intValue());
        
        // 待审核数 (状态: auditing)
        wrapper.clear();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getPaperStatus, PaperStatusEnum.AUDITING.getValue())
               .eq(PaperInfo::getIsDeleted, 0);
        Long pendingCount = paperInfoMapper.selectCount(wrapper);
        stats.setPendingCount(pendingCount.intValue());
        
        // 已通过数 (状态: completed)
        wrapper.clear();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getPaperStatus, PaperStatusEnum.COMPLETED.getValue())
               .eq(PaperInfo::getIsDeleted, 0);
        Long approvedCount = paperInfoMapper.selectCount(wrapper);
        stats.setApprovedCount(approvedCount.intValue());
        
        // 需修改数 (状态: rejected)
        wrapper.clear();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getPaperStatus, PaperStatusEnum.REJECTED.getValue())
               .eq(PaperInfo::getIsDeleted, 0);
        Long revisionCount = paperInfoMapper.selectCount(wrapper);
        stats.setRevisionCount(revisionCount.intValue());
        
        // 未通过数
        stats.setFailedCount(totalCount.intValue() - approvedCount.intValue() - pendingCount.intValue() - revisionCount.intValue());
        
        // 2. 计算完成度
        BigDecimal completionRate = totalCount > 0 ? 
            new BigDecimal(approvedCount).multiply(new BigDecimal(100)).divide(new BigDecimal(totalCount), 2, BigDecimal.ROUND_HALF_UP) : 
            BigDecimal.ZERO;
        stats.setCompletionRate(completionRate);
        
        // 3. 获取当前进度 (1-提交, 2-查重, 3-审核, 4-完成)
        LatestPaperDTO latestPaper = getLatestPaper(studentId);
        if (latestPaper != null) {
            stats.setCurrentProgress(getProgressFromStatus(latestPaper.getStatus()));
        } else {
            stats.setCurrentProgress(1);
        }
        
        // 4. 获取本周趋势
        LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Date weekStart = Date.from(startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        // 本周提交数
        wrapper.clear();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .ge(PaperInfo::getSubmitTime, weekStart)
               .eq(PaperInfo::getIsDeleted, 0);
        Long thisWeekSubmitted = paperInfoMapper.selectCount(wrapper);
        stats.setThisWeekSubmitted(thisWeekSubmitted.intValue());
        
        // 本周通过数
        wrapper.clear();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .ge(PaperInfo::getSubmitTime, weekStart)
               .eq(PaperInfo::getPaperStatus, PaperStatusEnum.COMPLETED.getValue())
               .eq(PaperInfo::getIsDeleted, 0);
        Long thisWeekApproved = paperInfoMapper.selectCount(wrapper);
        stats.setThisWeekApproved(thisWeekApproved.intValue());
        
        // 5. 计算平均分 (假设论文有评分字段)
        // 暂时设置为0，后续从数据库获取实际评分
        stats.setAvgScore(BigDecimal.ZERO);
        
        // 6. 计算总字数
        Integer totalWords = getTotalWordCount(studentId);
        stats.setTotalWordCount(totalWords != null ? totalWords : 0);
        
        return stats;
    }

    public LatestPaperDTO getLatestPaper(Long studentId) {
        PaperInfo latestPaper = paperInfoMapper.selectLatestPaper(studentId);
        if (latestPaper == null) {
            return null;
        }
        FileInfo fileInfo = fileInfoMapper.selectById(latestPaper.getFileId());
        LatestPaperDTO dto = new LatestPaperDTO();
        dto.setId(latestPaper.getId());
        dto.setWordCount(fileInfo.getWordCount());
        dto.setPageCount(fileInfo.getPageCount());
        dto.setTitle(latestPaper.getPaperTitle());
        dto.setStatus(latestPaper.getPaperStatus());
        dto.setSubmitTime(latestPaper.getSubmitTime());
        dto.setApproveTime(latestPaper.getSubmitTime());
        dto.setAllocationStatus(latestPaper.getAllocationStatus());
        dto.setAllocationTime(latestPaper.getAllocationTime());
        // 获取导师信息
        if (latestPaper.getTeacherId() != null) {
            SysUser advisor = sysUserMapper.selectById(latestPaper.getTeacherId());
            if (advisor != null) {
                dto.setAdvisorName(advisor.getRealName());
            }
        }

        // 获取最新的反馈
//        PaperFeedback feedback = paperInfoMapper.selectLatestFeedback(latestPaper.getId());
//        if (feedback != null) {
//            dto.setFeedback(feedback.getContent());
//        }

        // 获取查重率：优先使用已完成查重任务的数据，否则回退到论文本身的相似度
        Long latestPaperId = latestPaper.getId();
        CheckTask checkTask = checkTaskMapper.selectLatestByPaperId(latestPaperId);
        if (checkTask != null && "completed".equals(checkTask.getCheckStatus()) && checkTask.getCheckRate() != null) {
            dto.setSimilarity(checkTask.getCheckRate());
        } else if (latestPaper.getSimilarityRate() != null) {
            dto.setSimilarity(latestPaper.getSimilarityRate());
        }

        return dto;
    }

    public AdvisorInfoDTO getAdvisorInfo(Long studentId) {
        // 获取学生信息，找到分配的导师
        PaperInfo latestPaper = paperInfoMapper.selectLatestPaper(studentId);
        if (latestPaper == null || latestPaper.getTeacherId() == null) {
            return null;
        }

        // 权限检查：确保当前用户是学生本人或该学生的指导教师
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        if (currentUser != null) {
            boolean isStudent = currentUser.getId().equals(studentId);
            boolean isTeacher = currentUser.getId().equals(latestPaper.getTeacherId());
            boolean isAdmin = UserBusinessInfoUtils.isAdmin();
            if (!isStudent && !isTeacher && !isAdmin) {
                return null;
            }
        }

        SysUser teacher = sysUserMapper.selectById(latestPaper.getTeacherId());
        if (teacher == null) {
            return null;
        }

        AdvisorInfoDTO dto = new AdvisorInfoDTO();
        dto.setId(teacher.getId());
        dto.setName(teacher.getRealName());
//        dto.setTitle(teacher.getTitle());
        dto.setPhone(teacher.getPhone());
        dto.setEmail(teacher.getEmail());
//        dto.setOffice(teacher.getOffice());
        dto.setAvatar(teacher.getAvatar());
        
        // 从TeacherInfo表获取教师的研究方向
        TeacherInfo teacherInfo = teacherInfoService.getByUserId(teacher.getId());
        if (teacherInfo != null) {
            dto.setResearchField(teacherInfo.getResearchDirection());
        }

        // 专长领域（逗号分隔转换为列表）
//        if (StringUtils.isNotBlank(advisor.getExpertise())) {
//            dto.setExpertise(Arrays.asList(advisor.getExpertise().split(",")));
//        }

        // 导师统计
//        dto.setGuidedPapersCount(advisorInfoMapper.countGuidedPapers(advisor.getId()));
//        dto.setApprovalRate(advisorInfoMapper.calculateApprovalRate(advisor.getId()));
//        dto.setAverageScore(advisorInfoMapper.calculateAverageScore(advisor.getId()));
        // 在线状态：暂时设置为offline，后续从在线状态服务获取
        dto.setOnlineStatus("offline");

        return dto;
    }
    
    /**
     * 根据论文状态获取进度
     */
    private Integer getProgressFromStatus(String status) {
        switch (status) {
            case "pending": return 1;      // 已提交
            case "checking": return 2;     // 查重中
            case "auditing": return 3;     // 审核中
            case "completed": return 4;    // 已完成
            case "rejected": return 3;     // 需修改（回到审核阶段）
            default: return 1;
        }
    }
    
    /**
     * 获取学生论文总字数
     */
    private Integer getTotalWordCount(Long studentId) {
        LambdaQueryWrapper<PaperInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getIsDeleted, 0);
        
        List<PaperInfo> papers = paperInfoMapper.selectList(wrapper);
        if (papers == null || papers.isEmpty()) {
            return 0;
        }
        
        return papers.stream()
                .mapToInt(paper -> {
                    FileInfo fileInfo = fileInfoMapper.selectById(paper.getFileId());
                    return fileInfo != null && fileInfo.getWordCount() != null ? fileInfo.getWordCount() : 0;
                })
                .sum();
    }
    
    /**
     * 获取时间节点信息（从数据库读取系统配置）
     */
    public DeadlinesDTO getDeadlines() {
        DeadlinesDTO deadlines = new DeadlinesDTO();
        
        try {
            // 从 system_config 表读取时间节点配置
            // 提交截止日期
            SystemConfig submissionConfig = systemConfigMapper.selectByConfigKey("submission_deadline");
            if (submissionConfig != null) {
                deadlines.setSubmissionDeadline(submissionConfig.getConfigValue());
            } else {
                // 如果数据库没有配置，使用默认值
                deadlines.setSubmissionDeadline("2026-03-20");
                log.warn("未找到论文提交截止日期配置，使用默认值：{}", deadlines.getSubmissionDeadline());
            }
            
            // 审核截止日期
            SystemConfig reviewConfig = systemConfigMapper.selectByConfigKey("review_deadline");
            if (reviewConfig != null) {
                deadlines.setReviewDeadline(reviewConfig.getConfigValue());
            } else {
                deadlines.setReviewDeadline("2026-04-10");
                log.warn("未找到审核截止日期配置，使用默认值：{}", deadlines.getReviewDeadline());
            }
            
            // 答辩时间
            SystemConfig defenseConfig = systemConfigMapper.selectByConfigKey("defense_date");
            if (defenseConfig != null) {
                deadlines.setDefenseDate(defenseConfig.getConfigValue());
            } else {
                deadlines.setDefenseDate("2026-05-15");
                log.warn("未找到答辩时间配置，使用默认值：{}", deadlines.getDefenseDate());
            }
            
            // 预计毕业时间
            SystemConfig graduationConfig = systemConfigMapper.selectByConfigKey("graduation_date");
            if (graduationConfig != null) {
                deadlines.setGraduationDate(graduationConfig.getConfigValue());
            } else {
                deadlines.setGraduationDate("2026-06-30");
                log.warn("未找到预计毕业时间配置，使用默认值：{}", deadlines.getGraduationDate());
            }
            
            log.info("时间节点配置加载成功：提交截止={}, 审核截止={}, 答辩={}, 毕业={}",
                deadlines.getSubmissionDeadline(),
                deadlines.getReviewDeadline(),
                deadlines.getDefenseDate(),
                deadlines.getGraduationDate());
                
        } catch (Exception e) {
            log.error("读取时间节点配置失败，使用默认值", e);
            // 出现异常时使用默认值
            deadlines.setSubmissionDeadline("2026-03-20");
            deadlines.setReviewDeadline("2026-04-10");
            deadlines.setDefenseDate("2026-05-15");
            deadlines.setGraduationDate("2026-06-30");
        }
        
        return deadlines;
    }
    
    /**
     * 获取能力评估雷达图数据
     */
    public AbilityRadarDTO getAbilityRadar(Long studentId) {
        AbilityRadarDTO radar = new AbilityRadarDTO();
        
        // 1. 论文数量得分 (基于提交数量)
        LambdaQueryWrapper<PaperInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getIsDeleted, 0);
        Long totalCount = paperInfoMapper.selectCount(wrapper);
        radar.setPaperCount(Math.min(totalCount.intValue(), 10));
        
        // 2. 通过率
        wrapper.clear();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getPaperStatus, PaperStatusEnum.COMPLETED.getValue())
               .eq(PaperInfo::getIsDeleted, 0);
        Long approvedCount = paperInfoMapper.selectCount(wrapper);
        Integer passRate = totalCount > 0 ? 
            approvedCount.intValue() * 100 / totalCount.intValue() : 0;
        radar.setPassRate(passRate);
        
        // 3. 平均相似度
        List<PaperInfo> papers = paperInfoMapper.selectList(
            new LambdaQueryWrapper<PaperInfo>()
                .eq(PaperInfo::getStudentId, studentId)
                .eq(PaperInfo::getIsDeleted, 0)
        );
        
        if (!papers.isEmpty()) {
            double avgSimilarity = papers.stream()
                .mapToDouble(paper -> {
                    CheckTask task = checkTaskMapper.selectLatestByPaperId(paper.getId());
                    return task != null && task.getCheckRate() != null ? 
                        task.getCheckRate().doubleValue() : 0.0;
                })
                .average()
                .orElse(0.0);
            radar.setAverageSimilarity(BigDecimal.valueOf(avgSimilarity));
        } else {
            radar.setAverageSimilarity(BigDecimal.ZERO);
        }
        
        // 4. 修改次数 (简化计算，实际应统计每个论文的修改版本数)
        wrapper.clear();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getPaperStatus, PaperStatusEnum.REJECTED.getValue())
               .eq(PaperInfo::getIsDeleted, 0);
        Long revisionCount = paperInfoMapper.selectCount(wrapper);
        radar.setRevisionTimes(Math.min(revisionCount.intValue(), 5));
        
        // 5. 按时提交率 (简化计算)
        // 暂时设置为0，后续从数据库获取实际数据
        radar.setOnTimeSubmission(0);
        
        // 6. 导师评分 (简化计算)
        // 暂时设置为0，后续从数据库获取实际数据
        radar.setAdvisorRating(0);
        
        return radar;
    }
    
    /**
     * 获取相似度变化趋势（跨所有论文，按查重完成时间汇总）
     */
    public SimilarityTrendDTO getSimilarityTrend(Long studentId) {
        SimilarityTrendDTO trend = new SimilarityTrendDTO();

        // 获取该学生所有未删除的论文ID
        List<PaperInfo> papers = paperInfoMapper.selectList(
            new LambdaQueryWrapper<PaperInfo>()
                .eq(PaperInfo::getStudentId, studentId)
                .eq(PaperInfo::getIsDeleted, 0)
        );

        if (papers.isEmpty()) {
            trend.setVersions(new ArrayList<>());
            trend.setSimilarities(new ArrayList<>());
            return trend;
        }

        List<Long> paperIds = papers.stream().map(PaperInfo::getId).toList();

        // 获取这些论文的所有已完成查重任务，按时间升序
        List<CheckTask> tasks = checkTaskMapper.selectList(
            new LambdaQueryWrapper<CheckTask>()
                .in(CheckTask::getPaperId, paperIds)
                .eq(CheckTask::getIsDeleted, 0)
                .eq(CheckTask::getCheckStatus, "completed")
                .isNotNull(CheckTask::getCheckRate)
                .orderByAsc(CheckTask::getCreateTime)
        );

        List<String> versions = new ArrayList<>();
        List<BigDecimal> similarities = new ArrayList<>();

        int i = 1;
        for (CheckTask task : tasks) {
            versions.add("第" + i + "次");
            similarities.add(task.getCheckRate());
            i++;
        }

        if (versions.isEmpty()) {
            versions = new ArrayList<>();
            similarities = new ArrayList<>();
        }

        trend.setVersions(versions);
        trend.setSimilarities(similarities);

        return trend;
    }
    
    /**
     * 获取专业对比数据
     */
    public MajorComparisonDTO getMajorComparison(Long studentId) {
        MajorComparisonDTO comparison = new MajorComparisonDTO();

        List<String> dimensions = Arrays.asList("论文质量", "创新性", "规范性", "工作量", "答辩表现");
        comparison.setDimensions(dimensions);

        LambdaQueryWrapper<PaperInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getIsDeleted, 0)
               .orderByDesc(PaperInfo::getSubmitTime)
               .last("LIMIT 1");
        PaperInfo myPaper = paperInfoMapper.selectOne(wrapper);

        int myQuality = 75;
        int myInnovation = 70;
        int myNormative = 75;
        int myWorkload = 70;
        int myDefense = 75;

        if (myPaper != null) {
            if (myPaper.getFinalScore() != null) {
                myQuality = myPaper.getFinalScore().intValue();
                myDefense = myPaper.getFinalScore().intValue();
            }
            if (myPaper.getSimilarityRate() != null) {
                myQuality = Math.max(0, 100 - myPaper.getSimilarityRate().intValue());
            }
            if (myPaper.getVersion() != null) {
                myInnovation = Math.min(100, myPaper.getVersion() * 20);
            }
            if (myPaper.getWordCount() != null) {
                if (myPaper.getWordCount() > 10000) {
                    myWorkload = 90;
                } else if (myPaper.getWordCount() > 5000) {
                    myWorkload = 75;
                } else {
                    myWorkload = 60;
                }
                myNormative = Math.min(100, 60 + (myPaper.getWordCount() / 500));
            }
        }

        List<Integer> myLevel = Arrays.asList(myQuality, myInnovation, myNormative, myWorkload, myDefense);

        List<Integer> majorAverage = new ArrayList<>();
        majorAverage.add(75);
        majorAverage.add(70);
        majorAverage.add(72);
        majorAverage.add(68);
        majorAverage.add(73);

        if (myPaper != null && myPaper.getMajorId() != null) {
            wrapper.clear();
            wrapper.eq(PaperInfo::getMajorId, myPaper.getMajorId())
                   .eq(PaperInfo::getIsDeleted, 0);
            List<PaperInfo> majorPapers = paperInfoMapper.selectList(wrapper);

            if (!majorPapers.isEmpty()) {
                int sumQuality = 0, sumInnovation = 0, sumNormative = 0, sumWorkload = 0, sumDefense = 0;
                int countWithScore = 0;

                for (PaperInfo paper : majorPapers) {
                    int quality = 75;
                    int innovation = 70;
                    int normative = 75;
                    int workload = 70;
                    int defense = 75;

                    if (paper.getFinalScore() != null) {
                        quality = paper.getFinalScore().intValue();
                        defense = paper.getFinalScore().intValue();
                    }
                    if (paper.getSimilarityRate() != null) {
                        quality = Math.max(0, 100 - paper.getSimilarityRate().intValue());
                    }
                    if (paper.getVersion() != null) {
                        innovation = Math.min(100, paper.getVersion() * 20);
                    }
                    if (paper.getWordCount() != null) {
                        if (paper.getWordCount() > 10000) {
                            workload = 90;
                        } else if (paper.getWordCount() > 5000) {
                            workload = 75;
                        } else {
                            workload = 60;
                        }
                        normative = Math.min(100, 60 + (paper.getWordCount() / 500));
                    }

                    sumQuality += quality;
                    sumInnovation += innovation;
                    sumNormative += normative;
                    sumWorkload += workload;
                    sumDefense += defense;
                    countWithScore++;
                }

                if (countWithScore > 0) {
                    majorAverage.set(0, sumQuality / countWithScore);
                    majorAverage.set(1, sumInnovation / countWithScore);
                    majorAverage.set(2, sumNormative / countWithScore);
                    majorAverage.set(3, sumWorkload / countWithScore);
                    majorAverage.set(4, sumDefense / countWithScore);
                }
            }
        }

        comparison.setMyLevel(myLevel);
        comparison.setMajorAverage(majorAverage);

        return comparison;
    }
    
    /**
     * 获取待办事项列表
     */
    public List<TodoItemDTO> getTodoList(Long studentId) {
        List<TodoItemDTO> todoList = new ArrayList<>();
        
        // 1. 检查是否有待审核的论文
        LambdaQueryWrapper<PaperInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getPaperStatus, PaperStatusEnum.AUDITING.getValue())
               .eq(PaperInfo::getIsDeleted, 0);
        
        if (paperInfoMapper.selectCount(wrapper) > 0) {
            TodoItemDTO todo1 = new TodoItemDTO();
            todo1.setId(1L);
            todo1.setTitle("等待论文审核结果");
            todo1.setDescription("您有一篇论文正在审核中，请耐心等待");
            todo1.setPriority("normal");
            todo1.setCompleted(false);
            todo1.setDueDate(Date.from(LocalDate.now().plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            todoList.add(todo1);
        }
        
        // 2. 检查是否有需要修改的论文
        wrapper.clear();
        wrapper.eq(PaperInfo::getStudentId, studentId)
               .eq(PaperInfo::getPaperStatus, PaperStatusEnum.REJECTED.getValue())
               .eq(PaperInfo::getIsDeleted, 0);
        
        if (paperInfoMapper.selectCount(wrapper) > 0) {
            TodoItemDTO todo2 = new TodoItemDTO();
            todo2.setId(2L);
            todo2.setTitle("修改被退回的论文");
            todo2.setDescription("根据导师反馈修改论文内容");
            todo2.setPriority("high");
            todo2.setCompleted(false);
            todo2.setDueDate(Date.from(LocalDate.now().plusDays(14).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            todoList.add(todo2);
        }
        
        // 3. 添加其他常规待办事项
        TodoItemDTO todo3 = new TodoItemDTO();
        todo3.setId(3L);
        todo3.setTitle("准备答辩材料");
        todo3.setDescription("整理答辩PPT和相关资料");
        todo3.setPriority("medium");
        todo3.setCompleted(false);
        todo3.setDueDate(Date.from(LocalDate.now().plusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        todoList.add(todo3);
        
        return todoList;
    }
    
    /**
     * 获取通知消息列表
     */
    public List<NotificationDTO> getNotifications(Long studentId, Integer limit) {
        List<NotificationDTO> notifications = new ArrayList<>();
        
        // 从数据库获取通知数据
        // 暂时返回空列表，后续实现从数据库获取
        
        // 限制返回数量
        return limit != null && limit < notifications.size() ? 
            notifications.subList(0, limit) : notifications;
    }
    
    /**
     * 获取进度跟踪信息
     */
    public ProgressTrackingDTO getProgressTracking(Long studentId) {
        ProgressTrackingDTO progress = new ProgressTrackingDTO();
        
        // 获取最新论文状态确定当前步骤
        LatestPaperDTO latestPaper = getLatestPaper(studentId);
        if (latestPaper != null) {
            progress.setCurrentStep(getProgressFromStatus(latestPaper.getStatus()));
        } else {
            progress.setCurrentStep(0); // 未开始
        }
        
        // 从数据库获取预计完成时间
        // 暂时设置为空，后续从数据库获取
        progress.setEstimatedCompletion("");
        
        // 从数据库获取处理速度评价
        // 暂时设置为空，后续从数据库获取
        progress.setProcessingSpeed("");
        
        // 从数据库获取步骤详情
        // 暂时返回空列表，后续从数据库获取
        progress.setSteps(new ArrayList<>());
        
        return progress;
    }
}