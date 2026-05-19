package com.abin.checkrepeatsystem.teacher.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.pojo.entity.ReviewDraft;
import com.abin.checkrepeatsystem.teacher.dto.ReviewDraftDTO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 审核草稿服务接口
 */
public interface TeacherReviewDraftService extends IService<ReviewDraft> {

    /**
     * 保存或更新审核草稿
     * @param paperId 论文ID
     * @param teacherId 教师ID
     * @param reviewStatus 审核状态
     * @param reviewOpinion 审核意见
     * @param reviewAttach 附件路径
     * @return 保存结果
     */
    Result<ReviewDraftDTO> saveDraft(Long paperId, Long teacherId, String reviewStatus, String reviewOpinion, String reviewAttach);

    /**
     * 获取指定论文的审核草稿
     * @param paperId 论文ID
     * @param teacherId 教师ID
     * @return 草稿信息
     */
    Result<ReviewDraftDTO> getDraft(Long paperId, Long teacherId);

    /**
     * 删除指定论文的审核草稿
     * @param paperId 论文ID
     * @param teacherId 教师ID
     * @return 删除结果
     */
    Result<String> deleteDraft(Long paperId, Long teacherId);

    /**
     * 获取教师的所有草稿列表
     * @param teacherId 教师ID
     * @return 草稿列表
     */
    Result<List<ReviewDraftDTO>> listDrafts(Long teacherId);
}