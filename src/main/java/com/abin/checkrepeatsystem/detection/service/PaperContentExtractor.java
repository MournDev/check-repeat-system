package com.abin.checkrepeatsystem.detection.service;

import com.abin.checkrepeatsystem.common.Exception.BusinessException;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.service.PaperContentMinioService;
import com.abin.checkrepeatsystem.common.utils.IKAnalyzerUtils;
import com.abin.checkrepeatsystem.common.utils.TikaTextExtractor;
import com.abin.checkrepeatsystem.mapper.FileInfoMapper;
import com.abin.checkrepeatsystem.pojo.entity.FileInfo;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 论文内容提取服务
 * 负责从各种存储位置提取论文正文内容并进行分词预处理
 */
@Service
@Slf4j
public class PaperContentExtractor {

    @Resource
    private PaperInfoMapper paperInfoMapper;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private PaperContentMinioService paperContentMinioService;

    @Value("${file.upload.base-path:/data/upload/}")
    private String uploadBasePath;

    private final Tika tika = new Tika();

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
            
        } catch (Exception e) {
            log.error("初始化上传路径失败", e);
        }
    }

    /**
     * 提取论文原始内容（从文件系统或MinIO）
     *
     * @param paperId 论文ID
     * @return 论文原始文本内容
     */
    public String extractRawContent(Long paperId) {
        log.info("开始提取论文原始内容: paperId={}", paperId);

        try {
            // 1. 验证论文ID
            if (paperId == null) {
                log.warn("论文ID为空，无法提取内容");
                return null;
            }
            
            // 2. 获取论文信息
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                log.warn("论文不存在: paperId={}", paperId);
                return null;
            }

            log.info("获取论文信息成功: paperId={}, fileId={}, filePath={}", paperId, paperInfo.getFileId(), paperInfo.getFilePath());

            // 2. 优先从MinIO读取已提取的内容
            if (paperInfo.getContentPath() != null && !paperInfo.getContentPath().isEmpty()) {
                try {
                    String content = paperContentMinioService.readPaperContent(paperInfo.getContentPath());
                    log.info("从MinIO读取论文内容成功: paperId={}", paperId);
                    return content;
                } catch (Exception e) {
                    log.warn("从MinIO读取论文内容失败，尝试从文件提取: paperId={}, error={}", paperId, e.getMessage());
                }
            } else {
                log.info("论文contentPath为空，需要从文件提取: paperId={}", paperId);
            }

            // 3. 从原始文件提取内容
            log.info("开始从原始文件提取内容: paperId={}", paperId);
            String content = extractFromOriginalFile(paperInfo);
            log.info("从原始文件提取内容完成: paperId={}, contentLength={}", paperId, content != null ? content.length() : 0);

            // 4. 将提取的内容存储到MinIO（缓存）
            if (content != null && !content.isEmpty()) {
                try {
                    log.info("开始存储内容到MinIO: paperId={}", paperId);
                    String contentPath = paperContentMinioService.storePaperContent(content, paperId);
                    // 更新论文表的contentPath字段
                    paperInfo.setContentPath(contentPath);
                    int updateResult = paperInfoMapper.updateById(paperInfo);
                    log.info("论文内容已缓存到MinIO: paperId={}, path={}, updateResult={}", paperId, contentPath, updateResult);
                } catch (Exception e) {
                    log.error("缓存论文内容到MinIO失败: paperId={}", paperId, e);
                }
            } else {
                log.warn("提取的内容为空，无法存储到MinIO: paperId={}", paperId);
            }

            return content;

        } catch (BusinessException e) {
            log.error("提取论文内容失败: paperId={}", paperId, e);
            throw e;
        } catch (Exception e) {
            log.error("提取论文内容失败: paperId={}", paperId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "提取论文内容失败: " + e.getMessage());
        }
    }

    /**
     * 提取论文分词后的内容（从MinIO或实时分词）
     *
     * @param paperId 论文ID
     * @return IK分词后的文本（空格分隔）
     */
    public String extractSegmentedContent(Long paperId) {
        log.info("开始提取论文分词内容: paperId={}", paperId);

        try {
            // 1. 获取论文信息
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "论文不存在: " + paperId);
            }

            // 2. 优先从MinIO读取已分词的内容
            if (paperInfo.getSegmentedPath() != null && !paperInfo.getSegmentedPath().isEmpty()) {
                try {
                    String segmentedContent = paperContentMinioService.readSegmentedText(paperInfo.getSegmentedPath());
                    log.info("从MinIO读取分词内容成功: paperId={}", paperId);
                    return segmentedContent;
                } catch (Exception e) {
                    log.warn("从MinIO读取分词内容失败，尝试重新分词: paperId={}, error={}", paperId, e.getMessage());
                }
            }

            // 3. 获取原始内容并进行分词
            String rawContent = extractRawContent(paperId);
            if (rawContent == null || rawContent.isEmpty()) {
                return "";
            }

            // 4. 使用IK分词器进行分词
            String segmentedContent = IKAnalyzerUtils.segmentToString(rawContent);

            // 5. 将分词结果存储到MinIO（缓存）
            if (!segmentedContent.isEmpty()) {
                try {
                    String segmentedPath = paperContentMinioService.storeSegmentedText(segmentedContent, paperId);
                    // 更新论文表的segmentedPath字段
                    paperInfo.setSegmentedPath(segmentedPath);
                    paperInfoMapper.updateById(paperInfo);
                    log.info("论文分词内容已缓存到MinIO: paperId={}, path={}", paperId, segmentedPath);
                } catch (Exception e) {
                    log.warn("缓存分词内容到MinIO失败: paperId={}, error={}", paperId, e.getMessage());
                }
            }

            return segmentedContent;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("提取论文分词内容失败: paperId={}", paperId, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "提取论文分词内容失败: " + e.getMessage());
        }
    }

    /**
     * 从原始文件提取内容
     */
    private String extractFromOriginalFile(PaperInfo paperInfo) {
        Long fileId = paperInfo.getFileId();
        log.info("开始从原始文件提取内容: paperId={}, fileId={}", paperInfo.getId(), fileId);
        
        if (fileId == null) {
            log.error("论文未上传文件: paperId={}", paperInfo.getId());
            throw new BusinessException(ResultCode.PARAM_ERROR, "论文未上传文件");
        }

        // 查询文件信息
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null) {
            log.error("文件信息不存在: fileId={}", fileId);
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND, "文件信息不存在: " + fileId);
        }
        
        log.info("获取文件信息成功: fileId={}, storagePath={}, originalFilename={}", 
                fileId, fileInfo.getStoragePath(), fileInfo.getOriginalFilename());

        // 从本地存储路径读取（使用完整路径）
        String storagePath = fileInfo.getStoragePath();
        if (storagePath != null && !storagePath.isEmpty()) {
            // 使用Path类拼接路径，确保跨平台兼容性
            Path basePath = Paths.get(uploadBasePath);
            Path relativePath = Paths.get(storagePath);
            Path fullPath = basePath.resolve(relativePath);
            String fullPathStr = fullPath.toString();
            
            log.info("从本地存储路径读取: basePath={}, storagePath={}, fullPath={}", 
                    uploadBasePath, storagePath, fullPathStr);
            
            // 检查目录是否存在
            Path parentDir = fullPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                log.error("父目录不存在: {}", parentDir);
            } else if (parentDir != null) {
                log.info("父目录存在: {}", parentDir);
            }
            
            String content = extractFromFilePath(fullPathStr);
            if (content != null && !content.isEmpty()) {
                log.info("从本地存储路径读取成功: length={}", content.length());
                return content;
            }
            log.error("从本地存储路径读取失败: path={}", fullPathStr);
        }

        log.error("无法读取论文文件内容: paperId={}, fileId={}", paperInfo.getId(), fileId);
        throw new BusinessException(ResultCode.SYSTEM_ERROR, "无法读取论文文件内容");
    }

    /**
     * 从文件路径提取文本
     */
    private String extractFromFilePath(String filePath) {
        try {
            log.info("开始从文件路径提取文本: path={}", filePath);
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                log.warn("文件不存在: {}", filePath);
                return null;
            }
            log.info("文件存在: path={}", filePath);

            // 使用Tika提取文本
            log.info("开始使用Tika提取文本: path={}", filePath);
            String content = TikaTextExtractor.extractTextFromFile(filePath);
            log.info("从文件提取内容成功: path={}, length={}", filePath, content != null ? content.length() : 0);
            return content;

        } catch (Exception e) {
            log.error("从文件提取内容失败: path={}", filePath, e);
            return null;
        }
    }

    /**
     * 从URL提取文本（支持MinIO等对象存储）
     */
    private String extractFromUrl(String url) {
        try {
            // 如果是MinIO的URL，使用MinIO服务读取
            if (url.contains("minio") || url.contains("9000")) {
                // 尝试从MinIO读取
                try {
                    return paperContentMinioService.readPaperContent(url);
                } catch (Exception e) {
                    log.warn("从MinIO URL读取失败: {}", url);
                }
            }

            // 其他URL类型，可以使用HTTP客户端下载后提取
            // 这里简化处理，返回null
            log.warn("不支持的URL类型: {}", url);
            return null;

        } catch (Exception e) {
            log.error("从URL提取内容失败: url={}", url, e);
            return null;
        }
    }

    /**
     * 清除论文内容缓存（用于重新提取）
     */
    public void clearContentCache(Long paperId) {
        try {
            PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo != null) {
                // 清除MinIO中的缓存
                paperContentMinioService.deletePaperContent(paperId);

                // 清除数据库中的路径记录
                paperInfo.setContentPath(null);
                paperInfo.setSegmentedPath(null);
                paperInfoMapper.updateById(paperInfo);

                log.info("论文内容缓存已清除: paperId={}", paperId);
            }
        } catch (Exception e) {
            log.error("清除论文内容缓存失败: paperId={}", paperId, e);
        }
    }
}