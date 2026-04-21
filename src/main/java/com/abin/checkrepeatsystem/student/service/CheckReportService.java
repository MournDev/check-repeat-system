package com.abin.checkrepeatsystem.student.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.student.dto.ReportDataDTO;
import com.abin.checkrepeatsystem.student.dto.ReportPreviewDTO;
import com.abin.checkrepeatsystem.student.vo.ReportDownloadReq;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 查重报告服务接口
 */
public interface CheckReportService extends IService<CheckReport> {
    /**
     * 1. 生成查重报告（由查重任务触发）
     * @param taskId 查重任务ID
     * @param checkRate 总重复率
     * @param repeatDetails 重复详情（JSON字符串）
     * @return 生成的报告实体
     */
    CheckReport generateReport(Long taskId, Double checkRate, String repeatDetails);

    /**
     * 2. 在线预览报告详情
     * @param reportId 报告ID
     * @return 报告预览DTO（含标红段落）
     */
    Result<ReportPreviewDTO> previewReport(Long reportId);

    /**
     * 3. 下载报告文件（PDF/HTML）
     * @param downloadReq 下载请求参数
     * @param response HTTP响应（用于输出文件流）
     */
    void downloadReport(ReportDownloadReq downloadReq, HttpServletResponse response);

    /**
     * 4. 查询当前用户的历史报告列表
     * @param paperId 论文ID（可选，筛选指定论文的报告）
     * @return 报告列表（含任务关联信息）
     */
    Result<List<CheckReport>> getMyReportList(Long paperId);

    /**
     * 5. 删除历史报告（仅学生本人或管理员可操作，且关联任务未审核）
     *
     * @param reportId 报告ID
     * @return 删除结果
     */
    Result<String> deleteReport(Long reportId);
    
    /**
     * 6. 获取报告数据（用于前端查重报告详情页面）
     * @param reportId 报告ID
     * @return 报告数据DTO
     */
    Result<ReportDataDTO> getReportData(Long reportId);
    
    /**
     * 7. 详细对比接口：根据报告ID和来源ID，返回原文与相似内容的详细对比数据
     * @param reportId 报告ID
     * @param sourceId 来源ID
     * @return 对比数据
     */
    Result<Map<String, Object>> compareReport(Long reportId, Long sourceId);
    
    /**
     * 8. 审核操作接口：处理教师对论文的审核操作（通过）
     * @param reportId 报告ID
     * @param comment 审核意见
     * @return 操作结果
     */
    Result<String> approveReport(Long reportId, String comment);
    
    /**
     * 9. 审核操作接口：处理教师对论文的审核操作（要求修改）
     * @param reportId 报告ID
     * @param comment 审核意见
     * @return 操作结果
     */
    Result<String> requestRevision(Long reportId, String comment);
    
    /**
     * 10. 联系学生接口：教师向学生发送消息
     * @param reportId 报告ID
     * @param content 消息内容
     * @return 发送结果
     */
    Result<String> contactStudent(Long reportId, String content);
    
    /**
     * 11. 相似来源详情接口：获取相似来源的详细信息
     * @param sourceId 来源ID
     * @return 相似来源的详细信息
     */
    Result<Map<String, Object>> getSourceDetail(Long sourceId);
    
    /**
     * 12. 历史报告列表接口：获取论文的历史查重报告列表
     * @param paperId 论文ID
     * @return 历史报告列表
     */
    Result<List<CheckReport>> getHistoryReportList(Long paperId);
}
