package com.abin.checkrepeatsystem.common.constant;

/**
 * 操作类型常量类
 * 定义系统中所有的操作类型
 */
public class OperationTypeConstants {

    // ========== 用户管理 ==========
    public static final String USER_LOGIN = "user_login";
    public static final String USER_LOGOUT = "user_logout";
    public static final String USER_REGISTER = "user_register";
    public static final String USER_UPDATE = "user_update";
    public static final String USER_DELETE = "user_delete";
    public static final String USER_BATCH_DELETE = "user_batch_delete";
    public static final String USER_STATUS_UPDATE = "user_status_update";
    public static final String USER_PASSWORD_CHANGE = "user_password_change";
    public static final String USER_ROLE_ASSIGN = "user_role_assign";
    public static final String USER_UPLOAD_AVATAR = "user_upload_avatar";
    public static final String USER_VERIFY_EMAIL = "user_verify_email";
    public static final String USER_SEND_VERIFY_EMAIL = "user_send_verify_email";
    public static final String USER_SEND_EMAIL_CODE = "user_send_email_code";
    public static final String USER_UPDATE_EMAIL = "user_update_email";
    public static final String USER_LOGIN_HISTORY = "user_login_history";

    // ========== 论文管理 ==========
    public static final String PAPER_SUBMIT = "paper_submit";
    public static final String PAPER_UPDATE = "paper_update";
    public static final String PAPER_DELETE = "paper_delete";
    public static final String PAPER_REVIEW = "paper_review";
    public static final String PAPER_APPROVE = "paper_approve";
    public static final String PAPER_REJECT = "paper_reject";
    public static final String PAPER_BATCH_AUDIT = "paper_batch_audit";
    public static final String PAPER_RESUBMIT = "paper_resubmit";
    public static final String PAPER_WITHDRAW = "paper_withdraw";
    public static final String PAPER_MODIFY_REQUEST = "paper_modify_request";
    public static final String PAPER_BATCH_DOWNLOAD = "paper_batch_download";
    public static final String PAPER_BATCH_DELETE = "paper_batch_delete";
    public static final String ATTACHMENT_UPLOAD = "attachment_upload";
    public static final String ATTACHMENT_DELETE = "attachment_delete";

    // ========== 查重管理 ==========
    public static final String TASK_CREATE = "task_create";
    public static final String TASK_SUBMIT = "task_submit";
    public static final String TASK_CANCEL = "task_cancel";
    public static final String TASK_DELETE = "task_delete";
    public static final String TASK_RESULT_VIEW = "task_result_view";
    public static final String TASK_RESULT_EXPORT = "task_result_export";
    public static final String TASK_BATCH_CREATE = "task_batch_create";
    public static final String CHECK_TASK_LIST = "check_task_list";
    public static final String CHECK_TASK_DETAIL = "check_task_detail";
    public static final String CHECK_TASK_BY_ID = "check_task_by_id";
    public static final String CHECK_TASK_STATUS = "check_task_status";
    public static final String CHECK_TASK_RECHECK = "check_task_recheck";

    // ========== 查重规则 ==========
    public static final String CHECK_RULE_CREATE = "check_rule_create";
    public static final String CHECK_RULE_UPDATE = "check_rule_update";
    public static final String CHECK_RULE_DELETE = "check_rule_delete";

    // ========== 教师管理 ==========
    public static final String TEACHER_ASSIGN = "teacher_assign";
    public static final String TEACHER_UPDATE = "teacher_update";
    public static final String TEACHER_GET_INFO = "teacher_get_info";
    public static final String TEACHER_STUDENT_MANAGE = "teacher_student_manage";
    public static final String TEACHER_REVIEW_ASSIGN = "teacher_review_assign";
    public static final String TEACHER_REVIEW_COMPLETE = "teacher_review_complete";
    public static final String CONTACT_STUDENT = "contact_student";
    public static final String DELEGATE_REVIEW = "delegate_review";
    public static final String DELETE_STUDENT = "delete_student";
    public static final String BATCH_ASSIGN_ADVISOR = "batch_assign_advisor";
    public static final String BATCH_SEND_MESSAGE = "batch_send_message";
    public static final String BATCH_DELETE_STUDENT = "batch_delete_student";
    public static final String EXPORT_STUDENTS = "export_students";
    public static final String ADD_STUDENT = "add_student";
    public static final String IMPORT_STUDENTS = "import_students";
    public static final String SEND_MESSAGE = "send_message";

    // ========== 分配任务 ==========
    public static final String ASSIGNMENT_CREATE = "assignment_create";
    public static final String ASSIGNMENT_UPDATE = "assignment_update";
    public static final String ASSIGNMENT_DELETE = "assignment_delete";

    // ========== 教师分配 ==========
    public static final String TEACHER_ALLOCATION_CREATE = "teacher_allocation_create";
    public static final String TEACHER_ALLOCATION_REVOKE = "teacher_allocation_revoke";
    public static final String TEACHER_ALLOCATION_BATCH_CREATE = "teacher_allocation_batch_create";
    public static final String TEACHER_ASSIGN_CANCEL = "teacher_assign_cancel";
    public static final String BATCH_TEACHER_ASSIGN = "batch_teacher_assign";

    // ========== 自动分配 ==========
    public static final String AUTO_ALLOCATION_CREATE = "auto_allocation_create";

    // ========== 系统管理 ==========
    public static final String SYSTEM_CONFIG_UPDATE = "system_config_update";
    public static final String SYSTEM_PARAM_UPDATE = "system_param_update";
    public static final String SYSTEM_CONFIG_RESET = "system_config_reset";
    public static final String SYSTEM_PARAM_INIT = "system_param_init";
    public static final String SYSTEM_MAINTENANCE = "system_maintenance";
    public static final String DICT_DATA_MANAGE = "dict_data_manage";
    public static final String PERMISSION_MANAGE = "permission_manage";
    public static final String ROLE_MANAGE = "role_manage";
    public static final String ROLE_LIST = "role_list";
    public static final String ROLE_CREATE = "role_create";
    public static final String ROLE_UPDATE = "role_update";
    public static final String ROLE_DELETE = "role_delete";
    public static final String PERMISSION_TREE = "permission_tree";
    public static final String USER_ASSIGN_ROLE = "user_assign_role";

    // ========== 配置管理 ==========
    public static final String CONFIG_GET_ALL = "config_get_all";
    public static final String CONFIG_BASIC_UPDATE = "config_basic_update";
    public static final String CONFIG_PLAGIARISM_UPDATE = "config_plagiarism_update";
    public static final String CONFIG_SECURITY_UPDATE = "config_security_update";
    public static final String CONFIG_EMAIL_UPDATE = "config_email_update";
    public static final String CONFIG_PERFORMANCE_UPDATE = "config_performance_update";
    public static final String CONFIG_SAVE_ALL = "config_save_all";
    public static final String CONFIG_TEST_EMAIL = "config_test_email";
    public static final String CONFIG_EXPORT = "config_export";
    public static final String CONFIG_RESET_DEFAULT = "config_reset_default";
    public static final String CONFIG_REFRESH = "config_refresh";

    // ========== 学生管理 ==========
    public static final String STUDENT_INFO_UPDATE = "student_info_update";
    public static final String STUDENT_PAPER_MANAGE = "student_paper_manage";
    public static final String STUDENT_TASK_VIEW = "student_task_view";
    public static final String STUDENT_SUBMISSION_MANAGE = "student_submission_manage";

    // ========== 通知管理 ==========
    public static final String NOTICE_SEND = "notice_send";
    public static final String NOTIFICATION_SEND = "notification_send";
    public static final String NOTIFICATION_UPDATE = "notification_update";
    public static final String NOTIFICATION_DELETE = "notification_delete";
    public static final String NOTIFICATION_VIEW = "notification_view";
    public static final String NOTIFICATION_TEMPLATE_CREATE = "notification_template_create";
    public static final String NOTIFICATION_TEMPLATE_STATUS = "notification_template_status";
    public static final String NOTIFICATION_READ = "notification_read";
    public static final String BATCH_SEND_NOTIFICATION = "batch_send_notification";
    public static final String MESSAGE_TEMPLATE_CREATE = "message_template_create";
    public static final String MESSAGE_TEMPLATE_UPDATE = "message_template_update";
    public static final String MESSAGE_TEMPLATE_DELETE = "message_template_delete";
    public static final String MESSAGE_TEMPLATE_STATUS = "message_template_status";

    // ========== 日志管理 ==========
    public static final String LOG_EXPORT = "log_export";
    public static final String LOG_VIEW = "log_view";
    public static final String LOG_CLEAN = "log_clean";

    // ========== 管理员操作 ==========
    public static final String ADMIN_USER_CREATE = "admin_user_create";
    public static final String ADMIN_USER_UPDATE = "admin_user_update";
    public static final String ADMIN_USER_DELETE = "admin_user_delete";
    public static final String ADMIN_USER_BATCH_DELETE = "admin_user_batch_delete";
    public static final String ADMIN_USER_STATUS_UPDATE = "admin_user_status_update";
    public static final String ADMIN_USER_PASSWORD_RESET = "admin_user_password_reset";
    public static final String ADMIN_PAPER_AUDIT = "admin_paper_audit";
    public static final String ADMIN_PAPER_BATCH_AUDIT = "admin_paper_batch_audit";
    public static final String ADMIN_PAPER_DELETE = "admin_paper_delete";

    // ========== 查重任务（学生端） ==========
    public static final String CHECK_TASK_CREATE = "check_task_create";
    public static final String CHECK_TASK_CANCEL = "check_task_cancel";
    public static final String CHECK_TASK_DELETE = "check_task_delete";
    public static final String CHECK_TASK_BATCH_CREATE = "check_task_batch_create";

    // ========== 学生论文 ==========
    public static final String STUDENT_PAPER_SUBMIT = "student_paper_submit";
    public static final String STUDENT_PAPER_UPDATE = "student_paper_update";
    public static final String STUDENT_PAPER_RESUBMIT = "student_paper_resubmit";
    public static final String STUDENT_PAPER_WITHDRAW = "student_paper_withdraw";
    public static final String STUDENT_PAPER_MODIFY_REQUEST = "student_paper_modify_request";
    public static final String STUDENT_PAPER_BATCH_DOWNLOAD = "student_paper_batch_download";
    public static final String STUDENT_PAPER_BATCH_DELETE = "student_paper_batch_delete";
    public static final String STUDENT_ATTACHMENT_UPLOAD = "student_attachment_upload";
    public static final String STUDENT_ATTACHMENT_DELETE = "student_attachment_delete";

    // ========== 重新查重 ==========
    public static final String RECHECK_PLAGIARISM = "recheck_plagiarism";
    public static final String SEND_REMINDER = "send_reminder";
}