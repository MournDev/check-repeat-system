package com.abin.checkrepeatsystem.pojo.vo;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 查重引擎内部结果封装（用于引擎→服务层传递数据）
 */
@Data
public class CheckResult {
    private BigDecimal similarity; // 相似度（百分比，如25.32）
    private String checkSource; // 查重来源（本地库/第三方API）
    private String reportUrl; // 查重报告链接（本地库可为空）
    private String extraInfo; // 额外信息（如最相似论文标题、海明距离等）
    private boolean success; // 查重是否成功
    private String failReason; // 失败原因（仅success=false时生效）
    private List<SimilarPaper> similarPapers; // 相似论文列表
    private List<SimilarFragment> similarFragments; // 相似片段列表（新增）
    private Integer referenceCount; // 识别到的引用文献数量
    private List<ReferenceInfo> references; // 引用文献详情列表
    private BigDecimal referenceRate; // 引用率（引用内容占全文的百分比）

    /**
     * 相似论文信息
     */
    @Data
    public static class SimilarPaper {
        private Long paperId; // 相似论文ID
        private String paperTitle; // 相似论文标题
        private BigDecimal similarity; // 相似度
    }

    /**
     * 引用文献信息
     */
    @Data
    public static class ReferenceInfo {
        private String fullCitation; // 完整引用文本
        private String documentType; // 文献类型（期刊文章/图书/学位论文等）
        private Integer position; // 在原文中的位置
        private String language; // 语言（zh/en）

        public ReferenceInfo() {
        }

        public ReferenceInfo(String fullCitation, String documentType, Integer position, String language) {
            this.fullCitation = fullCitation;
            this.documentType = documentType;
            this.position = position;
            this.language = language;
        }
    }

    /**
     * 相似片段信息（新增）
     */
    @Data
    public static class SimilarFragment {
        private Long sourcePaperId; // 来源论文ID
        private String sourcePaperTitle; // 来源论文标题
        private String originalText; // 来源原文片段
        private String detectedText; // 检测到的相似片段
        private Integer originalStartPos; // 来源位置（起始）
        private Integer originalEndPos; // 来源位置（结束）
        private Integer detectedStartPos; // 检测位置（起始）
        private Integer detectedEndPos; // 检测位置（结束）
        private BigDecimal similarity; // 该片段相似度
        private String markedText; // 带标记的文本（重复部分用<mark>标签包裹）
        private String sourceType; // 来源类型（论文库/互联网/引用）

        public SimilarFragment() {}

        public SimilarFragment(Long sourcePaperId, String sourcePaperTitle, String originalText, 
                              String detectedText, Integer originalStartPos, Integer originalEndPos,
                              Integer detectedStartPos, Integer detectedEndPos, BigDecimal similarity,
                              String markedText, String sourceType) {
            this.sourcePaperId = sourcePaperId;
            this.sourcePaperTitle = sourcePaperTitle;
            this.originalText = originalText;
            this.detectedText = detectedText;
            this.originalStartPos = originalStartPos;
            this.originalEndPos = originalEndPos;
            this.detectedStartPos = detectedStartPos;
            this.detectedEndPos = detectedEndPos;
            this.similarity = similarity;
            this.markedText = markedText;
            this.sourceType = sourceType;
        }
    }
}