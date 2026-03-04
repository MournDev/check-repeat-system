package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.pojo.entity.*;
import com.abin.checkrepeatsystem.student.dto.ChatExportDTO;
import com.abin.checkrepeatsystem.student.dto.MessageSendDTO;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.student.service.StudentMessageService;
import com.abin.checkrepeatsystem.student.vo.AdvisorInfoVO;
import com.abin.checkrepeatsystem.student.vo.MessageSessionVO;
import com.abin.checkrepeatsystem.student.vo.MessageVO;
import com.abin.checkrepeatsystem.student.vo.SharedFileVO;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.user.service.Impl.UserQueryService;
import com.abin.checkrepeatsystem.user.service.ConversationService;
import com.abin.checkrepeatsystem.user.mapper.InstantMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 *学端消息服务实现类
 */
@Slf4j
@Service
public class StudentMessageServiceImpl extends ServiceImpl<PaperInfoMapper, PaperInfo> implements StudentMessageService {

    @Resource
    private InstantMessageMapper instantMessageMapper;
    
    @Resource
    private ConversationService conversationService;
    
    @Resource
    private FileService fileService;
    
    @Resource
    private UserQueryService userQueryService;
    
    @Resource
    private SysUserMapper sysUserMapper;
    
    @Value("${file.upload.base-path}")
    private String uploadBasePath;

    @Override
    public List<MessageSessionVO> getMessageSessions(Long studentId) {
        try {
            log.info("获取学生消息会话列表 -学生ID: {}", studentId);
            
            // 获取学生的论文信息，找到对应的导师
            LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
            paperWrapper.eq(PaperInfo::getStudentId, studentId)
                       .eq(PaperInfo::getIsDeleted, 0);
            List<PaperInfo> papers = this.list(paperWrapper);
            
            List<MessageSessionVO> sessions = new ArrayList<>();
            
            for (PaperInfo paper : papers) {
                if (paper.getTeacherId() != null) {
                    // 创建与导师的会话
                    MessageSessionVO session = new MessageSessionVO();
                    session.setId(paper.getId()); // 使用论文ID作为会话ID
                    session.setName("与" + paper.getTeacherName() + "的会话");
                    session.setType("PRIVATE");
                    
                    // 获取导师信息
                    SysUser teacher = sysUserMapper.selectById(paper.getTeacherId());
                    if (teacher != null) {
                        List<MessageSessionVO.SessionMemberVO> members = new ArrayList<>();
                        MessageSessionVO.SessionMemberVO teacherMember = new MessageSessionVO.SessionMemberVO();
                        teacherMember.setUserId(teacher.getId());
                        teacherMember.setUserName(teacher.getRealName());
                        teacherMember.setUserRole("TEACHER");
                        teacherMember.setAvatar(teacher.getAvatar());
                        members.add(teacherMember);
                        
                        MessageSessionVO.SessionMemberVO studentMember = new MessageSessionVO.SessionMemberVO();
                        studentMember.setUserId(studentId);
                        studentMember.setUserName("我");
                        studentMember.setUserRole("STUDENT");
                        studentMember.setAvatar(""); //学头像
                        members.add(studentMember);
                        
                        session.setMembers(members);
                    }
                    
                    // 获取最后一条消息
                    // 获取最后一条消息
                    InstantMessage lastMessage = getLastMessage(studentId, paper.getTeacherId());
                    if (lastMessage != null) {
                        session.setLastMessage(lastMessage.getContent());
                        session.setLastTime(lastMessage.getSentTime() != null ? 
                            lastMessage.getSentTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "");
                    } else {
                        session.setLastMessage("关于论文《" + paper.getPaperTitle() + "》的讨论");
                        session.setLastTime(paper.getSubmitTime() != null ? 
                            paper.getSubmitTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "");
                    }
                    
                    // 获取未读消息数
                    session.setUnreadCount(getUnreadMessageCount(studentId, paper.getTeacherId()));
                    session.setAvatar(paper.getTeacherName());
                    
                    sessions.add(session);
                }
            }
            
            log.info("获取消息会话列表成功 -学生ID: {}, 会话数量: {}", studentId, sessions.size());
            return sessions;
            
        } catch (Exception e) {
            log.error("获取消息会话列表失败 -学生ID: {}", studentId, e);
            throw new RuntimeException("获取会话列表失败: " + e.getMessage());
        }
    }

    @Override
    public Page<MessageVO> getMessageList(Long studentId, Long sessionId, Integer pageNum, Integer pageSize) {
        try {
            log.info("获取消息列表 -学生ID: {}, 会话ID: {}, 页码: {}, 页大小: {}", 
                    studentId, sessionId, pageNum, pageSize);
            
            // 获取会话中的具体消息列表
            LambdaQueryWrapper<InstantMessage> messageWrapper = new LambdaQueryWrapper<>();
            messageWrapper
                .and(wrapper -> wrapper
                    .eq(InstantMessage::getSenderId, studentId)
                    .eq(InstantMessage::getReceiverId, sessionId))
                .or(wrapper -> wrapper
                    .eq(InstantMessage::getSenderId, sessionId)
                    .eq(InstantMessage::getReceiverId, studentId))
                .eq(InstantMessage::getIsDeleted, 0)
                .orderByDesc(InstantMessage::getSentTime);
            
            // 使用分页查询
            Page<InstantMessage> messagePage = new Page<>(pageNum, pageSize);
            Page<InstantMessage> dbPage = instantMessageMapper.selectPage(messagePage, messageWrapper);
            
            //为VO
            List<MessageVO> messageVOs = dbPage.getRecords().stream()
                .map(this::convertToMessageVO)
                .collect(Collectors.toList());
            
            Page<MessageVO> page = new Page<>(pageNum, pageSize);
            page.setRecords(messageVOs);
            page.setTotal(dbPage.getTotal());
            
            log.info("获取消息列表成功");
            return page;
            
        } catch (Exception e) {
            log.error("获取消息列表失败", e);
            throw new RuntimeException("获取消息列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendMessage(Long studentId, MessageSendDTO sendDTO) {
        try {
            log.info("发送消息 - 学生ID: {}, 会话ID: {},接收者ID: {}", 
                    studentId, sendDTO.getSessionId(), sendDTO.getReceiverId());
            
            // 创建即时消息
            InstantMessage message = new InstantMessage();
            message.setSenderId(studentId);
            message.setReceiverId(sendDTO.getReceiverId());
            message.setConversationId(sendDTO.getSessionId());
            message.setContent(sendDTO.getContent());
            message.setMessageType(sendDTO.getMessageType());
            message.setContentType("TEXT");
            message.setStatus("SENT");
            message.setRelatedType(sendDTO.getRelatedType());
            message.setRelatedId(sendDTO.getRelatedId());
            message.setSentTime(LocalDateTime.now());
            message.setCreateTime(LocalDateTime.now());
            message.setUpdateTime(LocalDateTime.now());
            
            //直插入数据库
            int result = instantMessageMapper.insert(message);
            
            if (result > 0) {
                //为VO
                MessageVO messageVO = new MessageVO();
                messageVO.setId(message.getId());
                messageVO.setSenderId(studentId);
                messageVO.setSenderName("我");
                messageVO.setSenderRole("STUDENT");
                messageVO.setContent(sendDTO.getContent());
                messageVO.setSendTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                messageVO.setStatus("SENT");
                messageVO.setMessageType(sendDTO.getMessageType());
                
                log.info("消息发送成功 -消息ID: {}", message.getId());
                return messageVO;
            } else {
                throw new RuntimeException("消息发送失败: 数据库插入返回值为 " + result);
            }
            
        } catch (Exception e) {
            log.error("发送消息失败", e);
            throw new RuntimeException("消息发送失败: " + e.getMessage());
        }
    }

    @Override
    public com.abin.checkrepeatsystem.student.dto.FileUploadVO uploadFile(MultipartFile file, Long studentId) {
        try {
            log.info("上传文件 -学生ID: {}, 文件名: {}, 文件大小: {}", 
                    studentId, file.getOriginalFilename(), file.getSize());
            
            // 使用文件服务上传文件
            String fileId = fileService.uploadFile(file, studentId.toString());
            
            //构建响应VO
            com.abin.checkrepeatsystem.student.dto.FileUploadVO fileVO = 
                new com.abin.checkrepeatsystem.student.dto.FileUploadVO();
            fileVO.setId(fileId);
            fileVO.setName(file.getOriginalFilename());
            fileVO.setSize(file.getSize());
            fileVO.setType(getFileExtension(file.getOriginalFilename()));
            fileVO.setUrl("/api/student/messages/attachment/" + fileId);
            fileVO.setUploadTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info("文件上传成功 - 文件ID: {}", fileId);
            return fileVO;
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void downloadAttachment(String attachmentId, Long studentId, HttpServletResponse response) {
        try {
            log.info("下载附件 -学生ID: {}, 附件ID: {}", studentId, attachmentId);
            
            // 获取文件信息
            FileInfo fileInfo = fileService.getById(attachmentId);
            if (fileInfo != null && StringUtils.hasText(fileInfo.getStoragePath())) {
                String fullPath = Paths.get(uploadBasePath, fileInfo.getStoragePath()).toString();
                File file = new File(fullPath);
                
                if (file.exists()) {
                    // 设置响应头
                    String fileName = fileInfo.getOriginalFilename() != null ? 
                        fileInfo.getOriginalFilename() : "attachment_" + attachmentId;
                    response.setContentType(getContentType(fileName));
                    response.setHeader("Content-Disposition", 
                        "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"");
                    response.setContentLength((int) file.length());
                    
                    //写入文件内容
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            response.getOutputStream().write(buffer, 0, len);
                        }
                        response.getOutputStream().flush();
                    }
                    
                    log.info("附件下载成功 - 文件名: {}", fileName);
                } else {
                    throw new RuntimeException("附件文件不存在");
                }
            } else {
                throw new RuntimeException("附件信息不存在");
            }
            
        } catch (Exception e) {
            log.error("下载附件失败 - 附件ID: {}", attachmentId, e);
            throw new RuntimeException("附件下载失败: " + e.getMessage());
        }
    }

    @Override
    public void clearMessages(Long studentId, Long sessionId) {
        try {
            log.info("清空消息 -学生ID: {}, 会话ID: {}", studentId, sessionId);
            
            // 实现消息清空逻辑
            //将会话中所有消息标记为已删除
            LambdaQueryWrapper<InstantMessage> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper
                .and(wrapper -> wrapper
                    .eq(InstantMessage::getSenderId, studentId)
                    .eq(InstantMessage::getReceiverId, sessionId))
                .or(wrapper -> wrapper
                    .eq(InstantMessage::getSenderId, sessionId)
                    .eq(InstantMessage::getReceiverId, studentId))
                .eq(InstantMessage::getIsDeleted, 0);
            
            // 更新删除状态
            InstantMessage updateMessage = new InstantMessage();
            updateMessage.setIsDeleted(1);
            updateMessage.setUpdateTime(LocalDateTime.now());
            
            int deletedCount = instantMessageMapper.update(updateMessage, deleteWrapper);
            log.info("消息清空成功 - 删除消息数: {}", deletedCount);
            
        } catch (Exception e) {
            log.error("清空消息失败", e);
            throw new RuntimeException("清空消息失败: " + e.getMessage());
        }
    }

    @Override
    public void exportChatRecords(Long studentId, ChatExportDTO exportDTO, HttpServletResponse response) {
        try {
            log.info("导出聊天记录 - 学生ID: {}, 会话ID: {},格: {}", 
                    studentId, exportDTO.getSessionId(), exportDTO.getFormat());
            
            // 实现聊天记录导出逻辑
            // 查询会话中的所有消息
            LambdaQueryWrapper<InstantMessage> messageWrapper = new LambdaQueryWrapper<>();
            messageWrapper
                .and(wrapper -> wrapper
                    .eq(InstantMessage::getSenderId, studentId)
                    .eq(InstantMessage::getReceiverId, exportDTO.getSessionId()))
                .or(wrapper -> wrapper
                    .eq(InstantMessage::getSenderId, exportDTO.getSessionId())
                    .eq(InstantMessage::getReceiverId, studentId))
                .eq(InstantMessage::getIsDeleted, 0)
                .orderByAsc(InstantMessage::getSentTime);
                        
            List<InstantMessage> messages = instantMessageMapper.selectList(messageWrapper);
                        
            //根据格式生成导出内容
            String exportContent = generateChatExportContent(messages, exportDTO.getFormat());
                        
            // 设置响应头
            String fileName = "chat_export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + 
                             "." + exportDTO.getFormat();
            response.setContentType("application/octet-stream");
            String headerValue = "attachment; filename=" + fileName;
            response.setHeader("Content-Disposition", headerValue);
                        
            //写导出内容
            response.getWriter().write(exportContent);
            response.getWriter().flush();
                        
            log.info("聊天记录导出成功 - 消息数量: {},格: {}", messages.size(), exportDTO.getFormat());
            
        } catch (Exception e) {
            log.error("导出聊天记录失败", e);
            throw new RuntimeException("导出聊天记录失败: " + e.getMessage());
        }
    }

    @Override
    public List<SharedFileVO> getSharedFiles(Long studentId, Long sessionId) {
        try {
            log.info("获取共享文件列表 -学生ID: {}, 会话ID: {}", studentId, sessionId);
            
            // 实现共享文件列表获取逻辑
            //查询会话中的共享文件（通过附件消息）
            LambdaQueryWrapper<InstantMessage> fileWrapper = new LambdaQueryWrapper<>();
            fileWrapper
                .and(wrapper -> wrapper
                    .eq(InstantMessage::getSenderId, studentId)
                    .eq(InstantMessage::getReceiverId, sessionId))
                .or(wrapper -> wrapper
                    .eq(InstantMessage::getSenderId, sessionId)
                    .eq(InstantMessage::getReceiverId, studentId))
                .eq(InstantMessage::getIsDeleted, 0)
                .isNotNull(InstantMessage::getAttachments)
                .ne(InstantMessage::getAttachments, "")
                .orderByDesc(InstantMessage::getSentTime);
                        
            List<InstantMessage> messagesWithAttachments = instantMessageMapper.selectList(fileWrapper);
                        
            List<SharedFileVO> files = new ArrayList<>();
            for (InstantMessage message : messagesWithAttachments) {
                try {
                    //解析附件信息
                    if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        List<Map<String, Object>> attachments = 
                            objectMapper.readValue(message.getAttachments(), 
                                new TypeReference<List<Map<String, Object>>>() {});
                                    
                        for (Map<String, Object> attachment : attachments) {
                            SharedFileVO fileVO = new SharedFileVO();
                            fileVO.setId(String.valueOf(attachment.get("id")));
                            fileVO.setName((String) attachment.get("name"));
                            fileVO.setType(getFileExtension((String) attachment.get("name")));
                            fileVO.setSize(Long.valueOf(attachment.get("size").toString()));
                                        
                            //上传者信息
                            String senderName = "未知用户";
                            SysUser sender = sysUserMapper.selectById(message.getSenderId());
                            if (sender != null) {
                                senderName = sender.getRealName();
                            }
                            fileVO.setUploader(senderName);
                            fileVO.setUploadTime(message.getSentTime() != null ? 
                                message.getSentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");
                                        
                            //算计数 TODO:需要下载记录表
                            fileVO.setDownloadCount(0);
                                        
                            files.add(fileVO);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析附件信息失败 - 消息ID: {}", message.getId(), e);
                }
            }
            
            log.info("获取共享文件列表成功 - 文件数量: {}", files.size());
            return files;
            
        } catch (Exception e) {
            log.error("获取共享文件列表失败", e);
            throw new RuntimeException("获取共享文件列表失败: " + e.getMessage());
        }
    }

    @Override
    public void downloadSharedFile(String fileId, Long studentId, HttpServletResponse response) {
        try {
            log.info("下载共享文件 - 学生ID: {}, 文件ID: {}", studentId, fileId);
            
            //复附件下载逻辑
            downloadAttachment(fileId, studentId, response);
            
        } catch (Exception e) {
            log.error("下载共享文件失败 - 文件ID: {}", fileId, e);
            throw new RuntimeException("共享文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public void markMessagesRead(Long studentId, Long sessionId) {
        try {
            log.info("标记消息已读 -学生ID: {}, 会话ID: {}", studentId, sessionId);
            
            // 实现消息标记已读逻辑
            //将发送给学生的未读消息标记为已读
            LambdaQueryWrapper<InstantMessage> readWrapper = new LambdaQueryWrapper<>();
            readWrapper
                .eq(InstantMessage::getReceiverId, studentId)
                .eq(InstantMessage::getSenderId, sessionId)
                .eq(InstantMessage::getStatus, "SENT") // 未读状态
                .eq(InstantMessage::getIsDeleted, 0);
            
            // 更新状态为已读
            InstantMessage updateMessage = new InstantMessage();
            updateMessage.setStatus("READ");
            updateMessage.setReadTime(LocalDateTime.now());
            updateMessage.setUpdateTime(LocalDateTime.now());
            
            int updatedCount = instantMessageMapper.update(updateMessage, readWrapper);
            log.info("消息标记已读成功 - 更新消息数: {}", updatedCount);
            
        } catch (Exception e) {
            log.error("标记消息已读失败", e);
            throw new RuntimeException("标记消息已读失败: " + e.getMessage());
        }
    }

    @Override
    public void recallMessage(Long studentId, Long messageId) {
        try {
            log.info("撤回消息 -学生ID: {},消息ID: {}", studentId, messageId);
            
            // 实现消息撤回逻辑
            //检查消息是否可以撤回（必须是发送者且在2分钟内）
            InstantMessage message = instantMessageMapper.selectById(messageId);
            
            if (message == null) {
                throw new RuntimeException("消息不存在");
            }
            
            if (!message.getSenderId().equals(studentId)) {
                throw new RuntimeException("只能撤回自己发送的消息");
            }
            
            //检查时间限制（2分钟内）
            LocalDateTime now = LocalDateTime.now();
            if (message.getSentTime() != null && 
                message.getSentTime().plusMinutes(2).isBefore(now)) {
                throw new RuntimeException("消息发送超过2分钟，无法撤回");
            }
            
            // 更新消息状态为已撤回
            message.setStatus("RECALLED");
            message.setUpdateTime(LocalDateTime.now());
            message.setContent("[此消息已被撤回]");
            
            int result = instantMessageMapper.updateById(message);
            if (result <= 0) {
                throw new RuntimeException("消息撤回失败");
            }
            
            log.info("消息撤回成功 - 消息ID: {}", messageId);
            
        } catch (Exception e) {
            log.error("撤回消息失败", e);
            throw new RuntimeException("消息撤回失败: " + e.getMessage());
        }
    }

    @Override
    public AdvisorInfoVO getAdvisorInfo(Long studentId) {
        try {
            log.info("获取导师信息 - 学生ID: {}", studentId);
            
            // 获取学生的论文信息
            LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
            paperWrapper.eq(PaperInfo::getStudentId, studentId)
                       .eq(PaperInfo::getIsDeleted, 0)
                       .isNotNull(PaperInfo::getTeacherId);
            List<PaperInfo> papers = this.list(paperWrapper);
            
            if (papers.isEmpty()) {
                throw new RuntimeException("未找到指导老师信息");
            }
            
            // 获取第一个论文的导师信息
            PaperInfo paper = papers.get(0);
            SysUser teacher = sysUserMapper.selectById(paper.getTeacherId());
            
            if (teacher == null) {
                throw new RuntimeException("导师信息不存在");
            }
            
            //构建导师信息VO
            AdvisorInfoVO advisorInfo = new AdvisorInfoVO();
            advisorInfo.setId(teacher.getId().toString());
            advisorInfo.setName(teacher.getRealName());
            advisorInfo.setTitle(teacher.getProfessionalTitle() != null ? teacher.getProfessionalTitle() : "讲师");
            advisorInfo.setResearchField(teacher.getResearchDirection());
            advisorInfo.setEmail(teacher.getEmail());
            advisorInfo.setPhone(teacher.getPhone());
            advisorInfo.setOffice(teacher.getOffice());
            advisorInfo.setOfficeHours(teacher.getOfficeHours());
            advisorInfo.setAvatar(teacher.getAvatar());
            advisorInfo.setBio(teacher.getIntroduce());
            advisorInfo.setCollege(teacher.getCollegeName());
            
            //统计指导学生数
            LambdaQueryWrapper<PaperInfo> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(PaperInfo::getTeacherId, teacher.getId())
                       .eq(PaperInfo::getIsDeleted, 0);
            int studentCount = Math.toIntExact(this.count(countWrapper));
            advisorInfo.setStudentCount(studentCount);
            
            log.info("获取导师信息成功 -导师ID: {}", teacher.getId());
            return advisorInfo;
            
        } catch (Exception e) {
            log.error("获取导师信息失败", e);
            throw new RuntimeException("获取导师信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * 生成聊天记录导出内容
     */
    private String generateChatExportContent(List<InstantMessage> messages, String format) {
        StringBuilder content = new StringBuilder();
        
        if ("txt".equalsIgnoreCase(format)) {
            // TXT格式
            content.append("聊天记录导出\n");
            content.append("导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            content.append("========================================\n\n");
            
            for (InstantMessage message : messages) {
                String senderName = message.getSenderId() != null ? 
                    "用户" + message.getSenderId() : "未知用户";
                String time = message.getSentTime() != null ? 
                    message.getSentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
                
                content.append("[").append(time).append("] ").append(senderName).append(":\n");
                content.append(message.getContent()).append("\n\n");
            }
            
        } else {
            // PDF/DOC格式（简化为HTML格式，便于转换）
            content.append("<html><head><meta charset=UTF-8><title>聊天记录</title></head><body>");
            content.append("<h1>聊天记录导出</h1>");
            content.append("<p>导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</p>");
            content.append("<hr>");
            
            for (InstantMessage message : messages) {
                String senderName = message.getSenderId() != null ? 
                    "用户" + message.getSenderId() : "未知用户";
                String time = message.getSentTime() != null ? 
                    message.getSentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
                
                content.append("<div style=margin: 10px 0; padding: 10px; border: 1px solid #ccc;>");
                content.append("<strong>").append(time).append(" ").append(senderName).append(":</strong><br>");
                content.append(message.getContent());
                content.append("</div>");
            }
            
            content.append("</body></html>");
        }
        
        return content.toString();
    }
    
    /**
     *将InstantMessage转换为MessageVO
     */
    private MessageVO convertToMessageVO(InstantMessage message) {
        MessageVO messageVO = new MessageVO();
        messageVO.setId(message.getId());
        messageVO.setSenderId(message.getSenderId());
        messageVO.setSenderName(message.getSenderName() != null ? message.getSenderName() : "未知用户");
        messageVO.setSenderRole(message.getSenderId() != null ? "STUDENT" : "TEACHER"); //简化处理
        messageVO.setContent(message.getContent());
        messageVO.setSendTime(message.getSentTime() != null ? 
            message.getSentTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "");
        messageVO.setStatus(message.getStatus());
        messageVO.setMessageType(message.getMessageType());
        messageVO.setSenderAvatar(message.getSenderAvatar());
        return messageVO;
    }
    
    /**
     * 获取最后一条消息
     */
    private InstantMessage getLastMessage(Long studentId, Long teacherId) {
        try {
            LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper
                .and(w -> w
                    .eq(InstantMessage::getSenderId, studentId)
                    .eq(InstantMessage::getReceiverId, teacherId))
                .or(w -> w
                    .eq(InstantMessage::getSenderId, teacherId)
                    .eq(InstantMessage::getReceiverId, studentId))
                .eq(InstantMessage::getIsDeleted, 0)
                .orderByDesc(InstantMessage::getSentTime)
                .last("LIMIT 1");
            
            return instantMessageMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.warn("获取最后消息失败", e);
            return null;
        }
    }
    
    /**
     * 获取未读消息数
     */
    private Integer getUnreadMessageCount(Long studentId, Long teacherId) {
        try {
            LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper
                .eq(InstantMessage::getReceiverId, studentId)
                .eq(InstantMessage::getSenderId, teacherId)
                .eq(InstantMessage::getStatus, "SENT") // 未读状态
                .eq(InstantMessage::getIsDeleted, 0);
            
            return Math.toIntExact(instantMessageMapper.selectCount(wrapper));
        } catch (Exception e) {
            log.warn("获取未读消息数失败", e);
            return 0;
        }
    }
        
    /**
     *根据文件名获取内容类型
     */
    private String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerName.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else {
            return "application/octet-stream";
        }
    }
}