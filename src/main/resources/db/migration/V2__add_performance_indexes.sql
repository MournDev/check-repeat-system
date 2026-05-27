-- V2: 生产环境性能索引
-- 针对高频查询路径补建关键索引

DELIMITER $$

DROP PROCEDURE IF EXISTS create_index_if_not_exists$$

CREATE PROCEDURE create_index_if_not_exists()
BEGIN
    -- paper_info: 按学生查询论文
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'paper_info' AND index_name = 'idx_paper_info_student_id') THEN
        CREATE INDEX idx_paper_info_student_id ON paper_info (student_id);
    END IF;

    -- paper_info: 教师仪表盘查询（按教师+状态过滤）
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'paper_info' AND index_name = 'idx_paper_info_teacher_status') THEN
        CREATE INDEX idx_paper_info_teacher_status ON paper_info (teacher_id, paper_status, is_deleted);
    END IF;

    -- paper_info: 管理员仪表盘各状态统计
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'paper_info' AND index_name = 'idx_paper_info_status') THEN
        CREATE INDEX idx_paper_info_status ON paper_info (paper_status, is_deleted);
    END IF;

    -- sys_user: 清理重复邮箱数据（保留ID最小的记录）
    DELETE u1 FROM sys_user u1
    INNER JOIN sys_user u2
    WHERE u1.email = u2.email
      AND u1.id > u2.id;

    -- sys_user: 邮箱唯一查询（注册/登录/找回密码）
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_sys_user_email') THEN
        CREATE UNIQUE INDEX idx_sys_user_email ON sys_user (email);
    END IF;

    -- teacher_info: 按用户ID查询教师信息
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'teacher_info' AND index_name = 'idx_teacher_info_user_id') THEN
        CREATE INDEX idx_teacher_info_user_id ON teacher_info (user_id);
    END IF;

    -- student_info: 按用户ID查询学生信息
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'student_info' AND index_name = 'idx_student_info_user_id') THEN
        CREATE INDEX idx_student_info_user_id ON student_info (user_id);
    END IF;

    -- student_info: 按学院ID分组查询
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'student_info' AND index_name = 'idx_student_info_college_id') THEN
        CREATE INDEX idx_student_info_college_id ON student_info (college_id);
    END IF;

    -- check_result: 按任务ID查询结果
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'check_result' AND index_name = 'idx_check_result_task_id') THEN
        CREATE INDEX idx_check_result_task_id ON check_result (task_id);
    END IF;

    -- check_result: 按论文ID查询结果
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'check_result' AND index_name = 'idx_check_result_paper_id') THEN
        CREATE INDEX idx_check_result_paper_id ON check_result (paper_id);
    END IF;

    -- sys_login_log: 按用户ID和登录时间查询
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'sys_login_log' AND index_name = 'idx_login_log_user_time') THEN
        CREATE INDEX idx_login_log_user_time ON sys_login_log (user_id, login_time);
    END IF;
END$$

DELIMITER ;

CALL create_index_if_not_exists();

DROP PROCEDURE IF EXISTS create_index_if_not_exists;
