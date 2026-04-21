package com.abin.checkrepeatsystem.student.dto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 查重报告数据DTO：用于前端查重报告详情页面
 */
@Data
public class ReportDataDTO {
    /**
     * 论文标题
     */
    private String paperTitle;

    /**
     * 作者
     */
    private String author;

    /**
     * 学号
     */
    private Long studentId;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 检测时间
     */
    private LocalDateTime checkTime;

    /**
     * 总相似度
     */
    private Double totalSimilarity;

    /**
     * 总字数
     */
    private Integer wordCount;

    /**
     * 引用文献数
     */
    private Integer citationCount;

    /**
     * 相似文献数
     */
    private Integer similarSources;

    /**
     * 相似文献数（兼容前端字段）
     */
    private Integer similarSourceCount;

    /**
     * 检测引擎
     */
    private List<String> checkEngines;

    /**
     * 按章节分析
     */
    private Map<String, SectionInfo> sections;

    /**
     * 章节信息
     */
    @Data
    public static class SectionInfo {
        /**
         * 相似度
         */
        private Double similarity;

        /**
         * 字数
         */
        private Integer wordCount;
    }

    /**
     * 相似来源
     */
    private List<SimilarSource> similarSourceList;

    /**
     * 相似来源信息
     */
    @Data
    public static class SimilarSource {
        /**
         * 来源ID
         */
        private String sourceId;

        /**
         * 标题
         */
        private String title;

        /**
         * 作者
         */
        private String author;

        /**
         * 相似度
         */
        private Double similarity;

        /**
         * 匹配段落
         */
        private List<MatchedParagraph> matchedParagraphs;

        /**
         * 匹配段落信息
         */
        @Data
        public static class MatchedParagraph {
            /**
             * 原文
             */
            private String sourceText;

            /**
             * 本文
             */
            private String paperText;

            /**
             * 相似度
             */
            private Double similarity;
        }
    }
}