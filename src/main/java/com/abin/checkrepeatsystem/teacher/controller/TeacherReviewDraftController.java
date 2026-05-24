package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.teacher.dto.ReviewDraftDTO;
import com.abin.checkrepeatsystem.teacher.service.TeacherReviewDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审核草稿控制器：仅教师角色可访问
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/teacher/review-drafts")
@PreAuthorize("hasAuthority('TEACHER')")
@RequiredArgsConstructor
public class TeacherReviewDraftController {

    private final TeacherReviewDraftService reviewDraftService;

    /**
     * 保存或更新审核草稿
     * @param paperId 论文ID
     * @param reviewStatus 审核状态
     * @param reviewOpinion 审核意见
     * @param reviewAttach 附件路径（可选）
     */
    @PostMapping("/save")
    public Result<ReviewDraftDTO> saveDraft(
            @RequestParam("paperId") Long paperId,
            @RequestParam(value = "reviewStatus", required = false) String reviewStatus,
            @RequestParam(value = "reviewOpinion", required = false) String reviewOpinion,
            @RequestParam(value = "reviewAttach", required = false) String reviewAttach) {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("保存审核草稿 - teacherId: {}, paperId: {}", teacherId, paperId);
        return reviewDraftService.saveDraft(paperId, teacherId, reviewStatus, reviewOpinion, reviewAttach);
    }

    /**
     * 获取指定论文的审核草稿
     * @param paperId 论文ID
     */
    @GetMapping("/get")
    public Result<ReviewDraftDTO> getDraft(@RequestParam("paperId") Long paperId) {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("获取审核草稿 - teacherId: {}, paperId: {}", teacherId, paperId);
        return reviewDraftService.getDraft(paperId, teacherId);
    }

    /**
     * 删除指定论文的审核草稿
     * @param paperId 论文ID
     */
    @DeleteMapping("/delete")
    public Result<String> deleteDraft(@RequestParam("paperId") Long paperId) {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("删除审核草稿 - teacherId: {}, paperId: {}", teacherId, paperId);
        return reviewDraftService.deleteDraft(paperId, teacherId);
    }

    /**
     * 获取教师的所有草稿列表
     */
    @GetMapping("/list")
    public Result<List<ReviewDraftDTO>> listDrafts() {
        Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("获取教师草稿列表 - teacherId: {}", teacherId);
        return reviewDraftService.listDrafts(teacherId);
    }
}