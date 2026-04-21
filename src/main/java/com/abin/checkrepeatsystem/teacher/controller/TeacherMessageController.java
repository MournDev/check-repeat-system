package com.abin.checkrepeatsystem.teacher.controller;

import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.student.dto.ChatExportDTO;
import com.abin.checkrepeatsystem.student.dto.MessageSendDTO;
import com.abin.checkrepeatsystem.student.vo.MessageSessionVO;
import com.abin.checkrepeatsystem.student.vo.MessageVO;
import com.abin.checkrepeatsystem.student.vo.SharedFileVO;
import com.abin.checkrepeatsystem.teacher.service.TeacherMessageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 教师消息控制器
 */
@Slf4j
@RestController
@PreAuthorize("hasAuthority('TEACHER')")
@RequestMapping("/api/teacher/message")
public class TeacherMessageController {

    @Resource
    private TeacherMessageService teacherMessageService;

    /**
     * 获取消息会话列表
     * GET /api/teacher/message/sessions
     */
    @GetMapping("/sessions")
    public Result<List<MessageSessionVO>> getMessageSessions() {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("获取教师消息会话列表请求 - 教师 ID: {}", teacherId);
            
            List<MessageSessionVO> sessions = teacherMessageService.getMessageSessions(teacherId);
            return Result.success("获取会话列表成功", sessions);
        } catch (Exception e) {
            log.error("获取消息会话列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取消息列表
     * GET /api/teacher/message/list
     */
    @GetMapping("/list")
    public Result<Page<MessageVO>> getMessageList(
            @RequestParam Long sessionId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("获取教师消息列表请求 - 教师 ID: {}, 会话 ID: {}, 页码: {}, 页大小: {}", 
                    teacherId, sessionId, pageNum, pageSize);
            
            Page<MessageVO> messages = teacherMessageService.getMessageList(teacherId, sessionId, pageNum, pageSize);
            return Result.success("获取消息列表成功", messages);
        } catch (Exception e) {
            log.error("获取消息列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取消息列表失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息
     * POST /api/teacher/message/send
     */
    @PostMapping("/send")
    public Result<MessageVO> sendMessage(@RequestBody MessageSendDTO sendDTO) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("发送消息请求 - 教师 ID: {}, 会话 ID: {}, 接收者 ID: {}", 
                    teacherId, sendDTO.getSessionId(), sendDTO.getReceiverId());
            
            MessageVO message = teacherMessageService.sendMessage(teacherId, sendDTO);
            return Result.success("消息发送成功", message);
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件
     * POST /api/teacher/message/upload
     */
    @PostMapping("/upload")
    public Result<com.abin.checkrepeatsystem.student.dto.FileUploadVO> uploadFile(
            @RequestParam("file") MultipartFile file) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("上传文件请求 - 教师 ID: {}, 文件名: {}", teacherId, file.getOriginalFilename());
            
            com.abin.checkrepeatsystem.student.dto.FileUploadVO fileVO = teacherMessageService.uploadFile(file, teacherId);
            return Result.success("文件上传成功", fileVO);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载附件
     * GET /api/teacher/message/attachment/{attachmentId}
     */
    @GetMapping("/attachment/{attachmentId}")
    public void downloadAttachment(
            @PathVariable Long attachmentId,
            HttpServletResponse response) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("下载附件请求 - 教师 ID: {}, 附件 ID: {}", teacherId, attachmentId);
            
            teacherMessageService.downloadAttachment(attachmentId, teacherId, response);
        } catch (Exception e) {
            log.error("下载附件失败", e);
            response.setStatus(500);
        }
    }

    /**
     * 清空消息
     * POST /api/teacher/message/clear
     */
    @PostMapping("/clear")
    public Result<String> clearMessages(@RequestParam Long sessionId) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("清空消息请求 - 教师 ID: {}, 会话 ID: {}", teacherId, sessionId);
            
            teacherMessageService.clearMessages(teacherId, sessionId);
            return Result.success("消息清空成功");
        } catch (Exception e) {
            log.error("清空消息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "清空消息失败: " + e.getMessage());
        }
    }

    /**
     * 导出聊天记录
     * POST /api/teacher/message/export
     */
    @PostMapping("/export")
    public void exportChatRecords(
            @RequestBody ChatExportDTO exportDTO,
            HttpServletResponse response) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("导出聊天记录请求 - 教师 ID: {}, 会话 ID: {}, 格式: {}", 
                    teacherId, exportDTO.getSessionId(), exportDTO.getFormat());
            
            teacherMessageService.exportChatRecords(teacherId, exportDTO, response);
        } catch (Exception e) {
            log.error("导出聊天记录失败", e);
            response.setStatus(500);
        }
    }
    
    /**
     * 导出聊天记录（GET 版本，兼容旧请求）
     * GET /api/teacher/message/export
     */
    @GetMapping("/export")
    public void exportChatRecordsGet(
            @RequestParam Long teacherId,
            @RequestParam Long sessionId,
            @RequestParam String format,
            HttpServletResponse response) {
        try {
            log.info("导出聊天记录请求 (GET) - 教师 ID: {}, 会话 ID: {}, 格式: {}", 
                    teacherId, sessionId, format);
            
            ChatExportDTO exportDTO = new ChatExportDTO();
            exportDTO.setSessionId(sessionId);
            exportDTO.setFormat(format);
            
            teacherMessageService.exportChatRecords(teacherId, exportDTO, response);
        } catch (Exception e) {
            log.error("导出聊天记录失败", e);
            response.setStatus(500);
        }
    }

    /**
     * 获取共享文件列表
     * GET /api/teacher/message/files
     */
    @GetMapping("/files")
    public Result<List<SharedFileVO>> getSharedFiles(@RequestParam Long sessionId) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("获取共享文件列表请求 - 教师 ID: {}, 会话 ID: {}", teacherId, sessionId);
            
            List<SharedFileVO> files = teacherMessageService.getSharedFiles(teacherId, sessionId);
            return Result.success("获取共享文件列表成功", files);
        } catch (Exception e) {
            log.error("获取共享文件列表失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取共享文件列表失败: " + e.getMessage());
        }
    }

    /**
     * 下载共享文件
     * GET /api/teacher/message/files/{fileId}
     */
    @GetMapping("/files/{fileId}")
    public void downloadSharedFile(
            @PathVariable Long fileId,
            HttpServletResponse response) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("下载共享文件请求 - 教师 ID: {}, 文件 ID: {}", teacherId, fileId);
            
            teacherMessageService.downloadSharedFile(fileId, teacherId, response);
        } catch (Exception e) {
            log.error("下载共享文件失败", e);
            response.setStatus(500);
        }
    }

    /**
     * 标记消息已读
     * POST /api/teacher/message/mark-read
     */
    @PostMapping("/mark-read")
    public Result<String> markMessagesRead(@RequestParam Long sessionId) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("标记消息已读请求 - 教师 ID: {}, 会话 ID: {}", teacherId, sessionId);
            
            teacherMessageService.markMessagesRead(teacherId, sessionId);
            return Result.success("消息标记已读成功");
        } catch (Exception e) {
            log.error("标记消息已读失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "标记消息已读失败: " + e.getMessage());
        }
    }

    /**
     * 撤回消息
     * POST /api/teacher/message/recall
     */
    @PostMapping("/recall")
    public Result<String> recallMessage(@RequestParam Long messageId) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("撤回消息请求 - 教师 ID: {}, 消息 ID: {}", teacherId, messageId);
            
            teacherMessageService.recallMessage(teacherId, messageId);
            return Result.success("消息撤回成功");
        } catch (Exception e) {
            log.error("撤回消息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "撤回消息失败: " + e.getMessage());
        }
    }

    /**
     * 获取学生信息
     * GET /api/teacher/message/student-info
     */
    @GetMapping("/student-info")
    public Result<com.abin.checkrepeatsystem.student.vo.StudentInfoVO> getStudentInfo(@RequestParam Long studentId) {
        try {
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("获取学生信息请求 - 教师 ID: {}, 学生 ID: {}", teacherId, studentId);
            
            com.abin.checkrepeatsystem.student.vo.StudentInfoVO studentInfo = teacherMessageService.getStudentInfo(teacherId, studentId);
            return Result.success("获取学生信息成功", studentInfo);
        } catch (Exception e) {
            log.error("获取学生信息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取学生信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取学生信息（POST 版本，兼容前端请求）
     * POST /api/teacher/message/student-info
     */
    @PostMapping("/student-info")
    public Result<com.abin.checkrepeatsystem.student.vo.StudentInfoVO> getStudentInfoPost(@RequestBody Map<String, Long> requestBody) {
        try {
            Long studentId = requestBody.get("studentId");
            Long teacherId = UserBusinessInfoUtils.getCurrentUserId();
            log.info("获取学生信息请求 (POST) - 教师 ID: {}, 学生 ID: {}", teacherId, studentId);
            
            com.abin.checkrepeatsystem.student.vo.StudentInfoVO studentInfo = teacherMessageService.getStudentInfo(teacherId, studentId);
            return Result.success("获取学生信息成功", studentInfo);
        } catch (Exception e) {
            log.error("获取学生信息失败", e);
            return Result.error(ResultCode.SYSTEM_ERROR, "获取学生信息失败: " + e.getMessage());
        }
    }
}