package com.abin.checkrepeatsystem.common.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.VO.FileUploadResponse;
import com.abin.checkrepeatsystem.common.annotation.Idempotent;
import com.abin.checkrepeatsystem.common.annotation.RateLimit;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.service.FilePreviewService;
import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.common.service.Impl.FileValidationService;
import com.abin.checkrepeatsystem.common.service.PreviewTokenService;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.FileInfo;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.student.service.CheckReportService;
import com.abin.checkrepeatsystem.student.service.CheckTaskService;
import com.abin.checkrepeatsystem.student.service.PaperInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 统一文件上传控制器
 * 负责所有文件的通用上传，不包含具体业务逻辑
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@PreAuthorize("isAuthenticated()")
public class FileUploadController {

    @Autowired
    private PreviewTokenService previewTokenService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FileValidationService fileValidationService;

    @Autowired
    private PaperInfoService paperInfoService;

    @Autowired
    private CheckReportService checkReportService;

    @Autowired
    private CheckTaskService checkTaskService;

    @Value("${file.upload.base-path}")
    private String uploadBasePath;
    
    /**
     * 初始化上传路径，确保目录存在
     */
    @PostConstruct
    private void init() {
        try {
            // 如果是 Windows 环境且路径为 Unix 风格，转换为 Windows 路径
            if (System.getProperty("os.name").toLowerCase().contains("win") && 
                (uploadBasePath.startsWith("/") || uploadBasePath.startsWith("data"))) {
                // 使用用户主目录下的临时上传目录
                String userHome = System.getProperty("user.home");
                uploadBasePath = Paths.get(userHome, "check-repeat-system", "upload").toString();
                log.info("检测到 Windows 环境，使用上传路径：{}", uploadBasePath);
            }
            
            // 创建上传根目录
            File uploadDir = new File(uploadBasePath);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (created) {
                    log.info("创建上传目录成功：{}", uploadBasePath);
                } else {
                    log.warn("创建上传目录失败：{}", uploadBasePath);
                }
            } else {
                log.info("上传目录已存在：{}", uploadBasePath);
            }
            
            // 验证目录是否可写
            if (!uploadDir.canWrite()) {
                log.error("上传目录不可写：{}", uploadBasePath);
                throw new RuntimeException("上传目录不可写：" + uploadBasePath);
            }
            
        } catch (Exception e) {
            log.error("初始化上传路径失败", e);
            throw new RuntimeException("初始化上传路径失败：" + e.getMessage());
        }
    }

    @Autowired
    private FilePreviewService filePreviewService;

    /**
     * 通用文件上传接口
     * 只负责文件上传，返回文件ID，业务参数通过其他接口传递
     */
    @PostMapping("/upload")
    @RateLimit(maxRequests = 20, windowSeconds = 60, message = "上传过于频繁，请60秒后重试")
    @Idempotent(message = "请勿重复提交相同文件")
    public Result<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long userId
            ) {

        try {
            log.info("文件上传请求 - 文件名：{},",
                    file.getOriginalFilename());

            // 1. 文件基础校验
            Result<Void> validationResult = fileValidationService.validateFile(file);
            if (!validationResult.isSuccess()) {
                return Result.error(ResultCode.PARAM_ERROR, validationResult.getMessage());
            }

            // 2. 获取文件字节数组并计算文件 MD5（用于秒传和校验）
            byte[] fileBytes = file.getBytes();
            String fileMd5 = fileService.calculateFileMd5FromBytes(fileBytes);


            // 3. 检查是否已存在相同文件（秒传功能）
            FileInfo existingFile = fileService.getByMd5(fileMd5);
            if (existingFile != null) {
                log.info("文件已存在，使用秒传 - 文件 ID: {}", existingFile.getId());
                FileUploadResponse response = buildFileUploadResponse(existingFile, true);
                return Result.success("文件上传成功（秒传）", response);
            }

            // 4. 执行文件上传
            Long fileId = fileService.uploadFile(file, userId);

            // 5. 获取文件信息
            FileInfo fileInfo = fileService.getById(fileId);
            FileUploadResponse response = buildFileUploadResponse(fileInfo, false);

            log.info("文件上传成功 - 文件 ID: {}, 文件名：{}", fileId, file.getOriginalFilename());
            return Result.success("文件上传成功", response);

        } catch (Exception e) {
            log.error("文件上传失败 - 文件名：{}", file.getOriginalFilename(), e);
            return Result.error(ResultCode.SYSTEM_ERROR, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 根据MD5查询文件（用于前端秒传检查）
     */
    @GetMapping("/check")
    public Result<FileUploadResponse> checkFileByMd5(@RequestParam String md5) {
        try {
            FileInfo fileInfo = fileService.getByMd5(md5);
            if (fileInfo != null) {
                FileUploadResponse response = buildFileUploadResponse(fileInfo, true);
                return Result.success("文件已存在", response);
            } else {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "文件不存在");
            }
        } catch (Exception e) {
            log.error("文件检查失败 - MD5: {}", md5, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "文件检查失败");
        }
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/info")
    public Result<FileInfo> getFileInfo(@RequestParam Long fileId) {
        try {
            FileInfo fileInfo = fileService.getById(fileId);
            if (fileInfo == null) {
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "文件不存在");
            }
            return Result.success(fileInfo);
        } catch (Exception e) {
            log.error("获取文件信息失败 - 文件ID: {}", fileId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取文件信息失败");
        }
    }

    /**
     * 文件下载接口
     */
    @GetMapping("/download/{fileId}/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            @PathVariable(required = false) String fileName,
            HttpServletRequest request) {

        try {
            // 1. 获取文件信息
            FileInfo fileInfo = fileService.getById(fileId);
            if (fileInfo == null || fileInfo.getStoragePath() == null) {
                log.error("文件不存在或未找到存储路径 - fileId: {}", fileId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // 2. 使用传入的文件名或数据库中的文件名
            String actualFileName = (fileName != null && !fileName.trim().isEmpty()) ?
                    fileName : fileInfo.getOriginalFilename();

            // 3. 根据存储类型获取文件内容
            String storagePath = fileInfo.getStoragePath();
            byte[] fileContent;
            
            if (storagePath.startsWith("files/")) {
                // MinIO存储：从MinIO读取
                log.info("从MinIO读取文件 - fileId: {}, 存储路径: {}", fileId, storagePath);
                fileContent = fileService.getFileContentFromMinio(storagePath);
            } else {
                // 本地存储：从本地磁盘读取
                log.info("原始存储路径: {}", storagePath);
                
                storagePath = storagePath.replace("/data/upload/", "");
                storagePath = storagePath.replace("\\data\\upload\\", "");
                while (storagePath.startsWith("\\") || storagePath.startsWith("/")) {
                    storagePath = storagePath.substring(1);
                }
                storagePath = storagePath.replace("\\\\", "\\");
                
                String fileStoragePath = uploadBasePath + File.separator + storagePath;
                fileStoragePath = fileStoragePath.replace("\\", File.separator);
                fileStoragePath = fileStoragePath.replace("/", File.separator);
                
                File file = new File(fileStoragePath);

                log.info("上传基础路径: {}", uploadBasePath);
                log.info("最终文件路径: {}", fileStoragePath);
                log.info("文件是否存在: {}", file.exists());
                if (file.exists()) {
                    log.info("文件大小: {} bytes", file.length());
                }

                if (!file.exists()) {
                    log.error("文件不存在 - fileId: {}, 路径: {}", fileId, fileStoragePath);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
                }
                
                if (file.length() == 0) {
                    log.error("文件为空 - fileId: {}, 路径: {}", fileId, fileStoragePath);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
                
                fileContent = Files.readAllBytes(file.toPath());
                log.info("文件读取成功 - 读取大小: {} bytes", fileContent.length);
            }

            // 4. 日志打印
            log.info("文件下载请求成功 - fileId: {}, 文件名: {}, 大小: {} bytes",
                    fileId, actualFileName, fileContent.length);

            // 5. 获取Content-Type
            String contentType = getContentTypeFromExtension(actualFileName);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 6. 设置响应头 - 支持中文文件名下载
            String encodedFileName = URLEncoder.encode(actualFileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            // 构建符合RFC 5987标准的Content-Disposition头
            // filename使用ISO-8859-1编码（兼容旧浏览器），filename*使用UTF-8编码
            String isoFileName = actualFileName;
            try {
                isoFileName = new String(actualFileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            } catch (Exception e) {
                // 保持原文件名
            }
            
            String contentDisposition = String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s",
                    isoFileName, encodedFileName);

            // 7. 构建Resource对象
            Resource resource = new org.springframework.core.io.ByteArrayResource(fileContent);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileContent.length))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(resource);

        } catch (Exception e) {
            log.error("文件下载异常 - fileId: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 预览专用文件访问接口（无需JWT认证，通过临时令牌验证）
     * 用于KKFileView服务访问文件进行预览
     */
    @GetMapping("/preview/{token}/{fileName}")
    public ResponseEntity<Resource> previewFile(
            @PathVariable String token,
            @PathVariable(required = false) String fileName,
            HttpServletRequest request) {

        try {
            // 1. 验证预览令牌
            Long fileId = previewTokenService.validatePreviewToken(token);
            if (fileId == null) {
                log.warn("预览令牌无效或已过期 - token: {}", token);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            
            log.info("预览令牌验证成功 - fileId: {}", fileId);

            // 2. 获取文件信息
            FileInfo fileInfo = fileService.getById(fileId);
            if (fileInfo == null || fileInfo.getStoragePath() == null) {
                log.error("文件不存在或未找到存储路径 - fileId: {}", fileId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // 3. 使用传入的文件名或数据库中的文件名
            String actualFileName = (fileName != null && !fileName.trim().isEmpty()) ?
                    fileName : fileInfo.getOriginalFilename();

            // 4. 根据存储类型获取文件内容
            String storagePath = fileInfo.getStoragePath();
            byte[] fileContent;
            
            if (storagePath.startsWith("files/")) {
                // MinIO存储：从MinIO读取
                log.info("从MinIO读取预览文件 - fileId: {}, 存储路径: {}", fileId, storagePath);
                fileContent = fileService.getFileContentFromMinio(storagePath);
            } else {
                // 本地存储：从本地磁盘读取
                storagePath = storagePath.replace("/data/upload/", "");
                storagePath = storagePath.replace("\\data\\upload\\", "");
                while (storagePath.startsWith("\\") || storagePath.startsWith("/")) {
                    storagePath = storagePath.substring(1);
                }
                storagePath = storagePath.replace("\\\\", "\\");
                String fileStoragePath = uploadBasePath + File.separator + storagePath;
                fileStoragePath = fileStoragePath.replace("\\", File.separator);
                fileStoragePath = fileStoragePath.replace("/", File.separator);
                File file = new File(fileStoragePath);

                log.info("从本地磁盘读取预览文件 - fileId: {}, 存储路径: {}", fileId, fileStoragePath);

                if (!file.exists()) {
                    log.error("预览文件不存在 - fileId: {}, 路径: {}", fileId, fileStoragePath);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
                }
                
                // 检查文件大小
                long fileSize = file.length();
                log.info("文件大小: {} bytes", fileSize);
                
                if (fileSize == 0) {
                    log.error("预览文件为空 - fileId: {}, 路径: {}", fileId, fileStoragePath);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
                
                fileContent = Files.readAllBytes(file.toPath());
                log.info("文件读取成功 - 读取大小: {} bytes", fileContent.length);
            }

            // 5. 日志打印
            log.info("预览文件请求成功 - fileId: {}, 文件名: {}, 大小: {} bytes",
                    fileId, actualFileName, fileContent.length);

            // 6. 获取Content-Type
            String contentType = getContentTypeFromExtension(actualFileName);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 7. 设置响应头 - 支持中文文件名下载
            String encodedFileName = URLEncoder.encode(actualFileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            // 构建符合RFC 5987标准的Content-Disposition头
            String contentDisposition = String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s",
                    encodedFileName, encodedFileName);

            // 8. 构建Resource对象
            Resource resource = new ByteArrayResource(fileContent);

            log.info("准备返回预览文件 - ContentType: {}, ContentLength: {}", contentType, fileContent.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileContent.length))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header("Content-Transfer-Encoding", "binary")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);

        } catch (Exception e) {
            log.error("预览文件异常 - token: {}", token, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    private String getContentTypeFromExtension(String fileName) {
        String extension = getFileExtension(fileName);
        Map<String, String> mimeTypes = new HashMap<String, String>() {{
            put("pdf", "application/pdf");
            put("doc", "application/msword");
            put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            put("xls", "application/vnd.ms-excel");
            put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            put("ppt", "application/vnd.ms-powerpoint");
            put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            put("txt", "text/plain");
            put("jpg", "image/jpeg");
            put("jpeg", "image/jpeg");
            put("png", "image/png");
            put("gif", "image/gif");
        }};

        return mimeTypes.getOrDefault(extension.toLowerCase(), "application/octet-stream");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
    /**
     * KKFileView在线预览接口（通过URL）
     * 保持原有接口不变
     */
    @GetMapping("/onlinePreview")
    public ResponseEntity<byte[]> onlinePreview(@RequestParam String url) {
        log.info("接收KKFileView代理预览请求 - url: {}", url);
        return filePreviewService.onlinePreviewByUrl(url);
    }

    /**
     * 智能预览接口（推荐使用）
     * 自动根据文件类型选择最佳预览方式
     */
    @GetMapping("/smartPreview")
    public ResponseEntity<?> smartPreview(@RequestParam Long fileId) {
        log.info("接收智能预览请求 - fileId: {}", fileId);
        return filePreviewService.smartPreview(fileId);
    }
    /**
     * 构建文件上传响应
     */
    private FileUploadResponse buildFileUploadResponse(FileInfo fileInfo, boolean isFastUpload) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(fileInfo.getId());
        response.setFileName(fileInfo.getOriginalFilename());
        response.setFileSize(fileInfo.getFileSize());
        response.setFileSizeDesc(fileInfo.getFileSizeDesc());
        response.setMd5(fileInfo.getMd5());
        response.setUploadTime(fileInfo.getUploadTime());
        response.setFastUpload(isFastUpload);
        return response;
    }

    /**
     * 智能预览报告接口
     * 自动根据文件类型选择最佳预览方式
     */
    @GetMapping("/smartPreviewReport")
    public ResponseEntity<?> smartPreviewReport(@RequestParam String paperId) {
        log.info("接收智能预览请求 - paperId: {}", paperId);
        return filePreviewService.smartPreviewReport(paperId);
    }

    /**
     * 报告文件下载接口
     */
    @GetMapping("/downloadReport/{reportId}")
    public ResponseEntity<Resource> downloadReport(
            @PathVariable Long reportId,
            HttpServletResponse response) {

        try {
            // 1. 查询报告
            CheckReport checkReport = checkReportService.getById(reportId);
            if (checkReport == null) {
                return ResponseEntity.notFound().build();
            }

            // 2. 验证权限
            CheckTask checkTask = checkTaskService.getById(checkReport.getTaskId());
            if (checkTask == null) {
                return ResponseEntity.notFound().build();
            }

            PaperInfo paper = paperInfoService.getById(checkTask.getPaperId());
            if (paper == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 3. 检查文件
            String reportPath = checkReport.getReportPath();
            File file = new File(reportPath);

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 4. 准备文件流
            Path path = Paths.get(file.getAbsolutePath());
            Resource resource = new InputStreamResource(Files.newInputStream(path));

            // 5. 设置响应头
            String filename = "相似度报告_" + paper.getPaperTitle() + ".pdf";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodedFilename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(file.length())
                    .body(resource);

        } catch (Exception e) {
            log.error("下载报告失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除文件接口
     */
    @DeleteMapping("/delete/file")
    public Result<Void> deleteFile(@RequestParam Long fileId) {
        try {
            log.info("文件删除请求 - fileId: {}", fileId);
            boolean deleted = fileService.deleteFile(fileId);
            if (deleted) {
                log.info("文件删除成功 - fileId: {}", fileId);
                return Result.success("文件删除成功");
            } else {
                log.warn("文件删除失败 - fileId: {}", fileId);
                return Result.error(ResultCode.RESOURCE_NOT_FOUND, "文件不存在或删除失败");
            }
        } catch (Exception e) {
            log.error("文件删除异常 - fileId: {}", fileId, e);
            return Result.error(ResultCode.SYSTEM_ERROR, "文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 下载临时目录中的导出文件
     */
    @GetMapping("/download/export")
    public void downloadExportFile(
            @RequestParam String filePath,
            HttpServletResponse response) {
        try {
            log.info("下载导出文件请求 - filePath: {}", filePath);

            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                log.warn("导出文件不存在 - filePath: {}", filePath);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件不存在");
                return;
            }

            // 设置响应头
            String filename = file.getName();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");

            // 根据文件扩展名设置正确的Content-Type
            String contentType = "application/octet-stream";
            if (filename.toLowerCase().endsWith(".xlsx")) {
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else if (filename.toLowerCase().endsWith(".xls")) {
                contentType = "application/vnd.ms-excel";
            }

            response.setContentType(contentType);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFilename + "\"");
            response.setContentLengthLong(file.length());

            // 写入文件流
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            log.info("导出文件下载成功 - filePath: {}", filePath);

        } catch (Exception e) {
            log.error("下载导出文件失败 - filePath: {}", filePath, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("文件下载失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("发送错误响应失败", ioException);
            }
        }
    }
}