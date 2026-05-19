package com.abin.checkrepeatsystem.teacher.service.Impl;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.pojo.entity.ReviewDraft;
import com.abin.checkrepeatsystem.teacher.dto.ReviewDraftDTO;
import com.abin.checkrepeatsystem.teacher.mapper.ReviewDraftMapper;
import com.abin.checkrepeatsystem.teacher.service.TeacherReviewDraftService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审核草稿服务实现类
 */
@Service
@Slf4j
public class TeacherReviewDraftServiceImpl extends ServiceImpl<ReviewDraftMapper, ReviewDraft> implements TeacherReviewDraftService {

    @Resource
    private ReviewDraftMapper reviewDraftMapper;

    @Override
    @Transactional
    public Result<ReviewDraftDTO> saveDraft(Long paperId, Long teacherId, String reviewStatus, String reviewOpinion, String reviewAttach) {
        try {
            log.info("保存审核草稿 - paperId: {}, teacherId: {}", paperId, teacherId);

            // 查找是否已存在草稿
            LambdaQueryWrapper<ReviewDraft> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ReviewDraft::getPaperId, paperId)
                    .eq(ReviewDraft::getTeacherId, teacherId)
                    .eq(ReviewDraft::getIsDeleted, 0);

            ReviewDraft draft = reviewDraftMapper.selectOne(queryWrapper);

            if (draft == null) {
                // 创建新草稿
                draft = new ReviewDraft();
                draft.setPaperId(paperId);
                draft.setTeacherId(teacherId);
                draft.setCreateBy(teacherId);
                draft.setCreateTime(LocalDateTime.now());
                draft.setReviewStatus(reviewStatus);
                draft.setReviewOpinion(reviewOpinion);
                draft.setReviewAttach(reviewAttach);
                draft.setUpdateBy(teacherId);
                draft.setUpdateTime(LocalDateTime.now());
                reviewDraftMapper.insert(draft);
            } else {
                // 更新草稿内容
                draft.setReviewStatus(reviewStatus);
                draft.setReviewOpinion(reviewOpinion);
                draft.setReviewAttach(reviewAttach);
                draft.setUpdateBy(teacherId);
                draft.setUpdateTime(LocalDateTime.now());
                reviewDraftMapper.updateById(draft);
            }

            log.info("审核草稿保存成功 - draftId: {}", draft.getId());
            return Result.success(convertToDTO(draft));
        } catch (Exception e) {
            log.error("保存审核草稿失败 - paperId: {}, teacherId: {}", paperId, teacherId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "保存草稿失败: " + e.getMessage());
        }
    }

    @Override
    public Result<ReviewDraftDTO> getDraft(Long paperId, Long teacherId) {
        try {
            log.info("获取审核草稿 - paperId: {}, teacherId: {}", paperId, teacherId);

            LambdaQueryWrapper<ReviewDraft> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ReviewDraft::getPaperId, paperId)
                    .eq(ReviewDraft::getTeacherId, teacherId)
                    .eq(ReviewDraft::getIsDeleted, 0);

            ReviewDraft draft = reviewDraftMapper.selectOne(queryWrapper);

            if (draft == null) {
                return Result.success(null);
            }

            return Result.success(convertToDTO(draft));
        } catch (Exception e) {
            log.error("获取审核草稿失败 - paperId: {}, teacherId: {}", paperId, teacherId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取草稿失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<String> deleteDraft(Long paperId, Long teacherId) {
        try {
            log.info("删除审核草稿 - paperId: {}, teacherId: {}", paperId, teacherId);

            LambdaQueryWrapper<ReviewDraft> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ReviewDraft::getPaperId, paperId)
                    .eq(ReviewDraft::getTeacherId, teacherId)
                    .eq(ReviewDraft::getIsDeleted, 0);

            ReviewDraft draft = reviewDraftMapper.selectOne(queryWrapper);
            if (draft != null) {
                draft.setIsDeleted(1);
                draft.setUpdateBy(teacherId);
                draft.setUpdateTime(LocalDateTime.now());
                reviewDraftMapper.updateById(draft);
            }

            return Result.success("草稿已删除");
        } catch (Exception e) {
            log.error("删除审核草稿失败 - paperId: {}, teacherId: {}", paperId, teacherId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "删除草稿失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<ReviewDraftDTO>> listDrafts(Long teacherId) {
        try {
            log.info("获取教师草稿列表 - teacherId: {}", teacherId);

            LambdaQueryWrapper<ReviewDraft> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ReviewDraft::getTeacherId, teacherId)
                    .eq(ReviewDraft::getIsDeleted, 0)
                    .orderByDesc(ReviewDraft::getUpdateTime);

            List<ReviewDraft> drafts = reviewDraftMapper.selectList(queryWrapper);

            List<ReviewDraftDTO> dtoList = drafts.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return Result.success(dtoList);
        } catch (Exception e) {
            log.error("获取草稿列表失败 - teacherId: {}", teacherId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取草稿列表失败: " + e.getMessage());
        }
    }

    private ReviewDraftDTO convertToDTO(ReviewDraft draft) {
        if (draft == null) {
            return null;
        }
        return ReviewDraftDTO.builder()
                .id(draft.getId())
                .paperId(draft.getPaperId())
                .teacherId(draft.getTeacherId())
                .reviewStatus(draft.getReviewStatus())
                .reviewOpinion(draft.getReviewOpinion())
                .reviewAttach(draft.getReviewAttach())
                .createTime(draft.getCreateTime())
                .updateTime(draft.getUpdateTime())
                .build();
    }
}