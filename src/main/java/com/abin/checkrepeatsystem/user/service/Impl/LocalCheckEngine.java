package com.abin.checkrepeatsystem.user.service.Impl;

import com.abin.checkrepeatsystem.common.enums.CheckEngineTypeEnum;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.service.PaperContentMinioService;
import com.abin.checkrepeatsystem.common.utils.IKAnalyzerUtils;
import com.abin.checkrepeatsystem.common.utils.SpringContextUtil;
import com.abin.checkrepeatsystem.common.utils.TextSimilarityUtils;
import com.abin.checkrepeatsystem.detection.service.PaperContentExtractor;
import com.abin.checkrepeatsystem.detection.util.ReferenceExtractor;
import com.abin.checkrepeatsystem.detection.util.TextPreprocessor;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.vo.CheckResult;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.user.service.CheckEngine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class LocalCheckEngine implements CheckEngine {
    
    @Value("${check.local.enabled:true}")
    private boolean enabled;
    
    @Value("${check.local.cache-enabled:true}")
    private boolean cacheEnabled;

    private final PaperInfoMapper paperInfoMapper; // 论文信息Mapper
    private final TextSimilarityUtils textSimilarityUtils; // 现有相似度工具类
    private final PaperContentMinioService paperContentMinioService; // MinIO存储服务
    private final ReferenceExtractor referenceExtractor; // 引用文献提取器
    private final TextPreprocessor textPreprocessor; // 文本预处理工具

    // SimHash 缓存：paperId → SimHash，避免每次查重重复计算
    private final Map<Long, BigInteger> simHashCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("本地查重引擎初始化完成，enabled={}, cacheEnabled={}", enabled, cacheEnabled);
    }
    
    @Override
    public CheckEngineTypeEnum getEngineType() {
        return CheckEngineTypeEnum.LOCAL;
    }
    
    @Override
    public CheckResult check(String paperContent, String paperTitle) {
        CheckResult result = new CheckResult();

        // 0. 文本预处理（去除目录、章节标题、页码）
        TextPreprocessor.PreprocessOptions options = new TextPreprocessor.PreprocessOptions()
                .removeTableOfContents(true)
                .removeChapterTitles(true)
                .removePageNumbers(true);
        String processedContent = textPreprocessor.preprocess(paperContent, options);

        // 1. 引用文献识别与提取
        ReferenceExtractor.ReferenceResult refResult = referenceExtractor.extractReferences(processedContent);
        String contentWithoutReferences = refResult.getContentWithoutReferences();
        int referenceCount = refResult.getReferences().size();

        // 计算引用率（引用内容占全文的百分比）
        BigDecimal referenceRate = BigDecimal.ZERO;
        if (processedContent != null && processedContent.length() > 0) {
            int originalLength = processedContent.length();
            int contentLength = contentWithoutReferences.length();
            int referenceLength = originalLength - contentLength;
            if (referenceLength > 0) {
                referenceRate = BigDecimal.valueOf(referenceLength * 100.0 / originalLength)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
            }
        }

        log.info("引用文献识别完成: 共识别到 {} 条引用文献, 引用率: {}%", referenceCount, referenceRate);

        // 2. 预处理待查重论文（使用去除引用后的内容）
        if (contentWithoutReferences.isEmpty()) {
            result.setSimilarity(BigDecimal.ZERO);
            result.setCheckSource("论文内容仅包含引用文献");
            result.setReportUrl("/local/report/" + System.currentTimeMillis());
            result.setExtraInfo("论文内容为空或已全部识别为引用文献");
            result.setSuccess(true);
            result.setReferenceCount(referenceCount);
            result.setReferenceRate(referenceRate);
            result.setReferences(refResult.getReferences().stream()
                    .map(ref -> new CheckResult.ReferenceInfo(
                            ref.getFullCitation(),
                            ref.getDocumentType(),
                            ref.getPosition(),
                            ref.getLanguage()))
                    .collect(Collectors.toList()));
            return result;
        }

        String segmentedTargetText = IKAnalyzerUtils.segmentToString(contentWithoutReferences);
        if (segmentedTargetText.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "论文内容为空或分词后无有效内容");
        }
        BigInteger targetSimHash = textSimilarityUtils.calculateSimHash(segmentedTargetText);

        // 2. 查询有内容的论文（contentPath 非空）
        List<PaperInfo> paperList = paperInfoMapper.selectList(
                new LambdaQueryWrapper<PaperInfo>()
                        .isNotNull(PaperInfo::getContentPath)
                        .ne(PaperInfo::getContentPath, "")
                        .select(PaperInfo::getId, PaperInfo::getPaperTitle,
                                PaperInfo::getContentPath, PaperInfo::getSegmentedPath)
        );
        double maxSimilarity = 0.0;
        String mostSimilarPaperTitle = "";
        BigInteger mostSimilarSimHash = BigInteger.ZERO;
        List<CheckResult.SimilarPaper> similarPapers = new ArrayList<>();
        List<CheckResult.SimilarFragment> similarFragments = new ArrayList<>();

        // 记录候选论文（通过 SimHash 筛选的），延迟加载完整内容
        List<PaperInfo> candidates = new ArrayList<>();

        // 3. 第一轮：SimHash 快速筛选
        for (PaperInfo paper : paperList) {
            try {
                // 3.1 获取或计算库中论文的 SimHash（优先缓存）
                BigInteger librarySimHash = simHashCache.get(paper.getId());
                if (librarySimHash == null) {
                    String segmentedLibraryText = null;
                    if (paper.getSegmentedPath() != null && !paper.getSegmentedPath().isEmpty()) {
                        segmentedLibraryText = paperContentMinioService.readSegmentedText(paper.getSegmentedPath());
                    }
                    if (segmentedLibraryText == null || segmentedLibraryText.isEmpty()) {
                        // 无分词缓存，跳过（后续需要时再加载完整内容）
                        candidates.add(paper);
                        continue;
                    }
                    librarySimHash = textSimilarityUtils.calculateSimHash(segmentedLibraryText);
                    simHashCache.put(paper.getId(), librarySimHash);
                }

                int hammingDistance = textSimilarityUtils.calculateHammingDistance(targetSimHash, librarySimHash);
                if (hammingDistance <= textSimilarityUtils.getHammingThreshold()) {
                    candidates.add(paper);
                }
            } catch (Exception e) {
                log.warn("SimHash计算失败（ID：{}）：", paper.getId(), e);
            }
        }

        // 4. 第二轮：对候选论文进行精细比对（仅此时加载完整内容）
        for (PaperInfo paper : candidates) {
            try {
                String libraryContent = paperContentMinioService.readPaperContent(paper.getContentPath());
                if (libraryContent == null || libraryContent.isEmpty()) {
                    continue;
                }

                double similarity = textSimilarityUtils.calculateFinalSimilarity(contentWithoutReferences, libraryContent);

                if (similarity >= 5.0) {
                    CheckResult.SimilarPaper similarPaper = new CheckResult.SimilarPaper();
                    similarPaper.setPaperId(paper.getId());
                    similarPaper.setPaperTitle(paper.getPaperTitle());
                    similarPaper.setSimilarity(BigDecimal.valueOf(similarity).setScale(2, BigDecimal.ROUND_HALF_UP));
                    similarPapers.add(similarPaper);

                    List<CheckResult.SimilarFragment> fragments = extractSimilarFragments(
                            contentWithoutReferences, libraryContent,
                            paper.getId(), paper.getPaperTitle());
                    similarFragments.addAll(fragments);
                }

                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                    mostSimilarPaperTitle = paper.getPaperTitle();
                    mostSimilarSimHash = textSimilarityUtils.calculateSimHash(
                            IKAnalyzerUtils.segmentToString(libraryContent));
                }
            } catch (Exception e) {
                log.error("比对库中论文失败（ID：{}）：", paper.getId(), e);
            }
        }

        // 4. 处理查重率为0的情况：返回段落相似度为0的片段
        if (maxSimilarity == 0.0 && !contentWithoutReferences.isEmpty()) {
            List<CheckResult.SimilarFragment> zeroFragments = generateZeroSimilarityFragments(contentWithoutReferences);
            similarFragments.addAll(zeroFragments);
        }

        // 5. 封装查重结果
        BigDecimal similarityPercent = BigDecimal.valueOf(maxSimilarity)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        result.setSimilarity(similarityPercent);
        result.setCheckSource("论文信息库（SimHash+余弦相似度+N-gram字符级检测，已排除引用文献）");
        result.setReportUrl("/local/report/" + System.currentTimeMillis());
        result.setExtraInfo(String.format(
                "最高相似度论文：%s\nSimHash海明距离：%d\n相似度：%.2f%%\n比对论文总数：%d\n识别引用文献数：%d条\n引用率：%s%%\n相似片段数：%d",
                mostSimilarPaperTitle,
                textSimilarityUtils.calculateHammingDistance(targetSimHash, mostSimilarSimHash),
                similarityPercent,
                paperList.size(),
                referenceCount,
                referenceRate,
                similarFragments.size()
        ));
        result.setSuccess(true);
        result.setSimilarPapers(similarPapers);
        result.setSimilarFragments(similarFragments); // 设置相似片段列表
        result.setReferenceCount(referenceCount);
        result.setReferenceRate(referenceRate);
        result.setReferences(refResult.getReferences().stream()
                .map(ref -> new CheckResult.ReferenceInfo(
                        ref.getFullCitation(),
                        ref.getDocumentType(),
                        ref.getPosition(),
                        ref.getLanguage()))
                .collect(Collectors.toList()));

        return result;
    }

    /**
     * 提取相似片段（使用N-gram滑动窗口）
     */
    private List<CheckResult.SimilarFragment> extractSimilarFragments(
            String targetText, String sourceText, Long sourcePaperId, String sourcePaperTitle) {

        List<CheckResult.SimilarFragment> fragments = new ArrayList<>();
        int windowSize = 13; // 模拟知网13字符规则

        // 清理文本（去除标点和空格）
        String cleanTarget = targetText.replaceAll("[\\p{Punct}\\s]+", "");
        String cleanSource = sourceText.replaceAll("[\\p{Punct}\\s]+", "");

        if (cleanTarget.length() < windowSize || cleanSource.length() < windowSize) {
            return fragments;
        }

        // 使用滑动窗口查找相似片段
        int i = 0;
        while (i <= cleanTarget.length() - windowSize) {
            String targetWindow = cleanTarget.substring(i, i + windowSize);
            int sourcePos = cleanSource.indexOf(targetWindow);

            if (sourcePos != -1) {
                // 扩展找到完整的相似片段
                int targetStart = findOriginalPosition(targetText, i, cleanTarget);
                int sourceStart = findOriginalPosition(sourceText, sourcePos, cleanSource);

                // 向后扩展
                int targetEnd = i + windowSize;
                int sourceEnd = sourcePos + windowSize;
                while (targetEnd < cleanTarget.length() && sourceEnd < cleanSource.length()
                        && cleanTarget.charAt(targetEnd) == cleanSource.charAt(sourceEnd)) {
                    targetEnd++;
                    sourceEnd++;
                }

                int detectedEnd = findOriginalPosition(targetText, targetEnd - 1, cleanTarget) + 1;
                int originalEnd = findOriginalPosition(sourceText, sourceEnd - 1, cleanSource) + 1;

                // 创建相似片段
                CheckResult.SimilarFragment fragment = new CheckResult.SimilarFragment();
                fragment.setSourcePaperId(sourcePaperId);
                fragment.setSourcePaperTitle(sourcePaperTitle);
                fragment.setOriginalText(sourceText.substring(sourceStart, originalEnd));
                fragment.setDetectedText(targetText.substring(targetStart, detectedEnd));
                fragment.setOriginalStartPos(sourceStart);
                fragment.setOriginalEndPos(originalEnd);
                fragment.setDetectedStartPos(targetStart);
                fragment.setDetectedEndPos(detectedEnd);
                fragment.setSimilarity(BigDecimal.valueOf(100.0)); // 完全匹配
                fragment.setMarkedText("<mark>" + targetText.substring(targetStart, detectedEnd) + "</mark>");
                fragment.setSourceType("论文库");

                fragments.add(fragment);
                i = targetEnd; // 跳过已匹配的部分
            } else {
                i++;
            }
        }

        return fragments;
    }

    /**
     * 从清理后的文本位置映射回原始文本位置
     */
    private int findOriginalPosition(String originalText, int cleanPos, String cleanText) {
        int originalPos = 0;
        int cleanIndex = 0;

        while (originalPos < originalText.length() && cleanIndex <= cleanPos) {
            char c = originalText.charAt(originalPos);
            if (!Character.isWhitespace(c) && Character.isLetterOrDigit(c)) {
                if (cleanIndex == cleanPos) {
                    return originalPos;
                }
                cleanIndex++;
            }
            originalPos++;
        }

        return originalPos;
    }

    /**
     * 当查重率为0时，生成段落相似度为0的片段
     */
    private List<CheckResult.SimilarFragment> generateZeroSimilarityFragments(String content) {
        List<CheckResult.SimilarFragment> fragments = new ArrayList<>();

        // 按段落分割
        String[] paragraphs = content.split("[\\n\\r]+");

        int currentPos = 0;
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty() || paragraph.length() < 10) {
                currentPos += paragraph.length() + 1;
                continue;
            }

            CheckResult.SimilarFragment fragment = new CheckResult.SimilarFragment();
            fragment.setSourcePaperId(null);
            fragment.setSourcePaperTitle("无相似来源");
            fragment.setOriginalText("");
            fragment.setDetectedText(paragraph);
            fragment.setOriginalStartPos(null);
            fragment.setOriginalEndPos(null);
            fragment.setDetectedStartPos(currentPos);
            fragment.setDetectedEndPos(currentPos + paragraph.length());
            fragment.setSimilarity(BigDecimal.ZERO); // 相似度为0
            fragment.setMarkedText(paragraph); // 无红色标记
            fragment.setSourceType("原创");

            fragments.add(fragment);
            currentPos += paragraph.length() + 1;
        }

        return fragments;
    }
}