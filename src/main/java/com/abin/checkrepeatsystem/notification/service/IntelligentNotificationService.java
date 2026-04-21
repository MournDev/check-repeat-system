package com.abin.checkrepeatsystem.notification.service;

import com.abin.checkrepeatsystem.common.enums.NoticeType;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.user.service.Impl.InternalMessageNotificationService;
import com.abin.checkrepeatsystem.user.service.Impl.OptimizedEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 智能通知服务
 * 整合邮件和站内信，根据场景智能选择通知方式
 */
@Service
@Slf4j
public class IntelligentNotificationService {

    @Resource
    private OptimizedEmailService emailService;

    @Resource
    private InternalMessageNotificationService internalMessageService;

    /**
     * 发送论文提交成功通知
     */
    public void sendPaperSubmittedNotice(Long paperId, String paperTitle, Long studentId, SysUser student) {
        try {
            // 构建通知参数
            Map<String, Object> params = Map.of(
                    "studentName", student.getRealName(),
                    "paperTitle", paperTitle,
                    "submitTime", java.time.LocalDateTime.now().toString()
            );

            // 同时发送邮件和站内信
            sendDualNotification(student, NoticeType.PAPER_SUBMITTED, params, paperId, "PAPER");

        } catch (Exception e) {
            log.error("发送论文提交通知失败: paperId={}, studentId={}", paperId, studentId, e);
        }
    }

    /**
     * 发送查重完成通知
     */
    public void sendCheckCompletedNotice(Long paperId, String paperTitle, Long studentId, 
                                        SysUser student, Double similarityRate) {
        try {
            boolean needsReview = similarityRate > 30.0;

            Map<String, Object> params = Map.of(
                    "studentName", student.getRealName(),
                    "paperTitle", paperTitle,
                    "similarityRate", String.format("%.2f", similarityRate),
                    "checkTime", java.time.LocalDateTime.now().toString(),
                    "needsReview", needsReview
            );

            // 同时发送邮件和站内信
            sendDualNotification(student, NoticeType.PAPER_CHECK_COMPLETED, params, paperId, "PAPER");

        } catch (Exception e) {
            log.error("发送查重完成通知失败: paperId={}, studentId={}", paperId, studentId, e);
        }
    }

    /**
     * 发送论文需要修改通知
     */
    public void sendNeedsRevisionNotice(Long paperId, String paperTitle, Long studentId, 
                                       SysUser student, String feedback, Long advisorId, String advisorName) {
        try {
            Map<String, Object> params = Map.of(
                    "studentName", student.getRealName(),
                    "paperTitle", paperTitle,
                    "feedback", feedback,
                    "advisorName", advisorName
            );

            // 同时发送邮件和站内信
            sendDualNotification(student, NoticeType.PAPER_NEEDS_REVISION, params, paperId, "PAPER");

        } catch (Exception e) {
            log.error("发送修改通知失败: paperId={}, studentId={}", paperId, studentId, e);
        }
    }

    /**
     * 发送论文审核通过通知
     */
    public void sendPaperApprovedNotice(Long paperId, String paperTitle, Long studentId, SysUser student) {
        try {
            Map<String, Object> params = Map.of(
                    "studentName", student.getRealName(),
                    "paperTitle", paperTitle
            );

            // 同时发送邮件和站内信
            sendDualNotification(student, NoticeType.PAPER_APPROVED, params, paperId, "PAPER");

        } catch (Exception e) {
            log.error("发送审核通过通知失败: paperId={}, studentId={}", paperId, studentId, e);
        }
    }

    /**
     * 发送指导老师分配通知
     */
    public void sendAdvisorAssignedNotice(Long paperId, String paperTitle, Long studentId, 
                                         SysUser student, Long advisorId, SysUser advisor) {
        try {
            // 通知学生
            Map<String, Object> studentParams = Map.of(
                    "studentName", student.getRealName(),
                    "paperTitle", paperTitle,
                    "advisorName", advisor.getRealName(),
                    "advisorEmail", advisor.getEmail()
            );

            sendDualNotification(student, NoticeType.ADVISOR_ASSIGNED, studentParams, paperId, "PAPER");

            // 通知老师
            Map<String, Object> advisorParams = Map.of(
                    "advisorName", advisor.getRealName(),
                    "studentName", student.getRealName(),
                    "paperTitle", paperTitle,
                    "studentEmail", student.getEmail()
            );

            sendDualNotification(advisor, NoticeType.SYSTEM_ANNOUNCEMENT, advisorParams, paperId, "PAPER");

        } catch (Exception e) {
            log.error("发送老师分配通知失败: paperId={}, studentId={}, advisorId={}", 
                    paperId, studentId, advisorId, e);
        }
    }

    /**
     * 发送高相似度预警通知给指导老师
     */
    public void sendHighSimilarityAlertToAdvisor(Long paperId, String paperTitle, Long studentId, 
                                               SysUser student, Long advisorId, SysUser advisor, Double similarityRate) {
        try {
            Map<String, Object> params = Map.of(
                    "advisorName", advisor.getRealName(),
                    "studentName", student.getRealName(),
                    "paperTitle", paperTitle,
                    "similarityRate", String.format("%.2f", similarityRate)
            );

            // 同时发送邮件和站内信
            sendDualNotification(advisor, NoticeType.SYSTEM_ANNOUNCEMENT, params, paperId, "PAPER");

        } catch (Exception e) {
            log.error("发送高相似度预警通知失败: paperId={}, advisorId={}", paperId, advisorId, e);
        }
    }

    /**
     * 发送系统公告
     */
    public void sendSystemAnnouncement(String title, String content, SysUser user) {
        try {
            Map<String, Object> params = Map.of(
                    "title", title,
                    "content", content
            );

            // 同时发送邮件和站内信
            sendDualNotification(user, NoticeType.SYSTEM_ANNOUNCEMENT, params, null, "SYSTEM");

        } catch (Exception e) {
            log.error("发送系统公告失败: userId={}", user.getId(), e);
        }
    }

    /**
     * 发送账户激活通知
     */
    public void sendAccountActivationNotice(SysUser user, String activationUrl) {
        try {
            Map<String, Object> params = Map.of(
                    "userName", user.getRealName(),
                    "activationUrl", activationUrl
            );

            // 只发送邮件（重要通知）
            sendEmailNotification(user, NoticeType.ACCOUNT_ACTIVATION, params, null, "USER");

        } catch (Exception e) {
            log.error("发送账户激活通知失败: userId={}", user.getId(), e);
        }
    }

    /**
     * 发送密码重置通知
     */
    public void sendPasswordResetNotice(SysUser user, String resetUrl) {
        try {
            Map<String, Object> params = Map.of(
                    "userName", user.getRealName(),
                    "resetUrl", resetUrl
            );

            // 只发送邮件（安全通知）
            sendEmailNotification(user, NoticeType.PASSWORD_RESET, params, null, "USER");

        } catch (Exception e) {
            log.error("发送密码重置通知失败: userId={}", user.getId(), e);
        }
    }

    /**
     * 双重通知（邮件+站内信）
     */
    private void sendDualNotification(SysUser user, NoticeType noticeType, 
                                     Map<String, Object> params, Long relatedId, String relatedType) {
        // 异步发送邮件
        CompletableFuture.runAsync(() -> {
            try {
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    emailService.sendTaskCompletionEmailAsync(
                            user.getEmail(), noticeType, params, relatedId, relatedType);
                }
            } catch (Exception e) {
                log.warn("邮件发送失败: userId={}, error={}", user.getId(), e.getMessage());
            }
        });

        // 发送站内信
        try {
            internalMessageService.sendSimpleNotice(
                    user.getId(), 
                    noticeType.getTitle(), 
                    buildInternalMessageContent(noticeType, params)
            );
        } catch (Exception e) {
            log.warn("站内信发送失败: userId={}, error={}", user.getId(), e.getMessage());
        }
    }

    /**
     * 只发送邮件通知
     */
    private void sendEmailNotification(SysUser user, NoticeType noticeType, 
                                      Map<String, Object> params, Long relatedId, String relatedType) {
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendTaskCompletionEmailAsync(
                            user.getEmail(), noticeType, params, relatedId, relatedType);
                } catch (Exception e) {
                    log.warn("邮件发送失败: userId={}, error={}", user.getId(), e.getMessage());
                }
            });
        }
    }

    /**
     * 构建站内信内容
     */
    private String buildInternalMessageContent(NoticeType noticeType, Map<String, Object> params) {
        switch (noticeType) {
            case PAPER_SUBMITTED:
                return String.format("您的论文《%s》已成功提交，系统正在处理中。", 
                        params.get("paperTitle"));
            case PAPER_CHECK_COMPLETED:
                return String.format("您的论文《%s》查重已完成，相似度为%s%%，请登录系统查看详细报告。", 
                        params.get("paperTitle"), params.get("similarityRate"));
            case PAPER_NEEDS_REVISION:
                return String.format("您的论文《%s》需要修改，请登录系统查看详细修改意见。", 
                        params.get("paperTitle"));
            case PAPER_APPROVED:
                return String.format("恭喜！您的论文《%s》已通过审核。", 
                        params.get("paperTitle"));
            case ADVISOR_ASSIGNED:
                return String.format("您的论文《%s》已分配指导老师：%s。", 
                        params.get("paperTitle"), params.get("advisorName"));
            case SYSTEM_ANNOUNCEMENT:
                return String.format("【系统公告】%s\n%s", 
                        params.get("title"), params.get("content"));
            case ACCOUNT_ACTIVATION:
                return "您的账户需要激活，请点击邮件中的链接完成激活。";
            case PASSWORD_RESET:
                return "您的密码重置请求已收到，请点击邮件中的链接完成密码重置。";
            default:
                return noticeType.getDefaultContent();
        }
    }

    /**
     * 批量发送通知
     */
    public void sendBatchNotification(List<SysUser> users, NoticeType noticeType, 
                                     Map<String, Object> params, Long relatedId, String relatedType) {
        for (SysUser user : users) {
            try {
                sendDualNotification(user, noticeType, params, relatedId, relatedType);
            } catch (Exception e) {
                log.warn("批量通知失败: userId={}, error={}", user.getId(), e.getMessage());
            }
        }
    }

    /**
     * 发送紧急通知（高优先级）
     */
    public void sendEmergencyNotification(SysUser user, String title, String content, 
                                         Long relatedId, String relatedType) {
        // 同步发送邮件（确保及时送达）
        try {
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                emailService.sendNoticeEmail(
                        user.getEmail(), title, content, "EMERGENCY", relatedId, relatedType);
            }
        } catch (Exception e) {
            log.error("紧急邮件发送失败: userId={}, error={}", user.getId(), e.getMessage());
        }

        // 发送站内信
        try {
            internalMessageService.sendSimpleNotice(user.getId(), title, content);
        } catch (Exception e) {
            log.error("紧急站内信发送失败: userId={}, error={}", user.getId(), e.getMessage());
        }
    }

    /**
     * 发送简单邮件通知（兼容旧接口）
     */
    public boolean sendEmailNotification(String email, String title, String content) {
        try {
            Map<String, Object> params = Map.of(
                    "title", title,
                    "content", content
            );
            
            // 创建一个临时的 SysUser 对象
            SysUser user = new SysUser();
            user.setEmail(email);
            
            // 调用现有的方法
            sendEmailNotification(user, NoticeType.SYSTEM_ANNOUNCEMENT, params, null, "USER");
            return true;
        } catch (Exception e) {
            log.error("发送邮件通知失败: email={}, error={}", email, e.getMessage());
            return false;
        }
    }
}