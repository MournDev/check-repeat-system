package com.abin.checkrepeatsystem.user.service;

import com.abin.checkrepeatsystem.pojo.entity.SysNotice;
import com.abin.checkrepeatsystem.user.vo.PageResultVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 站内信服务接口（增强版：含分页查询、未读统计、标记已读、批量操作等）
 */
public interface SysNoticeService extends IService<SysNotice> {

    /**
     * 核心方法：发送通知（站内信+系统内邮箱，异步执行）
     * @param userId 接收用户ID（关联sys_user.id）
     * @param noticeType 通知类型：1-提交成功，2-查重完成，3-审核结果，4-截止提醒，5-系统通知，6-教师分配，7-论文修改请求，8-其他
     * @param title 通知标题（站内信标题+邮件主题）
     * @param content 通知内容（支持换行，邮箱会自动转为HTML格式）
     * @param sendEmail 是否同时发送系统内邮箱（true=发送，false=仅站内信）
     */
    void sendNotice(Long userId, Integer noticeType, String title, String content, boolean sendEmail);

    /**
     * 增强版：发送带优先级和相关业务信息的通知
     * @param userId 接收用户ID
     * @param noticeType 通知类型
     * @param priority 优先级：0-普通，1-重要，2-紧急
     * @param title 通知标题
     * @param content 通知内容
     * @param relatedId 相关业务ID
     * @param relatedType 相关业务类型
     * @param sendEmail 是否发送邮箱
     */
    void sendNoticeWithPriority(Long userId, Integer noticeType, Integer priority, String title, String content, Long relatedId, String relatedType, boolean sendEmail);

    /**
     * 批量发送通知
     * @param userIds 用户ID列表
     * @param noticeType 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param sendEmail 是否发送邮箱
     */
    void batchSendNotice(List<Long> userIds, Integer noticeType, String title, String content, boolean sendEmail);

    /**
     * 分页查询用户通知列表（增强版：支持已读/未读筛选、优先级筛选，按时间倒序）
     * @param userId 当前登录用户ID
     * @param isRead 是否已读（0=未读，1=已读，null=查询所有）
     * @param priority 优先级（0=普通，1=重要，2=紧急，null=查询所有）
     * @param noticeType 通知类型（null=查询所有）
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数（默认10）
     * @return 分页结果VO（含总条数、当前页数据）
     */
    PageResultVO<SysNotice> getUserNoticePage(Long userId, Integer isRead, Integer priority, Integer noticeType, Integer pageNum, Integer pageSize);

    /**
     * 统计用户未读通知数（原有逻辑：用于前端显示未读小红点）
     * @param userId 当前登录用户ID
     * @return 未读通知数量（>=0）
     */
    Integer getUnreadNoticeCount(Long userId);

    /**
     * 统计用户未读通知数（按优先级）
     * @param userId 当前登录用户ID
     * @return 按优先级统计的未读通知数量
     */
    Map<String, Integer> getUnreadNoticeCountByPriority(Long userId);

    /**
     * 标记单个通知为已读（原有逻辑：带权限校验，仅能标记自己的通知）
     * @param noticeId 通知ID（sys_notice.id）
     * @param userId 当前登录用户ID（防止越权操作）
     * @return true=标记成功，false=标记失败（通知不存在/已读/越权）
     */
    boolean markNoticeAsRead(Long noticeId, Long userId);

    /**
     * 批量标记通知为已读
     * @param noticeIds 通知ID列表
     * @param userId 当前登录用户ID
     * @return 成功标记的数量
     */
    Integer batchMarkAsRead(List<Long> noticeIds, Long userId);

    /**
     * 标记所有未读通知为已读（原有逻辑：批量操作，提升用户体验）
     * @param userId 当前登录用户ID
     * @return 成功标记的数量
     */
    Integer markAllUnreadAsRead(Long userId);

    /**
     * 删除单个通知
     * @param noticeId 通知ID
     * @param userId 当前登录用户ID
     * @return true=删除成功，false=删除失败（通知不存在/越权）
     */
    boolean deleteNotice(Long noticeId, Long userId);

    /**
     * 批量删除通知
     * @param noticeIds 通知ID列表
     * @param userId 当前登录用户ID
     * @return 成功删除的数量
     */
    Integer batchDeleteNotice(List<Long> noticeIds, Long userId);

    /**
     * 清空用户所有通知
     * @param userId 当前登录用户ID
     * @return 成功清空的数量
     */
    Integer clearAllNotice(Long userId);

    /**
     * 获取用户最新的通知
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 最新通知列表
     */
    List<SysNotice> getLatestNotice(Long userId, int limit);

    /**
     * 获取通知统计信息
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getNoticeStats(Long userId);
}