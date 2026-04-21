package com.abin.checkrepeatsystem.teacher.service.Impl;

import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.common.websocket.WebSocketMessage;
import com.abin.checkrepeatsystem.common.websocket.WebSocketMessageType;
import com.abin.checkrepeatsystem.common.websocket.WebSocketSender;
import com.abin.checkrepeatsystem.pojo.entity.*;
import com.abin.checkrepeatsystem.pojo.entity.FileInfo;
import com.abin.checkrepeatsystem.student.dto.ChatExportDTO;
import com.abin.checkrepeatsystem.student.dto.MessageSendDTO;
import com.abin.checkrepeatsystem.student.vo.MessageSessionVO;
import com.abin.checkrepeatsystem.student.vo.MessageVO;
import com.abin.checkrepeatsystem.student.vo.SharedFileVO;
import com.abin.checkrepeatsystem.teacher.service.TeacherMessageService;
import com.abin.checkrepeatsystem.mapper.SysUserMapper;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.user.mapper.ConversationMapper;
import com.abin.checkrepeatsystem.user.mapper.ConversationMemberMapper;
import com.abin.checkrepeatsystem.user.mapper.InstantMessageMapper;
import com.abin.checkrepeatsystem.user.service.ConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 教师消息服务实现类
 */
@Slf4j
@Service
public class TeacherMessageServiceImpl implements TeacherMessageService {

    @Resource
    private InstantMessageMapper instantMessageMapper;

    @Resource
    private FileService fileService;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private ConversationMemberMapper conversationMemberMapper;

    @Value("${file.upload.base-path}")
    private String uploadBasePath;

    @Resource
    private WebSocketSender webSocketSender;
    
    @Resource
    private PaperInfoMapper paperInfoMapper;


    @Override
    public List<MessageSessionVO> getMessageSessions(Long teacherId) {
        try {
            log.info("获取教师消息会话列表 - 教师 ID: {}", teacherId);

            // 1. 获取教师的所有学生 ID（通过论文关联）
            LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
            paperWrapper.eq(PaperInfo::getTeacherId, teacherId)
                       .eq(PaperInfo::getIsDeleted, 0)
                       .isNotNull(PaperInfo::getStudentId);
            List<PaperInfo> papers = paperInfoMapper.selectList(paperWrapper);
                
            // 2. 去重：同一个学生只创建一个会话（不管有多少篇论文）
            Map<Long, String> studentMap = new LinkedHashMap<>(); // studentId -> studentName
            for (PaperInfo paper : papers) {
                if (!studentMap.containsKey(paper.getStudentId())) {
                    studentMap.put(paper.getStudentId(), paper.getStudentName());
                }
            }
                
            log.info("教师 {} 共有 {} 位学生，将会话数量：{}", teacherId, studentMap.size(), studentMap.size());

            // 3. 为每位学生创建会话
            List<MessageSessionVO> sessions = new ArrayList<>();

            for (Map.Entry<Long, String> entry : studentMap.entrySet()) {
                Long studentId = entry.getKey();
                String studentName = entry.getValue();
                Long conversationId = null;

                // 检查会话是否已经存在于数据库中
                com.abin.checkrepeatsystem.pojo.entity.Conversation existingConversation = null;
                try {
                    // 先查询 conversation_members 表，看看是否已经存在一个会话，其中包含了指定的教师和学生
                    LambdaQueryWrapper<ConversationMember> memberWrapper1 = new LambdaQueryWrapper<>();
                    memberWrapper1.eq(ConversationMember::getUserId, teacherId);
                    List<ConversationMember> teacherMembers = conversationMemberMapper.selectList(memberWrapper1);

                    for (ConversationMember teacherMember : teacherMembers) {
                        Long convId = teacherMember.getConversationId();
                        LambdaQueryWrapper<ConversationMember> memberWrapper2 = new LambdaQueryWrapper<>();
                        memberWrapper2.eq(ConversationMember::getConversationId, convId)
                                     .eq(ConversationMember::getUserId, studentId);
                        Long count = conversationMemberMapper.selectCount(memberWrapper2);
                        if (count > 0) {
                            // 找到一个包含教师和学生的会话
                            existingConversation = conversationMapper.selectById(convId);
                            break;
                        }
                    }

                    // 如果没有找到，再根据消息的 conversationId 查询
                    if (existingConversation == null && conversationId != null) {
                        existingConversation = conversationMapper.selectById(conversationId);
                    }
                } catch (Exception e) {
                    log.warn("查询会话失败，可能会话不存在: {}", e.getMessage());
                }

                // 如果会话不存在，创建会话
                if (existingConversation == null) {
                    log.info("会话不存在，创建新会话");
                    
                    // 创建会话（ID、审计字段由MyBatis-Plus自动维护）
                    Conversation conversation = new Conversation();
                    conversation.setName("与" + studentName + "的会话");
                    conversation.setType("PRIVATE");
                    conversation.setCreatorId(teacherId);
                    conversation.setLastActiveTime(LocalDateTime.now());
                    conversation.setLastMessageTime(LocalDateTime.now());

                    // 保存会话（自动生成雪花ID和审计字段）
                    conversationMapper.insert(conversation);
                    Long newConversationId = conversation.getId();
                    log.info("会话创建成功: {}", newConversationId);

                    // 关联历史消息到新创建的会话
                    associateHistoricalMessages(newConversationId, teacherId, studentId);

                    // 添加会话成员（审计字段由MyBatis-Plus自动填充）
                    ConversationMember teacherMember = new ConversationMember();
                    teacherMember.setConversationId(newConversationId);
                    teacherMember.setUserId(teacherId);
                    teacherMember.setRole("MEMBER");
                    teacherMember.setJoinedAt(LocalDateTime.now());
                    teacherMember.setIsLeft(0);
                    teacherMember.setUnreadCount(0);
                    conversationMemberMapper.insert(teacherMember);

                    ConversationMember studentMember = new ConversationMember();
                    studentMember.setConversationId(newConversationId);
                    studentMember.setUserId(studentId);
                    studentMember.setRole("MEMBER");
                    studentMember.setJoinedAt(LocalDateTime.now());
                    studentMember.setIsLeft(0);
                    studentMember.setUnreadCount(0);
                    conversationMemberMapper.insert(studentMember);

                    log.info("会话成员添加成功: 教师 {}，学生 {}", teacherId, studentId);

                    // 更新conversationId为新生成的ID
                    conversationId = newConversationId;
                } else {
                    log.info("会话已存在: {}", existingConversation.getId());
                    conversationId = existingConversation.getId();
                }

                // 获取学生信息
                SysUser student = sysUserMapper.selectById(studentId);
                if (student == null) continue;

                MessageSessionVO session = new MessageSessionVO();
                session.setId(conversationId);
                session.setName("与" + student.getRealName() + "的会话");
                session.setType("PRIVATE");

                List<MessageSessionVO.SessionMemberVO> members = new ArrayList<>();

                // 获取当前教师信息
                SysUser teacher = sysUserMapper.selectById(teacherId);
                String teacherName = teacher != null ? teacher.getRealName() : "未知教师";

                if (student != null) {
                    MessageSessionVO.SessionMemberVO studentMember = new MessageSessionVO.SessionMemberVO();
                    studentMember.setUserId(student.getId());
                    studentMember.setUserName(student.getRealName());
                    studentMember.setUserRole("STUDENT");
                    studentMember.setAvatar(student.getAvatar());
                    members.add(studentMember);

                    MessageSessionVO.SessionMemberVO teacherMember = new MessageSessionVO.SessionMemberVO();
                    teacherMember.setUserId(teacherId);
                    teacherMember.setUserName(teacherName);
                    teacherMember.setUserRole("TEACHER");
                    teacherMember.setAvatar(teacher != null ? teacher.getAvatar() : "");
                    members.add(teacherMember);

                    session.setMembers(members);
                }

                // 获取最后一条消息
                InstantMessage lastMessage = getLastMessage(teacherId, studentId);
                
                // 设置最后一条消息
                if (lastMessage != null) {
                    session.setLastMessage(lastMessage.getContent());
                    session.setLastTime(lastMessage.getSentTime() != null ?
                        lastMessage.getSentTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "");
                } else {
                    session.setLastMessage("暂无消息");
                    session.setLastTime(null);
                }

                // 获取未读消息数
                session.setUnreadCount(getUnreadMessageCount(teacherId, studentId));

                // 设置会话头像为学生的头像 URL
                if (!members.isEmpty() && members.get(0).getAvatar() != null) {
                    session.setAvatar(members.get(0).getAvatar());
                } else {
                    session.setAvatar(null);
                }

                sessions.add(session);
            }

            log.info("获取消息会话列表成功 - 教师 ID: {}, 会话数量：{}", teacherId, sessions.size());
            return sessions;

        } catch (Exception e) {
            log.error("获取消息会话列表失败 - 教师 ID: {}", teacherId, e);
            throw new RuntimeException("获取会话列表失败: " + e.getMessage());
        }
    }

    @Override
    public Page<MessageVO> getMessageList(Long teacherId, Long sessionId, Integer pageNum, Integer pageSize) {
        try {
            log.info("获取消息列表 - 教师 ID: {}, 会话 ID: {}, 页码: {}, 页大小: {}",
                    teacherId, sessionId, pageNum, pageSize);

            // 使用 MyBatis-Plus 的标准方法获取会话消息历史
            Page<InstantMessage> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InstantMessage::getConversationId, sessionId)
                    .eq(InstantMessage::getIsDeleted, 0)
                    .orderByAsc(InstantMessage::getSentTime);

            IPage<InstantMessage> messagePage = instantMessageMapper.selectPage(page, wrapper);

            List<InstantMessage> messages = messagePage.getRecords();

            // 转换为 VO
            List<MessageVO> messageVOs = messages.stream()
                .map(this::convertToMessageVO)
                .collect(Collectors.toList());

            Page<MessageVO> pageVO = new Page<>(pageNum, pageSize);
            pageVO.setRecords(messageVOs);
            pageVO.setTotal(messagePage.getTotal());

            log.info("获取消息列表成功");
            return pageVO;

        } catch (Exception e) {
            log.error("获取消息列表失败", e);
            throw new RuntimeException("获取消息列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageVO sendMessage(Long teacherId, MessageSendDTO sendDTO) {
        try {
            // 使用当前登录用户 ID
            Long currentUserId = UserBusinessInfoUtils.getCurrentUserId();

            log.info("发送消息 - 当前用户 ID: {}, 会话 ID: {}, 接收者 ID: {}",
                    currentUserId, sendDTO.getSessionId(), sendDTO.getReceiverId());

            // 确保 conversation_id 正确设置
            InstantMessage message = new InstantMessage();
            message.setSenderId(currentUserId);
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

            // 直接插入数据库
            int result = instantMessageMapper.insert(message);

            if (result > 0) {
                // 更新会话的最后消息信息
                updateConversationLastMessage(sendDTO.getSessionId());

                // 转换为 VO
                MessageVO messageVO = new MessageVO();
                messageVO.setId(message.getId());
                messageVO.setSenderId(currentUserId);

                // 获取真实的教师姓名
                SysUser currentUser = sysUserMapper.selectById(currentUserId);
                String realSenderName = currentUser != null ? currentUser.getRealName() : "未知用户";
                messageVO.setSenderName(realSenderName);
                messageVO.setSenderAvatar(currentUser != null ? currentUser.getAvatar() : null); // ✅ 添加 senderAvatar 字段

                messageVO.setSenderRole("TEACHER");
                messageVO.setContent(sendDTO.getContent());
                String sendTimeStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                messageVO.setSendTime(sendTimeStr);
                messageVO.setFormattedTime(messageVO.getFormattedTime());
                messageVO.setStatus("SENT");
                messageVO.setMessageType(sendDTO.getMessageType());

                // 设置 sender 字段，兼容前端判断
                messageVO.setSender("advisor");
                messageVO.setSessionId(sendDTO.getSessionId());
                messageVO.setConversationId(sendDTO.getSessionId());

                // 【WebSocket推送】发送消息后，通过WebSocket推送新消息给接收者
                try {
                    WebSocketMessage wsMessage = new WebSocketMessage();
                    wsMessage.setType(WebSocketMessageType.NEW_MESSAGE.getValue());
                    wsMessage.setContent(messageVO);
                    wsMessage.setTimestamp(System.currentTimeMillis());
                    webSocketSender.sendToUser(sendDTO.getReceiverId(), wsMessage);
                    log.info("WebSocket消息推送成功 - 接收者ID: {}", sendDTO.getReceiverId());
                } catch (Exception e) {
                    log.error("WebSocket消息推送失败 - 接收者ID: {}", sendDTO.getReceiverId(), e);
                }

                // 如果有附件，也设置到 VO 中
                if (sendDTO.getAttachmentIds() != null && !sendDTO.getAttachmentIds().isEmpty()) {
                    // TODO: 加载附件信息并设置到 messageVO
                }

                log.info("消息发送成功 - 消息 ID: {}, 发送者：{}", message.getId(), realSenderName);
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
    public com.abin.checkrepeatsystem.student.dto.FileUploadVO uploadFile(MultipartFile file, Long teacherId) {
        try {
            log.info("上传文件 - 教师 ID: {}, 文件名: {}, 文件大小: {}",
                    teacherId, file.getOriginalFilename(), file.getSize());

            // 使用文件服务上传文件
            Long fileId = fileService.uploadFile(file, teacherId);

            // 构建响应 VO
            com.abin.checkrepeatsystem.student.dto.FileUploadVO fileVO =
                new com.abin.checkrepeatsystem.student.dto.FileUploadVO();
            fileVO.setId(fileId);
            fileVO.setName(file.getOriginalFilename());
            fileVO.setSize(file.getSize());
            fileVO.setType(getFileExtension(file.getOriginalFilename()));
            fileVO.setUrl("/api/teacher/messages/attachment/" + fileId);
            fileVO.setUploadTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            log.info("文件上传成功 - 文件 ID: {}", fileId);
            return fileVO;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void downloadAttachment(Long attachmentId, Long teacherId, HttpServletResponse response) {
        try {
            log.info("下载附件 - 教师 ID: {}, 附件 ID: {}", teacherId, attachmentId);

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

                    // 写入文件内容
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
            log.error("下载附件失败 - 附件 ID: {}", attachmentId, e);
            throw new RuntimeException("附件下载失败: " + e.getMessage());
        }
    }

    @Override
    public void clearMessages(Long teacherId, Long sessionId) {
        try {
            log.info("清空消息 - 教师 ID: {}, 会话 ID: {}", teacherId, sessionId);

            // 将会话中所有消息标记为已删除
            LambdaQueryWrapper<InstantMessage> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper
                .eq(InstantMessage::getConversationId, sessionId)
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
    public void exportChatRecords(Long teacherId, ChatExportDTO exportDTO, HttpServletResponse response) {
        try {
            log.info("导出聊天记录 - 教师 ID: {}, 会话 ID: {}, 格式: {}",
                    teacherId, exportDTO.getSessionId(), exportDTO.getFormat());

            // 查询会话中的所有消息
            LambdaQueryWrapper<InstantMessage> messageWrapper = new LambdaQueryWrapper<>();
            messageWrapper
                .eq(InstantMessage::getConversationId, exportDTO.getSessionId())
                .eq(InstantMessage::getIsDeleted, 0)
                .orderByAsc(InstantMessage::getSentTime);

            List<InstantMessage> messages = instantMessageMapper.selectList(messageWrapper);

            // 根据格式生成导出内容
            String exportContent = generateChatExportContent(messages, exportDTO.getFormat());

            // 设置响应头
            String fileName = "chat_export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                             "." + exportDTO.getFormat();
            response.setContentType("application/octet-stream");
            String headerValue = "attachment; filename=" + fileName;
            response.setHeader("Content-Disposition", headerValue);

            // 写入导出内容
            response.getWriter().write(exportContent);
            response.getWriter().flush();

            log.info("聊天记录导出成功 - 消息数量: {}, 格式: {}", messages.size(), exportDTO.getFormat());

        } catch (Exception e) {
            log.error("导出聊天记录失败", e);
            throw new RuntimeException("导出聊天记录失败: " + e.getMessage());
        }
    }

    @Override
    public List<SharedFileVO> getSharedFiles(Long teacherId, Long sessionId) {
        try {
            log.info("获取共享文件列表 - 教师 ID: {}, 会话 ID: {}", teacherId, sessionId);

            // 查询会话中的共享文件（通过附件消息）
            LambdaQueryWrapper<InstantMessage> fileWrapper = new LambdaQueryWrapper<>();
            fileWrapper
                .eq(InstantMessage::getConversationId, sessionId)
                .eq(InstantMessage::getIsDeleted, 0)
                .isNotNull(InstantMessage::getAttachments)
                .ne(InstantMessage::getAttachments, "")
                .orderByDesc(InstantMessage::getSentTime);

            List<InstantMessage> messagesWithAttachments = instantMessageMapper.selectList(fileWrapper);

            List<SharedFileVO> files = new ArrayList<>();
            for (InstantMessage message : messagesWithAttachments) {
                try {
                    // 解析附件信息
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

                            // 上传者信息
                            String senderName = "未知用户";
                            SysUser sender = sysUserMapper.selectById(message.getSenderId());
                            if (sender != null) {
                                senderName = sender.getRealName();
                            }
                            fileVO.setUploader(senderName);
                            fileVO.setUploadTime(message.getSentTime() != null ?
                                message.getSentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "");

                            // 计算下载次数 TODO: 需要下载记录表
                            fileVO.setDownloadCount(0);

                            files.add(fileVO);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析附件信息失败 - 消息 ID: {}", message.getId(), e);
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
    public void downloadSharedFile(Long fileId, Long teacherId, HttpServletResponse response) {
        try {
            log.info("下载共享文件 - 教师 ID: {}, 文件 ID: {}", teacherId, fileId);

            // 复用附件下载逻辑
            downloadAttachment(fileId, teacherId, response);

        } catch (Exception e) {
            log.error("下载共享文件失败 - 文件 ID: {}", fileId, e);
            throw new RuntimeException("共享文件下载失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessagesRead(Long teacherId, Long sessionId) {
        try {
            log.info("标记消息已读 - 教师 ID: {}, 会话 ID: {}", teacherId, sessionId);

            // 验证会话是否存在
            LambdaQueryWrapper<InstantMessage> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(InstantMessage::getConversationId, sessionId)
                       .last("LIMIT 1");

            InstantMessage existMessage = instantMessageMapper.selectOne(checkWrapper);
            if (existMessage == null) {
                log.warn("会话不存在或没有消息 - 会话 ID: {}", sessionId);
                return; // 幂等性处理
            }

            // 从历史消息中获取学生 ID
            Long studentId = null;
            if (existMessage.getSenderId().equals(teacherId)) {
                studentId = existMessage.getReceiverId();
            } else {
                studentId = existMessage.getSenderId();
            }

            log.info("从历史消息中获取学生 ID: {}", studentId);

            // 将发送给教师的未读消息标记为已读
            LambdaQueryWrapper<InstantMessage> readWrapper = new LambdaQueryWrapper<>();
            readWrapper
                .eq(InstantMessage::getReceiverId, teacherId)
                .eq(InstantMessage::getSenderId, studentId)  // 学生发送的消息
                .eq(InstantMessage::getStatus, "SENT") // 未读状态
                .eq(InstantMessage::getIsDeleted, 0);

            log.info("查询条件 - receiverId: {}, senderId: {}, status: SENT", teacherId, studentId);

            // 更新状态为已读
            InstantMessage updateMessage = new InstantMessage();
            updateMessage.setStatus("READ");
            updateMessage.setReadTime(LocalDateTime.now());
            updateMessage.setUpdateTime(LocalDateTime.now());

            int updatedCount = instantMessageMapper.update(updateMessage, readWrapper);
            log.info("消息标记已读成功 - 更新消息数：{}", updatedCount);

        } catch (Exception e) {
            log.error("标记消息已读失败", e);
            throw new RuntimeException("标记消息已读失败：" + e.getMessage());
        }
    }

    @Override
    public void recallMessage(Long teacherId, Long messageId) {
        try {
            log.info("撤回消息 - 教师 ID: {}, 消息 ID: {}", teacherId, messageId);

            // 检查消息是否可以撤回（必须是发送者且在2分钟内）
            InstantMessage message = instantMessageMapper.selectById(messageId);

            if (message == null) {
                throw new RuntimeException("消息不存在");
            }

            if (!message.getSenderId().equals(teacherId)) {
                throw new RuntimeException("只能撤回自己发送的消息");
            }

            // 检查时间限制（2分钟内）
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

            log.info("消息撤回成功 - 消息 ID: {}", messageId);

        } catch (Exception e) {
            log.error("撤回消息失败", e);
            throw new RuntimeException("消息撤回失败: " + e.getMessage());
        }
    }

    @Override
    public com.abin.checkrepeatsystem.student.vo.StudentInfoVO getStudentInfo(Long teacherId, Long studentId) {
        try {
            log.info("获取学生信息 - 教师 ID: {}, 学生 ID: {}", teacherId, studentId);

            // 获取学生信息
            SysUser student = sysUserMapper.selectById(studentId);
            if (student == null) {
                throw new RuntimeException("学生信息不存在");
            }

            // 构建学生信息 VO
            com.abin.checkrepeatsystem.student.vo.StudentInfoVO studentInfo = new com.abin.checkrepeatsystem.student.vo.StudentInfoVO();
            studentInfo.setId(student.getId().toString());
            studentInfo.setName(student.getRealName());
            studentInfo.setEmail(student.getEmail());
            studentInfo.setPhone(student.getPhone());
            studentInfo.setAvatar(student.getAvatar() != null && !student.getAvatar().trim().isEmpty() ? student.getAvatar() : null);

            log.info("获取学生信息成功 - 学生 ID: {}", studentId);
            return studentInfo;

        } catch (Exception e) {
            log.error("获取学生信息失败", e);
            throw new RuntimeException("获取学生信息失败：" + e.getMessage());
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
            content.append("导出时间: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
            content.append("========================================\n\n");

            for (InstantMessage message : messages) {
                String senderName = message.getSenderId() != null ?
                    "用户" + message.getSenderId() : "未知用户";
                String time = message.getSentTime() != null ?
                    message.getSentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";

                content.append("[" + time + "] " + senderName + ":\n");
                content.append(message.getContent() + "\n\n");
            }

        } else {
            // PDF/DOC格式（简化为HTML格式，便于转换）
            content.append("<html><head><meta charset=UTF-8><title>聊天记录</title></head><body>");
            content.append("<h1>聊天记录导出</h1>");
            content.append("<p>导出时间: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "</p>");
            content.append("<hr>");

            for (InstantMessage message : messages) {
                String senderName = message.getSenderId() != null ?
                    "用户" + message.getSenderId() : "未知用户";
                String time = message.getSentTime() != null ?
                    message.getSentTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";

                content.append("<div style=margin: 10px 0; padding: 10px; border: 1px solid #ccc;>");
                content.append("<strong>" + time + " " + senderName + ":</strong><br>");
                content.append(message.getContent());
                content.append("</div>");
            }

            content.append("</body></html>");
        }

        return content.toString();
    }

    /**
     * 将 InstantMessage 转换为 MessageVO
     */
    private MessageVO convertToMessageVO(InstantMessage message) {
        MessageVO messageVO = new MessageVO();
        messageVO.setId(message.getId());
        messageVO.setSenderId(message.getSenderId());

        // 根据 senderId 查询用户信息
        if (message.getSenderId() != null) {
            SysUser sender = sysUserMapper.selectById(message.getSenderId());
            if (sender != null) {
                messageVO.setSenderName(sender.getRealName());
                messageVO.setSenderRole(sender.getUserType() != null ? sender.getUserType() : "STUDENT");
                messageVO.setSenderAvatar(sender.getAvatar());
            } else {
                messageVO.setSenderName("未知用户");
                messageVO.setSenderRole("STUDENT");
                messageVO.setSenderAvatar(null);
            }
        } else {
            messageVO.setSenderName("未知用户");
            messageVO.setSenderRole("STUDENT");
            messageVO.setSenderAvatar(null);
        }

        messageVO.setContent(message.getContent());

        if (message.getSentTime() != null) {
            String sendTimeStr = message.getSentTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            messageVO.setSendTime(sendTimeStr);
            messageVO.setFormattedTime(messageVO.getFormattedTime());
        } else {
            messageVO.setSendTime("");
            messageVO.setFormattedTime("");
        }

        messageVO.setStatus(message.getStatus());
        messageVO.setMessageType(message.getMessageType());
        return messageVO;
    }

    /**
     * 关联历史消息到会话
     */
    private void associateHistoricalMessages(Long conversationId, Long userId1, Long userId2) {
        try {
            LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.and(w -> w
                    .eq(InstantMessage::getSenderId, userId1)
                    .eq(InstantMessage::getReceiverId, userId2))
                .or(w -> w
                    .eq(InstantMessage::getSenderId, userId2)
                    .eq(InstantMessage::getReceiverId, userId1))
                .isNull(InstantMessage::getConversationId)
                .eq(InstantMessage::getIsDeleted, 0);

            InstantMessage updateMessage = new InstantMessage();
            updateMessage.setConversationId(conversationId);
            updateMessage.setUpdateTime(LocalDateTime.now());

            int updatedCount = instantMessageMapper.update(updateMessage, wrapper);
            if (updatedCount > 0) {
                log.info("已关联 {} 条历史消息到会话 {}", updatedCount, conversationId);

                // 更新会话的最后消息时间
                updateConversationLastMessage(conversationId);
            }
        } catch (Exception e) {
            log.error("关联历史消息失败 - 会话 ID: {}", conversationId, e);
        }
    }

    /**
     * 更新会话的最后消息信息
     */
    private void updateConversationLastMessage(Long conversationId) {
        try {
            LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InstantMessage::getConversationId, conversationId)
                .eq(InstantMessage::getIsDeleted, 0)
                .orderByDesc(InstantMessage::getSentTime)
                .last("LIMIT 1");

            InstantMessage lastMessage = instantMessageMapper.selectOne(wrapper);
            if (lastMessage != null) {
                Conversation conversation = new Conversation();
                conversation.setId(conversationId);
                conversation.setLastMessageId(lastMessage.getId());
                conversation.setLastMessageTime(lastMessage.getSentTime());
                conversation.setLastActiveTime(lastMessage.getSentTime());
                conversationMapper.updateById(conversation);

                log.info("已更新会话 {} 的最后消息信息", conversationId);
            }
        } catch (Exception e) {
            log.error("更新会话最后消息失败 - 会话 ID: {}", conversationId, e);
        }
    }

    /**
     * 获取最后一条消息（基于师生关系）
     *
     * @param teacherId 教师 ID
     * @param studentId 学生 ID
     * @return 最后一条消息
     */
    private InstantMessage getLastMessage(Long teacherId, Long studentId) {
        try {
            LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper
                .and(w -> w
                    .eq(InstantMessage::getSenderId, teacherId)
                    .eq(InstantMessage::getReceiverId, studentId))
                .or(w -> w
                    .eq(InstantMessage::getSenderId, studentId)
                    .eq(InstantMessage::getReceiverId, teacherId))
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
     * 获取未读消息数（基于师生关系）
     *
     * @param teacherId 教师 ID
     * @param studentId 学生 ID
     * @return 未读消息数量
     */
    private Integer getUnreadMessageCount(Long teacherId, Long studentId) {
        try {
            LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper
                .eq(InstantMessage::getReceiverId, teacherId)
                .eq(InstantMessage::getSenderId, studentId)
                .eq(InstantMessage::getStatus, "SENT") // 未读状态
                .eq(InstantMessage::getIsDeleted, 0);

            return Math.toIntExact(instantMessageMapper.selectCount(wrapper));
        } catch (Exception e) {
            log.warn("获取未读消息数失败", e);
            return 0;
        }
    }

    /**
     * 根据文件名获取内容类型
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
