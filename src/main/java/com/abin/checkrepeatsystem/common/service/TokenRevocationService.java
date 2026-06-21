package com.abin.checkrepeatsystem.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Token 吊销服务：密码修改后使所有旧 token 失效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String REVOCATION_KEY_PREFIX = "token_revoked:";
    private static final long REVOCATION_TTL_HOURS = 24;

    /**
     * 吊销指定用户的所有 token（密码修改时调用）
     * 记录当前时间戳到 Redis，JWT 过滤器会拒绝签发时间早于此时间戳的 token
     */
    public void revokeAllTokensForUser(Long userId) {
        String key = REVOCATION_KEY_PREFIX + userId;
        long now = System.currentTimeMillis();
        redisTemplate.opsForValue().set(key, String.valueOf(now), REVOCATION_TTL_HOURS, TimeUnit.HOURS);
        log.info("已吊销用户所有 token: userId={}, timestamp={}", userId, now);
    }

    /**
     * 检查指定签发时间的 token 是否已被吊销
     * @return true 表示 token 已被吊销（应拒绝访问）
     */
    public boolean isTokenRevoked(Long userId, Date issuedAt) {
        if (userId == null || issuedAt == null) {
            return false;
        }
        String key = REVOCATION_KEY_PREFIX + userId;
        String revokedAtStr = redisTemplate.opsForValue().get(key);
        if (revokedAtStr == null) {
            return false;
        }
        try {
            long revokedAt = Long.parseLong(revokedAtStr);
            return issuedAt.getTime() < revokedAt;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
