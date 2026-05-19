package com.abin.checkrepeatsystem.common.VO;

import com.abin.checkrepeatsystem.common.enums.FileType;
import lombok.Data;

/**
 * 文件预览信息DTO
 */
@Data
public class FilePreviewInfoDTO {

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 是否支持原生预览
     */
    private boolean nativeSupported;

    /**
     * 文件大小
     */
    private Long size;

    /**
     * 原生预览URL（直接下载URL）
     */
    private String nativePreviewUrl;

    /**
     * KKFileView预览URL（需要转换的文件）
     */
    private String kkfileviewPreviewUrl;

    /**
     * 预览临时token（用于访问/api/file/preview/{token}/{fileName}）
     */
    private String previewToken;

    /**
     * 错误信息
     */
    private String errorMessage;

    public static FilePreviewInfoDTO success(Long fileId, String fileName, String fileType, 
                                            boolean nativeSupported, Long size,
                                            String nativePreviewUrl, String kkfileviewPreviewUrl) {
        return success(fileId, fileName, fileType, nativeSupported, size, 
                nativePreviewUrl, kkfileviewPreviewUrl, null);
    }

    public static FilePreviewInfoDTO success(Long fileId, String fileName, String fileType, 
                                            boolean nativeSupported, Long size,
                                            String nativePreviewUrl, String kkfileviewPreviewUrl,
                                            String previewToken) {
        FilePreviewInfoDTO dto = new FilePreviewInfoDTO();
        dto.setFileId(fileId);
        dto.setFileName(fileName);
        dto.setFileType(fileType);
        dto.setNativeSupported(nativeSupported);
        dto.setSize(size);
        dto.setNativePreviewUrl(nativePreviewUrl);
        dto.setKkfileviewPreviewUrl(kkfileviewPreviewUrl);
        // 使用传入的token（必须由调用方传入）
        dto.setPreviewToken(previewToken);
        return dto;
    }

    public static FilePreviewInfoDTO failure(String errorMessage) {
        FilePreviewInfoDTO dto = new FilePreviewInfoDTO();
        dto.setErrorMessage(errorMessage);
        return dto;
    }
}