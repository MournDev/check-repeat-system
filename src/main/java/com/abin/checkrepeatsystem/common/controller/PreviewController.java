package com.abin.checkrepeatsystem.common.controller;

import com.abin.checkrepeatsystem.common.VO.FilePreviewInfoDTO;
import com.abin.checkrepeatsystem.common.dto.PreviewResponse;
import com.abin.checkrepeatsystem.common.service.PreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预览控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/preview")
@Tag(name = "预览服务", description = "文件在线预览功能")
public class PreviewController {

    @Autowired
    private PreviewService previewService;

    /**
     * 获取文件预览URL（兼容旧接口）
     */
    @GetMapping("/file/{paperId}")
    @Operation(summary = "获取文件预览URL", description = "生成文件的在线预览URL（兼容旧接口）")
    public ResponseEntity<PreviewResponse> getPreviewUrl(
            @Parameter(description = "论文ID") @PathVariable Long paperId) {
        try {
            log.info("请求获取文件预览URL - paperId: {}", paperId);
            PreviewResponse response = previewService.getPreviewUrl(paperId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取预览URL失败 - paperId: {}", paperId, e);
            return ResponseEntity.ok(PreviewResponse.failure("获取预览URL失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文件预览信息（新接口，支持原生预览）
     */
    @GetMapping("/info/{paperId}")
    @Operation(summary = "获取文件预览信息", description = "获取文件预览信息，包括文件类型、是否支持原生预览等")
    public ResponseEntity<FilePreviewInfoDTO> getPreviewInfo(
            @Parameter(description = "论文ID") @PathVariable Long paperId) {
        try {
            log.info("请求获取文件预览信息 - paperId: {}", paperId);
            FilePreviewInfoDTO info = previewService.getPreviewInfo(paperId);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("获取预览信息失败 - paperId: {}", paperId, e);
            return ResponseEntity.ok(FilePreviewInfoDTO.failure("获取预览信息失败: " + e.getMessage()));
        }
    }

    /**
     * 获取KKFileView预览URL
     */
    @GetMapping("/kkfileview/{paperId}")
    @Operation(summary = "获取KKFileView预览URL", description = "获取需要转换的文件的KKFileView预览URL")
    public ResponseEntity<PreviewResponse> getKkfileviewUrl(
            @Parameter(description = "论文ID") @PathVariable Long paperId) {
        try {
            log.info("请求获取KKFileView预览URL - paperId: {}", paperId);
            PreviewResponse response = previewService.getKkfileviewPreviewUrl(paperId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取KKFileView预览URL失败 - paperId: {}", paperId, e);
            return ResponseEntity.ok(PreviewResponse.failure("获取预览URL失败: " + e.getMessage()));
        }
    }

    /**
     * 检查预览服务状态
     */
    @GetMapping("/status")
    @Operation(summary = "检查预览服务状态", description = "检查KKFileView服务是否可用")
    public ResponseEntity<PreviewResponse> checkStatus() {
        try {
            boolean status = previewService.checkServiceStatus();
            if (status) {
                return ResponseEntity.ok(PreviewResponse.success("预览服务正常"));
            } else {
                return ResponseEntity.ok(PreviewResponse.failure("预览服务暂时不可用"));
            }
        } catch (Exception e) {
            log.error("检查服务状态失败", e);
            return ResponseEntity.ok(PreviewResponse.failure("检查服务状态失败: " + e.getMessage()));
        }
    }
}