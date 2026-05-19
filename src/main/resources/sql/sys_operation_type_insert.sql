-- =====================================================
-- 补充插入缺失的操作类型SQL脚本
-- 执行此脚本前请先执行 sys_operation_type.sql 创建表结构
-- =====================================================

-- 插入缺失的操作类型（使用 INSERT IGNORE 避免重复插入）
INSERT IGNORE INTO `sys_operation_type` (`type`, `name`, `description`, `module`, `permission_level`, `status`) VALUES

-- ========== 用户管理补充 ==========
('user_delete', '删除用户', '删除用户账号', '用户管理', 'HIGH', 1),
('user_batch_delete', '批量删除用户', '批量删除用户账号', '用户管理', 'HIGH', 1),
('user_status_update', '更新用户状态', '更新用户账号状态', '用户管理', 'MEDIUM', 1),
('user_upload_avatar', '上传头像', '上传用户头像', '用户管理', 'LOW', 1),
('user_verify_email', '验证邮箱', '验证用户邮箱地址', '用户管理', 'LOW', 1),
('user_send_verify_email', '发送邮箱验证邮件', '发送邮箱验证邮件到用户邮箱', '用户管理', 'LOW', 1),
('user_send_email_code', '发送邮箱验证码', '发送邮箱验证码到用户邮箱', '用户管理', 'LOW', 1),
('user_update_email', '更新用户邮箱', '更新用户邮箱地址', '用户管理', 'MEDIUM', 1),
('user_login_history', '查询登录历史', '查询用户登录历史记录', '用户管理', 'LOW', 1),

-- ========== 论文管理补充 ==========
('paper_batch_audit', '批量审核论文', '批量审核学生论文', '论文管理', 'HIGH', 1),
('paper_resubmit', '重新提交论文', '学生重新提交论文', '论文管理', 'MEDIUM', 1),
('paper_withdraw', '撤回论文', '学生撤回已提交的论文', '论文管理', 'MEDIUM', 1),
('paper_modify_request', '申请修改论文', '学生申请修改论文', '论文管理', 'MEDIUM', 1),
('paper_batch_download', '批量下载论文', '批量下载论文文件', '论文管理', 'MEDIUM', 1),
('paper_batch_delete', '批量删除论文', '批量删除论文记录', '论文管理', 'HIGH', 1),
('attachment_upload', '上传附件', '上传论文附件', '论文管理', 'MEDIUM', 1),
('attachment_delete', '删除附件', '删除论文附件', '论文管理', 'MEDIUM', 1),

-- ========== 查重管理补充 ==========
('task_batch_create', '批量创建查重任务', '批量创建论文查重任务', '查重管理', 'MEDIUM', 1),
('check_task_list', '查询查重任务列表', '查询查重任务列表', '查重管理', 'LOW', 1),
('check_task_detail', '查询查重任务详情', '查询查重任务详情', '查重管理', 'LOW', 1),
('check_task_by_id', '查询指定任务详情', '根据ID查询查重任务详情', '查重管理', 'LOW', 1),
('check_task_status', '获取查重任务状态', '获取查重任务状态', '查重管理', 'LOW', 1),
('check_task_recheck', '重新发起查重', '重新发起论文查重', '查重管理', 'MEDIUM', 1),

-- ========== 查重规则 ==========
('check_rule_create', '创建查重规则', '创建论文查重规则', '查重管理', 'HIGH', 1),
('check_rule_update', '更新查重规则', '更新论文查重规则', '查重管理', 'HIGH', 1),
('check_rule_delete', '删除查重规则', '删除论文查重规则', '查重管理', 'HIGH', 1),

-- ========== 教师管理补充 ==========
('teacher_get_info', '获取教师信息', '获取教师个人信息', '教师管理', 'LOW', 1),
('contact_student', '联系学生', '教师联系学生', '教师管理', 'MEDIUM', 1),
('delegate_review', '委托审核', '委托其他教师审核', '教师管理', 'MEDIUM', 1),
('delete_student', '删除学生', '删除学生账号', '教师管理', 'HIGH', 1),
('batch_assign_advisor', '批量分配导师', '批量为学生分配导师', '教师管理', 'HIGH', 1),
('batch_send_message', '批量发送消息', '批量向学生发送消息', '教师管理', 'MEDIUM', 1),
('batch_delete_student', '批量删除学生', '批量删除学生账号', '教师管理', 'HIGH', 1),
('export_students', '导出学生数据', '导出学生信息到文件', '教师管理', 'MEDIUM', 1),
('add_student', '添加学生', '添加学生账号', '教师管理', 'MEDIUM', 1),
('import_students', '导入学生数据', '从文件导入学生信息', '教师管理', 'MEDIUM', 1),
('send_message', '发送消息', '向用户发送消息', '教师管理', 'MEDIUM', 1),

-- ========== 分配任务 ==========
('assignment_create', '创建分配任务', '创建分配任务记录', '教师管理', 'MEDIUM', 1),
('assignment_update', '更新分配任务', '更新分配任务信息', '教师管理', 'MEDIUM', 1),
('assignment_delete', '删除分配任务', '删除分配任务记录', '教师管理', 'HIGH', 1),

-- ========== 教师分配 ==========
('teacher_allocation_create', '创建教师分配记录', '创建教师分配记录', '教师管理', 'MEDIUM', 1),
('teacher_allocation_revoke', '撤销教师分配记录', '撤销已分配的教师', '教师管理', 'HIGH', 1),
('teacher_allocation_batch_create', '批量创建教师分配记录', '批量创建教师分配记录', '教师管理', 'HIGH', 1),
('teacher_assign_cancel', '取消导师分配', '取消已分配的导师', '教师管理', 'HIGH', 1),
('batch_teacher_assign', '批量分配导师', '批量为学生分配导师', '教师管理', 'HIGH', 1),

-- ========== 自动分配 ==========
('auto_allocation_create', '创建自动分配记录', '创建自动分配历史记录', '教师管理', 'MEDIUM', 1),

-- ========== 配置管理 ==========
('config_get_all', '获取所有配置', '获取系统所有配置信息', '系统管理', 'LOW', 1),
('config_basic_update', '更新基础配置', '更新系统基础配置', '系统管理', 'HIGH', 1),
('config_plagiarism_update', '更新查重配置', '更新查重相关配置', '系统管理', 'HIGH', 1),
('config_security_update', '更新安全配置', '更新系统安全配置', '系统管理', 'HIGH', 1),
('config_email_update', '更新邮件配置', '更新邮件发送配置', '系统管理', 'HIGH', 1),
('config_performance_update', '更新性能配置', '更新系统性能配置', '系统管理', 'HIGH', 1),
('config_save_all', '保存全部配置', '保存所有系统配置', '系统管理', 'HIGH', 1),
('config_test_email', '测试邮件配置', '测试邮件发送配置', '系统管理', 'MEDIUM', 1),
('config_export', '导出配置', '导出系统配置到文件', '系统管理', 'MEDIUM', 1),
('config_reset_default', '恢复默认配置', '将系统配置恢复为默认值', '系统管理', 'HIGH', 1),
('config_refresh', '刷新配置', '刷新系统配置缓存', '系统管理', 'MEDIUM', 1),

-- ========== 角色管理 ==========
('role_list', '获取角色列表', '获取系统角色列表', '系统管理', 'LOW', 1),
('role_create', '创建角色', '创建系统角色', '系统管理', 'HIGH', 1),
('role_update', '更新角色', '更新系统角色', '系统管理', 'HIGH', 1),
('role_delete', '删除角色', '删除系统角色', '系统管理', 'HIGH', 1),
('permission_tree', '获取权限树', '获取系统权限树', '系统管理', 'LOW', 1),
('user_assign_role', '分配用户角色', '为用户分配系统角色', '系统管理', 'HIGH', 1),

-- ========== 系统管理补充 ==========
('system_config_reset', '重置系统配置', '将系统配置恢复为默认值', '系统管理', 'HIGH', 1),
('system_param_init', '初始化系统参数', '初始化系统运行参数', '系统管理', 'HIGH', 1),
('system_maintenance', '系统维护设置', '设置系统维护状态', '系统管理', 'HIGH', 1),

-- ========== 通知管理补充 ==========
('notification_template_create', '创建通知模板', '创建消息通知模板', '通知管理', 'MEDIUM', 1),
('notification_template_status', '切换通知模板状态', '启用或禁用通知模板', '通知管理', 'MEDIUM', 1),
('notification_read', '标记已读', '标记通知为已读', '通知管理', 'LOW', 1),
('batch_send_notification', '批量发送通知', '批量向用户发送通知', '通知管理', 'MEDIUM', 1),
('message_template_create', '创建消息模板', '创建消息通知模板', '通知管理', 'MEDIUM', 1),
('message_template_update', '更新消息模板', '更新消息通知模板', '通知管理', 'MEDIUM', 1),
('message_template_delete', '删除消息模板', '删除消息通知模板', '通知管理', 'MEDIUM', 1),
('message_template_status', '切换消息模板状态', '启用或禁用消息模板', '通知管理', 'MEDIUM', 1),

-- ========== 管理员操作 ==========
('admin_user_create', '管理员创建用户', '管理员创建用户账号', '用户管理', 'HIGH', 1),
('admin_user_update', '管理员更新用户信息', '管理员更新用户信息', '用户管理', 'HIGH', 1),
('admin_user_delete', '管理员删除用户', '管理员删除用户账号', '用户管理', 'HIGH', 1),
('admin_user_batch_delete', '管理员批量删除用户', '管理员批量删除用户账号', '用户管理', 'HIGH', 1),
('admin_user_status_update', '管理员更新用户状态', '管理员更新用户账号状态', '用户管理', 'HIGH', 1),
('admin_user_password_reset', '管理员重置用户密码', '管理员重置用户密码', '用户管理', 'HIGH', 1),
('admin_paper_audit', '管理员审核论文', '管理员审核学生论文', '论文管理', 'HIGH', 1),
('admin_paper_batch_audit', '管理员批量审核论文', '管理员批量审核学生论文', '论文管理', 'HIGH', 1),
('admin_paper_delete', '管理员删除论文', '管理员删除论文记录', '论文管理', 'HIGH', 1),

-- ========== 学生论文 ==========
('student_paper_submit', '学生提交论文', '学生提交论文', '论文管理', 'MEDIUM', 1),
('student_paper_update', '学生更新论文信息', '学生更新论文信息', '论文管理', 'MEDIUM', 1),
('student_paper_resubmit', '撤回后重新提交', '撤回后重新提交论文', '论文管理', 'MEDIUM', 1),
('student_paper_withdraw', '学生撤回论文', '学生撤回已提交的论文', '论文管理', 'MEDIUM', 1),
('student_paper_modify_request', '学生申请修改论文', '学生申请修改论文', '论文管理', 'MEDIUM', 1),
('student_paper_batch_download', '学生批量下载论文', '学生批量下载论文文件', '论文管理', 'MEDIUM', 1),
('student_paper_batch_delete', '学生批量删除论文', '学生批量删除论文记录', '论文管理', 'MEDIUM', 1),
('student_attachment_upload', '学生上传论文附件', '学生上传论文附件', '论文管理', 'MEDIUM', 1),
('student_attachment_delete', '学生删除论文附件', '学生删除论文附件', '论文管理', 'MEDIUM', 1),

-- ========== 其他操作 ==========
('recheck_plagiarism', '重新查重检测', '重新进行论文查重检测', '查重管理', 'MEDIUM', 1),
('send_reminder', '发送提醒消息', '向用户发送提醒消息', '通知管理', 'MEDIUM', 1);

-- =====================================================
-- 验证插入结果
-- =====================================================
SELECT '操作类型总数' AS 项目, COUNT(*) AS 数量 FROM sys_operation_type
UNION ALL
SELECT '按模块分布' AS 项目, GROUP_CONCAT(DISTINCT module) FROM sys_operation_type
UNION ALL
SELECT '按权限等级分布' AS 项目, GROUP_CONCAT(DISTINCT permission_level) FROM sys_operation_type;

-- 查看各模块的操作类型数量
SELECT module, COUNT(*) as count FROM sys_operation_type GROUP BY module ORDER BY count DESC;

-- 查看权限等级分布
SELECT permission_level, COUNT(*) as count FROM sys_operation_type GROUP BY permission_level;
