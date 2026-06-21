package com.abin.checkrepeatsystem.common.service;

import com.abin.checkrepeatsystem.common.enums.PaperStatusEnum;
import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.notification.service.NotificationService;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.user.service.PaperStatusLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 论文状态转换服务
 * 统一管理所有论文状态的流转，确保状态机的严格约束
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class PaperStatusTransitionService {

    private final PaperInfoMapper paperInfoMapper;
    private final PaperStatusLogService paperStatusLogService;
    private final NotificationService notificationService;

    /**
     * 合法的状态转换映射表
     * Key: 当前状态, Value: 允许转换到的目标状态集合
     */
    private static final Map<PaperStatusEnum, Set<PaperStatusEnum>> VALID_TRANSITIONS = new HashMap<>();

    static {
        // PENDING → ASSIGNED (自动分配/手动分配)
        VALID_TRANSITIONS.put(PaperStatusEnum.PENDING, Set.of(
                PaperStatusEnum.ASSIGNED,
                PaperStatusEnum.WITHDRAWN
        ));

        // ASSIGNED → CHECKING (触发查重)
        VALID_TRANSITIONS.put(PaperStatusEnum.ASSIGNED, Set.of(
                PaperStatusEnum.CHECKING,
                PaperStatusEnum.WITHDRAWN
        ));

        // CHECKING → AUDITING (查重通过) / REJECTED (查重不通过) / ASSIGNED (取消查重，回退)
        VALID_TRANSITIONS.put(PaperStatusEnum.CHECKING, Set.of(
                PaperStatusEnum.AUDITING,
                PaperStatusEnum.REJECTED,
                PaperStatusEnum.ASSIGNED,
                PaperStatusEnum.WITHDRAWN
        ));

        // AUDITING → COMPLETED (审核通过) / REJECTED (审核不通过) / REVISION_NEEDED (需要修改)
        VALID_TRANSITIONS.put(PaperStatusEnum.AUDITING, Set.of(
                PaperStatusEnum.COMPLETED,
                PaperStatusEnum.REJECTED,
                PaperStatusEnum.REVISION_NEEDED,
                PaperStatusEnum.WITHDRAWN
        ));

        // REJECTED → ASSIGNED (重新提交) / PENDING (重新提交后重新走流程)
        VALID_TRANSITIONS.put(PaperStatusEnum.REJECTED, Set.of(
                PaperStatusEnum.ASSIGNED,
                PaperStatusEnum.PENDING,
                PaperStatusEnum.WITHDRAWN
        ));

        // COMPLETED → AUDITING (申请修改)
        VALID_TRANSITIONS.put(PaperStatusEnum.COMPLETED, Set.of(
                PaperStatusEnum.AUDITING
        ));

        // WITHDRAWN → ASSIGNED (撤回后重新提交) / PENDING (重新提交后重新走流程)
        VALID_TRANSITIONS.put(PaperStatusEnum.WITHDRAWN, Set.of(
                PaperStatusEnum.ASSIGNED,
                PaperStatusEnum.PENDING
        ));

        // REVISION_NEEDED → ASSIGNED (修改后重新提交) / PENDING (重新提交后重新走流程)
        VALID_TRANSITIONS.put(PaperStatusEnum.REVISION_NEEDED, Set.of(
                PaperStatusEnum.ASSIGNED,
                PaperStatusEnum.PENDING,
                PaperStatusEnum.WITHDRAWN
        ));
    }

    /**
     * 执行论文状态转换
     *
     * @param paperId      论文ID
     * @param targetStatus 目标状态
     * @param operatorId   操作人ID
     * @param reason       转换原因
     * @return 更新后的 PaperInfo
     */
    @Transactional(rollbackFor = Exception.class)
    public PaperInfo transition(Long paperId, PaperStatusEnum targetStatus, Long operatorId, String reason) {
        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "论文不存在: " + paperId);
        }

        PaperStatusEnum currentStatus = PaperStatusEnum.fromCode(paperInfo.getPaperStatus());
        validateTransition(currentStatus, targetStatus);

        String oldStatusValue = currentStatus.getValue();
        String newStatusValue = targetStatus.getValue();

        // 更新论文状态
        PaperInfo update = new PaperInfo();
        update.setId(paperId);
        update.setPaperStatus(newStatusValue);
        update.setUpdateTime(LocalDateTime.now());
        paperInfoMapper.updateById(update);

        // 记录状态变更日志
        paperStatusLogService.recordStatusLog(
                paperId, oldStatusValue, newStatusValue, reason, operatorId, null);

        log.info("论文状态转换成功: paperId={}, {} -> {}, 操作人={}, 原因={}",
                paperId, currentStatus.getDescription(), targetStatus.getDescription(),
                operatorId, reason);

        // 异步发送通知（不阻塞主流程）
        sendTransitionNotification(paperInfo, currentStatus, targetStatus, reason);

        return paperInfo;
    }

    /**
     * 执行论文状态转换（不抛异常，返回是否成功）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean transitionSilently(Long paperId, PaperStatusEnum targetStatus, Long operatorId, String reason) {
        try {
            transition(paperId, targetStatus, operatorId, reason);
            return true;
        } catch (BusinessException e) {
            log.warn("论文状态转换失败: paperId={}, targetStatus={}, reason={}", paperId, targetStatus, e.getMessage());
            return false;
        }
    }

    /**
     * 校验状态转换是否合法
     */
    public void validateTransition(PaperStatusEnum currentStatus, PaperStatusEnum targetStatus) {
        if (currentStatus == null) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_STATUS, "当前论文状态为空");
        }

        // 终态不能再转换（除非特定业务逻辑覆盖：重新提交到PENDING/ASSIGNED，或申请修改到AUDITING）
        if (currentStatus.isTerminalStatus() && targetStatus != PaperStatusEnum.AUDITING
                && targetStatus != PaperStatusEnum.ASSIGNED
                && targetStatus != PaperStatusEnum.PENDING) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_STATUS,
                    String.format("论文已处于终态（%s），不允许转换为%s",
                            currentStatus.getDescription(), targetStatus.getDescription()));
        }

        Set<PaperStatusEnum> allowedTargets = VALID_TRANSITIONS.get(currentStatus);
        if (allowedTargets == null || !allowedTargets.contains(targetStatus)) {
            throw new BusinessException(ResultCode.PERMISSION_NOT_STATUS,
                    String.format("不允许从[%s]转换为[%s]",
                            currentStatus.getDescription(), targetStatus.getDescription()));
        }
    }

    /**
     * 检查是否可以进行状态转换
     */
    public boolean canTransition(PaperStatusEnum currentStatus, PaperStatusEnum targetStatus) {
        if (currentStatus == null) return false;
        Set<PaperStatusEnum> allowedTargets = VALID_TRANSITIONS.get(currentStatus);
        return allowedTargets != null && allowedTargets.contains(targetStatus);
    }

    /**
     * 获取当前状态允许转换的目标状态列表
     */
    public Set<PaperStatusEnum> getAllowedTransitions(PaperStatusEnum currentStatus) {
        return VALID_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
    }

    /**
     * 发送状态转换通知
     */
    private void sendTransitionNotification(PaperInfo paperInfo, PaperStatusEnum from,
                                            PaperStatusEnum to, String reason) {
        try {
            Long studentId = paperInfo.getStudentId();
            String paperTitle = paperInfo.getPaperTitle();

            switch (to) {
                case AUDITING:
                    // 查重完成，进入待审核
                    if (from == PaperStatusEnum.CHECKING) {
                        notificationService.sendSimilarityCheckResultNotification(
                                studentId, paperInfo.getId(), paperTitle,
                                paperInfo.getSimilarityRate() != null ? paperInfo.getSimilarityRate().doubleValue() : 0.0,
                                paperInfo.getCheckResult());
                    }
                    break;
                case COMPLETED:
                    // 审核通过
                    notificationService.sendPaperReviewResultNotification(
                            studentId, paperInfo.getId(), paperTitle, "通过", reason);
                    break;
                case REVISION_NEEDED:
                    // 需要修改
                    notificationService.sendPaperReviewResultNotification(
                            studentId, paperInfo.getId(), paperTitle, "需要修改", reason);
                    break;
                case REJECTED:
                    // 审核不通过 / 查重不通过
                    notificationService.sendPaperReviewResultNotification(
                            studentId, paperInfo.getId(), paperTitle, "不通过", reason);
                    break;
                case ASSIGNED:
                    // 分配导师成功
                    notificationService.sendPaperStatusChangeNotification(
                            studentId, paperInfo.getId(), paperTitle, "已分配导师", reason);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.warn("发送状态转换通知失败: paperId={}, {} -> {}", paperInfo.getId(), from, to, e);
        }
    }
}
