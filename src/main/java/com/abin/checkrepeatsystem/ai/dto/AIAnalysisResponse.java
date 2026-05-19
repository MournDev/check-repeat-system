package com.abin.checkrepeatsystem.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIAnalysisResponse {

    private String overallAssessment;
    private List<HighRiskArea> highRiskAreas;
    private List<GoodAspect> goodAspects;
    private List<String> generalTips;

    @Data
    public static class HighRiskArea {
        private String section;
        private String similarity;
        private String issue;
        private String suggestion;
    }

    @Data
    public static class GoodAspect {
        private String section;
        private String similarity;
        private String strength;
        private String encouragement;
    }
}
