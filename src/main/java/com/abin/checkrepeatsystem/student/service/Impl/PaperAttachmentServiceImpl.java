package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.mapper.FileInfoMapper;
import com.abin.checkrepeatsystem.mapper.PaperAttachmentMapper;
import com.abin.checkrepeatsystem.pojo.entity.FileInfo;
import com.abin.checkrepeatsystem.pojo.entity.PaperAttachment;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaperAttachmentServiceImpl {

    private final PaperInfoMapper paperInfoMapper;
    private final PaperAttachmentMapper paperAttachmentMapper;
    private final FileInfoMapper fileInfoMapper;
    private final FileService fileService;

    @Transactional(rollbackFor = Exception.class)
    public PaperAttachment uploadAttachment(Long paperId, MultipartFile file, String attachmentType, Long studentId) {
        log.info("开始上传附件 - 论文ID: {}, 附件类型: {}", paperId, attachmentType);

        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null || !paperInfo.getStudentId().equals(studentId)) {
            throw new RuntimeException("论文不存在或无权限访问");
        }

        Long fileId = fileService.uploadFile(file, studentId);
        FileInfo fileInfo = fileInfoMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("文件上传失败");
        }

        PaperAttachment attachment = new PaperAttachment();
        attachment.setPaperId(paperId);
        attachment.setStudentId(studentId);
        attachment.setAdvisorId(paperInfo.getTeacherId());
        attachment.setOriginalFilename(file.getOriginalFilename());
        attachment.setStoragePath(fileInfo.getStoragePath());
        attachment.setFileType(fileInfo.getFileType());
        attachment.setFileSize(fileInfo.getFileSize());
        attachment.setFileMd5(fileInfo.getMd5());
        attachment.setAttachmentType(attachmentType);
        attachment.setCreateBy(studentId);
        attachment.setCreateTime(LocalDateTime.now());

        int result = paperAttachmentMapper.insert(attachment);
        if (result <= 0) {
            throw new RuntimeException("附件记录创建失败");
        }

        log.info("附件上传成功 - 附件ID: {}, 论文ID: {}", attachment.getId(), paperId);
        return attachment;
    }

    public List<PaperAttachment> getPaperAttachments(Long paperId, Long studentId) {
        log.info("获取论文附件列表 - 论文ID: {}, 学生ID: {}", paperId, studentId);

        PaperInfo paperInfo = paperInfoMapper.selectById(paperId);
        if (paperInfo == null || !paperInfo.getStudentId().equals(studentId)) {
            throw new RuntimeException("论文不存在或无权限访问");
        }

        List<PaperAttachment> attachments = paperAttachmentMapper.selectList(
            new LambdaQueryWrapper<PaperAttachment>()
                .eq(PaperAttachment::getPaperId, paperId)
                .eq(PaperAttachment::getIsDeleted, 0)
                .orderByDesc(PaperAttachment::getCreateTime)
        );

        for (PaperAttachment attachment : attachments) {
            attachment.setFileSizeDesc(formatFileSize(attachment.getFileSize()));
        }

        return attachments;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAttachment(Long attachmentId, Long studentId) {
        log.info("开始删除附件 - 附件ID: {}, 学生ID: {}", attachmentId, studentId);

        PaperAttachment attachment = paperAttachmentMapper.selectById(attachmentId);
        if (attachment == null || attachment.getIsDeleted() == 1) {
            throw new RuntimeException("附件不存在");
        }

        if (!attachment.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权限删除此附件");
        }

        int result = paperAttachmentMapper.deleteById(attachmentId);
        boolean success = result > 0;

        if (success) {
            log.info("附件删除成功 - 附件ID: {}", attachmentId);
        }

        return success;
    }

    private String formatFileSize(Long fileSize) {
        if (fileSize == null) {
            return "0 B";
        }
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
