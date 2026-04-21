package com.abin.checkrepeatsystem.user.service.Impl;

import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.notification.service.IntelligentNotificationService;
import com.abin.checkrepeatsystem.pojo.entity.SysNotice;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import com.abin.checkrepeatsystem.user.mapper.SysNoticeMapper;
import com.abin.checkrepeatsystem.user.service.SysNoticeService;
import com.abin.checkrepeatsystem.user.vo.PageResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 站内信服务实现（增强版：含分页、统计、批量操作等）
 */
@Slf4j
@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {

    @Resource
    private SysNoticeMapper sysNoticeMapper;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private IntelligentNotificationService intelligentNotificationService;

    // 通知类型映射
    private static final Map<Integer, String> NOTICE_TYPE_MAP = new HashMap<>();
    // 优先级映射
    private static final Map<Integer, String> PRIORITY_MAP = new HashMap<>();
    // 阅读状态映射
    private static final Map<Integer, String> READ_STATUS_MAP = new HashMap<>();

    static {
        // 初始化通知类型映射
        NOTICE_TYPE_MAP.put(1, "提交成功");
        NOTICE_TYPE_MAP.put(2, "查重完成");
        NOTICE_TYPE_MAP.put(3, "审核结果");
        NOTICE_TYPE_MAP.put(4, "截止提醒");
        NOTICE_TYPE_MAP.put(5, "系统通知");
        NOTICE_TYPE_MAP.put(6, "教师分配");
        NOTICE_TYPE_MAP.put(7, "论文修改请求");
        NOTICE_TYPE_MAP.put(8, "其他");

        // 初始化优先级映射
        PRIORITY_MAP.put(0, "普通");
        PRIORITY_MAP.put(1, "重要");
        PRIORITY_MAP.put(2, "紧急");

        // 初始化阅读状态映射
        READ_STATUS_MAP.put(0, "未读");
        READ_STATUS_MAP.put(1, "已读");
    }

    // ------------------------------ 发送通知相关方法 ------------------------------
    @Async("taskExecutor") // 异步执行，避免阻塞主流程
    @Override
    public void sendNotice(Long userId, Integer noticeType, String title, String content, boolean sendEmail) {
        sendNoticeWithPriority(userId, noticeType, 0, title, content, null, null, sendEmail);
    }

    @Async("taskExecutor")
    @Override
    public void sendNoticeWithPriority(Long userId, Integer noticeType, Integer priority, String title, String content, Long relatedId, String relatedType, boolean sendEmail) {
        try {
            // 1. 保存站内信（核心逻辑，确保登录后可查）
            SysNotice notice = new SysNotice();
            notice.setUserId(userId);
            notice.setNoticeType(noticeType);
            notice.setPriority(priority != null ? priority : 0);
            notice.setNoticeTitle(title);
            notice.setNoticeContent(content);
            notice.setRelatedId(relatedId);
            notice.setRelatedType(relatedType);
            notice.setIsRead(0); // 初始未读
            sysNoticeMapper.insert(notice);
            log.info("站内信保存成功：userId={}, noticeId={}, type={}, priority={}", userId, notice.getId(), noticeType, priority);

            // 2. 发送系统内邮箱（替代短信，无第三方依赖）
            if (sendEmail) {
                SysUser user = sysUserMapper.selectById(userId);
                if (user != null && StringUtils.hasText(user.getEmail())) {
                    boolean emailResult = intelligentNotificationService.sendEmailNotification(
                            user.getEmail(),
                            title,
                            content
                    );
                    if (!emailResult) {
                        log.warn("邮箱通知发送失败（站内信已正常保存）：userId={}, email={}", userId, user.getEmail());
                    }
                } else {
                    log.warn("用户邮箱不存在，无法发送邮箱通知：userId={}", userId);
                }
            }
        } catch (Exception e) {
            log.error("发送通知失败：userId={}, noticeType={}", userId, noticeType, e);
            // 通知失败不阻断主业务（如提交/查重），仅记录日志
        }
    }

    @Async("taskExecutor")
    @Override
    public void batchSendNotice(List<Long> userIds, Integer noticeType, String title, String content, boolean sendEmail) {
        try {
            for (Long userId : userIds) {
                sendNotice(userId, noticeType, title, content, sendEmail);
            }
            log.info("批量发送通知完成：用户数量={}, noticeType={}", userIds.size(), noticeType);
        } catch (Exception e) {
            log.error("批量发送通知失败", e);
        }
    }

    // ------------------------------ 查询相关方法 ------------------------------
    @Override
    public PageResultVO<SysNotice> getUserNoticePage(Long userId, Integer isRead, Integer priority, Integer noticeType, Integer pageNum, Integer pageSize) {
        // 1. 参数校验与默认值设置
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10; // 限制最大每页50条，避免性能问题
        int start = (pageNum - 1) * pageSize;

        // 2. 构建查询条件
        LambdaQueryWrapper<SysNotice> queryWrapper = new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getUserId, userId)
                .eq(SysNotice::getIsDeleted, 0);

        if (isRead != null) {
            queryWrapper.eq(SysNotice::getIsRead, isRead);
        }

        if (priority != null) {
            queryWrapper.eq(SysNotice::getPriority, priority);
        }

        if (noticeType != null) {
            queryWrapper.eq(SysNotice::getNoticeType, noticeType);
        }

        // 3. 查询当前页数据
        List<SysNotice> noticeList = sysNoticeMapper.selectList(
                queryWrapper
                        .orderByDesc(SysNotice::getCreateTime)
                        .last("LIMIT " + start + ", " + pageSize)
        );

        // 4. 查询总条数（用于分页计算）
        Integer totalCount = Math.toIntExact(sysNoticeMapper.selectCount(queryWrapper));

        // 5. 处理通知数据，添加冗余字段
        processNoticeList(noticeList);

        // 6. 组装分页结果VO（前端统一接收格式）
        return new PageResultVO<SysNotice>(
                pageNum,
                pageSize,
                totalCount,
                (totalCount + pageSize - 1) / pageSize, // 总页数（向上取整）
                noticeList
        );
    }

    @Override
    public Integer getUnreadNoticeCount(Long userId) {
        // 直接调用Mapper统计未读数量（仅查未删除+未读的通知）
        return sysNoticeMapper.countUnreadByUserId(userId);
    }

    @Override
    public Map<String, Integer> getUnreadNoticeCountByPriority(Long userId) {
        try {
            Map<String, Integer> priorityCountMap = new HashMap<>();
            
            // 统计各优先级的未读通知数量
            for (int priority = 0; priority <= 2; priority++) {
                long count = sysNoticeMapper.selectCount(
                        new LambdaQueryWrapper<SysNotice>()
                                .eq(SysNotice::getUserId, userId)
                                .eq(SysNotice::getIsRead, 0)
                                .eq(SysNotice::getPriority, priority)
                                .eq(SysNotice::getIsDeleted, 0)
                );
                priorityCountMap.put(PRIORITY_MAP.get(priority), Math.toIntExact(count));
            }
            
            return priorityCountMap;
        } catch (Exception e) {
            log.error("按优先级统计未读通知失败：userId={}", userId, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<SysNotice> getLatestNotice(Long userId, int limit) {
        try {
            List<SysNotice> noticeList = sysNoticeMapper.selectList(
                    new LambdaQueryWrapper<SysNotice>()
                            .eq(SysNotice::getUserId, userId)
                            .eq(SysNotice::getIsDeleted, 0)
                            .orderByDesc(SysNotice::getCreateTime)
                            .last("LIMIT " + limit)
            );
            
            processNoticeList(noticeList);
            return noticeList;
        } catch (Exception e) {
            log.error("获取最新通知失败：userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getNoticeStats(Long userId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 总通知数
            long totalCount = sysNoticeMapper.selectCount(
                    new LambdaQueryWrapper<SysNotice>()
                            .eq(SysNotice::getUserId, userId)
                            .eq(SysNotice::getIsDeleted, 0)
            );
            stats.put("totalCount", totalCount);
            
            // 未读通知数
            long unreadCount = sysNoticeMapper.selectCount(
                    new LambdaQueryWrapper<SysNotice>()
                            .eq(SysNotice::getUserId, userId)
                            .eq(SysNotice::getIsRead, 0)
                            .eq(SysNotice::getIsDeleted, 0)
            );
            stats.put("unreadCount", unreadCount);
            
            // 已读通知数
            stats.put("readCount", totalCount - unreadCount);
            
            // 按类型统计
            Map<String, Long> typeStats = new HashMap<>();
            List<SysNotice> notices = sysNoticeMapper.selectList(
                    new LambdaQueryWrapper<SysNotice>()
                            .eq(SysNotice::getUserId, userId)
                            .eq(SysNotice::getIsDeleted, 0)
            );
            
            for (SysNotice notice : notices) {
                String typeDesc = NOTICE_TYPE_MAP.getOrDefault(notice.getNoticeType(), "其他");
                typeStats.put(typeDesc, typeStats.getOrDefault(typeDesc, 0L) + 1);
            }
            stats.put("typeStats", typeStats);
            
            // 按优先级统计
            Map<String, Long> priorityStats = new HashMap<>();
            for (SysNotice notice : notices) {
                String priorityDesc = PRIORITY_MAP.getOrDefault(notice.getPriority(), "普通");
                priorityStats.put(priorityDesc, priorityStats.getOrDefault(priorityDesc, 0L) + 1);
            }
            stats.put("priorityStats", priorityStats);
            
            return stats;
        } catch (Exception e) {
            log.error("获取通知统计信息失败：userId={}", userId, e);
            return Collections.emptyMap();
        }
    }

    // ------------------------------ 标记已读相关方法 ------------------------------
    @Override
    public boolean markNoticeAsRead(Long noticeId, Long userId) {
        // 1. 先查询通知是否存在（避免无效操作）
        SysNotice notice = sysNoticeMapper.selectById(noticeId);
        if (notice == null || notice.getIsDeleted() == 1) {
            log.warn("标记通知已读失败：通知不存在或已删除，noticeId={}", noticeId);
            return false;
        }
        // 2. 权限校验：仅通知所属用户可标记
        if (!notice.getUserId().equals(userId)) {
            log.warn("标记通知已读失败：越权操作，noticeId={}, userId={}, ownerId={}", noticeId, userId, notice.getUserId());
            return false;
        }
        // 3. 已读无需重复标记
        if (notice.getIsRead() == 1) {
            log.info("通知已为已读状态，无需重复标记：noticeId={}", noticeId);
            return true;
        }
        // 4. 执行标记操作（更新is_read和read_time）
        int rows = sysNoticeMapper.markAsRead(noticeId, userId, LocalDateTime.now());
        return rows > 0;
    }

    @Override
    public Integer batchMarkAsRead(List<Long> noticeIds, Long userId) {
        try {
            int successCount = 0;
            for (Long noticeId : noticeIds) {
                if (markNoticeAsRead(noticeId, userId)) {
                    successCount++;
                }
            }
            return successCount;
        } catch (Exception e) {
            log.error("批量标记通知已读失败：userId={}", userId, e);
            return 0;
        }
    }

    @Override
    public Integer markAllUnreadAsRead(Long userId) {
        // 调用Mapper批量更新（仅更新当前用户的未读+未删除通知）
        return sysNoticeMapper.markAllUnreadAsRead(userId, LocalDateTime.now());
    }

    // ------------------------------ 删除相关方法 ------------------------------
    @Override
    public boolean deleteNotice(Long noticeId, Long userId) {
        // 1. 先查询通知是否存在（避免无效操作）
        SysNotice notice = sysNoticeMapper.selectById(noticeId);
        if (notice == null || notice.getIsDeleted() == 1) {
            log.warn("删除通知失败：通知不存在或已删除，noticeId={}", noticeId);
            return false;
        }
        // 2. 权限校验：仅通知所属用户可删除
        if (!notice.getUserId().equals(userId)) {
            log.warn("删除通知失败：越权操作，noticeId={}, userId={}, ownerId={}", noticeId, userId, notice.getUserId());
            return false;
        }
        // 3. 执行删除操作（软删除）
        notice.setIsDeleted(1);
        int rows = sysNoticeMapper.updateById(notice);
        return rows > 0;
    }

    @Override
    public Integer batchDeleteNotice(List<Long> noticeIds, Long userId) {
        try {
            int successCount = 0;
            for (Long noticeId : noticeIds) {
                if (deleteNotice(noticeId, userId)) {
                    successCount++;
                }
            }
            return successCount;
        } catch (Exception e) {
            log.error("批量删除通知失败：userId={}", userId, e);
            return 0;
        }
    }

    @Override
    public Integer clearAllNotice(Long userId) {
        try {
            SysNotice updateNotice = new SysNotice();
            updateNotice.setIsDeleted(1);
            
            int rows = sysNoticeMapper.update(
                    updateNotice,
                    new LambdaQueryWrapper<SysNotice>()
                            .eq(SysNotice::getUserId, userId)
                            .eq(SysNotice::getIsDeleted, 0)
            );
            return rows;
        } catch (Exception e) {
            log.error("清空所有通知失败：userId={}", userId, e);
            return 0;
        }
    }

    // ------------------------------ 辅助方法 ------------------------------
    /**
     * 处理通知列表，添加冗余字段
     */
    private void processNoticeList(List<SysNotice> noticeList) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (SysNotice notice : noticeList) {
            // 设置通知类型描述
            notice.setNoticeTypeDesc(NOTICE_TYPE_MAP.getOrDefault(notice.getNoticeType(), "其他"));
            
            // 设置优先级描述
            notice.setPriorityDesc(PRIORITY_MAP.getOrDefault(notice.getPriority(), "普通"));
            
            // 设置阅读状态文本
            notice.setReadStatusText(READ_STATUS_MAP.getOrDefault(notice.getIsRead(), "未知"));
            
            // 设置创建时间文本
            if (notice.getCreateTime() != null) {
                notice.setCreateTimeText(notice.getCreateTime().format(formatter));
            }
        }
    }
}
