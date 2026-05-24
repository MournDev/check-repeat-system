package com.abin.checkrepeatsystem.ai.prompt;

import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AcademicPromptTemplates {

    private static final Logger log = LoggerFactory.getLogger(AcademicPromptTemplates.class);

    public static String buildPersonalAdviceSystemPrompt() {
        return """
            你是一个学术诚信顾问，为大学生提供论文查重分析。
            你的分析基于查重报告数据，提供具体、可操作的修改建议。

            ## 重要规则
            1. 必须以严格的JSON格式输出，不要包含```json标记，直接返回JSON
            2. 任何建议必须基于提供的实际数据，不可凭空猜测
            3. 语气专业但友善，使用中文

            ## JSON格式
            {
              "overallAssessment": "2-3句话总体评估，包含总相似度数值和整体判断",
              "highRiskAreas": [
                {
                  "section": "具体章节名",
                  "similarity": "百分比如25.3%",
                  "issue": "具体问题描述，引用实际相似来源名称",
                  "suggestion": "可操作的修改建议，50-100字"
                }
              ],
              "goodAspects": [
                {
                  "section": "章节名",
                  "similarity": "百分比或'--'",
                  "strength": "该方面的优点",
                  "encouragement": "鼓励语"
                }
              ],
              "generalTips": ["3-5条具体可操作的改进建议，每条15-50字"]
            }

            ## 约束
            - highRiskAreas只包含需要关注的章节（相似度偏高或有具体问题），最多4个
            - goodAspects列出表现良好的方面，1-3个
            - 每条建议要具体，引用数据中的实际相似来源名称
            - generalTips要实用可操作，不要空泛说教
            """;
    }

    public static String buildPersonalAdviceUserMessage(
            PaperInfo paperInfo, CheckTask checkTask, CheckReport checkReport) {

        StringBuilder sb = new StringBuilder();
        sb.append("## 论文信息\n");
        sb.append("- 标题：").append(nullToEmpty(paperInfo.getPaperTitle())).append("\n");
        sb.append("- 摘要：").append(truncate(nullToEmpty(paperInfo.getPaperAbstract()), 500)).append("\n");
        sb.append("- 学科领域：").append(nullToEmpty(paperInfo.getSubjectCode())).append("\n");
        sb.append("- 总字数：").append(paperInfo.getWordCount() != null ? paperInfo.getWordCount() : 0).append("\n\n");

        sb.append("## 查重结果\n");
        double rate = checkTask.getCheckRate() != null ? checkTask.getCheckRate().doubleValue() : 0.0;
        sb.append("- 总相似度：").append(String.format("%.1f", rate)).append("%\n");

        List<SourceInfo> sources = parseRepeatDetails(checkReport.getRepeatDetails());
        sb.append("- 相似来源数：").append(sources.size()).append("个\n");
        if (!sources.isEmpty()) {
            sb.append("- 相似来源详情：\n");
            int count = 0;
            for (SourceInfo src : sources) {
                if (count >= 5) break;
                sb.append("  ").append(count + 1).append(". ")
                  .append(truncate(src.title, 60))
                  .append("（相似度: ").append(String.format("%.1f", src.similarity)).append("%）\n");
                count++;
            }
        } else {
            sb.append("- 未检测到明显相似来源，论文原创性较高\n");
        }

        sb.append("\n请基于以上数据进行分析。");
        return sb.toString();
    }

    private static List<SourceInfo> parseRepeatDetails(String repeatDetails) {
        List<SourceInfo> sources = new ArrayList<>();
        if (repeatDetails == null || repeatDetails.isEmpty()) {
            return sources;
        }
        try {
            JSONArray arr = JSON.parseArray(repeatDetails);
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject detail = arr.getJSONObject(i);
                    SourceInfo src = new SourceInfo();
                    src.title = detail.getString("source");
                    src.similarity = detail.getDoubleValue("similarity");
                    if (src.similarity > 0) {
                        sources.add(src);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析重复详情失败，返回空列表: {}", e.getMessage());
        }
        return sources;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private static class SourceInfo {
        String title;
        double similarity;
    }
}
