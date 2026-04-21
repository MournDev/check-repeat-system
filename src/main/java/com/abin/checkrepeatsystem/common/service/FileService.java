package com.abin.checkrepeatsystem.common.service;

import com.abin.checkrepeatsystem.common.Result;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class FileService {

    @Value("${file.upload.base-path}")
    private String uploadPath;

    @Value("${kkfileview.base-url}")
    private String kkfileviewUrl;
    
    /**
     * 初始化上传路径，确保目录存在
     */
    @PostConstruct
    private void init() {
        try {
            // 如果是 Windows 环境且路径为 Unix 风格，转换为 Windows 路径
            if (System.getProperty("os.name").toLowerCase().contains("win") && 
                (uploadPath.startsWith("/") || uploadPath.startsWith("data"))) {
                // 使用用户主目录下的临时上传目录
                String userHome = System.getProperty("user.home");
                uploadPath = Paths.get(userHome, "check-repeat-system", "upload").toString();
                log.info("检测到 Windows 环境，使用上传路径：{}", uploadPath);
            }
            
            // 创建上传根目录
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (created) {
                    log.info("创建上传目录成功：{}", uploadPath);
                } else {
                    log.warn("创建上传目录失败：{}", uploadPath);
                }
            } else {
                log.info("上传目录已存在：{}", uploadPath);
            }
            
            // 验证目录是否可写
            if (!uploadDir.canWrite()) {
                log.error("上传目录不可写：{}", uploadPath);
                throw new RuntimeException("上传目录不可写：" + uploadPath);
            }
            
        } catch (Exception e) {
            log.error("初始化上传路径失败", e);
            throw new RuntimeException("初始化上传路径失败：" + e.getMessage());
        }
    }

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private CheckTaskMapper checkTaskMapper;

    @Autowired
    private CheckReportMapper checkReportMapper;
    
    @Autowired
    private PaperContentMinioService paperContentMinioService;
    
    @Autowired
    private PaperContentExtractor paperContentExtractor;

    // 用于异步处理的线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    // Tika实例（线程安全，可以重用）
    private final Tika tika = new Tika();

    /**
     * 上传文件 - 改进版本
     */
    public Long uploadFile(MultipartFile file, Long userId) {
        String originalFilename = file.getOriginalFilename();
        log.info("开始处理文件上传 - 文件名：{}, 用户 ID: {}", originalFilename, userId);

        try {
            // 1. 计算文件 MD5（使用字节数组方式，避免依赖临时文件）
            byte[] fileBytes = file.getBytes();
            String fileMd5 = calculateFileMd5FromBytes(fileBytes);

            // 2. 同步检查文件是否存在并上传，防止并发插入导致唯一索引冲突
            synchronized (this) {
                // 检查是否已存在相同文件（通过MD5）
                FileInfo existingFile = getByMd5(fileMd5);
                if (existingFile != null) {
                    log.info("文件已存在，使用已有的文件信息 - 文件 ID: {}, MD5: {}", existingFile.getId(), fileMd5);
                    return existingFile.getId();
                }

                // 3. 生成文件 ID
                Long fileId = generateFileId();

                // 4. 保存文件到磁盘
                String filePath = saveFileToDisk(file, fileId, String.valueOf(userId));

                // 5. 保存文件基本信息到数据库（先不统计字数）
                FileInfo fileInfo = saveBasicFileInfo(file, fileId, fileMd5, filePath, String.valueOf(userId));

                // 6. 异步处理：统计字数和提取内容到Minio
                asyncProcessFile(fileBytes, fileInfo);

                log.info("文件上传成功 - 文件 ID: {}, 文件名：{}, 用户 ID: {}", fileId, originalFilename, userId);
                return fileId;
            }

        } catch (Exception e) {
            log.error("文件上传失败 - 文件名：{}, 用户 ID: {}", originalFilename, userId, e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 异步统计字数并更新数据库
     */
    /**
     * 异步处理文件：统计字数和提取内容到Minio
     */
    private void asyncProcessFile(byte[] fileBytes, FileInfo fileInfo) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始异步处理文件 - 文件 ID: {}, 文件名: {}", fileInfo.getId(), fileInfo.getOriginalFilename());
                
                // 1. 统计字数
                int wordCount = countWordsFromBytes(fileBytes);
                log.info("字数统计完成 - 文件 ID: {}, 字数: {}", fileInfo.getId(), wordCount);

                // 更新数据库中的字数
                FileInfo updateInfo = new FileInfo();
                updateInfo.setId(fileInfo.getId());
                updateInfo.setWordCount(wordCount);
                updateInfo.setUpdateTime(LocalDateTime.now());

                int updateResult = fileInfoMapper.updateById(updateInfo);
                log.info("字数更新到数据库 - 文件 ID: {}, 更新结果: {}", fileInfo.getId(), updateResult);

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

                // 失败时设置字数为0
                FileInfo updateInfo = new FileInfo();
                updateInfo.setId(fileInfo.getId());
                updateInfo.setWordCount(0);
                fileInfoMapper.updateById(updateInfo);
            }
        }, executorService);
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
     * 生成文件 ID
     */
    private Long generateFileId() {
        return System.currentTimeMillis();
    }

    /**
     * 计算文件MD5 - 改进版本
     */
    public String calculateFileMd5FromBytes(byte[] fileBytes) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
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
     * 保存文件到磁盘
     */
    private String saveFileToDisk(MultipartFile file, Long fileId, String userId) throws IOException {
        // 生成文件存储路径
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    
        // 清理文件名中的非法字符
        String safeFilename = file.getOriginalFilename()
                .replaceAll("[<>:\"|?*]", "_") // Windows 非法字符
                .replaceAll("[/\\\\]", "_");   // 路径分隔符
    
        String fileName = fileId + "_" + safeFilename;
        String relativePath = Paths.get(userId, datePath, fileName).toString();
        String fullPath = Paths.get(uploadPath, relativePath).toString();

        // 创建目录
        File destFile = new File(fullPath);
        File parentDir = destFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                log.error("创建目录失败: {}", parentDir.getAbsolutePath());
                throw new IOException("创建目录失败: " + parentDir.getAbsolutePath());
            }
            log.debug("创建目录成功: {}", parentDir.getAbsolutePath());
        }

        // 保存文件
        file.transferTo(destFile);
        log.debug("文件保存到磁盘成功: {}", fullPath);

        return relativePath;
    }

    /**
     * 保存基本文件信息到数据库（不包含字数统计）
     */
    private FileInfo saveBasicFileInfo(MultipartFile file, Long fileId, String fileMd5,
                                       String filePath, String userId) throws IOException {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(fileId);
        fileInfo.setOriginalFilename(file.getOriginalFilename());
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileSizeDesc(formatFileSize(file.getSize()));
        fileInfo.setStoragePath(filePath);
        fileInfo.setMd5(fileMd5);
        fileInfo.setUploadTime(LocalDateTime.now());
        fileInfo.setUploadUserId(userId);

        // 初始化字数为0，异步更新
        fileInfo.setWordCount(0);
        fileInfo.setCreateTime(LocalDateTime.now());
        fileInfo.setUpdateTime(LocalDateTime.now());

        fileInfoMapper.insert(fileInfo);
        // 异步或延迟进行字数统计，避免重复读取已删除的临时文件
        try {
            int wordCount = countWordsFromFile(new File(uploadPath, filePath));
            fileInfo.setWordCount(wordCount);
            fileInfoMapper.updateById(fileInfo);
        } catch (Exception e) {
            log.warn("文件字数统计失败 - 文件 ID: {}", fileId, e);
            fileInfo.setWordCount(0);
            fileInfoMapper.updateById(fileInfo);
        }
        return fileInfo;
    }
    /**
     * 从已保存的文件中统计字数
     */
    private int countWordsFromFile(File savedFile) throws IOException {
        try {
            // 使用 Apache Tika 提取文本内容
            org.apache.tika.Tika tika = new org.apache.tika.Tika();

            // 从已保存的文件中提取文本内容
            String content = tika.parseToString(savedFile);

            // 如果内容为空，返回0
            if (content == null || content.trim().isEmpty()) {
                return 0;
            }

            // 统计字数
            return countChineseAndEnglish(content.trim());

        } catch (Exception e) {
            log.warn("文件字数统计失败 - 文件路径: {}", savedFile.getAbsolutePath(), e);
            return 0;
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
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileInfo>()
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
                // 删除磁盘文件
                String fullPath = Paths.get(uploadPath, fileInfo.getStoragePath()).toString();
                boolean deleted = Files.deleteIfExists(Paths.get(fullPath));
    
                if (deleted) {
                    log.debug("磁盘文件删除成功：{}", fullPath);
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

    public Result<String> getReportPreviewUrl(String paperId) {
        LambdaQueryWrapper<CheckTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CheckTask::getPaperId, paperId)
                .eq(CheckTask::getIsDeleted, 0)
                .orderByDesc(CheckTask::getCreateTime)
                .last("LIMIT 1");
        CheckTask checkTask = checkTaskMapper.selectOne(queryWrapper);
        if (checkTask == null) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!checkTask.getCheckStatus().equals("completed")) {
            return Result.error(ResultCode.SYSTEM_ERROR, "查重尚未完成");
        }
        Long reportId = checkTask.getReportId();
        CheckReport checkReport = checkReportMapper.selectById(reportId);
        if (checkReport == null) {
            return Result.error(ResultCode.SYSTEM_ERROR);
        }
        String reportPath = checkReport.getReportPath();
        File reportFile = new File(reportPath);
        if (!reportFile.exists()) {
            return Result.error(ResultCode.RESOURCE_NOT_FOUND, "报告文件不存在");
        }
        log.info("报告文件路径: {}", reportPath);
        try {
            String encodedPath = URLEncoder.encode(reportPath, StandardCharsets.UTF_8);
            String previewUrl = kkfileviewUrl + "/onlinePreview?url=" + encodedPath;
            log.info("生成的预览URL: {}", previewUrl);
            return Result.success(previewUrl);
        } catch (Exception e) {
            log.error("生成预览URL失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "生成预览URL失败");
        }

    }
}