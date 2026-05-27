-- V3: 为 major 表增加指导任务数量相关字段
-- 用于支持指导教师上限管理功能

ALTER TABLE major ADD COLUMN user_id BIGINT DEFAULT NULL COMMENT '用户ID（关联sys_user.id）';
ALTER TABLE major ADD COLUMN current_advisor_count INT DEFAULT 0 COMMENT '当前指导任务数';
ALTER TABLE major ADD COLUMN max_advisor_count INT DEFAULT 10 COMMENT '最大指导任务上限';
CREATE INDEX idx_major_user_id ON major(user_id);
