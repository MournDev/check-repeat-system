package com.abin.checkrepeatsystem.detection.service;

import com.abin.checkrepeatsystem.common.websocket.WebSocketSender;
import com.abin.checkrepeatsystem.common.websocket.handler.CheckProgressWebSocketHandler;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查重进度 WebSocket 推送服务
 * 负责将查重进度实时推送给前端
 */
@Service
@Slf4j
public class CheckProgressWebSocketService {

    @Resource
    private WebSocketSender webSocketSender;

    private final Map<Long, CheckProgress> progressCache = new ConcurrentHashMap<>();

    public static final String TOPIC_CHECK_PROGRESS = "/topic/check-progress/";
    public static final String TOPIC_CHECK_COMPLETE = "/topic/check-complete/";
    public static final String TOPIC_CHECK_ERROR = "/topic/check-error/";

    /**
     * 发送查重开始消息
     */
    public void sendCheckStart(Long paperId, Long userId, String taskNo, int totalPapers) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "progress");
        message.put("paperId", paperId);
        message.put("userId", userId);
        message.put("taskNo", taskNo);
        message.put("totalPapers", totalPapers);
        message.put("progress", 0);
        message.put("message", "查重任务已启动");

        // 缓存进度信息
        progressCache.put(paperId, new CheckProgress(paperId, userId, taskNo, totalPapers, 0));

        // 发送到用户队列
        sendToUser(userId, TOPIC_CHECK_PROGRESS + paperId, message);
        
        // 通过原生 WebSocket 发送消息
        sendNativeWebSocketMessage(taskNo, message);
        
        log.info("发送查重开始消息: paperId={}, userId={}, taskNo={}", paperId, userId, taskNo);
    }

    /**
     * 发送查重进度更新
     */
    public void sendCheckProgress(Long paperId, Long userId, int completed, int total) {
        int progress = (int) ((completed * 100.0) / total);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "progress");
        message.put("paperId", paperId);
        message.put("userId", userId);
        message.put("completed", completed);
        message.put("total", total);
        message.put("progress", progress);
        message.put("message", String.format("正在比对: %d/%d", completed, total));

        // 更新缓存
        CheckProgress progressInfo = progressCache.get(paperId);
        if (progressInfo != null) {
            progressInfo.completed = completed;
            progressInfo.progress = progress;
            // 通过原生 WebSocket 发送消息
            sendNativeWebSocketMessage(progressInfo.taskNo, message);
        }

        // 发送到用户队列
        sendToUser(userId, TOPIC_CHECK_PROGRESS + paperId, message);

        if (completed % 10 == 0) {
            log.debug("发送查重进度: paperId={}, {}/{} ({}%)", paperId, completed, total, progress);
        }
    }

    /**
     * 发送查重完成消息
     */
    public void sendCheckComplete(Long paperId, Long userId, double similarity, String riskLevel) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "complete");
        message.put("paperId", paperId);
        message.put("userId", userId);
        message.put("similarity", similarity);
        message.put("riskLevel", riskLevel);
        message.put("progress", 100);
        message.put("message", "查重完成");

        // 从缓存中移除
        CheckProgress progressInfo = progressCache.remove(paperId);
        
        // 通过原生 WebSocket 发送消息
        if (progressInfo != null) {
            sendNativeWebSocketMessage(progressInfo.taskNo, message);
        }

        // 发送到完成主题
        sendToUser(userId, TOPIC_CHECK_COMPLETE + paperId, message);
        log.info("发送查重完成消息: paperId={}, similarity={}%, riskLevel={}", paperId, similarity, riskLevel);
    }

    /**
     * 发送查重失败消息
     */
    public void sendCheckError(Long paperId, Long userId, String errorMessage) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "error");
        message.put("paperId", paperId);
        message.put("userId", userId);
        message.put("progress", 0);
        message.put("message", "查重失败: " + errorMessage);

        // 从缓存中移除
        CheckProgress progressInfo = progressCache.remove(paperId);
        
        // 通过原生 WebSocket 发送消息
        if (progressInfo != null) {
            sendNativeWebSocketMessage(progressInfo.taskNo, message);
        }

        // 发送到错误主题
        sendToUser(userId, TOPIC_CHECK_ERROR + paperId, message);
        log.error("发送查重失败消息: paperId={}, error={}", paperId, errorMessage);
    }
    
    /**
     * 通过原生 WebSocket 发送消息
     */
    private void sendNativeWebSocketMessage(String taskId, Map<String, Object> message) {
        try {
            String messageJson = JSON.toJSONString(message);
            CheckProgressWebSocketHandler.sendProgressMessage(taskId, messageJson);
            log.debug("通过原生 WebSocket 发送查重进度消息成功 - 任务ID: {}", taskId);
        } catch (Exception e) {
            log.error("通过原生 WebSocket 发送查重进度消息失败 - 任务ID: {}", taskId, e);
        }
    }

    /**
     * 获取当前查重进度
     */
    public CheckProgress getProgress(Long paperId) {
        return progressCache.get(paperId);
    }

    /**
     * 取消查重任务
     */
    public void cancelCheck(Long paperId, Long userId) {
        progressCache.remove(paperId);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "CANCEL");
        message.put("paperId", paperId);
        message.put("userId", userId);
        message.put("message", "查重任务已取消");

        sendToUser(userId, TOPIC_CHECK_PROGRESS + paperId, message);
        log.info("查重任务已取消: paperId={}, userId={}", paperId, userId);
    }

    /**
     * 发送消息到指定用户
     */
    private void sendToUser(Long userId, String destination, Map<String, Object> message) {
        try {
            webSocketSender.sendToUser(userId, message);
        } catch (Exception e) {
            log.warn("WebSocket消息发送失败: error={}", e.getMessage());
        }
    }

    /**
     * 查重进度信息
     */
    public static class CheckProgress {
        public Long paperId;
        public Long userId;
        public String taskNo;
        public int total;
        public int completed;
        public int progress;
        public long startTime;

        public CheckProgress(Long paperId, Long userId, String taskNo, int total, int completed) {
            this.paperId = paperId;
            this.userId = userId;
            this.taskNo = taskNo;
            this.total = total;
            this.completed = completed;
            this.progress = total > 0 ? (int) ((completed * 100.0) / total) : 0;
            this.startTime = System.currentTimeMillis();
        }

        public long getElapsedSeconds() {
            return (System.currentTimeMillis() - startTime) / 1000;
        }

        public long getEstimatedRemainingSeconds() {
            if (completed == 0) {
                return -1;
            }
            long elapsed = getElapsedSeconds();
            return (long) (elapsed * (total - completed) / completed);
        }
    }
}