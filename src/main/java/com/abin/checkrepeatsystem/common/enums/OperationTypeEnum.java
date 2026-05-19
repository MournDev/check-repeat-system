package com.abin.checkrepeatsystem.common.enums;

import com.abin.checkrepeatsystem.common.constant.OperationTypeConstants;

/**
 * 操作类型枚举
 * 提供操作类型的详细信息，包含系统中所有的操作类型
 */
public enum OperationTypeEnum {

    // ========== 用户管理 ==========
    USER_LOGIN(OperationTypeConstants.USER_LOGIN, "用户登录", "用户登录系统", "用户管理", PermissionLevel.LOW),
    USER_LOGOUT(OperationTypeConstants.USER_LOGOUT, "用户登出", "用户退出系统", "用户管理", PermissionLevel.LOW),
    USER_REGISTER(OperationTypeConstants.USER_REGISTER, "用户注册", "用户注册账号", "用户管理", PermissionLevel.LOW),
    USER_UPDATE(OperationTypeConstants.USER_UPDATE, "更新用户信息", "更新用户基本信息", "用户管理", PermissionLevel.MEDIUM),
    USER_DELETE(OperationTypeConstants.USER_DELETE, "删除用户", "删除用户账号", "用户管理", PermissionLevel.HIGH),
    USER_BATCH_DELETE(OperationTypeConstants.USER_BATCH_DELETE, "批量删除用户", "批量删除用户账号", "用户管理", PermissionLevel.HIGH),
    USER_STATUS_UPDATE(OperationTypeConstants.USER_STATUS_UPDATE, "更新用户状态", "更新用户账号状态", "用户管理", PermissionLevel.MEDIUM),
    USER_PASSWORD_CHANGE(OperationTypeConstants.USER_PASSWORD_CHANGE, "修改密码", "修改用户密码", "用户管理", PermissionLevel.MEDIUM),
    USER_ROLE_ASSIGN(OperationTypeConstants.USER_ROLE_ASSIGN, "分配用户角色", "为用户分配角色权限", "用户管理", PermissionLevel.HIGH),
    USER_UPLOAD_AVATAR(OperationTypeConstants.USER_UPLOAD_AVATAR, "上传头像", "上传用户头像", "用户管理", PermissionLevel.LOW),
    USER_VERIFY_EMAIL(OperationTypeConstants.USER_VERIFY_EMAIL, "验证邮箱", "验证用户邮箱地址", "用户管理", PermissionLevel.LOW),
    USER_SEND_VERIFY_EMAIL(OperationTypeConstants.USER_SEND_VERIFY_EMAIL, "发送邮箱验证邮件", "发送邮箱验证邮件到用户邮箱", "用户管理", PermissionLevel.LOW),
    USER_SEND_EMAIL_CODE(OperationTypeConstants.USER_SEND_EMAIL_CODE, "发送邮箱验证码", "发送邮箱验证码到用户邮箱", "用户管理", PermissionLevel.LOW),
    USER_UPDATE_EMAIL(OperationTypeConstants.USER_UPDATE_EMAIL, "更新用户邮箱", "更新用户邮箱地址", "用户管理", PermissionLevel.MEDIUM),
    USER_LOGIN_HISTORY(OperationTypeConstants.USER_LOGIN_HISTORY, "查询登录历史", "查询用户登录历史记录", "用户管理", PermissionLevel.LOW),

    // ========== 论文管理 ==========
    PAPER_SUBMIT(OperationTypeConstants.PAPER_SUBMIT, "提交论文", "学生提交论文", "论文管理", PermissionLevel.MEDIUM),
    PAPER_UPDATE(OperationTypeConstants.PAPER_UPDATE, "更新论文", "更新论文信息", "论文管理", PermissionLevel.MEDIUM),
    PAPER_DELETE(OperationTypeConstants.PAPER_DELETE, "删除论文", "删除论文记录", "论文管理", PermissionLevel.HIGH),
    PAPER_REVIEW(OperationTypeConstants.PAPER_REVIEW, "论文审核", "审核学生论文", "论文管理", PermissionLevel.MEDIUM),
    PAPER_APPROVE(OperationTypeConstants.PAPER_APPROVE, "论文批准", "批准学生论文", "论文管理", PermissionLevel.MEDIUM),
    PAPER_REJECT(OperationTypeConstants.PAPER_REJECT, "论文拒绝", "拒绝学生论文", "论文管理", PermissionLevel.MEDIUM),
    PAPER_BATCH_AUDIT(OperationTypeConstants.PAPER_BATCH_AUDIT, "批量审核论文", "批量审核学生论文", "论文管理", PermissionLevel.HIGH),
    PAPER_RESUBMIT(OperationTypeConstants.PAPER_RESUBMIT, "重新提交论文", "学生重新提交论文", "论文管理", PermissionLevel.MEDIUM),
    PAPER_WITHDRAW(OperationTypeConstants.PAPER_WITHDRAW, "撤回论文", "学生撤回已提交的论文", "论文管理", PermissionLevel.MEDIUM),
    PAPER_MODIFY_REQUEST(OperationTypeConstants.PAPER_MODIFY_REQUEST, "申请修改论文", "学生申请修改论文", "论文管理", PermissionLevel.MEDIUM),
    PAPER_BATCH_DOWNLOAD(OperationTypeConstants.PAPER_BATCH_DOWNLOAD, "批量下载论文", "批量下载论文文件", "论文管理", PermissionLevel.MEDIUM),
    PAPER_BATCH_DELETE(OperationTypeConstants.PAPER_BATCH_DELETE, "批量删除论文", "批量删除论文记录", "论文管理", PermissionLevel.HIGH),
    ATTACHMENT_UPLOAD(OperationTypeConstants.ATTACHMENT_UPLOAD, "上传附件", "上传论文附件", "论文管理", PermissionLevel.MEDIUM),
    ATTACHMENT_DELETE(OperationTypeConstants.ATTACHMENT_DELETE, "删除附件", "删除论文附件", "论文管理", PermissionLevel.MEDIUM),

    // ========== 查重管理 ==========
    TASK_CREATE(OperationTypeConstants.TASK_CREATE, "创建查重任务", "创建论文查重任务", "查重管理", PermissionLevel.MEDIUM),
    TASK_SUBMIT(OperationTypeConstants.TASK_SUBMIT, "提交查重任务", "提交论文查重任务", "查重管理", PermissionLevel.MEDIUM),
    TASK_CANCEL(OperationTypeConstants.TASK_CANCEL, "取消查重任务", "取消未完成的查重任务", "查重管理", PermissionLevel.MEDIUM),
    TASK_DELETE(OperationTypeConstants.TASK_DELETE, "删除查重任务", "删除查重任务记录", "查重管理", PermissionLevel.HIGH),
    TASK_RESULT_VIEW(OperationTypeConstants.TASK_RESULT_VIEW, "查看查重结果", "查看论文查重结果", "查重管理", PermissionLevel.MEDIUM),
    TASK_RESULT_EXPORT(OperationTypeConstants.TASK_RESULT_EXPORT, "导出查重结果", "导出论文查重结果", "查重管理", PermissionLevel.MEDIUM),
    TASK_BATCH_CREATE(OperationTypeConstants.TASK_BATCH_CREATE, "批量创建查重任务", "批量创建论文查重任务", "查重管理", PermissionLevel.MEDIUM),
    CHECK_TASK_LIST(OperationTypeConstants.CHECK_TASK_LIST, "查询查重任务列表", "查询查重任务列表", "查重管理", PermissionLevel.LOW),
    CHECK_TASK_DETAIL(OperationTypeConstants.CHECK_TASK_DETAIL, "查询查重任务详情", "查询查重任务详情", "查重管理", PermissionLevel.LOW),
    CHECK_TASK_BY_ID(OperationTypeConstants.CHECK_TASK_BY_ID, "查询指定任务详情", "根据ID查询查重任务详情", "查重管理", PermissionLevel.LOW),
    CHECK_TASK_STATUS(OperationTypeConstants.CHECK_TASK_STATUS, "获取查重任务状态", "获取查重任务状态", "查重管理", PermissionLevel.LOW),
    CHECK_TASK_RECHECK(OperationTypeConstants.CHECK_TASK_RECHECK, "重新发起查重", "重新发起论文查重", "查重管理", PermissionLevel.MEDIUM),

    // ========== 查重规则 ==========
    CHECK_RULE_CREATE(OperationTypeConstants.CHECK_RULE_CREATE, "创建查重规则", "创建论文查重规则", "查重管理", PermissionLevel.HIGH),
    CHECK_RULE_UPDATE(OperationTypeConstants.CHECK_RULE_UPDATE, "更新查重规则", "更新论文查重规则", "查重管理", PermissionLevel.HIGH),
    CHECK_RULE_DELETE(OperationTypeConstants.CHECK_RULE_DELETE, "删除查重规则", "删除论文查重规则", "查重管理", PermissionLevel.HIGH),

    // ========== 教师管理 ==========
    TEACHER_ASSIGN(OperationTypeConstants.TEACHER_ASSIGN, "分配导师", "为学生分配导师", "教师管理", PermissionLevel.HIGH),
    TEACHER_UPDATE(OperationTypeConstants.TEACHER_UPDATE, "更新教师信息", "更新教师基本信息", "教师管理", PermissionLevel.MEDIUM),
    TEACHER_GET_INFO(OperationTypeConstants.TEACHER_GET_INFO, "获取教师信息", "获取教师个人信息", "教师管理", PermissionLevel.LOW),
    TEACHER_STUDENT_MANAGE(OperationTypeConstants.TEACHER_STUDENT_MANAGE, "管理学生", "教师管理学生信息", "教师管理", PermissionLevel.MEDIUM),
    TEACHER_REVIEW_ASSIGN(OperationTypeConstants.TEACHER_REVIEW_ASSIGN, "分配评审任务", "分配论文评审任务", "教师管理", PermissionLevel.MEDIUM),
    TEACHER_REVIEW_COMPLETE(OperationTypeConstants.TEACHER_REVIEW_COMPLETE, "完成评审任务", "完成论文评审任务", "教师管理", PermissionLevel.MEDIUM),
    CONTACT_STUDENT(OperationTypeConstants.CONTACT_STUDENT, "联系学生", "教师联系学生", "教师管理", PermissionLevel.MEDIUM),
    DELEGATE_REVIEW(OperationTypeConstants.DELEGATE_REVIEW, "委托审核", "委托其他教师审核", "教师管理", PermissionLevel.MEDIUM),
    DELETE_STUDENT(OperationTypeConstants.DELETE_STUDENT, "删除学生", "删除学生账号", "教师管理", PermissionLevel.HIGH),
    BATCH_ASSIGN_ADVISOR(OperationTypeConstants.BATCH_ASSIGN_ADVISOR, "批量分配导师", "批量为学生分配导师", "教师管理", PermissionLevel.HIGH),
    BATCH_SEND_MESSAGE(OperationTypeConstants.BATCH_SEND_MESSAGE, "批量发送消息", "批量向学生发送消息", "教师管理", PermissionLevel.MEDIUM),
    BATCH_DELETE_STUDENT(OperationTypeConstants.BATCH_DELETE_STUDENT, "批量删除学生", "批量删除学生账号", "教师管理", PermissionLevel.HIGH),
    EXPORT_STUDENTS(OperationTypeConstants.EXPORT_STUDENTS, "导出学生数据", "导出学生信息到文件", "教师管理", PermissionLevel.MEDIUM),
    ADD_STUDENT(OperationTypeConstants.ADD_STUDENT, "添加学生", "添加学生账号", "教师管理", PermissionLevel.MEDIUM),
    IMPORT_STUDENTS(OperationTypeConstants.IMPORT_STUDENTS, "导入学生数据", "从文件导入学生信息", "教师管理", PermissionLevel.MEDIUM),
    SEND_MESSAGE(OperationTypeConstants.SEND_MESSAGE, "发送消息", "向用户发送消息", "教师管理", PermissionLevel.MEDIUM),

    // ========== 分配任务 ==========
    ASSIGNMENT_CREATE(OperationTypeConstants.ASSIGNMENT_CREATE, "创建分配任务", "创建分配任务记录", "教师管理", PermissionLevel.MEDIUM),
    ASSIGNMENT_UPDATE(OperationTypeConstants.ASSIGNMENT_UPDATE, "更新分配任务", "更新分配任务信息", "教师管理", PermissionLevel.MEDIUM),
    ASSIGNMENT_DELETE(OperationTypeConstants.ASSIGNMENT_DELETE, "删除分配任务", "删除分配任务记录", "教师管理", PermissionLevel.HIGH),

    // ========== 教师分配 ==========
    TEACHER_ALLOCATION_CREATE(OperationTypeConstants.TEACHER_ALLOCATION_CREATE, "创建教师分配记录", "创建教师分配记录", "教师管理", PermissionLevel.MEDIUM),
    TEACHER_ALLOCATION_REVOKE(OperationTypeConstants.TEACHER_ALLOCATION_REVOKE, "撤销教师分配记录", "撤销已分配的教师", "教师管理", PermissionLevel.HIGH),
    TEACHER_ALLOCATION_BATCH_CREATE(OperationTypeConstants.TEACHER_ALLOCATION_BATCH_CREATE, "批量创建教师分配记录", "批量创建教师分配记录", "教师管理", PermissionLevel.HIGH),
    TEACHER_ASSIGN_CANCEL(OperationTypeConstants.TEACHER_ASSIGN_CANCEL, "取消导师分配", "取消已分配的导师", "教师管理", PermissionLevel.HIGH),
    BATCH_TEACHER_ASSIGN(OperationTypeConstants.BATCH_TEACHER_ASSIGN, "批量分配导师", "批量为学生分配导师", "教师管理", PermissionLevel.HIGH),

    // ========== 自动分配 ==========
    AUTO_ALLOCATION_CREATE(OperationTypeConstants.AUTO_ALLOCATION_CREATE, "创建自动分配记录", "创建自动分配历史记录", "教师管理", PermissionLevel.MEDIUM),

    // ========== 系统管理 ==========
    SYSTEM_CONFIG_UPDATE(OperationTypeConstants.SYSTEM_CONFIG_UPDATE, "更新系统配置", "更新系统基础配置", "系统管理", PermissionLevel.HIGH),
    SYSTEM_PARAM_UPDATE(OperationTypeConstants.SYSTEM_PARAM_UPDATE, "更新系统参数", "更新系统运行参数", "系统管理", PermissionLevel.HIGH),
    SYSTEM_CONFIG_RESET(OperationTypeConstants.SYSTEM_CONFIG_RESET, "重置系统配置", "将系统配置恢复为默认值", "系统管理", PermissionLevel.HIGH),
    SYSTEM_PARAM_INIT(OperationTypeConstants.SYSTEM_PARAM_INIT, "初始化系统参数", "初始化系统运行参数", "系统管理", PermissionLevel.HIGH),
    SYSTEM_MAINTENANCE(OperationTypeConstants.SYSTEM_MAINTENANCE, "系统维护设置", "设置系统维护状态", "系统管理", PermissionLevel.HIGH),
    DICT_DATA_MANAGE(OperationTypeConstants.DICT_DATA_MANAGE, "管理字典数据", "管理系统字典数据", "系统管理", PermissionLevel.MEDIUM),
    PERMISSION_MANAGE(OperationTypeConstants.PERMISSION_MANAGE, "管理权限", "管理系统权限", "系统管理", PermissionLevel.HIGH),
    ROLE_MANAGE(OperationTypeConstants.ROLE_MANAGE, "管理角色", "管理系统角色", "系统管理", PermissionLevel.HIGH),
    ROLE_LIST(OperationTypeConstants.ROLE_LIST, "获取角色列表", "获取系统角色列表", "系统管理", PermissionLevel.LOW),
    ROLE_CREATE(OperationTypeConstants.ROLE_CREATE, "创建角色", "创建系统角色", "系统管理", PermissionLevel.HIGH),
    ROLE_UPDATE(OperationTypeConstants.ROLE_UPDATE, "更新角色", "更新系统角色", "系统管理", PermissionLevel.HIGH),
    ROLE_DELETE(OperationTypeConstants.ROLE_DELETE, "删除角色", "删除系统角色", "系统管理", PermissionLevel.HIGH),
    PERMISSION_TREE(OperationTypeConstants.PERMISSION_TREE, "获取权限树", "获取系统权限树", "系统管理", PermissionLevel.LOW),
    USER_ASSIGN_ROLE(OperationTypeConstants.USER_ASSIGN_ROLE, "分配用户角色", "为用户分配系统角色", "系统管理", PermissionLevel.HIGH),

    // ========== 配置管理 ==========
    CONFIG_GET_ALL(OperationTypeConstants.CONFIG_GET_ALL, "获取所有配置", "获取系统所有配置信息", "系统管理", PermissionLevel.LOW),
    CONFIG_BASIC_UPDATE(OperationTypeConstants.CONFIG_BASIC_UPDATE, "更新基础配置", "更新系统基础配置", "系统管理", PermissionLevel.HIGH),
    CONFIG_PLAGIARISM_UPDATE(OperationTypeConstants.CONFIG_PLAGIARISM_UPDATE, "更新查重配置", "更新查重相关配置", "系统管理", PermissionLevel.HIGH),
    CONFIG_SECURITY_UPDATE(OperationTypeConstants.CONFIG_SECURITY_UPDATE, "更新安全配置", "更新系统安全配置", "系统管理", PermissionLevel.HIGH),
    CONFIG_EMAIL_UPDATE(OperationTypeConstants.CONFIG_EMAIL_UPDATE, "更新邮件配置", "更新邮件发送配置", "系统管理", PermissionLevel.HIGH),
    CONFIG_PERFORMANCE_UPDATE(OperationTypeConstants.CONFIG_PERFORMANCE_UPDATE, "更新性能配置", "更新系统性能配置", "系统管理", PermissionLevel.HIGH),
    CONFIG_SAVE_ALL(OperationTypeConstants.CONFIG_SAVE_ALL, "保存全部配置", "保存所有系统配置", "系统管理", PermissionLevel.HIGH),
    CONFIG_TEST_EMAIL(OperationTypeConstants.CONFIG_TEST_EMAIL, "测试邮件配置", "测试邮件发送配置", "系统管理", PermissionLevel.MEDIUM),
    CONFIG_EXPORT(OperationTypeConstants.CONFIG_EXPORT, "导出配置", "导出系统配置到文件", "系统管理", PermissionLevel.MEDIUM),
    CONFIG_RESET_DEFAULT(OperationTypeConstants.CONFIG_RESET_DEFAULT, "恢复默认配置", "将系统配置恢复为默认值", "系统管理", PermissionLevel.HIGH),
    CONFIG_REFRESH(OperationTypeConstants.CONFIG_REFRESH, "刷新配置", "刷新系统配置缓存", "系统管理", PermissionLevel.MEDIUM),

    // ========== 学生管理 ==========
    STUDENT_INFO_UPDATE(OperationTypeConstants.STUDENT_INFO_UPDATE, "更新学生信息", "更新学生基本信息", "学生管理", PermissionLevel.MEDIUM),
    STUDENT_PAPER_MANAGE(OperationTypeConstants.STUDENT_PAPER_MANAGE, "管理学生论文", "管理学生论文信息", "学生管理", PermissionLevel.MEDIUM),
    STUDENT_TASK_VIEW(OperationTypeConstants.STUDENT_TASK_VIEW, "查看学生任务", "查看学生任务信息", "学生管理", PermissionLevel.MEDIUM),
    STUDENT_SUBMISSION_MANAGE(OperationTypeConstants.STUDENT_SUBMISSION_MANAGE, "管理学生提交", "管理学生提交记录", "学生管理", PermissionLevel.MEDIUM),

    // ========== 通知管理 ==========
    NOTICE_SEND(OperationTypeConstants.NOTICE_SEND, "发送通知", "发送系统通知", "通知管理", PermissionLevel.MEDIUM),
    NOTIFICATION_SEND(OperationTypeConstants.NOTIFICATION_SEND, "发送通知", "发送系统通知", "通知管理", PermissionLevel.MEDIUM),
    NOTIFICATION_UPDATE(OperationTypeConstants.NOTIFICATION_UPDATE, "更新通知", "更新通知信息", "通知管理", PermissionLevel.MEDIUM),
    NOTIFICATION_DELETE(OperationTypeConstants.NOTIFICATION_DELETE, "删除通知", "删除通知记录", "通知管理", PermissionLevel.MEDIUM),
    NOTIFICATION_VIEW(OperationTypeConstants.NOTIFICATION_VIEW, "查看通知", "查看通知信息", "通知管理", PermissionLevel.LOW),
    NOTIFICATION_TEMPLATE_CREATE(OperationTypeConstants.NOTIFICATION_TEMPLATE_CREATE, "创建通知模板", "创建消息通知模板", "通知管理", PermissionLevel.MEDIUM),
    NOTIFICATION_TEMPLATE_STATUS(OperationTypeConstants.NOTIFICATION_TEMPLATE_STATUS, "切换通知模板状态", "启用或禁用通知模板", "通知管理", PermissionLevel.MEDIUM),
    NOTIFICATION_READ(OperationTypeConstants.NOTIFICATION_READ, "标记已读", "标记通知为已读", "通知管理", PermissionLevel.LOW),
    BATCH_SEND_NOTIFICATION(OperationTypeConstants.BATCH_SEND_NOTIFICATION, "批量发送通知", "批量向用户发送通知", "通知管理", PermissionLevel.MEDIUM),
    MESSAGE_TEMPLATE_CREATE(OperationTypeConstants.MESSAGE_TEMPLATE_CREATE, "创建消息模板", "创建消息通知模板", "通知管理", PermissionLevel.MEDIUM),
    MESSAGE_TEMPLATE_UPDATE(OperationTypeConstants.MESSAGE_TEMPLATE_UPDATE, "更新消息模板", "更新消息通知模板", "通知管理", PermissionLevel.MEDIUM),
    MESSAGE_TEMPLATE_DELETE(OperationTypeConstants.MESSAGE_TEMPLATE_DELETE, "删除消息模板", "删除消息通知模板", "通知管理", PermissionLevel.MEDIUM),
    MESSAGE_TEMPLATE_STATUS(OperationTypeConstants.MESSAGE_TEMPLATE_STATUS, "切换消息模板状态", "启用或禁用消息模板", "通知管理", PermissionLevel.MEDIUM),

    // ========== 日志管理 ==========
    LOG_EXPORT(OperationTypeConstants.LOG_EXPORT, "导出日志", "导出系统日志", "日志管理", PermissionLevel.MEDIUM),
    LOG_VIEW(OperationTypeConstants.LOG_VIEW, "查看日志", "查看系统日志", "日志管理", PermissionLevel.MEDIUM),
    LOG_CLEAN(OperationTypeConstants.LOG_CLEAN, "清理日志", "清理系统日志", "日志管理", PermissionLevel.HIGH),

    // ========== 管理员操作 ==========
    ADMIN_USER_CREATE(OperationTypeConstants.ADMIN_USER_CREATE, "管理员创建用户", "管理员创建用户账号", "用户管理", PermissionLevel.HIGH),
    ADMIN_USER_UPDATE(OperationTypeConstants.ADMIN_USER_UPDATE, "管理员更新用户信息", "管理员更新用户信息", "用户管理", PermissionLevel.HIGH),
    ADMIN_USER_DELETE(OperationTypeConstants.ADMIN_USER_DELETE, "管理员删除用户", "管理员删除用户账号", "用户管理", PermissionLevel.HIGH),
    ADMIN_USER_BATCH_DELETE(OperationTypeConstants.ADMIN_USER_BATCH_DELETE, "管理员批量删除用户", "管理员批量删除用户账号", "用户管理", PermissionLevel.HIGH),
    ADMIN_USER_STATUS_UPDATE(OperationTypeConstants.ADMIN_USER_STATUS_UPDATE, "管理员更新用户状态", "管理员更新用户账号状态", "用户管理", PermissionLevel.HIGH),
    ADMIN_USER_PASSWORD_RESET(OperationTypeConstants.ADMIN_USER_PASSWORD_RESET, "管理员重置用户密码", "管理员重置用户密码", "用户管理", PermissionLevel.HIGH),
    ADMIN_PAPER_AUDIT(OperationTypeConstants.ADMIN_PAPER_AUDIT, "管理员审核论文", "管理员审核学生论文", "论文管理", PermissionLevel.HIGH),
    ADMIN_PAPER_BATCH_AUDIT(OperationTypeConstants.ADMIN_PAPER_BATCH_AUDIT, "管理员批量审核论文", "管理员批量审核学生论文", "论文管理", PermissionLevel.HIGH),
    ADMIN_PAPER_DELETE(OperationTypeConstants.ADMIN_PAPER_DELETE, "管理员删除论文", "管理员删除论文记录", "论文管理", PermissionLevel.HIGH),

    // ========== 学生论文 ==========
    STUDENT_PAPER_SUBMIT(OperationTypeConstants.STUDENT_PAPER_SUBMIT, "学生提交论文", "学生提交论文", "论文管理", PermissionLevel.MEDIUM),
    STUDENT_PAPER_UPDATE(OperationTypeConstants.STUDENT_PAPER_UPDATE, "学生更新论文信息", "学生更新论文信息", "论文管理", PermissionLevel.MEDIUM),
    STUDENT_PAPER_RESUBMIT(OperationTypeConstants.STUDENT_PAPER_RESUBMIT, "撤回后重新提交", "撤回后重新提交论文", "论文管理", PermissionLevel.MEDIUM),
    STUDENT_PAPER_WITHDRAW(OperationTypeConstants.STUDENT_PAPER_WITHDRAW, "学生撤回论文", "学生撤回已提交的论文", "论文管理", PermissionLevel.MEDIUM),
    STUDENT_PAPER_MODIFY_REQUEST(OperationTypeConstants.STUDENT_PAPER_MODIFY_REQUEST, "学生申请修改论文", "学生申请修改论文", "论文管理", PermissionLevel.MEDIUM),
    STUDENT_PAPER_BATCH_DOWNLOAD(OperationTypeConstants.STUDENT_PAPER_BATCH_DOWNLOAD, "学生批量下载论文", "学生批量下载论文文件", "论文管理", PermissionLevel.MEDIUM),
    STUDENT_PAPER_BATCH_DELETE(OperationTypeConstants.STUDENT_PAPER_BATCH_DELETE, "学生批量删除论文", "学生批量删除论文记录", "论文管理", PermissionLevel.MEDIUM),
    STUDENT_ATTACHMENT_UPLOAD(OperationTypeConstants.STUDENT_ATTACHMENT_UPLOAD, "学生上传论文附件", "学生上传论文附件", "论文管理", PermissionLevel.MEDIUM),
    STUDENT_ATTACHMENT_DELETE(OperationTypeConstants.STUDENT_ATTACHMENT_DELETE, "学生删除论文附件", "学生删除论文附件", "论文管理", PermissionLevel.MEDIUM),

    // ========== 其他操作 ==========
    RECHECK_PLAGIARISM(OperationTypeConstants.RECHECK_PLAGIARISM, "重新查重检测", "重新进行论文查重检测", "查重管理", PermissionLevel.MEDIUM),
    SEND_REMINDER(OperationTypeConstants.SEND_REMINDER, "发送提醒消息", "向用户发送提醒消息", "通知管理", PermissionLevel.MEDIUM);

    private final String type;
    private final String name;
    private final String description;
    private final String module;
    private final PermissionLevel permissionLevel;

    OperationTypeEnum(String type, String name, String description, String module, PermissionLevel permissionLevel) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.module = module;
        this.permissionLevel = permissionLevel;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getModule() {
        return module;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    /**
     * 根据操作类型获取枚举实例
     */
    public static OperationTypeEnum getByType(String type) {
        for (OperationTypeEnum enumValue : values()) {
            if (enumValue.getType().equals(type)) {
                return enumValue;
            }
        }
        return null;
    }

    /**
     * 判断操作类型是否存在
     */
    public static boolean exists(String type) {
        return getByType(type) != null;
    }

    /**
     * 权限等级枚举
     */
    public enum PermissionLevel {
        LOW,    // 低权限
        MEDIUM, // 中权限
        HIGH    // 高权限
    }
}