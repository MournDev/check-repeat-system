-- 操作类型数据迁移脚本
-- 将历史数据中的操作类型映射到新的标准化操作类型

-- 1. 创建临时映射表用于存储旧类型到新类型的映射关系
CREATE TABLE IF NOT EXISTS `sys_operation_type_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `old_type` varchar(50) NOT NULL COMMENT '旧操作类型编码',
  `new_type` varchar(50) NOT NULL COMMENT '新操作类型编码',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态(1已映射,0未映射)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_old_type` (`old_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作类型映射表';

-- 2. 插入旧类型到新类型的映射关系
INSERT INTO `sys_operation_type_mapping` (`old_type`, `new_type`, `status`) VALUES
-- 用户管理
('admin_user_create', 'user_register', 1),
('admin_user_update', 'user_update', 1),
('admin_user_delete', 'user_delete', 1),
('admin_user_batch_delete', 'user_batch_delete', 1),
('admin_user_status_update', 'user_status_update', 1),
('admin_user_password_reset', 'user_password_change', 1),

-- 论文管理
('admin_paper_audit', 'paper_review', 1),
('admin_paper_batch_audit', 'paper_batch_audit', 1),
('admin_paper_delete', 'paper_delete', 1),

-- 教师相关
('paper_review', 'paper_review', 1),
('recheck_plagiarism', 'task_submit', 1),
('send_reminder', 'notification_send', 1),
('contact_student', 'contact_student', 1),
('delegate_review', 'delegate_review', 1),
('delete_student', 'delete_student', 1),
('batch_assign_advisor', 'batch_assign_advisor', 1),
('batch_send_message', 'batch_send_message', 1),
('batch_delete_student', 'batch_delete_student', 1),
('export_students', 'export_students', 1),
('add_student', 'add_student', 1),
('import_students', 'import_students', 1),

-- 查重任务
('check_task_create', 'task_create', 1),
('check_task_cancel', 'task_cancel', 1),
('check_task_delete', 'task_delete', 1),
('check_task_recheck', 'task_submit', 1),
('check_task_batch_create', 'task_batch_create', 1),

-- 教师信息
('teacher_update_info', 'teacher_update', 1),
('teacher_change_password', 'user_password_change', 1),

-- 系统配置
('config_basic_update', 'system_config_update', 1),
('config_plagiarism_update', 'system_config_update', 1),
('config_security_update', 'system_config_update', 1),
('config_email_update', 'system_config_update', 1),
('config_performance_update', 'system_config_update', 1),
('config_save_all', 'system_config_update', 1),
('config_export', 'log_export', 1),
('config_reset_default', 'system_config_reset', 1),

-- 权限管理
('role_create', 'role_manage', 1),
('role_update', 'role_manage', 1),
('role_delete', 'role_manage', 1),
('user_assign_role', 'user_role_assign', 1),

-- 学生信息
('user_update_info', 'user_update', 1),
('user_upload_avatar', 'user_upload_avatar', 1),
('user_change_password', 'user_password_change', 1),
('user_send_verify_email', 'notification_send', 1),
('user_verify_email', 'user_verify_email', 1),
('user_send_email_code', 'notification_send', 1),
('user_update_email', 'user_update', 1),

-- 学生论文
('student_paper_submit', 'paper_submit', 1),
('student_paper_update', 'paper_update', 1),
('student_paper_resubmit', 'paper_resubmit', 1),
('student_paper_withdraw', 'paper_withdraw', 1),
('student_paper_modify_request', 'paper_modify_request', 1),
('student_paper_batch_download', 'paper_batch_download', 1),
('student_paper_batch_delete', 'paper_delete', 1),
('student_attachment_upload', 'attachment_upload', 1),
('student_attachment_delete', 'attachment_delete', 1),

-- 消息模板
('message_template_create', 'notification_template_create', 1),
('message_template_update', 'notification_update', 1),
('message_template_delete', 'notification_delete', 1),
('message_template_status', 'notification_template_status', 1),

-- 系统参数
('system_param_init', 'system_param_init', 1),
('system_maintenance_status', 'system_maintenance', 1),

-- 教师分配
('teacher_allocation_create', 'teacher_assign', 1),
('teacher_allocation_revoke', 'teacher_assign_cancel', 1),
('teacher_allocation_batch_create', 'batch_teacher_assign', 1),

-- 字典数据
('dict_data_create', 'dict_data_manage', 1),
('dict_data_update', 'dict_data_manage', 1),
('dict_data_delete', 'dict_data_manage', 1),
('dict_data_batch_delete', 'dict_data_manage', 1),

-- 自动分配
('auto_allocation_create', 'teacher_assign', 1),

-- 查重规则
('check_rule_create', 'check_rule_create', 1),
('check_rule_update', 'check_rule_update', 1),
('check_rule_delete', 'check_rule_delete', 1),

-- 通知
('notice_create', 'notification_send', 1),
('notice_update', 'notification_update', 1),
('notice_delete', 'notification_delete', 1),
('notice_publish', 'notification_send', 1),

-- 分配任务
('assignment_create', 'teacher_assign', 1),
('assignment_update', 'assignment_update', 1),
('assignment_delete', 'assignment_delete', 1),

-- 导师分配
('advisor_assign', 'teacher_assign', 1),
('advisor_cancel', 'teacher_assign_cancel', 1);

-- 3. 添加缺失的新操作类型到sys_operation_type表
INSERT INTO `sys_operation_type` (`type`, `name`, `description`, `module`, `permission_level`, `status`) VALUES
-- 用户管理补充
('user_delete', '删除用户', '删除用户账号', '用户管理', 'HIGH', 1),
('user_batch_delete', '批量删除用户', '批量删除用户账号', '用户管理', 'HIGH', 1),
('user_status_update', '更新用户状态', '更新用户账号状态', '用户管理', 'MEDIUM', 1),
('user_upload_avatar', '上传头像', '上传用户头像', '用户管理', 'LOW', 1),
('user_verify_email', '验证邮箱', '验证用户邮箱地址', '用户管理', 'LOW', 1),

-- 论文管理补充
('paper_batch_audit', '批量审核论文', '批量审核学生论文', '论文管理', 'HIGH', 1),
('paper_resubmit', '重新提交论文', '学生重新提交论文', '论文管理', 'MEDIUM', 1),
('paper_withdraw', '撤回论文', '学生撤回已提交的论文', '论文管理', 'MEDIUM', 1),
('paper_modify_request', '申请修改论文', '学生申请修改论文', '论文管理', 'MEDIUM', 1),
('paper_batch_download', '批量下载论文', '批量下载论文文件', '论文管理', 'MEDIUM', 1),
('attachment_upload', '上传附件', '上传论文附件', '论文管理', 'MEDIUM', 1),
('attachment_delete', '删除附件', '删除论文附件', '论文管理', 'MEDIUM', 1),

-- 查重管理补充
('task_batch_create', '批量创建查重任务', '批量创建论文查重任务', '查重管理', 'MEDIUM', 1),

-- 教师管理补充
('contact_student', '联系学生', '教师联系学生', '教师管理', 'MEDIUM', 1),
('delegate_review', '委托审核', '委托其他教师审核', '教师管理', 'MEDIUM', 1),
('delete_student', '删除学生', '删除学生账号', '教师管理', 'HIGH', 1),
('batch_assign_advisor', '批量分配导师', '批量为学生分配导师', '教师管理', 'HIGH', 1),
('batch_send_message', '批量发送消息', '批量向学生发送消息', '教师管理', 'MEDIUM', 1),
('batch_delete_student', '批量删除学生', '批量删除学生账号', '教师管理', 'HIGH', 1),
('export_students', '导出学生数据', '导出学生信息到文件', '教师管理', 'MEDIUM', 1),
('add_student', '添加学生', '添加学生账号', '教师管理', 'MEDIUM', 1),
('import_students', '导入学生数据', '从文件导入学生信息', '教师管理', 'MEDIUM', 1),

-- 系统管理补充
('system_config_reset', '重置系统配置', '将系统配置恢复为默认值', '系统管理', 'HIGH', 1),
('system_param_init', '初始化系统参数', '初始化系统运行参数', '系统管理', 'HIGH', 1),
('system_maintenance', '系统维护设置', '设置系统维护状态', '系统管理', 'HIGH', 1),

-- 通知管理补充
('notification_template_create', '创建通知模板', '创建消息通知模板', '通知管理', 'MEDIUM', 1),
('notification_template_status', '切换通知模板状态', '启用或禁用通知模板', '通知管理', 'MEDIUM', 1),
('notification_read', '标记已读', '标记通知为已读', '通知管理', 'LOW', 1),

-- 分配管理补充
('teacher_assign_cancel', '取消导师分配', '取消已分配的导师', '教师管理', 'HIGH', 1),
('batch_teacher_assign', '批量分配导师', '批量为学生分配导师', '教师管理', 'HIGH', 1),
('assignment_update', '更新分配任务', '更新分配任务信息', '教师管理', 'MEDIUM', 1),
('assignment_delete', '删除分配任务', '删除分配任务记录', '教师管理', 'HIGH', 1),

-- 查重规则管理
('check_rule_create', '创建查重规则', '创建论文查重规则', '查重管理', 'HIGH', 1),
('check_rule_update', '更新查重规则', '更新论文查重规则', '查重管理', 'HIGH', 1),
('check_rule_delete', '删除查重规则', '删除论文查重规则', '查重管理', 'HIGH', 1),

-- 登录相关
('user_register', '用户注册', '用户注册账号', '用户管理', 'LOW', 1),
('user_login', '用户登录', '用户登录系统', '用户管理', 'LOW', 1),
('user_logout', '用户登出', '用户退出系统', '用户管理', 'LOW', 1),

-- 其他常用操作
('send_message', '发送消息', '向用户发送消息', '通知管理', 'MEDIUM', 1),
('batch_send_notification', '批量发送通知', '批量向用户发送通知', '通知管理', 'MEDIUM', 1)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- 4. 更新历史操作日志表中的操作类型
-- 注意：在执行此更新前，请先备份 sys_operation_log 表
-- 此更新将把旧的操作类型映射到新的标准化操作类型
UPDATE `sys_operation_log` log
INNER JOIN `sys_operation_type_mapping` mapping ON log.`operation_type` = mapping.`old_type`
SET log.`operation_type` = mapping.`new_type`
WHERE mapping.`status` = 1;

-- 5. 标记未映射的操作类型（可选，用于后续处理）
-- 将无法映射的操作类型记录到临时表
CREATE TABLE IF NOT EXISTS `sys_unmapped_operation_types` AS
SELECT DISTINCT operation_type FROM sys_operation_log
WHERE operation_type NOT IN (SELECT old_type FROM sys_operation_type_mapping);

-- 6. 清理临时表（可选）
-- DROP TABLE IF EXISTS sys_operation_type_mapping;
-- DROP TABLE IF EXISTS sys_unmapped_operation_types;
