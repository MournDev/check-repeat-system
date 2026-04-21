package com.abin.checkrepeatsystem.common.utils;

import com.abin.checkrepeatsystem.pojo.entity.*;
import com.abin.checkrepeatsystem.common.service.PaperContentMinioService;
import com.abin.checkrepeatsystem.student.dto.ReportPreviewDTO;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报告内容处理工具类：分段、标红、来源匹配
 */
@Slf4j
@Component
public class ReportContentProcessor {

    @Resource
    private PaperInfoMapper paperInfoMapper;
    @Resource
    private PaperContentMinioService paperContentMinioService;
    
    @Value("${report.preview.max-paragraph-length:500}")
    private int maxParagraphLength;

    private static final String PARAGRAPH_SEPARATOR = "[。！？.!?]";

    /**
     * 构建报告预览DTO
     */
    public ReportPreviewDTO buildReportPreviewDTO(
            CheckTask checkTask, 
            CheckReport checkReport,
            PaperInfo paperInfo,
            SysUser student,
            SysUser teacher) {
        
        ReportPreviewDTO previewDTO = new ReportPreviewDTO();
        
        // 1. 构建基本信息
        ReportPreviewDTO.ReportBaseInfoDTO baseInfo = buildBaseInfoDTO(
                checkTask, checkReport, paperInfo, student, teacher);
        previewDTO.setBaseInfo(baseInfo);
        
        // 2. 构建相似度统计
        ReportPreviewDTO.ReportRateStatDTO rateStat = buildRateStatDTO(
                checkTask, checkReport, baseInfo);
        previewDTO.setRateStat(rateStat);
        
        // 3. 构建分段信息（含标红）
        List<ReportPreviewDTO.ReportParagraphDTO> paragraphs = buildParagraphs(
                paperInfo.getFileId(), checkReport.getRepeatDetails());
        previewDTO.setParagraphs(paragraphs);
        
        // 4. 构建相似来源列表
        if (checkReport.getRepeatDetails() != null && !checkReport.getRepeatDetails().isEmpty()) {
            List<ReportPreviewDTO.ReportSimilarSourceDTO> similarSources = 
                    buildSimilarSources(checkReport.getRepeatDetails());
            previewDTO.setSimilarSources(similarSources);
        } else {
            previewDTO.setSimilarSources(new ArrayList<>());
        }
        
        return previewDTO;
    }

    /**
     * 构建基本信息DTO
     */
    private ReportPreviewDTO.ReportBaseInfoDTO buildBaseInfoDTO(
            CheckTask checkTask, 
            CheckReport checkReport,
            PaperInfo paperInfo,
            SysUser student,
            SysUser teacher) {
        
        ReportPreviewDTO.ReportBaseInfoDTO baseInfo = new ReportPreviewDTO.ReportBaseInfoDTO();
        
        baseInfo.setTaskId(checkTask.getId());
        baseInfo.setTaskNo(checkTask.getTaskNo());
        baseInfo.setReportNo(checkReport.getReportNo());
        baseInfo.setReportId(checkReport.getId());
        baseInfo.setPaperId(paperInfo.getId());
        baseInfo.setPaperTitle(paperInfo.getPaperTitle());
        baseInfo.setStudentName(student.getRealName());
        baseInfo.setTeacherName(teacher.getRealName());
        baseInfo.setAuthor(paperInfo.getAuthor());
        baseInfo.setRealName(student.getRealName());
        baseInfo.setUserName(student.getUsername());
        baseInfo.setSimilarityRate(checkReport.getTotalSimilarity());
        baseInfo.setStudentId(student.getId());
        baseInfo.setCheckRuleName(checkTask.getStartTime());
        baseInfo.setGenerateTime(checkReport.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 计算并设置新增字段
        String paperText = extractPaperText(paperInfo.getFileId());
        List<String> rawParagraphs = splitTextToParagraphs(paperText);
        int totalWords = paperText.length();
        
        // 解析重复详情，计算相似字数
        int similarWords = 0;
        if (checkReport.getRepeatDetails() != null && !checkReport.getRepeatDetails().isEmpty()) {
            try {
                List<Map<String, Object>> repeatDetails = new ObjectMapper().readValue(
                        checkReport.getRepeatDetails(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                if (repeatDetails != null && !repeatDetails.isEmpty()) {
                    similarWords = repeatDetails.size();
                }
            } catch (Exception e) {
                log.error("解析重复详情失败: {}", e.getMessage());
            }
        }
        
        baseInfo.setCitations(0); // 暂时设为0，可根据实际情况计算
        baseInfo.setTotalWords(totalWords);
        baseInfo.setSimilarWords(similarWords);
        baseInfo.setUniqueSentences(rawParagraphs.size());
        
        return baseInfo;
    }

    /**
     * 构建相似度统计DTO
     */
    private ReportPreviewDTO.ReportRateStatDTO buildRateStatDTO(
            CheckTask checkTask, 
            CheckReport checkReport,
            ReportPreviewDTO.ReportBaseInfoDTO baseInfo) {
        
        ReportPreviewDTO.ReportRateStatDTO rateStat = new ReportPreviewDTO.ReportRateStatDTO();
        
        rateStat.setReportId(checkReport.getId());
        rateStat.setReportNo(checkReport.getReportNo());
        
        BigDecimal repeatRate = checkTask.getCheckRate() != null ? checkTask.getCheckRate() : BigDecimal.ZERO;
        rateStat.setRepeatRate(repeatRate);
        rateStat.setOriginalRate(BigDecimal.valueOf(100).subtract(repeatRate));
        
        // 计算重复段落数和总段落数
        int totalParagraphCount = baseInfo.getUniqueSentences() != null ? baseInfo.getUniqueSentences() : 0;
        int repeatParagraphCount = 0;
        
        if (checkReport.getRepeatDetails() != null && !checkReport.getRepeatDetails().isEmpty()) {
            try {
                List<Map<String, Object>> repeatDetails = new ObjectMapper().readValue(
                        checkReport.getRepeatDetails(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                if (repeatDetails != null && !repeatDetails.isEmpty()) {
                    // 去重统计重复段落数
                    Set<Integer> repeatParaNoSet = new HashSet<>();
                    for (Map<String, Object> detail : repeatDetails) {
                        Object paraNoObj = detail.get("paragraphNo");
                        if (paraNoObj instanceof Number) {
                            repeatParaNoSet.add(((Number) paraNoObj).intValue());
                        }
                    }
                    repeatParagraphCount = repeatParaNoSet.size();
                }
            } catch (Exception e) {
                log.error("解析重复详情失败: {}", e.getMessage());
            }
        }
        
        rateStat.setRepeatParagraphCount(repeatParagraphCount);
        rateStat.setTotalParagraphCount(totalParagraphCount);
        
        return rateStat;
    }

    /**
     * 构建分段DTO（含标红）
     */
    private List<ReportPreviewDTO.ReportParagraphDTO> buildParagraphs(Long fileId, String repeatDetailsJson) {
        List<ReportPreviewDTO.ReportParagraphDTO> paragraphDTOs = new ArrayList<>();
        
        try {
            // 1. 提取论文文本并分段
            String paperText = extractPaperText(fileId);
            List<String> rawParagraphs = splitTextToParagraphs(paperText);
            
            // 2. 解析重复详情，按段落分组
            Map<Integer, List<Map<String, Object>>> repeatByParaNo = new HashMap<>();
            if (repeatDetailsJson != null && !repeatDetailsJson.isEmpty()) {
                List<Map<String, Object>> repeatDetails = new ObjectMapper().readValue(
                        repeatDetailsJson,
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                
                if (repeatDetails != null && !repeatDetails.isEmpty()) {
                    for (Map<String, Object> detail : repeatDetails) {
                        Object paraNoObj = detail.get("paragraphNo");
                        if (paraNoObj instanceof Number) {
                            int paraNo = ((Number) paraNoObj).intValue();
                            repeatByParaNo.computeIfAbsent(paraNo, k -> new ArrayList<>()).add(detail);
                        }
                    }
                }
            }
            
            // 3. 构建分段DTO
            for (int i = 0; i < rawParagraphs.size(); i++) {
                int paraNo = i + 1; // 段落序号从1开始
                String rawContent = rawParagraphs.get(i);
                // 截取超长段落（避免预览页面过长）
                String content = rawContent.length() > maxParagraphLength
                        ? rawContent.substring(0, maxParagraphLength) + "..."
                        : rawContent;
                
                ReportPreviewDTO.ReportParagraphDTO paraDTO = new ReportPreviewDTO.ReportParagraphDTO();
                paraDTO.setParagraphNo(paraNo);
                paraDTO.setContent(content);
                
                // 处理重复段落标红
                if (repeatByParaNo.containsKey(paraNo)) {
                    List<Map<String, Object>> paraRepeatDetails = repeatByParaNo.get(paraNo);
                    
                    // 优化：一次遍历同时获取最大相似度和对应的详情
                    Map<String, Object> maxSimilarDetail = null;
                    double maxSimValue = 0.0;
                    for (Map<String, Object> detail : paraRepeatDetails) {
                        Object simObj = detail.get("similarity");
                        if (simObj != null) {
                            double simValue = ((Number) simObj).doubleValue();
                            if (simValue > maxSimValue) {
                                maxSimValue = simValue;
                                maxSimilarDetail = detail;
                            }
                        }
                    }
                    
                    BigDecimal maxSimilarity = BigDecimal.valueOf(maxSimValue);
                    paraDTO.setSimilarity(maxSimilarity);
                    paraDTO.setIsRepeat(maxSimilarity.compareTo(BigDecimal.valueOf(5.0)) >= 0); // 相似度≥5%判定为重复

                    // 标红重复片段（简化逻辑：标红相似度最高的片段）
                    if (maxSimilarDetail != null) {
                        String repeatFragment = (String) maxSimilarDetail.get("repeatFragment");
                        if (repeatFragment != null && content.contains(repeatFragment)) {
                            // 用<span>标签包裹标红片段（只替换第一次出现）
                            String highlightedContent = content.replaceFirst(
                                    java.util.regex.Pattern.quote(repeatFragment),
                                    "<span style=\"color:red\">" + java.util.regex.Matcher.quoteReplacement(repeatFragment) + "</span>"
                            );
                            paraDTO.setContent(highlightedContent);
                        }
                    }
                    
                    // 关联相似来源ID
                    List<Long> sourceIds = paraRepeatDetails.stream()
                            .map(detail -> {
                                Object sourceIdObj = detail.get("sourceId");
                                if (sourceIdObj instanceof Long) {
                                    return (Long) sourceIdObj;
                                } else if (sourceIdObj instanceof Number) {
                                    return ((Number) sourceIdObj).longValue();
                                } else if (sourceIdObj instanceof String) {
                                    try {
                                        return Long.parseLong((String) sourceIdObj);
                                    } catch (NumberFormatException e) {
                                        return null;
                                    }
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                    paraDTO.setSourceIds(sourceIds);
                } else {
                    // 非重复段落
                    paraDTO.setSimilarity(BigDecimal.ZERO);
                    paraDTO.setIsRepeat(false);
                    paraDTO.setSourceIds(new ArrayList<>());
                }
                
                paragraphDTOs.add(paraDTO);
            }
        } catch (Exception e) {
            log.error("构建分段信息失败: {}", e.getMessage(), e);
        }
        
        return paragraphDTOs;
    }

    /**
     * 构建相似来源列表
     */
    private List<ReportPreviewDTO.ReportSimilarSourceDTO> buildSimilarSources(String repeatDetailsJson) {
        if (repeatDetailsJson == null || repeatDetailsJson.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> repeatDetails;
        try {
            repeatDetails = new ObjectMapper().readValue(
                    repeatDetailsJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (Exception e) {
            log.error("解析重复详情失败: {}", e.getMessage());
            return new ArrayList<>();
        }
        
        if (repeatDetails == null || repeatDetails.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 去重相似来源（按sourceId分组）
        Map<Long, Map<String, Object>> sourceMap = new HashMap<>();
        
        for (Map<String, Object> detail : repeatDetails) {
            Object sourceIdObj = detail.get("sourceId");
            if (sourceIdObj != null) {
                // 处理不同类型的sourceId
                Long sourceIdLong = null;
                if (sourceIdObj instanceof Long) {
                    sourceIdLong = (Long) sourceIdObj;
                } else if (sourceIdObj instanceof Integer) {
                    sourceIdLong = ((Integer) sourceIdObj).longValue();
                } else if (sourceIdObj instanceof String) {
                    try {
                        sourceIdLong = Long.parseLong((String) sourceIdObj);
                    } catch (NumberFormatException e) {
                        // 忽略无法转换的sourceId
                        continue;
                    }
                }
                
                if (sourceIdLong != null) {
                    if (sourceMap.containsKey(sourceIdLong)) {
                        // 合并相同来源的最大相似度
                        Map<String, Object> existingDetail = sourceMap.get(sourceIdLong);
                        double existingSimilarity = getSimilarityValue(existingDetail.get("similarity"));
                        double currentSimilarity = getSimilarityValue(detail.get("similarity"));
                        if (currentSimilarity > existingSimilarity) {
                            existingDetail.put("similarity", currentSimilarity);
                        }
                    } else {
                        sourceMap.put(sourceIdLong, detail);
                    }
                }
            }
        }

        // 转换为DTO
        return sourceMap.values().stream()
                .map(detail -> {
                    ReportPreviewDTO.ReportSimilarSourceDTO sourceDTO = new ReportPreviewDTO.ReportSimilarSourceDTO();
                    
                    // 安全的类型转换
                    Object sourceIdObj = detail.get("sourceId");
                    if (sourceIdObj != null) {
                        if (sourceIdObj instanceof Long) {
                            sourceDTO.setSourceId((Long) sourceIdObj);
                        } else if (sourceIdObj instanceof Number) {
                            sourceDTO.setSourceId(((Number) sourceIdObj).longValue());
                        } else if (sourceIdObj instanceof String) {
                            try {
                                sourceDTO.setSourceId(Long.parseLong((String) sourceIdObj));
                            } catch (NumberFormatException e) {
                                // 忽略转换错误
                            }
                        }
                    }
                    
                    Object sourceNameObj = detail.get("sourceName");
                    if (sourceNameObj != null) {
                        sourceDTO.setSourceName(sourceNameObj.toString());
                    }
                    
                    Object sourceTypeObj = detail.get("sourceType");
                    if (sourceTypeObj != null) {
                        sourceDTO.setSourceType(sourceTypeObj.toString());
                    }
                    
                    Object sourceUrlObj = detail.get("sourceUrl");
                    if (sourceUrlObj != null) {
                        sourceDTO.setSourceUrl(sourceUrlObj.toString());
                    }
                    
                    Object similarityObj = detail.get("similarity");
                    if (similarityObj != null) {
                        if (similarityObj instanceof java.math.BigDecimal) {
                            sourceDTO.setMaxSimilarity((java.math.BigDecimal) similarityObj);
                        } else if (similarityObj instanceof Double) {
                            sourceDTO.setMaxSimilarity(java.math.BigDecimal.valueOf((Double) similarityObj));
                        } else if (similarityObj instanceof Number) {
                            sourceDTO.setMaxSimilarity(java.math.BigDecimal.valueOf(((Number) similarityObj).doubleValue()));
                        } else if (similarityObj instanceof String) {
                            try {
                                sourceDTO.setMaxSimilarity(java.math.BigDecimal.valueOf(Double.parseDouble((String) similarityObj)));
                            } catch (NumberFormatException e) {
                                // 忽略转换错误
                            }
                        }
                    }
                    
                    return sourceDTO;
                })
                .filter(sourceDTO -> sourceDTO.getMaxSimilarity() != null) // 过滤掉相似度为null的来源
                .sorted((s1, s2) -> s1.getMaxSimilarity().compareTo(s2.getMaxSimilarity()) * -1) // 按相似度降序
                .collect(Collectors.toList());
    }
    
    /**
     * 获取相似度值
     */
    private double getSimilarityValue(Object similarityObj) {
        if (similarityObj == null) {
            return 0.0;
        }
        if (similarityObj instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) similarityObj).doubleValue();
        } else if (similarityObj instanceof Double) {
            return (Double) similarityObj;
        } else if (similarityObj instanceof Number) {
            return ((Number) similarityObj).doubleValue();
        } else if (similarityObj instanceof String) {
            try {
                return Double.parseDouble((String) similarityObj);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * 提取论文文本
     */
    public String extractPaperText(Long fileId) {
        try {
            // 1. 优先从Minio读取
            PaperInfo paperInfo = paperInfoMapper.selectList(
                    new LambdaQueryWrapper<PaperInfo>()
                            .eq(PaperInfo::getFileId, fileId)
                            .orderByDesc(PaperInfo::getCreateTime)
            ).stream().findFirst().orElse(null);
            
            if (paperInfo != null && paperInfo.getContentPath() != null && !paperInfo.getContentPath().isEmpty()) {
                try {
                    String minioContent = paperContentMinioService.readPaperContent(paperInfo.getContentPath());
                    if (minioContent != null && !minioContent.isEmpty()) {
                        log.info("从Minio获取论文内容成功");
                        return minioContent;
                    }
                } catch (Exception e) {
                    log.warn("从Minio读取论文内容失败: {}", e.getMessage());
                }
            }
            
            // 2. 从本地文件系统读取
            if (paperInfo != null && paperInfo.getFilePath() != null) {
                String filePath = paperInfo.getFilePath();
                if (!filePath.startsWith("/")) {
                    filePath = "/" + filePath;
                }
                String fullPath = "/data/upload" + filePath;
                String content = TikaTextExtractor.extractTextFromFile(fullPath);
                log.info("从本地文件系统获取论文内容成功: {}", fullPath);
                return content;
            }
            
            log.warn("未找到论文文件: fileId={}", fileId);
            return "";
        } catch (Exception e) {
            log.error("提取论文文本失败: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 文本分段（按中文标点分割，保留分割符）
     */
    private List<String> splitTextToParagraphs(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // 分割文本（保留分割符）
        String[] parts = text.split("(" + PARAGRAPH_SEPARATOR + ")");
        List<String> paragraphs = new ArrayList<>();
        StringBuilder currentPara = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            currentPara.append(parts[i]);
            // 每两个元素合并为一个段落（parts[i]为内容，parts[i+1]为分割符）
            if (i % 2 == 1 || i == parts.length - 1) {
                String para = currentPara.toString().trim();
                if (!para.isEmpty()) {
                    paragraphs.add(para);
                }
                currentPara.setLength(0);
            }
        }
        return paragraphs;
    }
}