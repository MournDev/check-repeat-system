package com.abin.checkrepeatsystem.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 个性化学术建议DTO
 */
@Data
@Schema(description = "个性化学术建议DTO")
public class PersonalAdviceDTO {
    
    @Schema(description = "报告版本号")
    private Integer version;
    
    @Schema(description = "高风险区域列表")
    private List<HighRiskAreaDTO> highRiskAreas;
    
    @Schema(description = "表现良好方面列表")
    private List<GoodAspectDTO> goodAspects;
    
    @Schema(description = "通用改进建议列表")
    private List<String> generalTips;
    
    /**
     * 高风险区域DTO
     */
    @Data
    @Schema(description = "高风险区域DTO")
    public static class HighRiskAreaDTO {
        @Schema(description = "章节名称")
        private String section;
        
        @Schema(description = "相似度描述")
        private String similarity;
        
        @Schema(description = "问题描述")
        private String issue;
        
        @Schema(description = "改进建议")
        private String suggestion;
    }
    
    /**
     * 表现良好方面DTO
     */
    @Data
    @Schema(description = "表现良好方面DTO")
    public static class GoodAspectDTO {
        @Schema(description = "章节名称")
        private String section;
        
        @Schema(description = "相似度描述")
        private String similarity;
        
        @Schema(description = "优势描述")
        private String strength;
        
        @Schema(description = "鼓励话语")
        private String encouragement;
    }
}