package com.abin.checkrepeatsystem.teacher.service;

import com.abin.checkrepeatsystem.student.dto.MessageSendDTO;
import com.abin.checkrepeatsystem.student.vo.MessageSessionVO;
import com.abin.checkrepeatsystem.student.vo.MessageVO;
import com.abin.checkrepeatsystem.student.vo.SharedFileVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 教师消息服务接口
 */
public interface TeacherMessageService {

    /**
     * 获取教师的消息会话列表
     * @param teacherId 教师ID
     * @return 消息会话列表
     */
    List<MessageSessionVO> getMessageSessions(Long teacherId);

    /**
     * 获取消息列表
     * @param teacherId 教师ID
     * @param sessionId 会话ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 消息列表
     */
    Page<MessageVO> getMessageList(Long teacherId, Long sessionId, Integer pageNum, Integer pageSize);

    /**
     * 发送消息
     * @param teacherId 教师ID
     * @param sendDTO 消息发送DTO
     * @return 发送的消息VO
     */
    MessageVO sendMessage(Long teacherId, MessageSendDTO sendDTO);

    /**
     * 上传文件
     * @param file 文件
     * @param teacherId 教师ID
     * @return 文件上传VO
     */
    com.abin.checkrepeatsystem.student.dto.FileUploadVO uploadFile(MultipartFile file, Long teacherId);

    /**
     * 下载附件
     * @param attachmentId 附件ID
     * @param teacherId 教师ID
     * @param response 响应
     */
    void downloadAttachment(Long attachmentId, Long teacherId, HttpServletResponse response);

    /**
     * 清空消息
     * @param teacherId 教师ID
     * @param sessionId 会话ID
     */
    void clearMessages(Long teacherId, Long sessionId);

    /**
     * 导出聊天记录
     * @param teacherId 教师ID
     * @param exportDTO 导出DTO
     * @param response 响应
     */
    void exportChatRecords(Long teacherId, com.abin.checkrepeatsystem.student.dto.ChatExportDTO exportDTO, HttpServletResponse response);

    /**
     * 获取共享文件列表
     * @param teacherId 教师ID
     * @param sessionId 会话ID
     * @return 共享文件列表
     */
    List<SharedFileVO> getSharedFiles(Long teacherId, Long sessionId);

    /**
     * 下载共享文件
     * @param fileId 文件ID
     * @param teacherId 教师ID
     * @param response 响应
     */
    void downloadSharedFile(Long fileId, Long teacherId, HttpServletResponse response);

    /**
     * 标记消息已读
     * @param teacherId 教师ID
     * @param sessionId 会话ID
     */
    void markMessagesRead(Long teacherId, Long sessionId);

    /**
     * 撤回消息
     * @param teacherId 教师ID
     * @param messageId 消息ID
     */
    void recallMessage(Long teacherId, Long messageId);

    /**
     * 获取学生信息
     * @param teacherId 教师ID
     * @param studentId 学生ID
     * @return 学生信息VO
     */
    com.abin.checkrepeatsystem.student.vo.StudentInfoVO getStudentInfo(Long teacherId, Long studentId);
}