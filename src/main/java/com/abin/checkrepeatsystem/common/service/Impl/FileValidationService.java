package com.abin.checkrepeatsystem.common.service.Impl;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Slf4j
@Service
public class FileValidationService {

    private static final long MAX_FILE_SIZE = 200 * 1024 * 1024; // 200MB
    private static final String[] ALLOWED_EXTENSIONS = {"doc", "docx", "pdf"};

    // MIME types for allowed document formats
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final Tika tika = new Tika();

    public Result<Void> validateFile(MultipartFile file) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            return Result.error(ResultCode.PARAM_ERROR, "论文文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error(ResultCode.PARAM_ERROR, "论文文件大小不能超过200MB");
        }

        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename).toLowerCase();

        boolean isValidExtension = false;
        for (String allowedExtension : ALLOWED_EXTENSIONS) {
            if (allowedExtension.equals(fileType)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            return Result.error(ResultCode.PARAM_ERROR, "仅支持doc、docx、pdf格式的论文文件");
        }

        // 检查文件MIME类型（防止扩展名伪造）
        try (InputStream is = file.getInputStream()) {
            String detectedType = tika.detect(is, originalFilename);
            if (!ALLOWED_MIME_TYPES.contains(detectedType)) {
                log.warn("文件MIME类型不匹配: 文件名={}, 声称扩展名={}, 检测到MIME={}",
                    originalFilename, fileType, detectedType);
                return Result.error(ResultCode.PARAM_ERROR,
                    "文件内容与扩展名不匹配，请上传有效的doc/docx/pdf文件");
            }
        } catch (IOException e) {
            log.error("文件MIME类型检测失败: {}", originalFilename, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "文件校验失败，请重试");
        }

        return Result.success(null);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}

