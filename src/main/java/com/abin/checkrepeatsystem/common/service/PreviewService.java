package com.abin.checkrepeatsystem.common.service;

import com.abin.checkrepeatsystem.common.VO.FilePreviewInfoDTO;
import com.abin.checkrepeatsystem.common.dto.PreviewResponse;

/**
 * 预览服务接口
 */
public interface PreviewService {

    /**
     * 获取文件预览URL（兼容旧接口）
     * @param paperId 论文ID
     * @return 预览响应
     */
    PreviewResponse getPreviewUrl(Long paperId);

    /**
     * 获取文件预览信息（新接口，支持原生预览）
     * @param paperId 论文ID
     * @return 文件预览信息
     */
    FilePreviewInfoDTO getPreviewInfo(Long paperId);

    /**
     * 获取KKFileView预览URL
     * @param paperId 论文ID
     * @return 预览响应
     */
    PreviewResponse getKkfileviewPreviewUrl(Long paperId);

    /**
     * 检查KKFileView服务状态
     * @return 是否可用
     */
    boolean checkServiceStatus();
}
