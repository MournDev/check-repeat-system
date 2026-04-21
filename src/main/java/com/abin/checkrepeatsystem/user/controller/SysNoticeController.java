package com.abin.checkrepeatsystem.user.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.annotation.OperationLog;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.user.service.SysNoticeService;
import com.abin.checkrepeatsystem.user.vo.PageResultVO;
import com.abin.checkrepeatsystem.pojo.entity.SysNotice;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notices")
public class SysNoticeController {

    @Resource
    private SysNoticeService sysNoticeService;

    /**
     * 获取用户通知列表（分页，增强版）
     */
    @GetMapping("/list")
    public Result<PageResultVO<SysNotice>> getNoticeList(
            @RequestParam(required = false) Integer isRead,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) Integer noticeType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            PageResultVO<SysNotice> result = sysNoticeService.getUserNoticePage(userId, isRead, priority, noticeType, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取通知列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取通知列表失败");
        }
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            Integer count = sysNoticeService.getUnreadNoticeCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取未读通知数量失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取未读通知数量失败");
        }
    }

    /**
     * 按优先级获取未读通知数量
     */
    @GetMapping("/unread-count-by-priority")
    public Result<Map<String, Integer>> getUnreadCountByPriority() {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            Map<String, Integer> countMap = sysNoticeService.getUnreadNoticeCountByPriority(userId);
            return Result.success(countMap);
        } catch (Exception e) {
            log.error("按优先级获取未读通知数量失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取未读通知数量失败");
        }
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/read/{noticeId}")
    @OperationLog(type = "notice_read", description = "标记通知为已读", recordResult = true)
    public Result<String> markAsRead(@PathVariable Long noticeId) {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            boolean success = sysNoticeService.markNoticeAsRead(noticeId, userId);
            if (success) {
                return Result.success("标记已读成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "标记已读失败");
            }
        } catch (Exception e) {
            log.error("标记通知已读失败 - 通知ID: {}", noticeId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "标记已读失败: " + e.getMessage());
        }
    }

    /**
     * 批量标记通知为已读
     */
    @PutMapping("/batch-read")
    @OperationLog(type = "notice_batch_read", description = "批量标记通知为已读", recordResult = true)
    public Result<String> batchMarkAsRead(@RequestBody List<Long> noticeIds) {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            Integer count = sysNoticeService.batchMarkAsRead(noticeIds, userId);
            return Result.success("成功标记 " + count + " 条通知为已读");
        } catch (Exception e) {
            log.error("批量标记通知已读失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量标记已读失败: " + e.getMessage());
        }
    }

    /**
     * 批量标记所有未读通知为已读
     */
    @PutMapping("/read-all")
    @OperationLog(type = "notice_read_all", description = "批量标记所有未读通知为已读", recordResult = true)
    public Result<String> markAllAsRead() {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            Integer count = sysNoticeService.markAllUnreadAsRead(userId);
            return Result.success("成功标记 " + count + " 条通知为已读");
        } catch (Exception e) {
            log.error("批量标记未读通知失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量标记已读失败: " + e.getMessage());
        }
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/delete/{noticeId}")
    @OperationLog(type = "notice_delete", description = "删除通知", recordResult = true)
    public Result<String> deleteNotice(@PathVariable Long noticeId) {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            boolean success = sysNoticeService.deleteNotice(noticeId, userId);
            if (success) {
                return Result.success("删除通知成功");
            } else {
                return Result.error(ResultCode.SYSTEM_ERROR, "删除通知失败");
            }
        } catch (Exception e) {
            log.error("删除通知失败 - 通知ID: {}", noticeId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "删除通知失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除通知
     */
    @DeleteMapping("/batch-delete")
    @OperationLog(type = "notice_batch_delete", description = "批量删除通知", recordResult = true)
    public Result<String> batchDeleteNotice(@RequestBody List<Long> noticeIds) {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            Integer count = sysNoticeService.batchDeleteNotice(noticeIds, userId);
            return Result.success("成功删除 " + count + " 条通知");
        } catch (Exception e) {
            log.error("批量删除通知失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "批量删除通知失败: " + e.getMessage());
        }
    }

    /**
     * 清空所有通知
     */
    @DeleteMapping("/clear-all")
    @OperationLog(type = "notice_clear_all", description = "清空所有通知", recordResult = true)
    public Result<String> clearAllNotice() {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            Integer count = sysNoticeService.clearAllNotice(userId);
            return Result.success("成功清空 " + count + " 条通知");
        } catch (Exception e) {
            log.error("清空所有通知失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "清空通知失败: " + e.getMessage());
        }
    }

    /**
     * 获取最新通知
     */
    @GetMapping("/latest")
    public Result<List<SysNotice>> getLatestNotice(@RequestParam(defaultValue = "5") int limit) {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            List<SysNotice> notices = sysNoticeService.getLatestNotice(userId, limit);
            return Result.success(notices);
        } catch (Exception e) {
            log.error("获取最新通知失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取最新通知失败");
        }
    }

    /**
     * 获取通知统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getNoticeStats() {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            Map<String, Object> stats = sysNoticeService.getNoticeStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取通知统计信息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取通知统计信息失败");
        }
    }

    /**
     * 发送测试通知
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> sendTestNotice() {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            sysNoticeService.sendNotice(
                    userId,
                    1,
                    "测试通知",
                    "这是一条测试通知，用于验证通知功能是否正常工作。",
                    true
            );
            return Result.success("测试通知发送成功");
        } catch (Exception e) {
            log.error("发送测试通知失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "发送测试通知失败: " + e.getMessage());
        }
    }

    /**
     * 发送带优先级的测试通知
     */
    @PostMapping("/test-priority")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> sendTestNoticeWithPriority() {
        try {
            Long userId = com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils.getCurrentUserId();
            sysNoticeService.sendNoticeWithPriority(
                    userId,
                    5,
                    2,
                    "紧急测试通知",
                    "这是一条紧急测试通知，用于验证优先级功能是否正常工作。",
                    null,
                    null,
                    true
            );
            return Result.success("紧急测试通知发送成功");
        } catch (Exception e) {
            log.error("发送紧急测试通知失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "发送紧急测试通知失败: " + e.getMessage());
        }
    }
}
