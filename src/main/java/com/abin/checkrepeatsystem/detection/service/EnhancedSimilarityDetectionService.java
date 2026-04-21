package com.abin.checkrepeatsystem.detection.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.CheckTaskStatusEnum;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.TextSimilarityUtils;
import com.abin.checkrepeatsystem.detection.dto.SimilarityDetectionResult;
import com.abin.checkrepeatsystem.detection.dto.SimilaritySegment;
import com.abin.checkrepeatsystem.monitor.service.ApplicationMonitorService;
import com.abin.checkrepeatsystem.pojo.entity.CheckResult;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.admin.mapper.CheckResultMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 增强版查重检测服务
 * 提供多维度相似度计算、智能段落比对、可视化报告生成等功能
 */
@Slf4j
@Service
public class EnhancedSimilarityDetectionService {

    @Resource
    private PaperInfoMapper paperInfoMapper;

    @Resource
    private CheckTaskMapper checkTaskMapper;

    @Resource
    private CheckResultMapper checkResultMapper;

    @Resource
    private PaperContentExtractor contentExtractor;

    @Resource
    private TextSimilarityUtils textSimilarityUtils;

    @Resource
    private CheckProgressWebSocketService progressWebSocketService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ApplicationMonitorService monitorService;

    @Value("${check.task.max-concurrent:10}")
    private int maxConcurrentTasks;

    @Value("${check.task.timeout:3600000}")
    private long taskTimeout;

    // 用于并行处理的线程池 - 使用有界队列防止OOM
    private final ThreadPoolExecutor executorService;

    public EnhancedSimilarityDetectionService() {
        // 创建自定义线程池：核心线程数、最大线程数、有界队列
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 2;
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "check-thread-" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        this.executorService = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * 执行完整的论文查重检测（带任务管理）
     *
     * @param paperId        待检测论文ID
     * @param targetPaperIds 比对论文ID列表（null表示全库比对）
     * @return 查重检测结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<SimilarityDetectionResult> detectPaperSimilarity(Long paperId, List<Long> targetPaperIds) {
        // 生成任务编号
        String taskNo = generateTaskNo();
        log.info("开始论文查重检测: taskNo={}, paperId={}, targetCount={}",
                taskNo, paperId, targetPaperIds != null ? targetPaperIds.size() : "全库");

        // 1. 创建查重任务记录
        CheckTask checkTask = createCheckTask(paperId, taskNo);
        Long userId = getPaperStudentId(paperId);

        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            // 2. 更新任务状态为进行中
            updateTaskStatus(checkTask.getId(), CheckTaskStatusEnum.CHECKING, null);

            // 3. 发送WebSocket开始消息
            try {
                progressWebSocketService.sendCheckStart(paperId, userId, taskNo, 0);
            } catch (Exception e) {
                log.warn("WebSocket推送失败（任务开始）: {}", e.getMessage());
            }

            // 4. 执行查重检测
            SimilarityDetectionResult result = performDetection(paperId, targetPaperIds, checkTask, userId);

            // 5. 保存查重结果
            saveCheckResult(checkTask, result);

            // 6. 更新论文的查重状态
            updatePaperCheckStatus(paperId, result);

            // 7. 更新任务状态为完成
            updateTaskStatus(checkTask.getId(), CheckTaskStatusEnum.COMPLETED, null);

            // 8. 发送WebSocket完成消息
            try {
                progressWebSocketService.sendCheckComplete(paperId, userId,
                        result.getOverallSimilarity(), result.getRiskLevel());
            } catch (Exception e) {
                log.warn("WebSocket推送失败（任务完成）: {}", e.getMessage());
            }

            success = true;
            log.info("论文查重检测完成: taskNo={}, paperId={}, similarity={}%",
                    taskNo, paperId, result.getOverallSimilarity());

            return Result.success("查重检测完成", result);

        } catch (Exception e) {
            log.error("论文查重检测失败: taskNo={}, paperId={}", taskNo, paperId, e);
            // 更新任务状态为失败
            updateTaskStatus(checkTask.getId(), CheckTaskStatusEnum.FAILURE, e.getMessage());

            // 发送WebSocket失败消息
            try {
                progressWebSocketService.sendCheckError(paperId, userId, e.getMessage());
            } catch (Exception wsEx) {
                log.warn("WebSocket推送失败（任务失败）: {}", wsEx.getMessage());
            }

            return Result.error(ResultCode.SYSTEM_ERROR, "查重检测失败: " + e.getMessage());
        } finally {
            // 记录查重任务执行时间
            long duration = System.currentTimeMillis() - startTime;
            monitorService.recordCheckTaskTime(paperId, duration, success);
        }
    }

    /**
     * 执行查重检测核心逻辑
     */
    private SimilarityDetectionResult performDetection(Long paperId, List<Long> targetPaperIds, CheckTask checkTask, Long userId) {
        // 尝试从缓存获取查重结果
        String cacheKey = "check:result:" + paperId + ":" + (targetPaperIds != null ? String.join(",", targetPaperIds.stream().map(String::valueOf).collect(Collectors.toList())) : "all");
        try {
            SimilarityDetectionResult cachedResult = (SimilarityDetectionResult) redisTemplate.opsForValue().get(cacheKey);
            if (cachedResult != null) {
                log.info("从缓存获取查重结果: paperId={}", paperId);
                return cachedResult;
            }
        } catch (Exception e) {
            log.warn("Redis缓存读取失败: {}", e.getMessage());
        }

        // 1. 提取待检测论文内容
        String targetContent = contentExtractor.extractSegmentedContent(paperId);
        if (targetContent == null || targetContent.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "论文内容为空，无法进行查重");
        }

        // 2. 获取待检测论文信息
        PaperInfo targetPaper = paperInfoMapper.selectById(paperId);
        if (targetPaper == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "待检测论文不存在");
        }

        // 3. 获取比对论文列表
        List<PaperInfo> comparisonPapers = getComparisonPapers(targetPaperIds, targetPaper.getCollegeId(), paperId);
        if (comparisonPapers.isEmpty()) {
            log.warn("无可用的比对论文，返回空结果: paperId={}", paperId);
            return createEmptyResult(targetPaper);
        }

        log.info("开始并行查重检测: paperId={}, comparisonCount={}", paperId, comparisonPapers.size());

        // 4. 执行并行查重检测（带进度推送）
        SimilarityDetectionResult result = performParallelDetectionWithProgress(
                targetContent, comparisonPapers, targetPaper, paperId, userId);

        // 5. 生成检测报告
        generateDetectionReport(result, targetPaper, comparisonPapers);

        // 缓存查重结果（有效期1小时）
        try {
            redisTemplate.opsForValue().set(cacheKey, result, 1, TimeUnit.HOURS);
            log.info("查重结果已缓存: paperId={}", paperId);
        } catch (Exception e) {
            log.warn("Redis缓存写入失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 创建查重任务记录
     */
    private CheckTask createCheckTask(Long paperId, String taskNo) {
        CheckTask task = new CheckTask();
        task.setPaperId(paperId);
        task.setTaskNo(taskNo);
        task.setCheckStatus(CheckTaskStatusEnum.PENDING.getCode());
        task.setStartTime(LocalDateTime.now());
        task.setFileId(getPaperFileId(paperId));

        checkTaskMapper.insert(task);
        log.info("查重任务创建成功: taskNo={}, taskId={}", taskNo, task.getId());
        return task;
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(Long taskId, CheckTaskStatusEnum status, String failReason) {
        CheckTask task = new CheckTask();
        task.setId(taskId);
        task.setCheckStatus(status.getCode());

        if (status == CheckTaskStatusEnum.COMPLETED || status == CheckTaskStatusEnum.FAILURE) {
            task.setEndTime(LocalDateTime.now());
        }

        if (failReason != null) {
            task.setFailReason(failReason.length() > 500 ? failReason.substring(0, 500) : failReason);
        }

        checkTaskMapper.updateById(task);
    }

    /**
     * 保存查重结果
     */
    private void saveCheckResult(CheckTask checkTask, SimilarityDetectionResult result) {
        CheckResult checkResult = new CheckResult();
        checkResult.setTaskId(checkTask.getId());
        checkResult.setPaperId(checkTask.getPaperId());
        checkResult.setRepeatRate(BigDecimal.valueOf(result.getOverallSimilarity()));
        checkResult.setCheckTime(LocalDateTime.now());
        checkResult.setCheckSource("LOCAL");

        // 保存相似段落详情（JSON格式）
        if (result.getSimilarSegments() != null && !result.getSimilarSegments().isEmpty()) {
            String segmentsJson = convertSegmentsToJson(result.getSimilarSegments());
            checkResult.setCheckDetails(segmentsJson);
        }

        // 保存统计信息
        if (result.getStatistics() != null) {
            checkResult.setCheckDetails(convertStatisticsToJson(result.getStatistics()));
        }

        checkResultMapper.insert(checkResult);

        // 更新任务的报告ID
        checkTask.setReportId(checkResult.getId());
        checkTaskMapper.updateById(checkTask);

        log.info("查重结果保存成功: taskId={}, resultId={}", checkTask.getId(), checkResult.getId());
    }

    /**
     * 更新论文的查重状态
     */
    private void updatePaperCheckStatus(Long paperId, SimilarityDetectionResult result) {
        PaperInfo paperInfo = new PaperInfo();
        paperInfo.setId(paperId);
        paperInfo.setSimilarityRate(BigDecimal.valueOf(result.getOverallSimilarity()));
        paperInfo.setCheckResult(result.getRiskLevel());
        paperInfo.setCheckTime(LocalDateTime.now());
        paperInfo.setCheckCompleted(1);
        paperInfo.setCheckEngineType("SIMHASH_COSINE");

        paperInfoMapper.updateById(paperInfo);
    }

    /**
     * 获取比对论文列表
     */
    private List<PaperInfo> getComparisonPapers(List<Long> targetPaperIds, Long excludeCollegeId, Long excludePaperId) {
        List<PaperInfo> papers = new ArrayList<>();

        try {
            if (targetPaperIds != null && !targetPaperIds.isEmpty()) {
                // 指定比对论文
                for (Long paperId : targetPaperIds) {
                    if (paperId.equals(excludePaperId)) {
                        continue; // 跳过自己
                    }
                    PaperInfo paper = paperInfoMapper.selectById(paperId);
                    if (paper != null && paper.getIsDeleted() != null && paper.getIsDeleted() == 0) {
                        papers.add(paper);
                    }
                }
            } else {
                // 全库比对（排除同学院和当前论文）
                LambdaQueryWrapper<PaperInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(PaperInfo::getIsDeleted, 0)
                        .ne(PaperInfo::getId, excludePaperId)
                        .isNotNull(PaperInfo::getFileId)
                        .or()
                        .isNotNull(PaperInfo::getFilePath);

                // 限制比对数量，避免性能问题
                wrapper.last("LIMIT 100");

                papers = paperInfoMapper.selectList(wrapper);
            }
        } catch (Exception e) {
            log.error("获取比对论文列表失败", e);
        }

        return papers;
    }

    /**
     * 执行并行查重检测（带进度推送）
     */
    private SimilarityDetectionResult performParallelDetectionWithProgress(String targetContent,
                                                                            List<PaperInfo> comparisonPapers,
                                                                            PaperInfo targetPaper,
                                                                            Long paperId,
                                                                            Long userId) {
        SimilarityDetectionResult result = new SimilarityDetectionResult();
        List<Future<SimilaritySegment>> futures = new ArrayList<>();
        int totalPapers = comparisonPapers.size();

        // 发送开始进度消息
        progressWebSocketService.sendCheckStart(paperId, userId, "", totalPapers);

        // 为每篇比对论文创建异步检测任务
        for (PaperInfo paper : comparisonPapers) {
            Future<SimilaritySegment> future = executorService.submit(() -> {
                return detectSimilarityWithPaper(targetContent, paper);
            });
            futures.add(future);
        }

        // 等待所有任务完成
        List<SimilaritySegment> segments = new ArrayList<>();
        int completedCount = 0;
        for (Future<SimilaritySegment> future : futures) {
            try {
                SimilaritySegment segment = future.get(30, TimeUnit.SECONDS); // 单个任务超时30秒
                if (segment != null && segment.getSimilarity() > 0) {
                    segments.add(segment);
                }
                completedCount++;

                // 每完成5个任务推送一次进度
                if (completedCount % 5 == 0 || completedCount == totalPapers) {
                    progressWebSocketService.sendCheckProgress(paperId, userId, completedCount, totalPapers);
                    log.debug("查重进度: {}/{} ({}%)", completedCount, totalPapers,
                            (int) ((completedCount * 100.0) / totalPapers));
                }
            } catch (TimeoutException e) {
                log.warn("单篇论文查重超时");
                future.cancel(true);
            } catch (Exception e) {
                log.warn("单篇论文查重失败: {}", e.getMessage());
            }
        }

        // 按相似度降序排列
        segments.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

        // 只保留前20个最相似的结果
        if (segments.size() > 20) {
            segments = segments.subList(0, 20);
        }

        // 计算总体相似度
        double overallSimilarity = calculateOverallSimilarity(segments);

        result.setTargetPaperId(targetPaper.getId());
        result.setTargetPaperTitle(targetPaper.getPaperTitle());
        result.setTargetAuthor(getPaperAuthor(targetPaper));
        result.setTargetCollege(targetPaper.getCollegeName());
        result.setOverallSimilarity(overallSimilarity);
        result.setSimilarSegments(segments);
        result.setTotalComparisons(comparisonPapers.size());
        result.setDetectionTime(new Date());

        return result;
    }

    /**
     * 与单篇论文进行相似度检测
     */
    private SimilaritySegment detectSimilarityWithPaper(String targetContent, PaperInfo comparisonPaper) {
        SimilaritySegment segment = new SimilaritySegment();
        segment.setPaperId(comparisonPaper.getId());
        segment.setPaperTitle(comparisonPaper.getPaperTitle());
        segment.setAuthor(getPaperAuthor(comparisonPaper));
        segment.setCollege(comparisonPaper.getCollegeName());

        try {
            // 提取比对论文的分词内容
            String comparisonContent = contentExtractor.extractSegmentedContent(comparisonPaper.getId());
            if (comparisonContent != null && !comparisonContent.trim().isEmpty()) {
                // 执行多维度相似度计算
                double similarity = textSimilarityUtils.calculateComprehensiveSimilarity(
                        targetContent, comparisonContent);

                // 保留2位小数
                similarity = Math.round(similarity * 100) / 100.0;
                segment.setSimilarity(similarity);

                // 提取重复片段（仅当相似度大于5%时）
                if (similarity > 5.0) {
                    List<String> repeatedFragments = extractRepeatedFragments(
                            targetContent, comparisonContent, similarity);
                    segment.setRepeatedFragments(repeatedFragments);
                }
            } else {
                segment.setSimilarity(0.0);
            }
        } catch (Exception e) {
            log.warn("与论文{}比对失败: {}", comparisonPaper.getId(), e.getMessage());
            segment.setSimilarity(0.0);
        }

        return segment;
    }

    /**
     * 计算总体相似度
     */
    private double calculateOverallSimilarity(List<SimilaritySegment> segments) {
        if (segments.isEmpty()) {
            return 0.0;
        }

        // 取最高相似度作为总体相似度（最严格的方式）
        double maxSimilarity = segments.stream()
                .mapToDouble(SimilaritySegment::getSimilarity)
                .max()
                .orElse(0.0);

        // 或者使用加权平均（考虑相似论文数量）
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (SimilaritySegment segment : segments) {
            double similarity = segment.getSimilarity();
            // 高相似度论文权重更大
            double weight = Math.pow(similarity / 100.0, 2);
            weightedSum += similarity * weight;
            totalWeight += weight;
        }

        double weightedAvg = totalWeight > 0 ? weightedSum / totalWeight : 0.0;

        // 取最大值和加权平均的较大者
        double overallSimilarity = Math.max(maxSimilarity, weightedAvg);

        // 保留2位小数
        return Math.round(overallSimilarity * 100) / 100.0;
    }

    /**
     * 提取重复片段
     */
    private List<String> extractRepeatedFragments(String content1, String content2, double similarity) {
        List<String> fragments = new ArrayList<>();

        if (similarity < 5.0) {
            return fragments;
        }

        try {
            // 将内容按句子分割
            String[] sentences1 = content1.split("[。！？.!?]");
            String[] sentences2 = content2.split("[。！？.!?]");

            Set<String> sentenceSet2 = new HashSet<>(Arrays.asList(sentences2));

            // 找出相似的句子
            for (String sentence : sentences1) {
                String trimmedSentence = sentence.trim();
                if (trimmedSentence.length() < 10) {
                    continue; // 跳过太短的句子
                }

                for (String s2 : sentenceSet2) {
                    String trimmedS2 = s2.trim();
                    if (trimmedS2.length() < 10) {
                        continue;
                    }

                    // 计算句子相似度
                    double sentenceSim = calculateSentenceSimilarity(trimmedSentence, trimmedS2);
                    if (sentenceSim > 0.8) { // 句子相似度阈值
                        fragments.add(trimmedSentence);
                        break;
                    }
                }

                if (fragments.size() >= 5) {
                    break; // 最多返回5个片段
                }
            }
        } catch (Exception e) {
            log.warn("提取重复片段失败: {}", e.getMessage());
        }

        return fragments;
    }

    /**
     * 计算句子相似度（基于公共子串）
     */
    private double calculateSentenceSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        int maxLen = 0;
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    maxLen = Math.max(maxLen, dp[i][j]);
                }
            }
        }

        // 使用最长公共子串长度计算相似度
        return (double) maxLen / Math.max(s1.length(), s2.length());
    }

    /**
     * 生成检测报告
     */
    private void generateDetectionReport(SimilarityDetectionResult result,
                                         PaperInfo targetPaper,
                                         List<PaperInfo> comparisonPapers) {
        // 风险等级评估
        result.setRiskLevel(evaluateRiskLevel(result.getOverallSimilarity()));

        // 建议措施
        result.setRecommendations(generateRecommendations(result.getOverallSimilarity()));

        // 统计信息
        Map<String, Object> statistics = new HashMap<>();
        long highSimilarityCount = result.getSimilarSegments().stream()
                .filter(s -> s.getSimilarity() > 30).count();
        long mediumSimilarityCount = result.getSimilarSegments().stream()
                .filter(s -> s.getSimilarity() > 15 && s.getSimilarity() <= 30).count();
        long lowSimilarityCount = result.getSimilarSegments().stream()
                .filter(s -> s.getSimilarity() <= 15).count();

        statistics.put("highSimilarityCount", highSimilarityCount);
        statistics.put("mediumSimilarityCount", mediumSimilarityCount);
        statistics.put("lowSimilarityCount", lowSimilarityCount);
        statistics.put("totalCompared", comparisonPapers.size());
        statistics.put("detectionTime", new Date());

        result.setStatistics(statistics);
    }

    /**
     * 评估风险等级
     */
    private String evaluateRiskLevel(double similarity) {
        if (similarity <= 15) return "低风险";
        if (similarity <= 30) return "中等风险";
        if (similarity <= 50) return "高风险";
        return "极高风险";
    }

    /**
     * 生成建议措施
     */
    private List<String> generateRecommendations(double similarity) {
        List<String> recommendations = new ArrayList<>();

        if (similarity <= 15) {
            recommendations.add("论文原创性良好，请继续保持");
            recommendations.add("建议检查引用格式是否规范");
        } else if (similarity <= 30) {
            recommendations.add("存在一定程度的重复内容，需要适当修改");
            recommendations.add("重点检查高相似度段落的表述方式");
            recommendations.add("加强文献综述部分的原创性表达");
        } else {
            recommendations.add("重复率较高，建议大幅修改相关内容");
            recommendations.add("重新组织高相似度段落的内容结构");
            recommendations.add("增加原创性分析和观点阐述");
            recommendations.add("严格按照学术规范处理引用内容");
        }

        return recommendations;
    }

    /**
     * 创建空结果（当没有比对论文时）
     */
    private SimilarityDetectionResult createEmptyResult(PaperInfo targetPaper) {
        SimilarityDetectionResult result = new SimilarityDetectionResult();
        result.setTargetPaperId(targetPaper.getId());
        result.setTargetPaperTitle(targetPaper.getPaperTitle());
        result.setTargetAuthor(getPaperAuthor(targetPaper));
        result.setTargetCollege(targetPaper.getCollegeName());
        result.setOverallSimilarity(0.0);
        result.setSimilarSegments(new ArrayList<>());
        result.setTotalComparisons(0);
        result.setDetectionTime(new Date());
        result.setRiskLevel("低风险");
        result.setRecommendations(Collections.singletonList("暂无可比对的论文库数据"));
        result.setStatistics(new HashMap<>());
        return result;
    }

    /**
     * 获取论文作者信息
     */
    private String getPaperAuthor(PaperInfo paper) {
        if (paper.getAuthor() != null && !paper.getAuthor().isEmpty()) {
            return paper.getAuthor();
        }
        return "作者" + paper.getStudentId();
    }

    /**
     * 获取论文文件ID
     */
    private Long getPaperFileId(Long paperId) {
        PaperInfo paper = paperInfoMapper.selectById(paperId);
        return paper != null ? paper.getFileId() : null;
    }

    /**
     * 获取论文所属学生ID
     */
    private Long getPaperStudentId(Long paperId) {
        PaperInfo paper = paperInfoMapper.selectById(paperId);
        return paper != null ? paper.getStudentId() : null;
    }

    /**
     * 生成任务编号
     */
    private String generateTaskNo() {
        return "CHECK" + System.currentTimeMillis();
    }

    /**
     * 将相似段落转换为JSON
     */
    private String convertSegmentsToJson(List<SimilaritySegment> segments) {
        // 简化实现，实际可以使用JSON库
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < segments.size(); i++) {
            SimilaritySegment seg = segments.get(i);
            sb.append("{");
            sb.append("\"paperId\":").append(seg.getPaperId()).append(",");
            sb.append("\"paperTitle\":\"").append(seg.getPaperTitle()).append("\",");
            sb.append("\"similarity\":").append(seg.getSimilarity());
            sb.append("}");
            if (i < segments.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 将统计信息转换为JSON
     */
    private String convertStatisticsToJson(Map<String, Object> statistics) {
        // 简化实现
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : statistics.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else {
                sb.append(entry.getValue());
            }
            if (i < statistics.size() - 1) {
                sb.append(",");
            }
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    // 内部业务异常类
    private static class BusinessException extends RuntimeException {
        private final ResultCode resultCode;

        public BusinessException(ResultCode resultCode, String message) {
            super(message);
            this.resultCode = resultCode;
        }

        public ResultCode getResultCode() {
            return resultCode;
        }
    }
}