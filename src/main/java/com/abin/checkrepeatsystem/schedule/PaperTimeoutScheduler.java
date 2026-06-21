package com.abin.checkrepeatsystem.schedule;

import com.abin.checkrepeatsystem.common.constant.DictConstants;
import com.abin.checkrepeatsystem.common.enums.NoticeType;
import com.abin.checkrepeatsystem.common.enums.UserTypeEnum;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysNotice;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.user.mapper.SysNoticeMapper;
import com.abin.checkrepeatsystem.user.service.SysNoticeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 论文超时定时处理任务
 * 对长期滞留在各状态的论文进行提醒通知
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaperTimeoutScheduler {

    private final PaperInfoMapper paperInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final SysNoticeService sysNoticeService;
    private final SysNoticeMapper sysNoticeMapper;

    private static final String RELATED_TYPE = "PAPER";

    /**
     * 每天 09:00 处理 PENDING 超时（>7天）
     * 论文提交后长期未分配给教师
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void handlePendingTimeout() {
        log.info("开始检查 PENDING 超时论文...");
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        List<PaperInfo> papers = paperInfoMapper.selectList(
                new LambdaQueryWrapper<PaperInfo>()
                        .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.PENDING)
                        .lt(PaperInfo::getCreateTime, threshold)
                        .eq(PaperInfo::getIsDeleted, 0)
        );

        if (papers.isEmpty()) {
            log.info("未发现 PENDING 超时论文");
            return;
        }

        log.warn("发现 {} 篇 PENDING 超时论文（>7天）", papers.size());
        List<Long> adminIds = getAdminUserIds();

        for (PaperInfo paper : papers) {
            long waitDays = Duration.between(paper.getCreateTime(), LocalDateTime.now()).toDays();
            String title = NoticeType.PAPER_PENDING_TIMEOUT.getTitle();
            String content = String.format("论文《%s》（提交者：%s）已等待 %d 天未分配审核老师，请及时处理。",
                    paper.getPaperTitle(), paper.getAuthor(), waitDays);

            notifyAdmins(adminIds, NoticeType.PAPER_PENDING_TIMEOUT, title, content, paper.getId());
        }

        log.info("PENDING 超时处理完成，共处理 {} 篇", papers.size());
    }

    /**
     * 每天 09:30 处理 ASSIGNED 超时（>3天）
     * 论文已分配教师但长时间未进入查重流程
     */
    @Scheduled(cron = "0 30 9 * * ?")
    public void handleSubmittedTimeout() {
        log.info("开始检查 ASSIGNED 超时论文...");
        LocalDateTime threshold = LocalDateTime.now().minusDays(3);

        List<PaperInfo> papers = paperInfoMapper.selectList(
                new LambdaQueryWrapper<PaperInfo>()
                        .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.ASSIGNED)
                        .lt(PaperInfo::getSubmitTime, threshold)
                        .and(w -> w.isNull(PaperInfo::getCheckCompleted).or().eq(PaperInfo::getCheckCompleted, 0))
                        .eq(PaperInfo::getIsDeleted, 0)
        );

        if (papers.isEmpty()) {
            log.info("未发现 ASSIGNED 超时论文");
            return;
        }

        log.warn("发现 {} 篇 ASSIGNED 超时论文（>3天未查重）", papers.size());
        List<Long> adminIds = getAdminUserIds();

        for (PaperInfo paper : papers) {
            long waitDays = Duration.between(paper.getSubmitTime(), LocalDateTime.now()).toDays();

            // 通知学生
            String studentTitle = NoticeType.PAPER_SUBMITTED_TIMEOUT.getTitle();
            String studentContent = String.format("您的论文《%s》已提交 %d 天，查重流程尚未完成，请耐心等待或联系管理员。",
                    paper.getPaperTitle(), waitDays);
            notifyUser(paper.getStudentId(), NoticeType.PAPER_SUBMITTED_TIMEOUT, 1,
                    studentTitle, studentContent, paper.getId());

            // 通知管理员
            String adminTitle = "论文查重流程延迟";
            String adminContent = String.format("论文《%s》（提交者：%s）已提交 %d 天未完成查重，请排查处理。",
                    paper.getPaperTitle(), paper.getAuthor(), waitDays);
            notifyAdmins(adminIds, NoticeType.PAPER_SUBMITTED_TIMEOUT, adminTitle, adminContent, paper.getId());
        }

        log.info("ASSIGNED 超时处理完成，共处理 {} 篇", papers.size());
    }

    /**
     * 每天 10:00 处理 AUDITING 超时（>7天）
     * 论文等待教师审核时间过长
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void handleAuditingTimeout() {
        log.info("开始检查 AUDITING 超时论文...");
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        List<PaperInfo> papers = paperInfoMapper.selectList(
                new LambdaQueryWrapper<PaperInfo>()
                        .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.AUDITING)
                        .lt(PaperInfo::getUpdateTime, threshold)
                        .eq(PaperInfo::getIsDeleted, 0)
        );

        if (papers.isEmpty()) {
            log.info("未发现 AUDITING 超时论文");
            return;
        }

        log.warn("发现 {} 篇 AUDITING 超时论文（>7天未审核）", papers.size());
        List<Long> adminIds = getAdminUserIds();

        for (PaperInfo paper : papers) {
            long waitDays = Duration.between(paper.getUpdateTime(), LocalDateTime.now()).toDays();

            // 催审教师
            if (paper.getTeacherId() != null) {
                String teacherTitle = NoticeType.PAPER_AUDITING_TIMEOUT.getTitle();
                String teacherContent = String.format("论文《%s》（学生：%s）已等待审核 %d 天，请尽快完成审核。",
                        paper.getPaperTitle(), paper.getAuthor(), waitDays);
                notifyUser(paper.getTeacherId(), NoticeType.PAPER_AUDITING_TIMEOUT, 2,
                        teacherTitle, teacherContent, paper.getId());
            }

            // 通知管理员
            String adminTitle = "论文审核超时催办";
            String adminContent = String.format("论文《%s》（学生：%s，教师：%s）已等待审核 %d 天，请关注处理。",
                    paper.getPaperTitle(), paper.getAuthor(),
                    paper.getTeacherName() != null ? paper.getTeacherName() : "未分配", waitDays);
            notifyAdmins(adminIds, NoticeType.PAPER_AUDITING_TIMEOUT, adminTitle, adminContent, paper.getId());
        }

        log.info("AUDITING 超时处理完成，共处理 {} 篇", papers.size());
    }

    /**
     * 每天 10:30 处理 REJECTED 超时（>14天）
     * 论文审核不通过后学生长时间未重新提交
     */
    @Scheduled(cron = "0 30 10 * * ?")
    public void handleRevisedTimeout() {
        log.info("开始检查 REJECTED 超时论文...");
        LocalDateTime threshold = LocalDateTime.now().minusDays(14);

        List<PaperInfo> papers = paperInfoMapper.selectList(
                new LambdaQueryWrapper<PaperInfo>()
                        .eq(PaperInfo::getPaperStatus, DictConstants.PaperStatus.REJECTED)
                        .lt(PaperInfo::getUpdateTime, threshold)
                        .eq(PaperInfo::getIsDeleted, 0)
        );

        if (papers.isEmpty()) {
            log.info("未发现 REJECTED 超时论文");
            return;
        }

        log.warn("发现 {} 篇 REJECTED 超时论文（>14天未重新提交）", papers.size());

        for (PaperInfo paper : papers) {
            long waitDays = Duration.between(paper.getUpdateTime(), LocalDateTime.now()).toDays();

            // 提醒学生
            String studentTitle = NoticeType.PAPER_REVISED_TIMEOUT.getTitle();
            String studentContent = String.format("您的论文《%s》已被驳回 %d 天，请尽快修改并重新提交。",
                    paper.getPaperTitle(), waitDays);
            notifyUser(paper.getStudentId(), NoticeType.PAPER_REVISED_TIMEOUT, 1,
                    studentTitle, studentContent, paper.getId());

            // 通知导师
            if (paper.getTeacherId() != null) {
                String teacherTitle = "学生论文修改超时提醒";
                String teacherContent = String.format("学生 %s 的论文《%s》已被驳回 %d 天未重新提交，请关注。",
                        paper.getAuthor(), paper.getPaperTitle(), waitDays);
                notifyUser(paper.getTeacherId(), NoticeType.PAPER_REVISED_TIMEOUT, 1,
                        teacherTitle, teacherContent, paper.getId());
            }
        }

        log.info("REVISED 超时处理完成，共处理 {} 篇", papers.size());
    }

    /**
     * 获取所有管理员用户ID
     */
    private List<Long> getAdminUserIds() {
        List<SysUser> admins = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getUserType, UserTypeEnum.ROLE_ADMIN, UserTypeEnum.ROLE_SUPER_ADMIN)
                        .eq(SysUser::getIsDeleted, 0)
        );
        return admins.stream().map(SysUser::getId).collect(Collectors.toList());
    }

    /**
     * 向管理员批量发送通知（带去重）
     */
    private void notifyAdmins(List<Long> adminIds, NoticeType noticeType, String title, String content, Long paperId) {
        for (Long adminId : adminIds) {
            if (!hasNotifiedToday(adminId, noticeType, paperId)) {
                notifyUser(adminId, noticeType, 1, title, content, paperId);
            }
        }
    }

    /**
     * 向单个用户发送通知
     */
    private void notifyUser(Long userId, NoticeType noticeType, Integer priority,
                            String title, String content, Long paperId) {
        try {
            sysNoticeService.sendNoticeWithPriority(
                    userId, 5, priority, title, content, paperId, RELATED_TYPE, false);
        } catch (Exception e) {
            log.error("发送超时通知失败：userId={}, paperId={}, type={}", userId, paperId, noticeType, e);
        }
    }

    /**
     * 检查当天是否已向该用户发送过同类型同论文的通知（防重复提醒）
     */
    private boolean hasNotifiedToday(Long userId, NoticeType noticeType, Long paperId) {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        Long count = sysNoticeMapper.selectCount(
                new LambdaQueryWrapper<SysNotice>()
                        .eq(SysNotice::getUserId, userId)
                        .eq(SysNotice::getNoticeTitle, noticeType.getTitle())
                        .eq(SysNotice::getRelatedId, paperId)
                        .eq(SysNotice::getRelatedType, RELATED_TYPE)
                        .ge(SysNotice::getCreateTime, todayStart)
        );
        return count != null && count > 0;
    }
}
