-- 操作类型配置表
CREATE TABLE IF NOT EXISTS `sys_operation_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` varchar(50) NOT NULL COMMENT '操作类型编码',
  `name` varchar(100) NOT NULL COMMENT '操作类型名称',
  `description` varchar(500) COMMENT '操作类型描述',
  `module` varchar(100) NOT NULL COMMENT '所属模块',
  `permission_level` varchar(20) NOT NULL COMMENT '权限等级(LOW/MEDIUM/HIGH)',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态(1启用,0禁用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作类型配置表';

-- 插入初始操作类型数据
INSERT INTO `sys_operation_type` (`type`, `name`, `description`, `module`, `permission_level`, `status`) VALUES
-- 用户管理
('user_login', '用户登录', '用户登录系统', '用户管理', 'LOW', 1),
('user_logout', '用户登出', '用户退出系统', '用户管理', 'LOW', 1),
('user_register', '用户注册', '用户注册账号', '用户管理', 'LOW', 1),
('user_update', '更新用户信息', '更新用户基本信息', '用户管理', 'MEDIUM', 1),
('user_password_change', '修改密码', '修改用户密码', '用户管理', 'MEDIUM', 1),
('user_role_assign', '分配用户角色', '为用户分配角色权限', '用户管理', 'HIGH', 1),

-- 论文管理
('paper_submit', '提交论文', '学生提交论文', '论文管理', 'MEDIUM', 1),
('paper_update', '更新论文', '更新论文信息', '论文管理', 'MEDIUM', 1),
('paper_delete', '删除论文', '删除论文记录', '论文管理', 'HIGH', 1),
('paper_review', '论文审核', '审核学生论文', '论文管理', 'MEDIUM', 1),
('paper_approve', '论文批准', '批准学生论文', '论文管理', 'MEDIUM', 1),
('paper_reject', '论文拒绝', '拒绝学生论文', '论文管理', 'MEDIUM', 1),

-- 查重管理
('task_create', '创建查重任务', '创建论文查重任务', '查重管理', 'MEDIUM', 1),
('task_submit', '提交查重任务', '提交论文查重任务', '查重管理', 'MEDIUM', 1),
('task_cancel', '取消查重任务', '取消未完成的查重任务', '查重管理', 'MEDIUM', 1),
('task_delete', '删除查重任务', '删除查重任务记录', '查重管理', 'HIGH', 1),
('task_result_view', '查看查重结果', '查看论文查重结果', '查重管理', 'MEDIUM', 1),
('task_result_export', '导出查重结果', '导出论文查重结果', '查重管理', 'MEDIUM', 1),

-- 教师管理
('teacher_assign', '分配导师', '为学生分配导师', '教师管理', 'HIGH', 1),
('teacher_update', '更新教师信息', '更新教师基本信息', '教师管理', 'MEDIUM', 1),
('teacher_student_manage', '管理学生', '教师管理学生信息', '教师管理', 'MEDIUM', 1),
('teacher_review_assign', '分配评审任务', '分配论文评审任务', '教师管理', 'MEDIUM', 1),
('teacher_review_complete', '完成评审任务', '完成论文评审任务', '教师管理', 'MEDIUM', 1),

-- 系统管理
('system_config_update', '更新系统配置', '更新系统基础配置', '系统管理', 'HIGH', 1),
('system_param_update', '更新系统参数', '更新系统运行参数', '系统管理', 'HIGH', 1),
('dict_data_manage', '管理字典数据', '管理系统字典数据', '系统管理', 'MEDIUM', 1),
('permission_manage', '管理权限', '管理系统权限', '系统管理', 'HIGH', 1),
('role_manage', '管理角色', '管理系统角色', '系统管理', 'HIGH', 1),
('notice_send', '发送通知', '发送系统通知', '系统管理', 'MEDIUM', 1),

-- 学生管理
('student_info_update', '更新学生信息', '更新学生基本信息', '学生管理', 'MEDIUM', 1),
('student_paper_manage', '管理学生论文', '管理学生论文信息', '学生管理', 'MEDIUM', 1),
('student_task_view', '查看学生任务', '查看学生任务信息', '学生管理', 'MEDIUM', 1),
('student_submission_manage', '管理学生提交', '管理学生提交记录', '学生管理', 'MEDIUM', 1),

-- 通知管理
('notification_send', '发送通知', '发送系统通知', '通知管理', 'MEDIUM', 1),
('notification_update', '更新通知', '更新通知信息', '通知管理', 'MEDIUM', 1),
('notification_delete', '删除通知', '删除通知记录', '通知管理', 'MEDIUM', 1),
('notification_view', '查看通知', '查看通知信息', '通知管理', 'LOW', 1),

-- 日志管理
('log_export', '导出日志', '导出系统日志', '日志管理', 'MEDIUM', 1),
('log_view', '查看日志', '查看系统日志', '日志管理', 'MEDIUM', 1),
('log_clean', '清理日志', '清理系统日志', '日志管理', 'HIGH', 1);
