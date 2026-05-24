package com.abin.checkrepeatsystem.common.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface FilePreviewService {

    /**
     * 直接文件预览
     */
    ResponseEntity<Resource> directPreview(Long fileId);

    /**
     * KKFileView代理预览（通过URL）
     */
    ResponseEntity<byte[]> onlinePreviewByUrl(String url);

    /**
     * 智能预览（自动选择预览方式）
     * @param fileId 文件ID
     * @param token 预览令牌（可选）
     */
    ResponseEntity<?> smartPreview(Long fileId, String token);

    /**
     * 智能预览报告
     * @param paperId 论文ID
     * @param token 预览令牌（可选）
     */
    ResponseEntity<?> smartPreviewReport(String paperId, String token);
}
