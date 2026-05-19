package com.abin.checkrepeatsystem.common.service.Impl;

import com.abin.checkrepeatsystem.mapper.SysNoticeLogMapper;
import com.abin.checkrepeatsystem.pojo.entity.SysNoticeLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoticeLogService {

    private final SysNoticeLogMapper noticeLogMapper;

    /**
     * 保存通知日志
     */
    public void saveNoticeLog(String toEmail, String subject, String content,
                              String noticeType, boolean success, String errorMsg,
                              Long relatedId, String relatedType) {
        try {
            SysNoticeLog noticeLog = new SysNoticeLog();
            noticeLog.setToEmail(toEmail);
            noticeLog.setSubject(subject);
            noticeLog.setContent(content);
            noticeLog.setNoticeType(noticeType);
            noticeLog.setSuccess(success);
            noticeLog.setErrorMsg(errorMsg);
            noticeLog.setRelatedId(relatedId);
            noticeLog.setRelatedType(relatedType);
            noticeLog.setSendTime(LocalDateTime.now());
            noticeLog.setCreateTime(LocalDateTime.now());

            noticeLogMapper.insert(noticeLog);

            log.debug("通知日志保存成功：toEmail={}, noticeType={}", toEmail, noticeType);
        } catch (Exception e) {
            log.error("保存通知日志失败：toEmail={}, noticeType={}", toEmail, noticeType, e);
        }
    }

    /**
     * 查询通知发送记录
     */
    public List<SysNoticeLog> getNoticeLogsByRelatedId(Long relatedId, String relatedType) {
        LambdaQueryWrapper<SysNoticeLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysNoticeLog::getRelatedId, relatedId)
                .eq(SysNoticeLog::getRelatedType, relatedType)
                .orderByDesc(SysNoticeLog::getSendTime);
        return noticeLogMapper.selectList(queryWrapper);
    }

    /**
     * 统计通知发送情况
     */
    public NoticeStats getNoticeStats(LocalDate startDate, LocalDate endDate) {
        List<SysNoticeLog> logs = noticeLogMapper.selectList(new LambdaQueryWrapper<SysNoticeLog>()
                .ge(SysNoticeLog::getSendTime, startDate.atStartOfDay())
                .le(SysNoticeLog::getSendTime, endDate.plusDays(1).atStartOfDay()));

        long total = logs.size();
        long successCount = logs.stream().filter(SysNoticeLog::getSuccess).count();
        double successRate = total > 0 ? (double) successCount / total * 100 : 0;

        return NoticeStats.builder()
                .totalCount(total)
                .successCount(successCount)
                .failureCount(total - successCount)
                .successRate(String.format("%.2f%%", successRate))
                .build();
    }
}

@Data
@Builder
class NoticeStats {
    private Long totalCount;
    private Long successCount;
    private Long failureCount;
    private String successRate;
}
