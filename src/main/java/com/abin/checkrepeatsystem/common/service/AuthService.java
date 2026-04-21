package com.abin.checkrepeatsystem.common.service;

import com.abin.checkrepeatsystem.common.Exception.PermissionDeniedException;
import com.abin.checkrepeatsystem.pojo.entity.CheckReport;
import com.abin.checkrepeatsystem.pojo.entity.PaperInfo;
import com.abin.checkrepeatsystem.pojo.entity.SysUser;

/**
 * 权限管理服务
 * 集中处理权限校验逻辑，提供统一的权限校验方法
 */
public interface AuthService {

    /**
     * 校验当前用户是否为管理员
     * @return true=管理员，false=非管理员
     */
    boolean isAdmin();

    /**
     * 校验当前用户是否为学生
     * @return true=学生，false=非学生
     */
    boolean isStudent();

    /**
     * 校验当前用户是否为教师
     * @return true=教师，false=非教师
     */
    boolean isTeacher();

    /**
     * 获取当前用户
     * @return 当前用户
     */
    SysUser getCurrentUser();

    /**
     * 获取当前用户ID
     * @return 当前用户ID
     */
    Long getCurrentUserId();

    /**
     * 校验当前用户是否有权限访问论文
     * @param paperInfo 论文信息
     * @return 有权限返回true，无权限抛异常
     * @throws PermissionDeniedException 权限拒绝异常
     */
    boolean checkPaperAccess(PaperInfo paperInfo) throws PermissionDeniedException;

    /**
     * 校验当前用户是否有权限访问查重报告
     * @param checkReport 查重报告
     * @return 有权限返回true，无权限抛异常
     * @throws PermissionDeniedException 权限拒绝异常
     */
    boolean checkReportAccess(CheckReport checkReport) throws PermissionDeniedException;

    /**
     * 校验当前用户是否有权限修改指导任务上限
     * @param operatorId 操作人ID
     * @param targetUserId 目标教师ID
     * @param majorId 专业ID
     * @return 有权限返回true，无权限抛异常
     * @throws PermissionDeniedException 权限拒绝异常
     */
    boolean checkModifyMaxCountAuth(Long operatorId, Long targetUserId, Long majorId) throws PermissionDeniedException;

    /**
     * 校验当前用户是否有权限访问文件
     * @param fileInfo 文件信息
     * @return 有权限返回true，无权限抛异常
     * @throws PermissionDeniedException 权限拒绝异常
     */
    boolean checkFileAccess(Object fileInfo) throws PermissionDeniedException;

    /**
     * 校验当前用户是否有权限执行操作
     * @param operation 操作类型
     * @param targetId 目标ID
     * @return 有权限返回true，无权限抛异常
     * @throws PermissionDeniedException 权限拒绝异常
     */
    boolean checkOperationAccess(String operation, Long targetId) throws PermissionDeniedException;
}
