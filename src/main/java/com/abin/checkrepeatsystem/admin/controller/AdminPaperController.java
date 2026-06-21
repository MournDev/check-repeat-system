package com.abin.checkrepeatsystem.admin.controller;

import com.abin.checkrepeatsystem.admin.service.AdminPaperService;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.enums.CheckStatusFilterEnum;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.student.service.PaperInfoService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;


/**
 * 管理员论文管理控制器
 * 职责：处理管理员对论文的查询、审核、管理等操作
 */
@RestController
@RequestMapping("/api/v1/admin/papers")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminPaperController {

    private static final Logger log = LoggerFactory.getLogger(AdminPaperController.class);

    private final AdminPaperService adminPaperService;
    private final PaperInfoService paperInfoService;

    /**
     * 获取论文列表（分页）
     */
    @GetMapping("/list")
    public Result<Page<PaperInfo>> getPaperList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String paperStatus,
            @RequestParam(required = false) String paperType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) String majorName,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String checkStatus,
            @RequestParam(required = false) Double minSimilarity,
            @RequestParam(required = false) Double maxSimilarity) {
        
        log.info("接收获取论文列表请求: page={}, size={}, paperStatus={}, paperType={}, keyword={}, collegeId={}, majorId={}, majorName={}, grade={}, minSimilarity={}, maxSimilarity={}, checkStatus={}",
                page, size, paperStatus, paperType, keyword, collegeId, majorId, majorName, grade, minSimilarity, maxSimilarity, checkStatus);
        // 验证查重状态参数
        if (checkStatus != null && !CheckStatusFilterEnum.isValidCode(checkStatus)) {
            log.warn("无效的查重状态参数，忽略该筛选条件: {}", checkStatus);
            checkStatus = null;
        }

        return adminPaperService.getPaperList(page, size, paperStatus, paperType, keyword, startDate, endDate, collegeId, majorId, majorName, grade, checkStatus, minSimilarity, maxSimilarity);
    }
    
    /**
     * 验证相似度范围参数是否有效
     */
    private boolean isValidSimilarityRange(String range) {
        if (range == null || range.isEmpty()) {
            return false;
        }
        
        // 支持的范围值
        String[] validRanges = {"<20%", "lt20", "20%-50%", "20to50", ">50%", "gt50"};
        
        for (String validRange : validRanges) {
            if (validRange.equalsIgnoreCase(range)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 获取论文详情
     */
    @GetMapping("/{paperId:[0-9]+}")
    public Result<PaperInfo> getPaperDetail(@PathVariable Long paperId) {
        log.info("接收获取论文详情请求: paperId={}", paperId);
        return adminPaperService.getPaperDetail(paperId);
    }

    /**
     * 审核论文
     */
    @PutMapping("/{paperId:[0-9]+}/audit")
    @OperationLog(type = "admin_paper_audit", description = "管理员审核论文", recordResult = true)
    public Result<String> auditPaper(@PathVariable Long paperId,
                                   @RequestBody Map<String, Object> auditRequest) {
        log.info("接收审核论文请求: paperId={}, auditRequest={}", paperId, auditRequest);
        String auditResult = (String) auditRequest.get("auditResult");
        String auditComment = (String) auditRequest.get("auditComment");

        if (auditResult == null || auditResult.isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "审核结果不能为空");
        }

        return adminPaperService.auditPaper(paperId, auditResult, auditComment);
    }

    /**
     * 批量审核论文
     */
    @PostMapping("/batch-audit")
    @OperationLog(type = "admin_paper_batch_audit", description = "管理员批量审核论文", recordResult = true)
    public Result<String> batchAuditPapers(@RequestBody Map<String, Object> batchAuditRequest) {
        log.info("接收批量审核论文请求: batchAuditRequest={}", batchAuditRequest);
        @SuppressWarnings("unchecked")
        List<Long> paperIds = (List<Long>) batchAuditRequest.get("paperIds");
        String auditResult = (String) batchAuditRequest.get("auditResult");
        String auditComment = (String) batchAuditRequest.get("auditComment");

        if (paperIds == null || paperIds.isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "论文ID列表不能为空");
        }
        if (auditResult == null || auditResult.isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "审核结果不能为空");
        }

        return adminPaperService.batchAuditPapers(paperIds, auditResult, auditComment);
    }

    /**
     * 删除论文
     */
    @DeleteMapping("/{paperId:[0-9]+}")
    @OperationLog(type = "admin_paper_delete", description = "管理员删除论文")
    public Result<String> deletePaper(@PathVariable Long paperId) {
        log.info("接收删除论文请求: paperId={}", paperId);
        return adminPaperService.deletePaper(paperId);
    }

    /**
     * 批量删除论文
     */
    @PostMapping("/batch-delete")
    @OperationLog(type = "admin_paper_batch_delete", description = "管理员批量删除论文")
    public Result<String> batchDeletePapers(@RequestBody List<Long> paperIds) {
        log.info("接收批量删除论文请求: paperIds={}", paperIds);
        return adminPaperService.batchDeletePapers(paperIds);
    }

    /**
     * 获取论文统计信息
     */
    @GetMapping({"/statistics", "/stats"})
    public Result<Map<String, Object>> getPaperStatistics() {
        log.info("接收获取论文统计信息请求");
        return adminPaperService.getPaperStatistics();
    }

    /**
     * 导出论文列表
     */
    @GetMapping("/export")
    public void exportPaperList(@RequestParam Map<String, Object> params, HttpServletResponse response) {
        log.info("接收导出论文列表请求: params={}", params);
        try {
            adminPaperService.exportPaperList(params, response);
        } catch (Exception e) {
            log.error("导出论文列表失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 下载论文文件
     */
    @GetMapping("/{paperId:[0-9]+}/download")
    public void downloadPaper(@PathVariable Long paperId, HttpServletResponse response) {
        try {
            PaperInfo paperInfo = paperInfoService.getById(paperId);
            if (paperInfo == null || paperInfo.getIsDeleted() == 1) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("论文不存在");
                return;
            }
            if (paperInfo.getFileId() == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("论文文件不存在");
                return;
            }
            paperInfoService.downloadPaper(paperId, paperInfo.getStudentId(), response);
        } catch (Exception e) {
            log.error("管理员下载论文失败: paperId={}", paperId, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("论文下载失败，请查看服务器日志");
            } catch (Exception ex) {
                log.error("设置错误响应失败", ex);
            }
        }
    }
    
    /**
     * 内部查重检测
     */
    @PostMapping("/{paperId:[0-9]+}/internal-check")
    @OperationLog(type = "admin_paper_internal_check", description = "管理员发起内部查重")
    public Result<String> internalCheckPaper(@PathVariable Long paperId) {
        log.info("接收内部查重检测请求：paperId={}", paperId);
        return adminPaperService.schoolInternalCheckPaper(paperId);
    }
    
    /**
     * 批量内部查重检测
     */
    @PostMapping("/batch-internal-check")
    @OperationLog(type = "admin_paper_batch_internal_check", description = "管理员批量内部查重")
    public Result<String> batchInternalCheckPaper(@RequestBody List<Long> paperIds) {
        log.info("接收批量内部查重检测请求：paperIds={}", paperIds);
        return adminPaperService.batchSchoolInternalCheckPaper(paperIds);
    }
    
    /**
     * 批量第三方查重检测
     */
    @PostMapping("/batch-third-party-check")
    @OperationLog(type = "admin_paper_batch_third_party_check", description = "管理员批量第三方查重")
    public Result<String> batchThirdPartyCheckPaper(@RequestBody List<Long> paperIds) {
        log.info("接收批量第三方查重检测请求：paperIds={}", paperIds);
        return adminPaperService.batchThirdPartyCheckPaper(paperIds);
    }
    
    /**
     * 第三方查重检测（单篇）
     */
    @PostMapping("/{paperId:[0-9]+}/third-party-check")
    public Result<String> thirdPartyCheckPaper(@PathVariable Long paperId) {
        log.info("接收第三方查重检测请求：paperId={}", paperId);
        List<Long> paperIds = new ArrayList<>();
        paperIds.add(paperId);
        return adminPaperService.batchThirdPartyCheckPaper(paperIds);
    }
}