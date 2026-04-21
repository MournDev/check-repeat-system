package com.abin.checkrepeatsystem.common.service.Impl;

import com.abin.checkrepeatsystem.common.Exception.PermissionDeniedException;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.abin.checkrepeatsystem.common.service.AuthService;
import com.abin.checkrepeatsystem.common.utils.UserBusinessInfoUtils;
import com.abin.checkrepeatsystem.student.mapper.PaperInfoMapper;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 权限管理服务实现类
 */
@Service
public class PermissionServiceImpl implements AuthService {

    @Resource
    private PaperInfoMapper paperInfoMapper;

    /**
     * 检查用户是否有权限访问论文
     */
    @Override
    public boolean checkPaperAccess(PaperInfo paperInfo) throws PermissionDeniedException {
        if (paperInfo == null) {
            throw new PermissionDeniedException(ResultCode.RESOURCE_NOT_FOUND, getCurrentUserId(), "paper_access", "论文不存在");
        }

        // 管理员可以访问所有论文
        if (isAdmin()) {
            return true;
        }

        // 学生只能访问自己的论文
        if (isStudent()) {
            Long currentUserId = getCurrentUserId();
            if (paperInfo.getStudentId().equals(currentUserId)) {
                return true;
            } else {
                throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, getCurrentUserId(), "paper_access", "无权限访问该论文");
            }
        }

        // 教师只能访问自己指导的论文
        if (isTeacher()) {
            Long currentUserId = getCurrentUserId();
            if (paperInfo.getTeacherId() != null && paperInfo.getTeacherId().equals(currentUserId)) {
                return true;
            } else {
                throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, getCurrentUserId(), "paper_access", "无权限访问该论文");
            }
        }

        // 其他角色无权限
        throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, getCurrentUserId(), "paper_access", "无权限访问该论文");
    }

    @Override
    public boolean checkReportAccess(CheckReport checkReport) throws PermissionDeniedException {
        if (checkReport == null) {
            throw new PermissionDeniedException(ResultCode.RESOURCE_NOT_FOUND, getCurrentUserId(), "report_access", "查重报告不存在");
        }

        // 管理员可以访问所有报告
        if (isAdmin()) {
            return true;
        }

        // 获取报告关联的论文
        PaperInfo paperInfo = paperInfoMapper.selectById(checkReport.getPaperId());
        if (paperInfo == null) {
            throw new PermissionDeniedException(ResultCode.RESOURCE_NOT_FOUND, getCurrentUserId(), "report_access", "报告关联的论文不存在");
        }

        // 学生只能访问自己论文的报告
        if (isStudent()) {
            Long currentUserId = getCurrentUserId();
            if (paperInfo.getStudentId().equals(currentUserId)) {
                return true;
            } else {
                throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, getCurrentUserId(), "report_access", "无权限访问该报告");
            }
        }

        // 教师只能访问自己指导论文的报告
        if (isTeacher()) {
            Long currentUserId = getCurrentUserId();
            if (paperInfo.getTeacherId() != null && paperInfo.getTeacherId().equals(currentUserId)) {
                return true;
            } else {
                throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, getCurrentUserId(), "report_access", "无权限访问该报告");
            }
        }

        // 其他角色无权限
        throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, getCurrentUserId(), "report_access", "无权限访问该报告");
    }

    @Override
    public boolean checkModifyMaxCountAuth(Long operatorId, Long targetUserId, Long majorId) throws PermissionDeniedException {
        // 校验操作人是否存在
        SysUser operator = getCurrentUser();
        if (operator == null) {
            throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, operatorId, "modify_max_count", "操作人不存在");
        }

        // 管理员可以修改所有教师的上限
        if (isAdmin()) {
            return true;
        }

        // 教师无权限修改上限
        if (isTeacher()) {
            throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, operatorId, "modify_max_count", "教师角色无法修改指导任务上限");
        }

        // 学生无权限
        if (isStudent()) {
            throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, operatorId, "modify_max_count", "学生角色无法修改指导任务上限");
        }

        // 其他角色无权限
        throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, operatorId, "modify_max_count", "无权限修改指导任务上限");
    }

    @Override
    public boolean checkFileAccess(Object fileInfo) throws PermissionDeniedException {
        // 暂时返回true，后续根据实际业务逻辑实现
        // 例如：检查当前用户是否为文件的所有者或具有访问权限
        return true;
    }

    @Override
    public boolean checkOperationAccess(String operation, Long targetId) throws PermissionDeniedException {
        // 根据操作类型和目标ID进行权限校验
        switch (operation) {
            case "paper_submit":
                // 学生可以提交自己的论文
                if (isStudent()) {
                    return true;
                } else {
                    throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, getCurrentUserId(), operation, "只有学生可以提交论文");
                }
            case "paper_review":
                // 教师可以审核自己指导的论文
                if (isTeacher()) {
                    return true;
                } else if (isAdmin()) {
                    return true;
                } else {
                    throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, getCurrentUserId(), operation, "只有教师和管理员可以审核论文");
                }
            case "system_config":
                // 只有管理员可以修改系统配置
                if (isAdmin()) {
                    return true;
                } else {
                    throw new PermissionDeniedException(ResultCode.PERMISSION_ADMIN_ONLY, getCurrentUserId(), operation, "只有管理员可以修改系统配置");
                }
            default:
                throw new PermissionDeniedException(ResultCode.PERMISSION_NO_ACCESS, getCurrentUserId(), operation, "未知的操作类型");
        }
    }

    @Override
    public boolean isAdmin() {
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        return currentUser != null && "ADMIN".equals(currentUser.getUserType());
    }

    @Override
    public boolean isStudent() {
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        return currentUser != null && "STUDENT".equals(currentUser.getUserType());
    }

    @Override
    public boolean isTeacher() {
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        return currentUser != null && "TEACHER".equals(currentUser.getUserType());
    }

    @Override
    public SysUser getCurrentUser() {
        return UserBusinessInfoUtils.getCurrentSysUser();
    }

    @Override
    public Long getCurrentUserId() {
        SysUser currentUser = UserBusinessInfoUtils.getCurrentSysUser();
        return currentUser != null ? currentUser.getId() : null;
    }
}