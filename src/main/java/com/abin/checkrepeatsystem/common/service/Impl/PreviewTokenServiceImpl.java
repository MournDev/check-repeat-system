package com.abin.checkrepeatsystem.common.service.Impl;

import com.abin.checkrepeatsystem.common.service.PreviewTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
@Service
public class PreviewTokenServiceImpl implements PreviewTokenService {

    private static final String PREVIEW_TOKEN_PREFIX = "preview:token:";
    private static final String TOKEN_VALUE_DELIMITER = "|";

    private final StringRedisTemplate redisTemplate;

    @Override
    public String generatePreviewToken(Long fileId) {
        return generatePreviewToken(fileId, null);
    }

    @Override
    public String generatePreviewToken(Long fileId, Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = PREVIEW_TOKEN_PREFIX + token;
        // 存储格式: fileId 或 fileId|userId
        String value = userId != null ? fileId + TOKEN_VALUE_DELIMITER + userId : fileId.toString();
        redisTemplate.opsForValue().set(key, value, 5, TimeUnit.MINUTES);
        log.info("生成预览令牌 - fileId: {}, boundUser: {}", fileId, userId != null ? userId : "none");
        return token;
    }

    @Override
    public Long validatePreviewToken(String token) {
        return validatePreviewToken(token, null);
    }

    @Override
    public Long validatePreviewToken(String token, Long currentUserId) {
        if (token == null) return null;
        String key = PREVIEW_TOKEN_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            log.debug("预览令牌不存在或已过期");
            return null;
        }
        String[] parts = value.split("\\" + TOKEN_VALUE_DELIMITER.replace("|", "\\|"));
        Long fileId = Long.parseLong(parts[0]);
        // 如果令牌绑定了用户，校验当前用户是否匹配
        if (parts.length > 1 && currentUserId != null) {
            Long boundUserId = Long.parseLong(parts[1]);
            if (!boundUserId.equals(currentUserId)) {
                log.warn("预览令牌用户不匹配 - 令牌绑定用户: {}, 当前用户: {}", boundUserId, currentUserId);
                return null;
            }
        }
        log.debug("预览令牌验证通过 - fileId: {}", fileId);
        return fileId;
    }
}
