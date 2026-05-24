package com.abin.checkrepeatsystem.user.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.pojo.entity.InstantMessage;
import com.abin.checkrepeatsystem.user.service.InstantMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
public class InstantMessageController {

    private final InstantMessageService instantMessageService;

    @PostMapping("/send")
    public Result<Boolean> sendMessage(@RequestBody InstantMessage message) {
        Long senderId = UserBusinessInfoUtils.getCurrentUserId();
        message.setSenderId(senderId);
        log.info("发送即时消息请求 - 发送者ID: {}, 接收者ID: {}, 消息类型: {}",
                senderId, message.getReceiverId(), message.getMessageType());
        return instantMessageService.sendMessage(message);
    }

    @GetMapping("/conversations")
    public Result<List<InstantMessage>> getConversations() {
        Long userId = UserBusinessInfoUtils.getCurrentUserId();
        log.debug("获取会话列表请求 - 用户ID: {}", userId);
        return instantMessageService.getConversations(userId);
    }

    @GetMapping("/history/{conversationId}")
    public Result<?> getMessageHistory(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.debug("获取会话消息历史请求 - 会话ID: {}, 页码: {}, 每页大小: {}",
                conversationId, pageNum, pageSize);
        return instantMessageService.getMessageHistory(conversationId, pageNum, pageSize);
    }

    @GetMapping("/private/{otherUserId}")
    public Result<?> getPrivateMessages(
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long currentUserId = UserBusinessInfoUtils.getCurrentUserId();
        log.debug("获取私信历史请求 - 当前用户: {}, 对方用户: {}, 页码: {}, 每页大小: {}",
                currentUserId, otherUserId, pageNum, pageSize);
        return instantMessageService.getPrivateMessages(currentUserId, otherUserId, pageNum, pageSize);
    }

    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount() {
        Long userId = UserBusinessInfoUtils.getCurrentUserId();
        log.debug("获取未读消息数量请求 - 用户ID: {}", userId);
        return instantMessageService.getUnreadCount(userId);
    }

    @PutMapping("/read")
    public Result<Boolean> markAsRead(@RequestBody List<Long> messageIds) {
        Long userId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("标记消息已读请求 - 用户ID: {}, 消息数量: {}", userId, messageIds.size());
        return instantMessageService.markAsRead(messageIds, userId);
    }

    @DeleteMapping("/{messageId}")
    public Result<Boolean> deleteMessage(@PathVariable Long messageId) {
        Long userId = UserBusinessInfoUtils.getCurrentUserId();
        log.info("删除消息请求 - 消息ID: {}, 用户ID: {}", messageId, userId);
        return instantMessageService.deleteMessage(messageId, userId);
    }
}