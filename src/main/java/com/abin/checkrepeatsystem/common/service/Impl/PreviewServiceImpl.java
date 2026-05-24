package com.abin.checkrepeatsystem.common.service.Impl;

import com.abin.checkrepeatsystem.common.VO.FilePreviewInfoDTO;
import com.abin.checkrepeatsystem.common.component.MinioProp;
import com.abin.checkrepeatsystem.common.dto.PreviewResponse;
import com.abin.checkrepeatsystem.common.enums.FileType;
import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.common.service.PreviewService;
import com.abin.checkrepeatsystem.common.service.PreviewTokenService;
import com.abin.checkrepeatsystem.pojo.entity.FileInfo;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 预览服务实现
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class PreviewServiceImpl implements PreviewService {

    private final FileService fileService;

    private final PaperInfoMapper paperInfoMapper;

    private final MinioClient minioClient;

    private final MinioProp minioProp;

    private final PreviewTokenService previewTokenService;

    @Value("${kkfileview.base-url}")
    private String kkfileviewUrl;

    @Value("${server.address:0.0.0.0}")
    private String serverAddress;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${server.external-address:localhost}")
    private String externalAddress;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public PreviewResponse getPreviewUrl(Long paperId) {
        try {
            log.info("获取论文预览URL（兼容旧接口） - paperId: {}", paperId);

            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                log.warn("论文不存在 - paperId: {}", paperId);
                return PreviewResponse.failure("论文不存在");
            }

            Long fileId = paperInfo.getFileId();
            String fileName = paperInfo.getPaperTitle() + ".docx";

            FileInfo fileInfo = null;
            if (fileId != null) {
                fileInfo = fileService.getById(fileId);
                if (fileInfo != null) {
                    fileName = fileInfo.getOriginalFilename();
                }
            }

            if (fileInfo == null && paperInfo.getContentPath() != null) {
                log.info("使用contentPath构建预览URL - contentPath: {}", paperInfo.getContentPath());
                return buildMinioPreviewUrl(paperInfo.getContentPath(), fileName);
            }

            if (fileInfo == null) {
                log.warn("文件信息不存在 - paperId: {}", paperId);
                return PreviewResponse.failure("文件信息不存在");
            }

            String previewUrl = buildPreviewUrl(fileInfo, fileName);
            return PreviewResponse.success(previewUrl);

        } catch (Exception e) {
            log.error("生成预览URL失败 - paperId: {}", paperId, e);
            return PreviewResponse.failure("生成预览URL失败: " + e.getMessage());
        }
    }

    @Override
    public FilePreviewInfoDTO getPreviewInfo(Long paperId) {
        try {
            log.info("获取论文预览信息 - paperId: {}", paperId);

            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                log.warn("论文不存在 - paperId: {}", paperId);
                return FilePreviewInfoDTO.failure("论文不存在");
            }

            Long fileId = paperInfo.getFileId();
            String fileName = paperInfo.getPaperTitle() + ".docx";
            Long fileSize = null;

            FileInfo fileInfo = null;
            if (fileId != null) {
                fileInfo = fileService.getById(fileId);
                if (fileInfo != null) {
                    fileName = fileInfo.getOriginalFilename();
                    fileSize = fileInfo.getFileSize();
                }
            }

            if (fileInfo == null && paperInfo.getContentPath() != null) {
                fileName = getFileNameFromPath(paperInfo.getContentPath());
                FileType fileType = FileType.fromFileName(fileName);
                
                String kkfileviewUrl = buildMinioPreviewUrl(paperInfo.getContentPath(), fileName).getPreviewUrl();
                
                return FilePreviewInfoDTO.success(
                        fileId,
                        fileName,
                        fileType.getType(),
                        fileType.isNativeSupported(),
                        fileSize,
                        null,
                        kkfileviewUrl
                );
            }

            if (fileInfo == null) {
                log.warn("文件信息不存在 - paperId: {}", paperId);
                return FilePreviewInfoDTO.failure("文件信息不存在");
            }

            FileType fileType = FileType.fromFileName(fileName);
            String nativePreviewUrl = null;
            String kkfileviewPreviewUrl = null;
            String previewToken = null;

            // 只生成一次token，用于所有地方
            if (fileInfo.getStoragePath() != null && fileInfo.getStoragePath().startsWith("files/")) {
                // MinIO存储，使用MinIO预签名URL，不需要token
                if (fileType.isNativeSupported()) {
                    nativePreviewUrl = buildNativePreviewUrl(fileInfo, fileName);
                } else {
                    kkfileviewPreviewUrl = buildMinioPreviewUrl(fileInfo.getStoragePath(), fileName).getPreviewUrl();
                }
            } else {
                // 本地存储，生成一次token
                previewToken = previewTokenService.generatePreviewToken(fileId);
                
                if (fileType.isNativeSupported()) {
                    nativePreviewUrl = buildNativePreviewUrl(fileInfo, fileName, previewToken);
                } else {
                    kkfileviewPreviewUrl = buildPreviewUrl(fileInfo, fileName, previewToken);
                }
            }

            return FilePreviewInfoDTO.success(
                    fileId,
                    fileName,
                    fileType.getType(),
                    fileType.isNativeSupported(),
                    fileSize,
                    nativePreviewUrl,
                    kkfileviewPreviewUrl,
                    previewToken
            );

        } catch (Exception e) {
            log.error("获取预览信息失败 - paperId: {}", paperId, e);
            return FilePreviewInfoDTO.failure("获取预览信息失败: " + e.getMessage());
        }
    }

    @Override
    public PreviewResponse getKkfileviewPreviewUrl(Long paperId) {
        try {
            log.info("获取KKFileView预览URL - paperId: {}", paperId);

            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                log.warn("论文不存在 - paperId: {}", paperId);
                return PreviewResponse.failure("论文不存在");
            }

            Long fileId = paperInfo.getFileId();
            String fileName = paperInfo.getPaperTitle() + ".docx";

            FileInfo fileInfo = null;
            if (fileId != null) {
                fileInfo = fileService.getById(fileId);
                if (fileInfo != null) {
                    fileName = fileInfo.getOriginalFilename();
                }
            }

            if (fileInfo == null && paperInfo.getContentPath() != null) {
                return buildMinioPreviewUrl(paperInfo.getContentPath(), fileName);
            }

            if (fileInfo == null) {
                log.warn("文件信息不存在 - paperId: {}", paperId);
                return PreviewResponse.failure("文件信息不存在");
            }

            String previewUrl = buildPreviewUrl(fileInfo, fileName);
            return PreviewResponse.success(previewUrl);

        } catch (Exception e) {
            log.error("获取KKFileView预览URL失败 - paperId: {}", paperId, e);
            return PreviewResponse.failure("获取预览URL失败: " + e.getMessage());
        }
    }

    @Override
    public boolean checkServiceStatus() {
        try {
            String checkUrl = kkfileviewUrl + "/";
            ResponseEntity<String> response = restTemplate.getForEntity(checkUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("KKFileView服务检查失败: {}", e.getMessage());
            return true;
        }
    }

    private PreviewResponse buildMinioPreviewUrl(String contentPath, String fileName) {
        try {
            String presignedUrl = generateMinioPresignedUrl(contentPath);
            log.info("构建MinIO预签名URL: {}", presignedUrl);

            String encodedUrl = Base64.getUrlEncoder().encodeToString(presignedUrl.getBytes(StandardCharsets.UTF_8));
            String previewUrl = kkfileviewUrl + "/onlinePreview?url=" + encodedUrl;
            log.info("构建KKFileView预览URL: {}", previewUrl);

            return PreviewResponse.success(previewUrl);

        } catch (Exception e) {
            log.error("构建MinIO预览URL失败", e);
            return PreviewResponse.failure("构建预览URL失败: " + e.getMessage());
        }
    }

    private String buildPreviewUrl(FileInfo fileInfo, String fileName, String previewToken) {
        String storagePath = fileInfo.getStoragePath();
        
        log.info("文件存储路径检查 - storagePath: '{}', startsWith('files/'): {}", 
                storagePath, storagePath != null && storagePath.startsWith("files/"));
        
        if (storagePath != null && storagePath.startsWith("files/")) {
            return buildMinioPreviewUrl(storagePath, fileName).getPreviewUrl();
        } else {
            return buildLocalFilePreviewUrl(fileInfo.getId(), fileName, previewToken);
        }
    }

    // 保留向后兼容的方法
    private String buildPreviewUrl(FileInfo fileInfo, String fileName) {
        // 生成新token（向后兼容）
        String previewToken = previewTokenService.generatePreviewToken(fileInfo.getId());
        return buildPreviewUrl(fileInfo, fileName, previewToken);
    }

    private String buildLocalFilePreviewUrl(Long fileId, String fileName, String previewToken) {
        // 使用传入的token，而不是重新生成
        String fileUrl = String.format("http://%s:%s%s/api/file/preview/%s/%s",
                externalAddress,
                serverPort,
                contextPath,
                previewToken,
                URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        log.info("构建本地文件访问URL: {}", fileUrl);

        String encodedUrl = Base64.getUrlEncoder().encodeToString(fileUrl.getBytes(StandardCharsets.UTF_8));
        return kkfileviewUrl + "/onlinePreview?url=" + encodedUrl;
    }

    private String buildNativePreviewUrl(FileInfo fileInfo, String fileName, String previewToken) {
        String storagePath = fileInfo.getStoragePath();
        
        if (storagePath != null && storagePath.startsWith("files/")) {
            try {
                return generateMinioPresignedUrl(storagePath);
            } catch (Exception e) {
                log.error("生成MinIO预签名URL失败", e);
                return null;
            }
        } else {
            // 使用传入的token构建预览URL
            return String.format("http://%s:%s%s/api/file/preview/%s/%s",
                    externalAddress,
                    serverPort,
                    contextPath,
                    previewToken,
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        }
    }

    // 保留向后兼容的方法
    private String buildNativePreviewUrl(FileInfo fileInfo, String fileName) {
        return buildNativePreviewUrl(fileInfo, fileName, null);
    }

    /**
     * 生成MinIO预签名URL
     * KKFileView和MinIO在同一台虚拟机上，可以直接访问
     */
    private String generateMinioPresignedUrl(String storagePath) throws Exception {
        // 移除前缀 "files/"
        String objectName = storagePath.replaceFirst("^files/", "");
        
        // 获取bucket名称
        String bucketName = minioProp.getBucket().getFile();
        
        // 生成预签名URL，有效期5分钟
        String presignedUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(5, TimeUnit.MINUTES)
                        .build()
        );
        
        log.info("生成MinIO预签名URL - bucket: {}, object: {}, url: {}", 
                bucketName, objectName, presignedUrl);
        
        return presignedUrl;
    }

    private String getFileNameFromPath(String path) {
        if (path == null) {
            return "document";
        }
        String[] parts = path.split("/");
        return parts[parts.length - 1];
    }
}