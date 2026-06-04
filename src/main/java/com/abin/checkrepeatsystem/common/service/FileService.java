package com.abin.checkrepeatsystem.common.service;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.component.MinioProp;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.service.PaperContentMinioService;
import com.abin.checkrepeatsystem.detection.service.PaperContentExtractor;
import com.abin.checkrepeatsystem.mapper.FileInfoMapper;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.CheckTask;
import com.abin.checkrepeatsystem.pojo.entity.FileInfo;
import com.abin.checkrepeatsystem.student.mapper.CheckReportMapper;
import com.abin.checkrepeatsystem.student.mapper.CheckTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.abin.checkrepeatsystem.common.utils.FileMimeTypeUtils;

@RequiredArgsConstructor
@Service
@Slf4j
public class FileService {

    @Value("${file.upload.base-path}")
    private String uploadPath;

    @Value("${kkfileview.base-url}")
    private String kkfileviewUrl;
    
    private final MinioClient minioClient;

    private final MinioProp minioProp;

    private final Executor taskExecutor;

    /**
     * 初始化MinIO存储，确保bucket存在
     */
    @PostConstruct
    private void init() {
        try {
            String bucketName = minioProp.getBucket().getFile();
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建MinIO bucket成功：{}", bucketName);
            } else {
                log.info("MinIO bucket已存在：{}", bucketName);
            }
        } catch (Exception e) {
            log.error("初始化MinIO bucket失败", e);
            throw new RuntimeException("初始化MinIO bucket失败：" + e.getMessage());
        }
    }

    private final FileInfoMapper fileInfoMapper;

    private final CheckTaskMapper checkTaskMapper;

    private final CheckReportMapper checkReportMapper;
    
    private final PaperContentMinioService paperContentMinioService;

    // Tika实例（线程安全，可以重用）
    private final Tika tika = new Tika();

    /**
     * 上传文件 - MinIO存储版本
     */
    public Long uploadFile(MultipartFile file, Long userId) {
        String originalFilename = file.getOriginalFilename();
        log.info("开始处理文件上传 - 文件名：{}, 用户 ID: {}", originalFilename, userId);

        try {
            // 1. 计算文件 MD5
            byte[] fileBytes = file.getBytes();
            String fileMd5 = calculateFileMd5FromBytes(fileBytes);

            // 2. 快速检查：已存在则直接返回（无需加锁）
            FileInfo existingFile = getByMd5(fileMd5);
            if (existingFile != null) {
                log.info("文件已存在，使用秒传 - 文件 ID: {}, MD5: {}", existingFile.getId(), fileMd5);
                return existingFile.getId();
            }

            // 3. synchronized 仅保护 DB 写入（最小化锁范围）
            Long fileId;
            synchronized (this) {
                // 双重检查：并发场景下可能已被其他线程插入
                existingFile = getByMd5(fileMd5);
                if (existingFile != null) {
                    return existingFile.getId();
                }

                FileInfo fileInfo = new FileInfo();
                fileInfo.setMd5(fileMd5);
                fileInfo.setOriginalFilename(originalFilename);
                fileInfo.setFileSize(file.getSize());
                fileInfo.setFileSizeDesc(formatFileSize(file.getSize()));
                fileInfo.setUploadTime(LocalDateTime.now());
                fileInfo.setUploadUserId(String.valueOf(userId));
                fileInfo.setWordCount(0);
                fileInfo.setPageCount(0);
                fileInfo.setCreateTime(LocalDateTime.now());
                fileInfo.setUpdateTime(LocalDateTime.now());

                fileInfoMapper.insert(fileInfo);
                fileId = fileInfo.getId();
            }

            // 4. MinIO 上传在锁外执行（IO 操作不阻塞其他文件上传）
            String filePath = saveFileToMinio(file, fileId, String.valueOf(userId), fileBytes);

            // 5. 更新文件路径
            FileInfo updateInfo = new FileInfo();
            updateInfo.setId(fileId);
            updateInfo.setStoragePath(filePath);
            updateInfo.setAccessUrl(minioProp.getEndpoint() + "/" + minioProp.getBucket().getFile() + "/" + filePath);
            fileInfoMapper.updateById(updateInfo);

            // 6. 异步处理：统计字数和页数
            FileInfo fileInfoForAsync = new FileInfo();
            fileInfoForAsync.setId(fileId);
            fileInfoForAsync.setOriginalFilename(originalFilename);
            asyncProcessFile(fileBytes, fileInfoForAsync);

            log.info("文件上传成功 - 文件 ID: {}, 文件名：{}, 用户 ID: {}", fileId, originalFilename, userId);
            return fileId;

        } catch (Exception e) {
            log.error("文件上传失败 - 文件名：{}, 用户 ID: {}", originalFilename, userId, e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 异步处理文件：统计字数、页数和提取内容到Minio
     */
    private void asyncProcessFile(byte[] fileBytes, FileInfo fileInfo) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始异步处理文件 - 文件 ID: {}, 文件名: {}", fileInfo.getId(), fileInfo.getOriginalFilename());

                
                // 1. 统计字数
                int wordCount = countWordsFromBytes(fileBytes);
                log.info("字数统计完成 - 文件 ID: {}, 字数: {}", fileInfo.getId(), wordCount);

                // 2. 统计页数
                int pageCount = countPagesFromBytes(fileBytes, fileInfo.getOriginalFilename());
                log.info("页数统计完成 - 文件 ID: {}, 页数: {}", fileInfo.getId(), pageCount);

                // 更新数据库中的字数和页数
                FileInfo updateInfo = new FileInfo();
                updateInfo.setId(fileInfo.getId());
                updateInfo.setWordCount(wordCount);
                updateInfo.setPageCount(pageCount);
                updateInfo.setUpdateTime(LocalDateTime.now());

                int updateResult = fileInfoMapper.updateById(updateInfo);
                log.info("字数和页数更新到数据库 - 文件 ID: {}, 更新结果: {}", fileInfo.getId(), updateResult);

                // 2. 提取内容并存储到Minio
                try {
                    log.info("开始提取内容到Minio - 文件 ID: {}", fileInfo.getId());
                    // 使用Tika提取文本内容
                    String content = tika.parseToString(new ByteArrayInputStream(fileBytes));
                    log.info("Tika提取内容完成 - 文件 ID: {}, 内容长度: {}", fileInfo.getId(), content != null ? content.length() : 0);
                    
                    if (content != null && !content.trim().isEmpty()) {
                        // 存储到Minio
                        String contentPath = paperContentMinioService.storePaperContent(content, fileInfo.getId());
                        log.info("文件内容已存储到Minio - 文件 ID: {}, 路径: {}",
                                fileInfo.getId(), contentPath);
                    } else {
                        log.warn("提取的内容为空，无法存储到Minio - 文件 ID: {}", fileInfo.getId());
                    }
                } catch (Exception e) {
                    log.error("提取内容到Minio失败 - 文件 ID: {}, 文件名: {}",
                            fileInfo.getId(), fileInfo.getOriginalFilename(), e);
                }

            } catch (Exception e) {
                log.error("异步处理文件失败 - 文件 ID: {}, 文件名: {}",
                        fileInfo.getId(), fileInfo.getOriginalFilename(), e);

                // 失败时设置字数和页数为0
                FileInfo updateInfo = new FileInfo();
                updateInfo.setId(fileInfo.getId());
                updateInfo.setWordCount(0);
                updateInfo.setPageCount(0);
                fileInfoMapper.updateById(updateInfo);
            }
        }, taskExecutor);
    }

    /**
     * 方法1：从字节数组统计字数
     */
    private int countWordsFromBytes(byte[] fileBytes) throws IOException {
        try {
            // 验证字节数组不为空
            if (fileBytes == null || fileBytes.length == 0) {
                return 0;
            }

            // 使用Tika从字节数组解析
            String content = tika.parseToString(new ByteArrayInputStream(fileBytes));

            if (content == null || content.trim().isEmpty()) {
                return 0;
            }

            return countChineseAndEnglish(content.trim());

        } catch (Exception e) {
            log.warn("从字节数组统计字数失败", e);
            return 0;
        }
    }

    /**
     * 从字节数组统计页数
     */
    private int countPagesFromBytes(byte[] fileBytes, String fileName) {
        try {
            // 验证字节数组不为空
            if (fileBytes == null || fileBytes.length == 0) {
                return 0;
            }

            // 根据文件扩展名判断文件类型
            String fileExtension = FileMimeTypeUtils.getFileExtension(fileName).toLowerCase();
            log.info("文件类型: {}", fileExtension);

            switch (fileExtension) {
                case "pdf":
                    return countPdfPages(fileBytes);
                case "docx":
                    return countDocxPages(fileBytes);
                case "doc":
                    return countDocPages(fileBytes);
                case "txt":
                    // 文本文件按默认每页30行估算
                    return countTxtPages(fileBytes);
                default:
                    // 其他文件类型，返回1页
                    log.warn("不支持的文件类型，返回1页: {}", fileExtension);
                    return 1;
            }

        } catch (Exception e) {
            log.warn("统计页数失败", e);
            return 1;
        }
    }


    /**
     * 统计PDF文件页数
     */
    private int countPdfPages(byte[] fileBytes) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            // 简单的PDF页数检测：搜索PDF文件中的页数标记
            byte[] buffer = new byte[1024];
            int bytesRead;
            StringBuilder content = new StringBuilder();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead));
            }

            // 搜索PDF中的页数信息
            // 注意：这是一个简化的实现，可能不适用于所有PDF文件
            String pdfContent = content.toString();
            
            // 方法1：查找 /Pages 字典中的 Count 条目
            int pagesIndex = pdfContent.indexOf("/Pages");
            if (pagesIndex != -1) {
                int countIndex = pdfContent.indexOf("/Count", pagesIndex);
                if (countIndex != -1) {
                    int start = countIndex + 6;
                    int end = pdfContent.indexOf(" ", start);
                    if (end != -1) {
                        String countStr = pdfContent.substring(start, end).trim();
                        try {
                            return Integer.parseInt(countStr);
                        } catch (NumberFormatException e) {
                            // 解析失败，继续尝试其他方法
                        }
                    }
                }
            }

            // 方法2：计算 %%Page: 标记的数量
            int pageCount = 0;
            int pageMarkerIndex = pdfContent.indexOf("%%Page:");
            while (pageMarkerIndex != -1) {
                pageCount++;
                pageMarkerIndex = pdfContent.indexOf("%%Page:", pageMarkerIndex + 1);
            }

            if (pageCount > 0) {
                return pageCount;
            }

            // 方法3：估算：按文件大小估算，每100KB一页
            int estimatedPages = (int) Math.ceil(fileBytes.length / 102400.0);
            return Math.max(1, estimatedPages);
        }
    }

    /**
     * 统计DOCX文件页数
     */
    private int countDocxPages(byte[] fileBytes) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().equals("docProps/app.xml")) {
                    // 读取app.xml文件，其中包含页数信息
                    StringBuilder content = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                        content.append(new String(buffer, 0, bytesRead));
                    }

                    String xmlContent = content.toString();
                    // 查找页数信息
                    int pagesStart = xmlContent.indexOf("<Pages>");
                    int pagesEnd = xmlContent.indexOf("</Pages>", pagesStart);
                    if (pagesStart != -1 && pagesEnd != -1) {
                        String pagesStr = xmlContent.substring(pagesStart + 7, pagesEnd).trim();
                        try {
                            return Integer.parseInt(pagesStr);
                        } catch (NumberFormatException e) {
                            // 解析失败，继续
                        }
                    }
                    break;
                }
                zipInputStream.closeEntry();
            }
        }

        // 如果无法从app.xml获取页数，按字数估算
        try {
            int wordCount = countWordsFromBytes(fileBytes);
            // 假设每页500字
            return Math.max(1, (int) Math.ceil(wordCount / 500.0));
        } catch (Exception e) {
            // 估算失败，返回1
            return 1;
        }
    }

    /**
     * 统计DOC文件页数
     */
    private int countDocPages(byte[] fileBytes) {
        // DOC文件格式复杂，这里使用简单估算
        // 按文件大小估算，每50KB一页
        int estimatedPages = (int) Math.ceil(fileBytes.length / 51200.0);
        return Math.max(1, estimatedPages);
    }

    /**
     * 统计TXT文件页数
     */
    private int countTxtPages(byte[] fileBytes) throws Exception {
        String content = new String(fileBytes, StandardCharsets.UTF_8);
        String[] lines = content.split("\\r?\\n");
        // 假设每页30行
        int estimatedPages = (int) Math.ceil(lines.length / 30.0);
        return Math.max(1, estimatedPages);
    }

    /**
     * 保存文件到MinIO
     */
    private String saveFileToMinio(MultipartFile file, Long fileId, String userId, byte[] fileBytes) throws Exception {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        String safeFilename = file.getOriginalFilename()
                .replaceAll("[<>:\"|?*]", "_")
                .replaceAll("[/\\\\]", "_");
        
        String objectName = "files/" + userId + "/" + datePath + "/" + fileId + "_" + safeFilename;
        
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProp.getBucket().getFile())
                    .object(objectName)
                    .stream(inputStream, fileBytes.length, -1)
                    .contentType(file.getContentType())
                    .build());
        }
        
        log.info("文件上传到MinIO成功：{}", objectName);
        return objectName;
    }

    /**
     * 计算文件MD5 - 改进版本
     */
    public String calculateFileMd5FromBytes(byte[] fileBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(fileBytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            log.warn("计算文件MD5失败", e);
            return "md5_" + System.currentTimeMillis();
        }
    }

    /**
     * 流式计算文件MD5 - 避免将整个文件加载到内存
     * 适用于大文件上传场景
     */
    public String calculateFileMd5FromStream(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            log.warn("流式计算文件MD5失败", e);
            return "md5_" + System.currentTimeMillis();
        }
    }


    /**
     * 中文按字符统计，英文按单词统计 - 优化版本
     */
    private int countChineseAndEnglish(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int count = 0;

        // 更准确的分割方式，考虑中英文混合
        String[] segments = text.split("(?<=[\\p{Punct}\\s])|(?=[\\p{Punct}\\s])");

        for (String segment : segments) {
            if (segment.trim().isEmpty()) {
                continue;
            }

            // 判断是否为纯中文片段
            if (isChineseSegment(segment)) {
                // 中文字符数
                for (char c : segment.toCharArray()) {
                    if (isChineseChar(c)) {
                        count++;
                    }
                }
            } else if (segment.matches("[a-zA-Z]+")) {
                // 纯英文单词
                count++;
            } else if (segment.matches("[a-zA-Z0-9]+")) {
                // 英文单词或数字
                count++;
            }
            // 标点符号和空白字符不计入字数
        }

        return count;
    }

    /**
     * 判断是否为中文片段
     */
    private boolean isChineseSegment(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        int chineseCount = 0;
        int totalCount = 0;

        for (char c : str.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                totalCount++;
                if (isChineseChar(c)) {
                    chineseCount++;
                }
            }
        }

        // 如果超过50%的字符是中文，认为是中文片段
        return totalCount > 0 && (chineseCount * 2 >= totalCount);
    }

    /**
     * 判断字符是否为中文字符（扩展范围）
     */
    private boolean isChineseChar(char c) {
        // Unicode中的CJK统一表意文字范围（包括扩展区）
        return (c >= 0x4E00 && c <= 0x9FFF) ||      // 基本汉字
                (c >= 0x3400 && c <= 0x4DBF) ||      // 扩展A
                (c >= 0x20000 && c <= 0x2A6DF) ||    // 扩展B（需要代理对处理）
                (c >= 0x2A700 && c <= 0x2B73F) ||    // 扩展C
                (c >= 0x2B740 && c <= 0x2B81F) ||    // 扩展D
                (c >= 0x2B820 && c <= 0x2CEAF) ||    // 扩展E
                (c >= 0x2CEB0 && c <= 0x2EBEF) ||    // 扩展F
                (c >= 0x30000 && c <= 0x3134F);      // 扩展G
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + "B";
        }

        double kbSize = size / 1024.0;
        if (kbSize < 1024) {
            return String.format("%.1fKB", kbSize);
        }

        double mbSize = kbSize / 1024.0;
        if (mbSize < 1024) {
            return String.format("%.1fMB", mbSize);
        }

        double gbSize = mbSize / 1024.0;
        return String.format("%.1fGB", gbSize);
    }

    /**
     * 根据文件 ID 获取文件信息
     */
    public FileInfo getById(Long fileId) {
        return fileInfoMapper.selectById(fileId);
    }

    /**
     * 根据MD5查找文件
     */
    public FileInfo getByMd5(String md5) {
        try {
            List<FileInfo> fileInfos = fileInfoMapper.selectList(
                    new LambdaQueryWrapper<FileInfo>()
                            .eq(FileInfo::getMd5, md5));
            
            if (fileInfos == null || fileInfos.isEmpty()) {
                return null;
            }
            // 如果有多条记录，返回第一条
            return fileInfos.get(0);
        } catch (Exception e) {
            log.error("根据MD5查询文件失败", e);
            return null;
        }
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(Long fileId) {
        try {
            FileInfo fileInfo = getById(fileId);
            if (fileInfo != null) {
                // 从MinIO删除文件
                try {
                    minioClient.removeObject(
                        io.minio.RemoveObjectArgs.builder()
                            .bucket(minioProp.getBucket().getFile())
                            .object(fileInfo.getStoragePath())
                            .build()
                    );
                    log.debug("MinIO文件删除成功：{}", fileInfo.getStoragePath());
                } catch (Exception e) {
                    log.warn("MinIO文件删除失败：{}", fileInfo.getStoragePath(), e);
                }
    
                // 删除数据库记录
                int rows = fileInfoMapper.deleteById(fileId);
                if (rows > 0) {
                    log.debug("数据库记录删除成功 - 文件 ID: {}", fileId);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("删除文件失败 - 文件 ID: {}", fileId, e);
            return false;
        }
    }

    /**
     * 从MinIO读取文件内容
     */
    public byte[] getFileContentFromMinio(String objectName) throws Exception {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProp.getBucket().getFile())
                        .object(objectName)
                        .build())) {
            return inputStream.readAllBytes();
        }
    }

    /**
     * 从MinIO获取文件输入流（用于流式下载，避免大文件占用堆内存）
     * 注意：调用方负责关闭返回的 InputStream
     */
    public InputStream getFileStreamFromMinio(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProp.getBucket().getFile())
                        .object(objectName)
                        .build());
    }

    /**
     * 从MinIO读取文件内容
     */
    public byte[] getFileContent(Long fileId) throws Exception {
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }

        String storagePath = fileInfo.getStoragePath();
        if (storagePath == null || storagePath.isEmpty()) {
            throw new RuntimeException("文件存储路径为空");
        }

        return getFileContentFromMinio(storagePath);
    }
}