package com.abin.checkrepeatsystem.admin.service.Impl;

import com.abin.checkrepeatsystem.admin.mapper.CheckResultMapper;
import com.abin.checkrepeatsystem.admin.service.AdminSchoolService;
import com.abin.checkrepeatsystem.admin.vo.CollegePaperStatsVO;
import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.pojo.entity.CheckResult;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysLoginLog;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.user.mapper.SysLoginLogMapper;
import com.abin.checkrepeatsystem.user.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminSchoolServiceImpl implements AdminSchoolService {

    private static final Logger log = LoggerFactory.getLogger(AdminSchoolServiceImpl.class);

    @Resource
    private SysUserService sysUserService;

    @Resource
    private PaperInfoMapper paperInfoMapper;

    @Resource
    private CheckResultMapper checkResultMapper;

    @Resource
    private SysLoginLogMapper sysLoginLogMapper;

    @Override
    public Map<String, Object> getSchoolOverview() {
        Map<String, Object> overview = new HashMap<>();

        Long totalUsers = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getIsDeleted, 0));
        Long students = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, "STUDENT")
                .eq(SysUser::getIsDeleted, 0));
        Long teachers = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, "TEACHER")
                .eq(SysUser::getIsDeleted, 0));
        Long admins = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, "ADMIN")
                .eq(SysUser::getIsDeleted, 0));

        overview.put("totalUsers", totalUsers);
        overview.put("students", students);
        overview.put("teachers", teachers);
        overview.put("admins", admins);

        Long totalPapers = paperInfoMapper.selectCount(null);
        Long pendingPapers = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.PENDING));
        Long checkedPapers = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                .ne(PaperInfo::getPaperStatus, DictConstants.PaperStatus.PENDING));

        overview.put("totalPapers", totalPapers);
        overview.put("pendingPapers", pendingPapers);
        overview.put("checkedPapers", checkedPapers);

        Long highSimilarity = checkResultMapper.selectCount(
                new LambdaQueryWrapper<CheckResult>()
                        .apply("repeat_rate >= {0}", 80));

        overview.put("highSimilarityPapers", highSimilarity);

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long onlineUsers = sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLog>()
                .ge(SysLoginLog::getLoginTime, oneHourAgo));

        overview.put("onlineUsers", onlineUsers);

        return overview;
    }

    @Override
    public Map<String, Object> getCollegeDistribution() {
        Map<String, Object> collegeStats = new HashMap<>();

        try {
            List<CollegePaperStatsVO> collegePaperStats = paperInfoMapper.selectCollegePaperStats();
            List<Map<String, Object>> result = collegePaperStats.stream()
                    .map(vo -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("collegeId", vo.getCollegeId());
                        map.put("collegeName", vo.getCollegeName());
                        map.put("paperCount", vo.getPaperCount());
                        return map;
                    })
                    .collect(Collectors.toList());

            collegeStats.put("distribution", result);
            collegeStats.put("totalCount", result.size());
        } catch (Exception e) {
            log.error("获取学院分布统计失败", e);
            collegeStats.put("distribution", new ArrayList<>());
            collegeStats.put("totalCount", 0);
        }

        return collegeStats;
    }

    @Override
    public List<Map<String, Object>> getMonthlyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

            Long submissionCount = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                    .ge(PaperInfo::getCreateTime, monthStart)
                    .le(PaperInfo::getCreateTime, monthEnd));

            Long checkCount = checkResultMapper.selectCount(new LambdaQueryWrapper<CheckResult>()
                    .ge(CheckResult::getCreateTime, monthStart)
                    .le(CheckResult::getCreateTime, monthEnd));

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
            monthData.put("submissionCount", submissionCount);
            monthData.put("checkCount", checkCount);

            trend.add(monthData);
        }

        return trend;
    }

    @Override
    public Map<String, Object> getSimilarityDistribution() {
        Map<String, Object> distribution = new HashMap<>();

        Long lowSimilarity = checkResultMapper.selectCount(new LambdaQueryWrapper<CheckResult>()
                .lt(CheckResult::getRepeatRate, new BigDecimal("30")));

        Long mediumSimilarity = checkResultMapper.selectCount(new LambdaQueryWrapper<CheckResult>()
                .ge(CheckResult::getRepeatRate, new BigDecimal("30"))
                .lt(CheckResult::getRepeatRate, new BigDecimal("60")));

        Long highSimilarity = checkResultMapper.selectCount(new LambdaQueryWrapper<CheckResult>()
                .ge(CheckResult::getRepeatRate, new BigDecimal("60")));

        distribution.put("lowSimilarity", lowSimilarity);
        distribution.put("mediumSimilarity", mediumSimilarity);
        distribution.put("highSimilarity", highSimilarity);

        return distribution;
    }

    @Override
    public Map<String, Object> getRealtimeStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long todayPapers = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                .ge(PaperInfo::getCreateTime, todayStart));
        stats.put("todayPapers", todayPapers);

        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        Long weekReviews = checkResultMapper.selectCount(
                new LambdaQueryWrapper<CheckResult>()
                        .ge(CheckResult::getCreateTime, weekStart));
        stats.put("weekReviews", weekReviews);

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long onlineUsers = sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLog>()
                .ge(SysLoginLog::getLoginTime, oneHourAgo));
        stats.put("onlineUsers", onlineUsers);

        stats.put("systemLoad", new Random().nextInt(30) + 30);

        Long activeTasks = paperInfoMapper.selectCount(new LambdaQueryWrapper<PaperInfo>()
                .in(PaperInfo::getPaperStatus, Arrays.asList(
                        DictConstants.PaperStatus.CHECKING,
                        DictConstants.PaperStatus.AUDITING)));
        stats.put("activeTasks", activeTasks);

        Long todayNewUsers = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .ge(SysUser::getCreateTime, todayStart)
                .eq(SysUser::getIsDeleted, 0));
        stats.put("todayNewUsers", todayNewUsers);

        return stats;
    }
}
