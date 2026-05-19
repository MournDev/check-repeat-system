-- =====================================================
-- 日志表操作类型匹配与更新脚本
-- 此脚本用于将现有日志表中的操作类型与新插入的标准化操作类型进行匹配和更新
-- =====================================================

-- 1. 查看现有日志表中的操作类型分布
SELECT operation_type, COUNT(*) as count 
FROM sys_operation_log 
GROUP BY operation_type 
ORDER BY count DESC;

-- 2. 查看现有日志表中与新操作类型表不匹配的操作类型
SELECT DISTINCT operation_type 
FROM sys_operation_log 
WHERE operation_type NOT IN (SELECT type FROM sys_operation_type);

-- 3. 创建操作类型映射表（如果不存在）
CREATE TABLE IF NOT EXISTS `sys_operation_type_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `old_type` varchar(50) NOT NULL COMMENT '旧操作类型编码',
  `new_type` varchar(50) NOT NULL COMMENT '新操作类型编码',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态(1已映射,0未映射)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_old_type` (`old_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作类型映射表';

-- 4. 插入常见操作类型的映射关系
INSERT IGNORE INTO `sys_operation_type_mapping` (`old_type`, `new_type`, `status`) VALUES
-- 登录相关
('login', 'user_login', 1),
('logout', 'user_logout', 1),
('user_login', 'user_login', 1),
('user_logout', 'user_logout', 1),

-- 论文相关
('submit_paper', 'paper_submit', 1),
('review_paper', 'paper_review', 1),
('paper_submit', 'paper_submit', 1),
('paper_review', 'paper_review', 1),
('paper_approve', 'paper_approve', 1),
('paper_reject', 'paper_reject', 1),

-- 分配相关
('assign_advisor', 'teacher_assign', 1),
('teacher_assign', 'teacher_assign', 1),

-- 任务相关
('create_task', 'task_create', 1),
('submit_task', 'task_submit', 1),
('cancel_task', 'task_cancel', 1),
('delete_task', 'task_delete', 1),
('task_create', 'task_create', 1),
('task_submit', 'task_submit', 1),
('task_cancel', 'task_cancel', 1),
('task_delete', 'task_delete', 1),

-- 系统相关
('update_config', 'system_config_update', 1),
('system_config_update', 'system_config_update', 1),
('update_param', 'system_param_update', 1),
('system_param_update', 'system_param_update', 1),

-- 教师相关
('update_teacher', 'teacher_update', 1),
('teacher_update', 'teacher_update', 1),
('manage_student', 'teacher_student_manage', 1),
('teacher_student_manage', 'teacher_student_manage', 1),

-- 学生相关
('update_student', 'student_info_update', 1),
('student_info_update', 'student_info_update', 1),
('manage_paper', 'student_paper_manage', 1),
('student_paper_manage', 'student_paper_manage', 1),

-- 通知相关
('send_notice', 'notice_send', 1),
('notice_send', 'notice_send', 1),
('send_notification', 'notification_send', 1),
('notification_send', 'notification_send', 1),

-- 日志相关
('export_log', 'log_export', 1),
('log_export', 'log_export', 1),
('view_log', 'log_view', 1),
('log_view', 'log_view', 1),
('clean_log', 'log_clean', 1),
('log_clean', 'log_clean', 1);

-- 5. 执行操作类型更新
-- 注意：在执行此更新前，请先备份 sys_operation_log 表
UPDATE `sys_operation_log` log
INNER JOIN `sys_operation_type_mapping` mapping ON log.`operation_type` = mapping.`old_type`
SET log.`operation_type` = mapping.`new_type`
WHERE mapping.`status` = 1;

-- 6. 查看更新结果
SELECT 
    '更新前操作类型数量' AS 项目, 
    (SELECT COUNT(DISTINCT operation_type) FROM sys_operation_log) AS 数量
UNION ALL
SELECT 
    '更新后操作类型数量' AS 项目, 
    (SELECT COUNT(DISTINCT operation_type) FROM sys_operation_log) AS 数量
UNION ALL
SELECT 
    '已匹配的操作类型' AS 项目, 
    (SELECT COUNT(DISTINCT operation_type) FROM sys_operation_log WHERE operation_type IN (SELECT type FROM sys_operation_type)) AS 数量
UNION ALL
SELECT 
    '未匹配的操作类型' AS 项目, 
    (SELECT COUNT(DISTINCT operation_type) FROM sys_operation_log WHERE operation_type NOT IN (SELECT type FROM sys_operation_type)) AS 数量;

-- 7. 查看未匹配的操作类型（需要手动处理）
SELECT DISTINCT operation_type, COUNT(*) as count
FROM sys_operation_log 
WHERE operation_type NOT IN (SELECT type FROM sys_operation_type)
GROUP BY operation_type 
ORDER BY count DESC;

-- 8. 生成未匹配操作类型的插入语句（可手动调整）
SELECT DISTINCT 
    CONCAT(
        "INSERT IGNORE INTO `sys_operation_type` (`type`, `name`, `description`, `module`, `permission_level`, `status`) VALUES ('",
        operation_type, "', '",
        operation_type, "', '自动生成: ",
        operation_type, "', '其他', 'MEDIUM', 1);"
    ) AS insert_sql
FROM sys_operation_log 
WHERE operation_type NOT IN (SELECT type FROM sys_operation_type);

-- =====================================================
-- 执行建议
-- =====================================================
-- 1. 先执行步骤 1-2，查看现有数据情况
-- 2. 执行步骤 3-4，创建映射表并插入映射关系
-- 3. 备份 sys_operation_log 表
-- 4. 执行步骤 5，更新操作类型
-- 5. 执行步骤 6-7，查看更新结果
-- 6. 对于未匹配的操作类型，执行步骤 8 生成的插入语句
-- 7. 再次执行步骤 5，更新剩余的操作类型
-- =====================================================
