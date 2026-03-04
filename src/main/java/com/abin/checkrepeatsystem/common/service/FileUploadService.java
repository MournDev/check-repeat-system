package com.abin.checkrepeatsystem.common.service;

import com.abin.checkrepeatsystem.pojo.base.FileBaseParam;
import com.abin.checkrepeatsystem.pojo.base.FileBusinessBindParam;
import com.abin.checkrepeatsystem.pojo.base.FileUploadResp;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileUploadService {
    // 核心上传方法（参数分离，支持所有场景）
    FileUploadResp uploadFile(FileBaseParam baseParam, FileBusinessBindParam businessParam, Long loginUserId) throws IOException;
    // 检查文件是否已存在（通过MD5）
    Long checkFileExists(String fileMd5);
}