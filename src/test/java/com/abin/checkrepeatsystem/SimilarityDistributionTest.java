package com.abin.checkrepeatsystem;

import com.abin.checkrepeatsystem.teacher.service.TeacherDataAnalysisService;
import com.abin.checkrepeatsystem.teacher.vo.SimilarityDistributionVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class SimilarityDistributionTest {

    @Autowired
    private TeacherDataAnalysisService teacherDataAnalysisService;

    @Test
    public void testSimilarityDistribution() {
        // 使用数据库中存在的教师ID进行测试
        Long teacherId = 1986648778328793089L;
        String timeRange = "year"; // 使用较大的时间范围
        
        System.out.println("=== 测试相似度分布查询 ===");
        System.out.println("教师ID: " + teacherId);
        System.out.println("时间范围: " + timeRange);
        
        try {
            List<SimilarityDistributionVO> distribution = teacherDataAnalysisService.getSimilarityDistribution(teacherId, timeRange);
            
            System.out.println("\n=== 查询结果 ===");
            if (distribution != null && !distribution.isEmpty()) {
                System.out.println("找到 " + distribution.size() + " 个相似度区间:");
                for (SimilarityDistributionVO item : distribution) {
                    System.out.println("- 区间: " + item.getRange() + 
                                     ", 论文数: " + item.getPaperCount() + 
                                     ", 百分比: " + item.getPercentage() + "%");
                }
            } else {
                System.out.println("未找到任何相似度分布数据");
            }
        } catch (Exception e) {
            System.err.println("查询出现异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}