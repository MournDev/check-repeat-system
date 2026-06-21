package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.PaperSubmit;
import com.abin.checkrepeatsystem.admin.mapper.PaperSubmitMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.student.dto.*;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaperCheckHistoryServiceImpl {

    private final PaperInfoMapper paperInfoMapper;
    private final CheckTaskMapper checkTaskMapper;
    private final CheckReportMapper checkReportMapper;
    private final PaperSubmitMapper paperSubmitMapper;

    public CheckHistoryResponseDTO getCheckHistory(Long paperId, Long studentId) {
        log.info("获取论文查重历史记录 - 论文ID: {}, 学生ID: {}", paperId, studentId);

        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null || !paperInfo.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限访问该论文");
        }

        List<CheckTask> checkTasks = checkTaskMapper.selectList(
            new LambdaQueryWrapper<CheckTask>()
                .eq(CheckTask::getPaperId, paperId)
                .eq(CheckTask::getCheckStatus, DictConstants.CheckStatus.COMPLETED)
                .eq(CheckTask::getIsDeleted, 0)
                .orderByDesc(CheckTask::getCreateTime)
        );

        Set<Long> fileIds = new HashSet<>();
        Set<Long> reportIds = new HashSet<>();
        for (CheckTask task : checkTasks) {
            if (task.getFileId() != null) fileIds.add(task.getFileId());
            if (task.getReportId() != null) reportIds.add(task.getReportId());
        }

        Map<Long, PaperSubmit> submitMap = fileIds.isEmpty() ? Collections.emptyMap() :
            paperSubmitMapper.selectList(
                new LambdaQueryWrapper<PaperSubmit>()
                    .eq(PaperSubmit::getPaperId, paperId)
                    .in(PaperSubmit::getFileId, fileIds)
                    .eq(PaperSubmit::getIsDeleted, 0)
            ).stream().collect(java.util.stream.Collectors.toMap(
                PaperSubmit::getFileId, s -> s, (a, b) -> a));

        Map<Long, CheckReport> reportMap = reportIds.isEmpty() ? Collections.emptyMap() :
            checkReportMapper.selectBatchIds(reportIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                    CheckReport::getId, r -> r, (a, b) -> a));

        List<CheckHistoryDTO> history = new ArrayList<>();
        BigDecimal lowestSimilarity = null;
        BigDecimal currentSimilarity = null;

        for (int i = 0; i < checkTasks.size(); i++) {
            CheckTask task = checkTasks.get(i);
            CheckHistoryDTO historyDTO = new CheckHistoryDTO();

            historyDTO.setVersion(checkTasks.size() - i);

            PaperSubmit matchingSubmit = submitMap.get(task.getFileId());
            historyDTO.setSubmitVersion(matchingSubmit != null ? matchingSubmit.getSubmitVersion() : checkTasks.size() - i);

            if (task.getReportId() != null) {
                CheckReport report = reportMap.get(task.getReportId());
                if (report != null) {
                    historyDTO.setReportId(String.valueOf(report.getId()));
                }
            }

            historyDTO.setCheckTime(task.getCreateTime());
            historyDTO.setSimilarity(task.getCheckRate());
            historyDTO.setRating(calculateRating(task.getCheckRate()));
            historyDTO.setIsCurrent(i == 0);
            if (i == 0) {
                currentSimilarity = task.getCheckRate();
            }

            historyDTO.setChanges(generateChangesDescription(i, task.getCheckRate()));

            if (i < checkTasks.size() - 1) {
                CheckTask previousTask = checkTasks.get(i + 1);
                if (previousTask.getCheckRate() != null && task.getCheckRate() != null) {
                    BigDecimal improvement = previousTask.getCheckRate().subtract(task.getCheckRate());
                    historyDTO.setImprovementFromPrevious(improvement);
                }
            }

            historyDTO.setSectionChanges(extractSectionChangesFromReport(task));

            history.add(historyDTO);

            if (task.getCheckRate() != null && (lowestSimilarity == null || task.getCheckRate().compareTo(lowestSimilarity) < 0)) {
                lowestSimilarity = task.getCheckRate();
            }
        }

        CheckHistoryResponseDTO.TrendAnalysisDTO trendAnalysis = buildTrendAnalysis(checkTasks);

        CheckHistoryResponseDTO.PaperInfoDTO paperInfoDTO = new CheckHistoryResponseDTO.PaperInfoDTO();
        paperInfoDTO.setTitle(paperInfo.getPaperTitle());
        paperInfoDTO.setCurrentSimilarity(currentSimilarity);
        paperInfoDTO.setLowestSimilarity(lowestSimilarity);
        paperInfoDTO.setVersionCount(checkTasks.size());

        // 计算统计数据
        CheckHistoryResponseDTO.StatisticsDTO statisticsDTO = new CheckHistoryResponseDTO.StatisticsDTO();
        if (!checkTasks.isEmpty()) {
            BigDecimal earliestSim = checkTasks.get(checkTasks.size() - 1).getCheckRate();
            BigDecimal latestSim = checkTasks.get(0).getCheckRate();
            if (earliestSim != null && latestSim != null && earliestSim.compareTo(BigDecimal.ZERO) > 0) {
                int rate = earliestSim.subtract(latestSim)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(earliestSim, 0, RoundingMode.HALF_UP)
                    .intValue();
                statisticsDTO.setImprovementRate(rate);
            } else {
                statisticsDTO.setImprovementRate(0);
            }
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;
            for (CheckTask t : checkTasks) {
                if (t.getCheckRate() != null) {
                    sum = sum.add(t.getCheckRate());
                    count++;
                }
            }
            statisticsDTO.setAverageSimilarity(count > 0 ?
                sum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            statisticsDTO.setImprovementSpeed(checkTasks.size() > 1 ?
                (checkTasks.size() - 1) + "个版本" : "首个版本");
        } else {
            statisticsDTO.setImprovementRate(0);
            statisticsDTO.setAverageSimilarity(BigDecimal.ZERO);
            statisticsDTO.setImprovementSpeed("暂无数据");
        }

        CheckHistoryResponseDTO response = new CheckHistoryResponseDTO();
        response.setHistory(history);
        response.setTrendAnalysis(trendAnalysis);
        response.setPaperInfo(paperInfoDTO);
        response.setStatistics(statisticsDTO);

        log.info("查重历史记录获取成功 - 论文ID: {}, 记录数: {}", paperId, history.size());
        return response;
    }

    public SimilarityTrendDTO getSimilarityTrend(Long paperId, Long studentId, Integer period) {
        log.info("获取相似度趋势数据 - 论文ID: {}, 学生ID: {}, 周期: {}天", paperId, studentId, period);

        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null || !paperInfo.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限访问该论文");
        }

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(period);

        List<CheckTask> checkTasks = checkTaskMapper.selectList(
            new LambdaQueryWrapper<CheckTask>()
                .eq(CheckTask::getPaperId, paperId)
                .eq(CheckTask::getCheckStatus, DictConstants.CheckStatus.COMPLETED)
                .ge(CheckTask::getCreateTime, startDate)
                .le(CheckTask::getCreateTime, endDate)
                .eq(CheckTask::getIsDeleted, 0)
                .orderByAsc(CheckTask::getCreateTime)
        );

        List<String> dates = new ArrayList<>();
        List<BigDecimal> similarities = new ArrayList<>();

        for (CheckTask task : checkTasks) {
            dates.add(task.getCreateTime().toLocalDate().toString());
            similarities.add(task.getCheckRate());
        }

        if (dates.isEmpty()) {
            dates.add(endDate.toLocalDate().toString());
            similarities.add(BigDecimal.ZERO);
        }

        SimilarityTrendDTO trendDTO = new SimilarityTrendDTO();
        trendDTO.setDates(dates);
        trendDTO.setSimilarities(similarities);

        log.info("相似度趋势数据获取成功 - 论文ID: {}, 数据点数: {}", paperId, dates.size());
        return trendDTO;
    }

    public VersionCompareResponseDTO compareVersions(Long paperId, Long studentId, VersionCompareRequestDTO request) {
        log.info("版本对比分析 - 论文ID: {}, 学生ID: {}, 从版本: {}, 到版本: {}",
            paperId, studentId, request.getFromVersion(), request.getToVersion());

        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null || !paperInfo.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限访问该论文");
        }

        List<CheckTask> checkTasks = checkTaskMapper.selectList(
            new LambdaQueryWrapper<CheckTask>()
                .eq(CheckTask::getPaperId, paperId)
                .eq(CheckTask::getCheckStatus, DictConstants.CheckStatus.COMPLETED)
                .eq(CheckTask::getIsDeleted, 0)
                .orderByDesc(CheckTask::getCreateTime)
        );

        if (request.getFromVersion() < 1 || request.getToVersion() < 1 ||
            request.getFromVersion() > checkTasks.size() || request.getToVersion() > checkTasks.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "版本号超出范围");
        }

        int fromIndex = checkTasks.size() - request.getFromVersion();
        int toIndex = checkTasks.size() - request.getToVersion();

        CheckTask fromTask = checkTasks.get(fromIndex);
        CheckTask toTask = checkTasks.get(toIndex);

        BigDecimal overallChange = fromTask.getCheckRate().subtract(toTask.getCheckRate());

        List<VersionCompareResponseDTO.SectionComparisonDTO> sectionComparisons = new ArrayList<>();

        CheckReport reportFrom = checkReportMapper.selectOne(
            new LambdaQueryWrapper<CheckReport>()
                .eq(CheckReport::getTaskId, fromTask.getId())
                .eq(CheckReport::getIsDeleted, 0)
                .last("LIMIT 1")
        );

        CheckReport reportTo = checkReportMapper.selectOne(
            new LambdaQueryWrapper<CheckReport>()
                .eq(CheckReport::getTaskId, toTask.getId())
                .eq(CheckReport::getIsDeleted, 0)
                .last("LIMIT 1")
        );

        if (reportFrom != null && reportTo != null &&
            StringUtils.hasText(reportFrom.getRepeatDetails()) &&
            StringUtils.hasText(reportTo.getRepeatDetails())) {
            try {
                List<Map<String, Object>> detailsFrom = JSON.parseObject(
                    reportFrom.getRepeatDetails(),
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                List<Map<String, Object>> detailsTo = JSON.parseObject(
                    reportTo.getRepeatDetails(),
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                sectionComparisons = buildSectionComparisons(detailsFrom, detailsTo);
            } catch (Exception e) {
                log.warn("解析章节对比数据失败，使用默认章节列表");
            }
        }

        if (sectionComparisons.isEmpty()) {
            sectionComparisons = buildDefaultSectionComparisons(fromTask, toTask);
        }

        VersionCompareResponseDTO response = new VersionCompareResponseDTO();
        response.setFromVersion(request.getFromVersion());
        response.setToVersion(request.getToVersion());
        response.setOverallChange(overallChange);
        response.setSectionComparison(sectionComparisons);

        log.info("版本对比分析完成 - 论文ID: {}, 总体变化: {}", paperId, overallChange);
        return response;
    }

    public StatisticsDTO getPaperStatistics(Long paperId, Long studentId) {
        log.info("获取论文统计分析数据 - 论文ID: {}, 学生ID: {}", paperId, studentId);

        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null || !paperInfo.getStudentId().equals(studentId)) {
            throw new BusinessException(ResultCode.PERMISSION_NO_ACCESS, "无权限访问该论文");
        }

        List<CheckTask> checkTasks = checkTaskMapper.selectList(
            new LambdaQueryWrapper<CheckTask>()
                .eq(CheckTask::getPaperId, paperId)
                .eq(CheckTask::getCheckStatus, DictConstants.CheckStatus.COMPLETED)
                .eq(CheckTask::getIsDeleted, 0)
                .orderByAsc(CheckTask::getCreateTime)
        );

        if (checkTasks.isEmpty()) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "暂无查重记录");
        }

        BigDecimal firstSimilarity = checkTasks.get(0).getCheckRate();
        BigDecimal latestSimilarity = checkTasks.get(checkTasks.size() - 1).getCheckRate();

        BigDecimal improvement = firstSimilarity.subtract(latestSimilarity);
        int improvementRate = firstSimilarity.compareTo(BigDecimal.ZERO) > 0 ?
            improvement.multiply(BigDecimal.valueOf(100))
                      .divide(firstSimilarity, 0, RoundingMode.HALF_UP)
                      .intValue() :
            0;

        BigDecimal sum = BigDecimal.ZERO;
        for (CheckTask task : checkTasks) {
            sum = sum.add(task.getCheckRate());
        }
        BigDecimal averageSimilarity = sum.divide(BigDecimal.valueOf(checkTasks.size()), 2, RoundingMode.HALF_UP);

        String improvementSpeed = evaluateImprovementSpeed(checkTasks);

        StatisticsDTO statistics = new StatisticsDTO();
        statistics.setImprovementRate(improvementRate);
        statistics.setAverageSimilarity(averageSimilarity);
        statistics.setImprovementSpeed(improvementSpeed);
        statistics.setTotalChecks(checkTasks.size());
        statistics.setFirstCheckSimilarity(firstSimilarity);
        statistics.setLatestCheckSimilarity(latestSimilarity);

        log.info("论文统计分析完成 - 论文ID: {}, 总查重次数: {}", paperId, checkTasks.size());
        return statistics;
    }

    private String calculateRating(BigDecimal similarity) {
        if (similarity == null) return "unknown";
        double rate = similarity.doubleValue();
        if (rate <= 10) return "excellent";
        if (rate <= 20) return "good";
        if (rate <= 30) return "fair";
        return "poor";
    }

    private List<VersionCompareResponseDTO.SectionComparisonDTO> buildSectionComparisons(
            List<Map<String, Object>> detailsFrom,
            List<Map<String, Object>> detailsTo) {

        List<VersionCompareResponseDTO.SectionComparisonDTO> comparisons = new ArrayList<>();

        try {
            for (Map<String, Object> detail : detailsFrom) {
                if (detail.containsKey("section")) {
                    String sectionName = detail.get("section").toString();
                    BigDecimal fromRate = detail.containsKey("similarity") ?
                        new BigDecimal(detail.get("similarity").toString()) : BigDecimal.ZERO;

                    BigDecimal toRate = BigDecimal.ZERO;
                    for (Map<String, Object> toDetail : detailsTo) {
                        if (sectionName.equals(toDetail.get("section"))) {
                            toRate = toDetail.containsKey("similarity") ?
                                new BigDecimal(toDetail.get("similarity").toString()) : BigDecimal.ZERO;
                            break;
                        }
                    }

                    VersionCompareResponseDTO.SectionComparisonDTO dto =
                        new VersionCompareResponseDTO.SectionComparisonDTO();
                    dto.setName(sectionName);
                    dto.setFrom(fromRate);
                    dto.setTo(toRate);
                    dto.setChange(fromRate.subtract(toRate));

                    comparisons.add(dto);
                }
            }
        } catch (Exception e) {
            log.error("构建章节对比数据失败", e);
        }

        return comparisons;
    }

    private List<VersionCompareResponseDTO.SectionComparisonDTO> buildDefaultSectionComparisons(
            CheckTask fromTask, CheckTask toTask) {

        List<VersionCompareResponseDTO.SectionComparisonDTO> comparisons = new ArrayList<>();

        // 尝试从报告中提取章节数据
        Map<String, BigDecimal> fromSections = extractSectionSimilarities(fromTask);
        Map<String, BigDecimal> toSections = extractSectionSimilarities(toTask);

        if (!fromSections.isEmpty() || !toSections.isEmpty()) {
            Set<String> allSections = new LinkedHashSet<>();
            allSections.addAll(fromSections.keySet());
            allSections.addAll(toSections.keySet());

            for (String section : allSections) {
                VersionCompareResponseDTO.SectionComparisonDTO dto =
                    new VersionCompareResponseDTO.SectionComparisonDTO();
                BigDecimal fromRate = fromSections.getOrDefault(section, BigDecimal.ZERO);
                BigDecimal toRate = toSections.getOrDefault(section, BigDecimal.ZERO);
                dto.setName(section);
                dto.setFrom(fromRate);
                dto.setTo(toRate);
                dto.setChange(fromRate.subtract(toRate));
                comparisons.add(dto);
            }
        } else {
            // 无章节数据时，仅展示总体对比
            VersionCompareResponseDTO.SectionComparisonDTO dto =
                new VersionCompareResponseDTO.SectionComparisonDTO();
            dto.setName("总体相似度");
            dto.setFrom(fromTask.getCheckRate() != null ? fromTask.getCheckRate() : BigDecimal.ZERO);
            dto.setTo(toTask.getCheckRate() != null ? toTask.getCheckRate() : BigDecimal.ZERO);
            BigDecimal from = dto.getFrom();
            BigDecimal to = dto.getTo();
            dto.setChange(from.subtract(to));
            comparisons.add(dto);
        }

        return comparisons;
    }

    private Map<String, BigDecimal> extractSectionSimilarities(CheckTask task) {
        Map<String, BigDecimal> sections = new LinkedHashMap<>();
        if (task.getReportId() == null) return sections;

        CheckReport report = checkReportMapper.selectById(task.getReportId());
        if (report == null || !StringUtils.hasText(report.getRepeatDetails())) return sections;

        try {
            List<Map<String, Object>> details = JSON.parseObject(
                report.getRepeatDetails(),
                new TypeReference<List<Map<String, Object>>>() {}
            );
            if (details == null) return sections;

            for (Map<String, Object> detail : details) {
                String name = null;
                if (detail.containsKey("section")) name = detail.get("section").toString();
                else if (detail.containsKey("chapter")) name = detail.get("chapter").toString();
                else if (detail.containsKey("source")) name = detail.get("source").toString();

                if (name != null && detail.containsKey("similarity")) {
                    Object simValue = detail.get("similarity");
                    BigDecimal sim = null;
                    if (simValue instanceof Number) {
                        sim = new BigDecimal(simValue.toString());
                    } else if (simValue instanceof String) {
                        try { sim = new BigDecimal((String) simValue); } catch (NumberFormatException ignored) {}
                    }
                    if (sim != null) sections.put(name, sim);
                }
            }
        } catch (Exception e) {
            log.warn("解析章节相似度数据失败: {}", e.getMessage());
        }

        return sections;
    }

    private String generateChangesDescription(int versionIndex, BigDecimal similarity) {
        if (versionIndex == 0) return "初次提交查重";

        if (similarity == null) return "第" + (versionIndex + 1) + "次查重";

        double rate = similarity.doubleValue();
        if (rate < 15) return "查重率" + String.format("%.1f", rate) + "%，原创性优秀";
        if (rate < 30) return "查重率" + String.format("%.1f", rate) + "%，需小幅修改";
        if (rate < 50) return "查重率" + String.format("%.1f", rate) + "%，需重点修改重复段落";
        return "查重率" + String.format("%.1f", rate) + "%，需大幅重写高重复内容";
    }

    private Map<String, CheckHistoryDTO.SectionChangeDTO> extractSectionChangesFromReport(CheckTask task) {
        Map<String, CheckHistoryDTO.SectionChangeDTO> sectionChanges = new HashMap<>();

        try {
            if (task.getReportId() == null) {
                return sectionChanges;
            }

            CheckReport report = checkReportMapper.selectById(task.getReportId());
            if (report == null || !StringUtils.hasText(report.getRepeatDetails())) {
                return sectionChanges;
            }

            List<Map<String, Object>> details = JSON.parseObject(
                report.getRepeatDetails(),
                new TypeReference<List<Map<String, Object>>>() {}
            );

            if (details == null || details.isEmpty()) {
                return sectionChanges;
            }

            for (Map<String, Object> detail : details) {
                String sectionName = null;
                BigDecimal similarity = null;

                if (detail.containsKey("section")) {
                    sectionName = detail.get("section").toString();
                } else if (detail.containsKey("chapter")) {
                    sectionName = detail.get("chapter").toString();
                } else if (detail.containsKey("source")) {
                    sectionName = detail.get("source").toString();
                }

                if (detail.containsKey("similarity")) {
                    Object simValue = detail.get("similarity");
                    if (simValue instanceof Number) {
                        similarity = new BigDecimal(simValue.toString());
                    } else if (simValue instanceof String) {
                        try {
                            similarity = new BigDecimal((String) simValue);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析相似度值：{}", simValue);
                        }
                    }
                }

                if (sectionName != null && similarity != null) {
                    CheckHistoryDTO.SectionChangeDTO sectionChange = new CheckHistoryDTO.SectionChangeDTO();
                    sectionChange.setFrom(similarity);
                    sectionChange.setTo(similarity);
                    sectionChange.setChange(BigDecimal.ZERO);

                    sectionChanges.put(sectionName, sectionChange);
                }
            }

            if (sectionChanges.isEmpty()) {
                sectionChanges = buildDefaultSectionChanges(task);
            }

        } catch (Exception e) {
            log.error("从查重报告提取章节变化失败 - taskId: {}", task.getId(), e);
            sectionChanges = buildDefaultSectionChanges(task);
        }

        return sectionChanges;
    }

    private Map<String, CheckHistoryDTO.SectionChangeDTO> buildDefaultSectionChanges(CheckTask task) {
        Map<String, CheckHistoryDTO.SectionChangeDTO> sectionChanges = new HashMap<>();

        BigDecimal overallSimilarity = task.getCheckRate() != null ? task.getCheckRate() : BigDecimal.ZERO;

        // 无章节详情时，仅展示总体相似度
        CheckHistoryDTO.SectionChangeDTO sectionChange = new CheckHistoryDTO.SectionChangeDTO();
        sectionChange.setFrom(overallSimilarity);
        sectionChange.setTo(overallSimilarity);
        sectionChange.setChange(BigDecimal.ZERO);
        sectionChanges.put("总体相似度", sectionChange);

        return sectionChanges;
    }

    private CheckHistoryResponseDTO.TrendAnalysisDTO buildTrendAnalysis(List<CheckTask> checkTasks) {
        CheckHistoryResponseDTO.TrendAnalysisDTO trend = new CheckHistoryResponseDTO.TrendAnalysisDTO();

        if (checkTasks.size() < 2) {
            trend.setDirection("stable");
            trend.setTotalImprovement(BigDecimal.ZERO);
            trend.setAverageImprovementPerVersion(BigDecimal.ZERO);
            trend.setBestVersion(1);
            return trend;
        }

        BigDecimal firstRate = checkTasks.get(checkTasks.size() - 1).getCheckRate();
        BigDecimal lastRate = checkTasks.get(0).getCheckRate();
        if (firstRate == null) firstRate = BigDecimal.ZERO;
        if (lastRate == null) lastRate = BigDecimal.ZERO;
        BigDecimal totalImprovement = firstRate.subtract(lastRate);

        String direction = totalImprovement.compareTo(BigDecimal.ZERO) < 0 ? "decreasing" :
                          totalImprovement.compareTo(BigDecimal.ZERO) > 0 ? "increasing" : "stable";

        BigDecimal averageImprovement = totalImprovement.divide(
            BigDecimal.valueOf(checkTasks.size() - 1), 2, RoundingMode.HALF_UP);

        int bestVersion = 1;
        BigDecimal lowestRate = firstRate;
        for (int i = 0; i < checkTasks.size(); i++) {
            BigDecimal rate = checkTasks.get(i).getCheckRate();
            if (rate == null) continue;
            if (rate.compareTo(lowestRate) < 0) {
                lowestRate = rate;
                bestVersion = checkTasks.size() - i;
            }
        }

        trend.setDirection(direction);
        trend.setTotalImprovement(totalImprovement.abs());
        trend.setAverageImprovementPerVersion(averageImprovement.abs());
        trend.setBestVersion(bestVersion);

        return trend;
    }

    private String evaluateImprovementSpeed(List<CheckTask> checkTasks) {
        if (checkTasks.size() < 2) return "暂无数据";

        BigDecimal totalImprovement = checkTasks.get(0).getCheckRate()
            .subtract(checkTasks.get(checkTasks.size() - 1).getCheckRate());

        double avgImprovement = Math.abs(totalImprovement.doubleValue() / (checkTasks.size() - 1));

        if (avgImprovement >= 10) return "很快";
        if (avgImprovement >= 5) return "较快";
        if (avgImprovement >= 2) return "一般";
        return "较慢";
    }
}
