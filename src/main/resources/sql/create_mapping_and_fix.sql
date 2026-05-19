-- =====================================================
-- 修复字符集冲突并创建映射表的脚本
-- 解决 "Illegal mix of collations" 错误
-- =====================================================

-- 1. 先创建映射表（如果不存在）
CREATE TABLE IF NOT EXISTS `sys_operation_type_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `old_type` varchar(50) NOT NULL COMMENT '旧操作类型编码',
  `new_type` varchar(50) NOT NULL COMMENT '新操作类型编码',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态(1已映射,0未映射)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_old_type` (`old_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作类型映射表';

-- 2. 插入常见操作类型的映射关系
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

-- 3. 查看表的字符集和排序规则
SHOW CREATE TABLE sys_operation_log;
SHOW CREATE TABLE sys_operation_type;
SHOW CREATE TABLE sys_operation_type_mapping;

-- 4. 执行更新操作，使用 COLLATE 子句解决字符集冲突
UPDATE `sys_operation_log` log
INNER JOIN `sys_operation_type_mapping` mapping 
ON log.`operation_type` COLLATE utf8mb4_unicode_ci = mapping.`old_type` COLLATE utf8mb4_unicode_ci
SET log.`operation_type` = mapping.`new_type`
WHERE mapping.`status` = 1;

-- 5. 查看更新结果
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
    (SELECT COUNT(DISTINCT operation_type) FROM sys_operation_log 
     WHERE operation_type COLLATE utf8mb4_unicode_ci IN 
     (SELECT type COLLATE utf8mb4_unicode_ci FROM sys_operation_type)) AS 数量
UNION ALL
SELECT 
    '未匹配的操作类型' AS 项目, 
    (SELECT COUNT(DISTINCT operation_type) FROM sys_operation_log 
     WHERE operation_type COLLATE utf8mb4_unicode_ci NOT IN 
     (SELECT type COLLATE utf8mb4_unicode_ci FROM sys_operation_type)) AS 数量;

-- 6. 查看未匹配的操作类型
SELECT DISTINCT operation_type, COUNT(*) as count
FROM sys_operation_log 
WHERE operation_type COLLATE utf8mb4_unicode_ci NOT IN 
(SELECT type COLLATE utf8mb4_unicode_ci FROM sys_operation_type)
GROUP BY operation_type 
ORDER BY count DESC;

-- 7. 生成未匹配操作类型的插入语句
SELECT DISTINCT 
    CONCAT(
        "INSERT IGNORE INTO `sys_operation_type` (`type`, `name`, `description`, `module`, `permission_level`, `status`) VALUES ('",
        operation_type, "', '",
        operation_type, "', '自动生成: ",
        operation_type, "', '其他', 'MEDIUM', 1);"
    ) AS insert_sql
FROM sys_operation_log 
WHERE operation_type COLLATE utf8mb4_unicode_ci NOT IN 
(SELECT type COLLATE utf8mb4_unicode_ci FROM sys_operation_type);

-- =====================================================
-- 验证映射表数据
-- =====================================================

-- 查看映射表数据
SELECT * FROM sys_operation_type_mapping LIMIT 20;

-- 查看映射关系数量
SELECT COUNT(*) AS mapping_count FROM sys_operation_type_mapping;

-- =====================================================
-- 简化版执行步骤
-- =====================================================
-- 1. 执行此脚本创建映射表并插入映射关系
-- 2. 执行更新操作
-- 3. 查看更新结果
-- 4. 处理未匹配的操作类型
-- =====================================================
