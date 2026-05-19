package com.abin.checkrepeatsystem.common.dto;

import lombok.Data;

/**
 * 预览响应DTO
 */
@Data
public class PreviewResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 预览URL
     */
    private String previewUrl;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 构造方法
     */
    public PreviewResponse() {
    }

    /**
     * 构造方法
     */
    public PreviewResponse(boolean success, String previewUrl, String message) {
        this.success = success;
        this.previewUrl = previewUrl;
        this.message = message;
    }

    /**
     * 成功响应
     */
    public static PreviewResponse success(String previewUrl) {
        return new PreviewResponse(true, previewUrl, null);
    }

    /**
     * 失败响应
     */
    public static PreviewResponse failure(String message) {
        return new PreviewResponse(false, null, message);
    }
}
