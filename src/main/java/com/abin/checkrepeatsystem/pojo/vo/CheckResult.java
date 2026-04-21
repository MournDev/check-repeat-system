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
    
    /**
     * 相似论文信息
     */
    @Data
    public static class SimilarPaper {
        private Long paperId; // 相似论文ID
        private String paperTitle; // 相似论文标题
        private BigDecimal similarity; // 相似度
    }
}