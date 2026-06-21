package com.abin.checkrepeatsystem.user.vo;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 令牌刷新响应 VO
 */
@Data
public class RefreshTokenVO {
    /** 新JWT令牌 */
    private String newToken;

    /** 新令牌过期时间（毫秒时间戳） */
    private Long expireTime;

    /**
     * 兼容前端读取 res.data.data.token 的场景
     */
    @JsonProperty("token")
    public String getToken() {
        return newToken;
    }
}
