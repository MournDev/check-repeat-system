package com.abin.checkrepeatsystem.common.service.Impl;

import com.abin.checkrepeatsystem.common.service.PreviewTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PreviewTokenServiceImpl implements PreviewTokenService {

    private static final String PREVIEW_TOKEN_PREFIX = "preview:token:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public String generatePreviewToken(Long fileId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = PREVIEW_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, fileId.toString(), 5, TimeUnit.MINUTES);
        log.info("生成预览令牌 - token: {}, fileId: {}", token.substring(0, 8) + "...", fileId);
        return token;
    }

    @Override
    public Long validatePreviewToken(String token) {
        String key = PREVIEW_TOKEN_PREFIX + token;
        String fileIdStr = redisTemplate.opsForValue().get(key);
        Long fileId = fileIdStr != null ? Long.parseLong(fileIdStr) : null;
        log.info("验证预览令牌 - token: {}, fileId: {}",
                token != null ? token.substring(0, 8) + "..." : "null",
                fileId);
        return fileId;
    }
}
