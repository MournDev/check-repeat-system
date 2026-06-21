package com.abin.checkrepeatsystem.common.service;

public interface PreviewTokenService {

    String generatePreviewToken(Long fileId);

    /**
     * 生成预览令牌（绑定用户，用于需要权限校验的场景）
     */
    String generatePreviewToken(Long fileId, Long userId);

    Long validatePreviewToken(String token);

    /**
     * 验证预览令牌并校验绑定用户
     * @return fileId，用户不匹配时返回null
     */
    Long validatePreviewToken(String token, Long currentUserId);
}
