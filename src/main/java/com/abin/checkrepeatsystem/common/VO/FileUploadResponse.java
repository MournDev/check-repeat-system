package com.abin.checkrepeatsystem.common.VO;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文件上传响应对象
 */
@Data
public class FileUploadResponse {
    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件大小描述（如：1.2MB）
     */
    private String fileSizeDesc;

    /**
     * 文件MD5
     */
    private String md5;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 是否秒传
     */
    private Boolean fastUpload;
}
