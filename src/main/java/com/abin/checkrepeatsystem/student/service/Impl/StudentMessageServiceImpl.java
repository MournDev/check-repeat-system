package com.abin.checkrepeatsystem.student.service.Impl;

import com.abin.checkrepeatsystem.common.service.FileService;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.common.websocket.WebSocketMessage;
import com.abin.checkrepeatsystem.common.websocket.WebSocketMessageType;
import com.abin.checkrepeatsystem.common.websocket.WebSocketSender;
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
import com.abin.checkrepeatsystem.user.mapper.ConversationMapper;
import com.abin.checkrepeatsystem.user.mapper.ConversationMemberMapper;
import com.abin.checkrepeatsystem.user.mapper.InstantMessageMapper;
import com.abin.checkrepeatsystem.user.service.ConversationService;
import com.abin.checkrepeatsystem.user.service.TeacherInfoDataService;
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
 *学端消息服务实现类
 */
@Slf4j
@Service
public class StudentMessageServiceImpl implements StudentMessageService {

    @Resource
    private InstantMessageMapper instantMessageMapper;

    @Resource
    private FileService fileService;
    
    @Resource
    private SysUserMapper sysUserMapper;
    
    @Resource
    private PaperInfoMapper paperInfoMapper;

    @Resource
    private TeacherInfoDataService teacherInfoService;
    
    @Resource
    private ConversationService conversationService;
    
    @Resource
    private ConversationMemberMapper conversationMemberMapper;
    
    @Resource
    private ConversationMapper conversationMapper;
    
    @Value("${file.upload.base-path}")
    private String uploadBasePath;
    
    @Resource
    private WebSocketSender webSocketSender;
    
    /**
     * 获取已存在的会话
     */
    private Conversation getExistingConversation(Long studentId, Long teacherId) {
        // 先查询 conversation_members 表，看看是否已经存在一个会话，其中包含了指定的学生和教师
        LambdaQueryWrapper<ConversationMember> memberWrapper1 = new LambdaQueryWrapper<>();
        memberWrapper1.eq(ConversationMember::getUserId, studentId);
        List<ConversationMember> studentMembers = conversationMemberMapper.selectList(memberWrapper1);
        
        for (ConversationMember studentMember : studentMembers) {
            Long conversationId = studentMember.getConversationId();
            LambdaQueryWrapper<ConversationMember> memberWrapper2 = new LambdaQueryWrapper<>();
            memberWrapper2.eq(ConversationMember::getConversationId, conversationId)
                         .eq(ConversationMember::getUserId, teacherId);
            Long count = conversationMemberMapper.selectCount(memberWrapper2);
            if (count > 0) {
                // 找到一个包含学生和教师的会话
                return conversationMapper.selectById(conversationId);
            }
        }
        
        // 如果没有找到，再查询消息表
        LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(InstantMessage::getSenderId, studentId)
                .eq(InstantMessage::getReceiverId, teacherId))
            .or(w -> w
                .eq(InstantMessage::getSenderId, teacherId)
                .eq(InstantMessage::getReceiverId, studentId))
            .eq(InstantMessage::getIsDeleted, 0)
            .orderByDesc(InstantMessage::getSentTime);
        
        // 使用分页查询获取第一条记录
        Page<InstantMessage> page = new Page<>(1, 1);
        IPage<InstantMessage> result = instantMessageMapper.selectPage(page, wrapper);
        
        if (result.getRecords() == null || result.getRecords().isEmpty()) {
            return null;
        }
        
        InstantMessage message = result.getRecords().get(0);
        if (message.getConversationId() == null) {
            return null;
        }
        
        return conversationMapper.selectById(message.getConversationId());
    }
    
    /**
     * 构建会话VO
     */
    private MessageSessionVO buildSessionVO(Conversation conversation, Long studentId, Long teacherId, String teacherName) {
        MessageSessionVO session = new MessageSessionVO();
        session.setId(conversation.getId());
        session.setName("与" + teacherName + "的会话");
        session.setType("PRIVATE");
        
        SysUser teacher = sysUserMapper.selectById(teacherId);
        List<MessageSessionVO.SessionMemberVO> members = new ArrayList<>();
        
        SysUser student = sysUserMapper.selectById(studentId);
        String studentName = student != null ? student.getRealName() : "未知用户";
        
        if (teacher != null) {
            MessageSessionVO.SessionMemberVO teacherMember = new MessageSessionVO.SessionMemberVO();
            teacherMember.setUserId(teacher.getId());
            teacherMember.setUserName(teacher.getRealName());
            teacherMember.setUserRole("TEACHER");
            teacherMember.setAvatar(teacher.getAvatar());
            members.add(teacherMember);
            
            MessageSessionVO.SessionMemberVO studentMember = new MessageSessionVO.SessionMemberVO();
            studentMember.setUserId(studentId);
            studentMember.setUserName(studentName);
            studentMember.setUserRole("STUDENT");
            studentMember.setAvatar(student != null ? student.getAvatar() : "");
            members.add(studentMember);
            
            session.setMembers(members);
        }
        
        InstantMessage lastMessage = getLastMessage(studentId, teacherId);
        if (lastMessage != null) {
            session.setLastMessage(lastMessage.getContent());
            session.setLastTime(lastMessage.getSentTime() != null ? 
                lastMessage.getSentTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "");
        } else {
            session.setLastMessage("暂无消息");
            session.setLastTime(null);
        }
        
        session.setUnreadCount(getUnreadMessageCount(studentId, teacherId));
        
        if (!members.isEmpty() && members.get(0).getAvatar() != null) {
            session.setAvatar(members.get(0).getAvatar());
        } else {
            session.setAvatar(null);
        }
        
        return session;
    }
    
    /**
     * 根据用户 ID 获取会话 ID（用于查询现有会话）
     * 
     * @param userId1 用户 ID 1
     * @param userId2 用户 ID 2
     * @return 会话 ID，如果不存在则返回 null
     */
    private Long getExistingConversationId(Long userId1, Long userId2) {
        // 查询数据库中是否已存在该会话的消息
        LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InstantMessage::getSenderId, userId1)
               .eq(InstantMessage::getReceiverId, userId2)
               .or()
               .eq(InstantMessage::getSenderId, userId2)
               .eq(InstantMessage::getReceiverId, userId1)
               .orderByDesc(InstantMessage::getSentTime);
        
        // 使用分页查询获取第一条记录
        Page<InstantMessage> page = new Page<>(1, 1);
        IPage<InstantMessage> result = instantMessageMapper.selectPage(page, wrapper);
        
        if (result.getRecords() == null || result.getRecords().isEmpty()) {
            return null;
        }
        
        InstantMessage message = result.getRecords().get(0);
        return message != null ? message.getConversationId() : null;
    }

    @Override
    public List<MessageSessionVO> getMessageSessions(Long studentId) {
        try {
            log.info("获取学生消息会话列表 - 学生 ID: {}", studentId);
                
            // 1. 获取学生的所有导师 ID（通过论文关联）
            LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
            paperWrapper.eq(PaperInfo::getStudentId, studentId)
                       .eq(PaperInfo::getIsDeleted, 0)
                       .isNotNull(PaperInfo::getTeacherId);
            List<PaperInfo> papers = paperInfoMapper.selectList(paperWrapper);
                
            // 2. 去重：同一个导师只创建一个会话（不管有多少篇论文）
            Map<Long, String> teacherMap = new LinkedHashMap<>(); // teacherId -> teacherName
            for (PaperInfo paper : papers) {
                if (!teacherMap.containsKey(paper.getTeacherId())) {
                    teacherMap.put(paper.getTeacherId(), paper.getTeacherName());
                }
            }
                
            log.info("学生 {} 共有 {} 位导师，将会话数量：{}", studentId, teacherMap.size(), teacherMap.size());
                
            // 3. 为每位导师创建会话
            List<MessageSessionVO> sessions = new ArrayList<>();
                
            for (Map.Entry<Long, String> entry : teacherMap.entrySet()) {
                Long teacherId = entry.getKey();
                String teacherName = entry.getValue();
                
                // 检查是否已存在该师生的会话
                Conversation existingConversation = getExistingConversation(studentId, teacherId);
                
                if (existingConversation != null) {
                    log.info("会话已存在 - 学生 ID: {}, 导师 ID: {}, 会话 ID: {}", studentId, teacherId, existingConversation.getId());
                    
                    MessageSessionVO session = buildSessionVO(existingConversation, studentId, teacherId, teacherName);
                    sessions.add(session);
                } else {
                    log.info("创建新会话 - 学生 ID: {}, 导师 ID: {}", studentId, teacherId);
                    
                    // 创建会话（ID、审计字段、软删除字段均由MyBatis-Plus自动维护）
                    Conversation conversation = new Conversation();
                    conversation.setName("与" + teacherName + "的会话");
                    conversation.setType("PRIVATE");
                    conversation.setCreatorId(studentId);
                    conversation.setLastActiveTime(LocalDateTime.now());
                    conversation.setLastMessageTime(LocalDateTime.now());
                    
                    // 保存会话（自动生成雪花ID和审计字段）
                    conversationMapper.insert(conversation);
                    Long conversationId = conversation.getId();
                    log.info("会话创建成功 - 会话 ID: {}", conversationId);
                    
                    // 关联历史消息到新创建的会话
                    associateHistoricalMessages(conversationId, studentId, teacherId);
                    
                    // 添加会话成员（审计字段由MyBatis-Plus自动填充）
                    ConversationMember studentMember = new ConversationMember();
                    studentMember.setConversationId(conversationId);
                    studentMember.setUserId(studentId);
                    studentMember.setRole("MEMBER");
                    studentMember.setJoinedAt(LocalDateTime.now());
                    studentMember.setIsLeft(0);
                    studentMember.setUnreadCount(0);
                    conversationMemberMapper.insert(studentMember);
                    
                    ConversationMember teacherMember = new ConversationMember();
                    teacherMember.setConversationId(conversationId);
                    teacherMember.setUserId(teacherId);
                    teacherMember.setRole("MEMBER");
                    teacherMember.setJoinedAt(LocalDateTime.now());
                    teacherMember.setIsLeft(0);
                    teacherMember.setUnreadCount(0);
                    conversationMemberMapper.insert(teacherMember);
                    
                    log.info("会话成员添加成功 - 学生 {}，导师 {}", studentId, teacherId);
                    
                    MessageSessionVO session = buildSessionVO(conversation, studentId, teacherId, teacherName);
                    sessions.add(session);
                }
            }
                        
            // 按最后消息时间倒序排序，最新的会话在最上面
            sessions.sort((s1, s2) -> {
                if (s1.getLastTime() == null && s2.getLastTime() == null) {
                    return 0;
                } else if (s1.getLastTime() == null) {
                    return 1; // 没有最后消息时间的会话排在后面
                } else if (s2.getLastTime() == null) {
                    return -1; // 有最后消息时间的会话排在前面
                } else {
                    return s2.getLastTime().compareTo(s1.getLastTime()); // 倒序排序
                }
            });
            
            log.info("获取消息会话列表成功 - 学生 ID: {}, 会话数量：{}", studentId, sessions.size());
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
            
            // 【关键】sessionId 现在是独立的会话 ID（conversation_id）
            // 使用 MyBatis-Plus 的标准方法获取消息
            Page<InstantMessage> messagePage = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<InstantMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InstantMessage::getConversationId, sessionId)
                    .eq(InstantMessage::getIsDeleted, 0)
                    .orderByAsc(InstantMessage::getSentTime);
            
            com.baomidou.mybatisplus.core.metadata.IPage<InstantMessage> dbPage = instantMessageMapper.selectPage(messagePage, wrapper);
            
            // 为每条消息设置发送者名称和头像
            for (InstantMessage message : dbPage.getRecords()) {
                SysUser sender = sysUserMapper.selectById(message.getSenderId());
                if (sender != null) {
                    message.setSenderName(sender.getRealName());
                    message.setSenderAvatar(sender.getAvatar());
                }
            }
            
            // 转换为 VO
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
            // 使用当前登录用户 ID，而不是参数中的 studentId
            Long currentUserId = UserBusinessInfoUtils.getCurrentUserId();
                    
            log.info("发送消息 - 当前用户 ID: {}, 会话 ID: {},接收者 ID: {}", 
                    currentUserId, sendDTO.getSessionId(), sendDTO.getReceiverId());
            InstantMessage message = new InstantMessage();
            message.setSenderId(currentUserId); // 使用当前登录用户 ID
            message.setReceiverId(sendDTO.getReceiverId());
            message.setConversationId(sendDTO.getSessionId()); // ✅ 直接使用前端传递的会话 ID
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
                // 更新会话的最后消息信息
                updateConversationLastMessage(sendDTO.getSessionId());
                
                //为 VO
                MessageVO messageVO = new MessageVO();
                messageVO.setId(message.getId());
                messageVO.setSenderId(currentUserId); // 使用当前登录用户 ID
                            
                // 【修复】获取真实的学生姓名，而不是硬编码"我"
                SysUser currentUser = sysUserMapper.selectById(currentUserId);
                String realSenderName = currentUser != null ? currentUser.getRealName() : "未知用户";
                messageVO.setSenderName(realSenderName); // ✅ 使用真实姓名
                messageVO.setSenderAvatar(currentUser != null ? currentUser.getAvatar() : null); // ✅ 添加 senderAvatar 字段
                            
                messageVO.setSenderRole("STUDENT");
                messageVO.setContent(sendDTO.getContent());
                String sendTimeStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                messageVO.setSendTime(sendTimeStr);
                messageVO.setFormattedTime(messageVO.getFormattedTime()); // 设置格式化时间
                messageVO.setStatus("SENT");
                messageVO.setMessageType(sendDTO.getMessageType());
                            
                // 【新增】设置 sender 字段，兼容前端判断
                messageVO.setSender("student"); // ✅ 添加 sender 字段
                messageVO.setSessionId(sendDTO.getSessionId()); // ✅ 添加 sessionId 字段
                
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
                            
                log.info("消息发送成功 - 消息ID: {}, 发送者：{}", message.getId(), realSenderName);
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
            Long fileId = fileService.uploadFile(file, studentId);
                        
            //构建响应 VO
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
    public void downloadAttachment(Long attachmentId, Long studentId, HttpServletResponse response) {
        try {
            log.info("下载附件 - 学生 ID: {}, 附件 ID: {}", studentId, attachmentId);
                
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
    public void downloadSharedFile(Long fileId, Long studentId, HttpServletResponse response) {
        try {
            log.info("下载共享文件 - 学生 ID: {}, 文件 ID: {}", studentId, fileId);
                
            //复附件下载逻辑
            downloadAttachment(fileId, studentId, response);
            
        } catch (Exception e) {
            log.error("下载共享文件失败 - 文件ID: {}", fileId, e);
            throw new RuntimeException("共享文件下载失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessagesRead(Long studentId, Long sessionId) {
        try {
            log.info("标记消息已读 - 学生 ID: {}, 会话 ID: {}", studentId, sessionId);
            
            // 1. 获取会话信息
            Conversation conversation = conversationMapper.selectById(sessionId);
            if (conversation == null) {
                log.warn("会话不存在 - 会话 ID: {}", sessionId);
                return;
            }
            
            // 2. 获取会话的所有成员
            LambdaQueryWrapper<ConversationMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(ConversationMember::getConversationId, sessionId);
            List<ConversationMember> members = conversationMemberMapper.selectList(memberWrapper);
            
            // 3. 找到会话中的另一个用户（教师或学生）
            Long otherUserId = null;
            for (ConversationMember member : members) {
                if (!member.getUserId().equals(studentId)) {
                    otherUserId = member.getUserId();
                    break;
                }
            }
            
            if (otherUserId == null) {
                log.warn("会话中没有其他成员 - 会话 ID: {}", sessionId);
                return;
            }
            
            log.info("会话中的另一个用户 ID: {}", otherUserId);
            
            // 4. 将发送给学生的未读消息标记为已读（同时考虑 conversationId 和 senderId）
            final Long finalOtherUserId = otherUserId;
            LambdaQueryWrapper<InstantMessage> readWrapper = new LambdaQueryWrapper<>();
            readWrapper.eq(InstantMessage::getReceiverId, studentId)
                .eq(InstantMessage::getStatus, "SENT")
                .eq(InstantMessage::getIsDeleted, 0)
                .and(w -> w
                    .eq(InstantMessage::getConversationId, sessionId)
                    .or(w2 -> w2
                        .eq(InstantMessage::getSenderId, finalOtherUserId)));
                
            log.info("查询条件 - receiverId: {}, status: SENT, (conversationId: {} OR senderId: {})", studentId, sessionId, finalOtherUserId);
                    
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
            log.info("获取导师信息 - 学生 ID: {}", studentId);
            
            // 获取学生的论文信息
            LambdaQueryWrapper<PaperInfo> paperWrapper = new LambdaQueryWrapper<>();
            paperWrapper.eq(PaperInfo::getStudentId, studentId)
                       .eq(PaperInfo::getIsDeleted, 0)
                       .isNotNull(PaperInfo::getTeacherId);
            List<PaperInfo> papers = paperInfoMapper.selectList(paperWrapper);
            
            if (papers.isEmpty()) {
                throw new RuntimeException("未找到指导老师信息");
            }
            
            // 获取第一个论文的导师信息
            PaperInfo paper = papers.get(0);
            SysUser teacher = sysUserMapper.selectById(paper.getTeacherId());
            
            if (teacher == null) {
                throw new RuntimeException("导师信息不存在");
            }
            
            //构建导师信息 VO
            AdvisorInfoVO advisorInfo = new AdvisorInfoVO();
            advisorInfo.setId(teacher.getId().toString());
            advisorInfo.setName(teacher.getRealName());
            advisorInfo.setEmail(teacher.getEmail());
            advisorInfo.setPhone(teacher.getPhone());
            // 设置头像，如果数据库中没有则设为 null
            advisorInfo.setAvatar(teacher.getAvatar() != null && !teacher.getAvatar().trim().isEmpty() ? teacher.getAvatar() : null);
            advisorInfo.setBio(teacher.getIntroduce());
            
            // 从TeacherInfo表获取教师信息
            TeacherInfo teacherInfo = teacherInfoService.getByUserId(teacher.getId());
            if (teacherInfo != null) {
                advisorInfo.setTitle(teacherInfo.getProfessionalTitle() != null ? teacherInfo.getProfessionalTitle() : "讲师");
                advisorInfo.setResearchField(teacherInfo.getResearchDirection());
                advisorInfo.setOffice(teacherInfo.getOffice());
                advisorInfo.setOfficeHours(teacherInfo.getOfficeHours());
                advisorInfo.setCollege(teacherInfo.getCollegeName());
            }
            
            //统计指导学生数
            LambdaQueryWrapper<PaperInfo> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(PaperInfo::getTeacherId, teacher.getId())
                       .eq(PaperInfo::getIsDeleted, 0);
            int studentCount = Math.toIntExact(paperInfoMapper.selectCount(countWrapper));
            advisorInfo.setStudentCount(studentCount);
            
            log.info("获取导师信息成功 -导师 ID: {}", teacher.getId());
            return advisorInfo;
            
        } catch (Exception e) {
            log.error("获取导师信息失败", e);
            throw new RuntimeException("获取导师信息失败：" + e.getMessage());
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
     *将 InstantMessage 转换为 MessageVO
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
                messageVO.setSender(sender.getUserType() != null && sender.getUserType().equals("TEACHER") ? "teacher" : "student");
            } else {
                messageVO.setSenderName("未知用户");
                messageVO.setSenderRole("STUDENT");
                messageVO.setSenderAvatar(null);
                messageVO.setSender("student");
            }
        } else {
            messageVO.setSenderName("未知用户");
            messageVO.setSenderRole("STUDENT");
            messageVO.setSenderAvatar(null);
            messageVO.setSender("student");
        }
        
        messageVO.setContent(message.getContent());
            
        if (message.getSentTime() != null) {
            String sendTimeStr = message.getSentTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            messageVO.setSendTime(sendTimeStr);
            messageVO.setFormattedTime(messageVO.getFormattedTime()); // 设置格式化时间
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
                .orderByDesc(InstantMessage::getSentTime);
            
            // 使用分页查询获取第一条记录
            Page<InstantMessage> page = new Page<>(1, 1);
            IPage<InstantMessage> result = instantMessageMapper.selectPage(page, wrapper);
            
            if (result.getRecords() != null && !result.getRecords().isEmpty()) {
                InstantMessage lastMessage = result.getRecords().get(0);
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
     * @param studentId 学生 ID
     * @param teacherId 导师 ID
     * @return 最后一条消息
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
                .orderByDesc(InstantMessage::getSentTime);
            
            // 使用分页查询获取第一条记录
            Page<InstantMessage> page = new Page<>(1, 1);
            IPage<InstantMessage> result = instantMessageMapper.selectPage(page, wrapper);
            
            if (result.getRecords() != null && !result.getRecords().isEmpty()) {
                return result.getRecords().get(0);
            }
            return null;
        } catch (Exception e) {
            log.warn("获取最后消息失败", e);
            return null;
        }
    }
    
    /**
     * 获取未读消息数（基于师生关系）
     * 
     * @param studentId 学生 ID
     * @param teacherId 导师 ID
     * @return 未读消息数量
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