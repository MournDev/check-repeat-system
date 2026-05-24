package com.abin.checkrepeatsystem.ai.service.impl;

import com.abin.checkrepeatsystem.ai.config.AIConfigProperties;
import com.abin.checkrepeatsystem.ai.service.AIService;
import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Service
public class

DeepSeekAIServiceImpl implements AIService {

    private final AIConfigProperties config;

    private volatile OkHttpClient client;
    private volatile String lastBaseUrl;

    private OkHttpClient getClient() {
        String baseUrl = config.getBaseUrl();
        if (client == null || !baseUrl.equals(lastBaseUrl)) {
            synchronized (this) {
                if (client == null || !baseUrl.equals(lastBaseUrl)) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(Duration.ofSeconds(10))
                            .readTimeout(Duration.ofSeconds(30))
                            .writeTimeout(Duration.ofSeconds(10))
                            .build();
                    lastBaseUrl = baseUrl;
                }
            }
        }
        return client;
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        if (!config.isEnabled()) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 服务未启用");
        }

        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI API Key 未配置");
        }

        JSONObject body = new JSONObject();
        body.put("model", config.getModel());
        body.put("temperature", config.getTemperature());
        body.put("max_tokens", config.getMaxTokens());

        JSONArray messages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        body.put("messages", messages);

        String url = config.getBaseUrl() + "/v1/chat/completions";
        log.info("调用 DeepSeek API: model={}, url={}", config.getModel(), url);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")))
                .build();

        try (Response response = getClient().newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("DeepSeek API 返回错误: status={}, body={}", response.code(), responseBody);
                throw new BusinessException(ResultCode.SYSTEM_ERROR,
                        "AI 服务调用失败: HTTP " + response.code());
            }

            JSONObject respJson = JSON.parseObject(responseBody);
            JSONArray choices = respJson.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("DeepSeek API 返回空 choices: {}", responseBody);
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 返回结果为空");
            }

            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String content = message.getString("content");
            if (content == null || content.isBlank()) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 返回内容为空");
            }

            String trimmed = content.trim();
            if (trimmed.startsWith("```json")) {
                trimmed = trimmed.substring(7);
            }
            if (trimmed.startsWith("```")) {
                trimmed = trimmed.substring(3);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();

            log.info("DeepSeek API 调用成功，返回内容长度: {}", trimmed.length());
            return trimmed;

        } catch (IOException e) {
            log.error("DeepSeek API 网络异常: {}", e.getMessage());
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "AI 服务网络异常: " + e.getMessage());
        }
    }
}
