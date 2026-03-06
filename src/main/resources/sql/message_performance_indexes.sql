-- =============================================
-- 消息系统性能优化索引脚本
-- 创建时间：2026-03-04
-- 说明：为消息相关表添加索引以提升查询性能
-- =============================================

-- 1. instant_message 表索引
-- =============================================

-- 复合索引：发送者 - 接收者查询（最常用）
CREATE INDEX IF NOT EXISTS idx_im_sender_receiver 
ON instant_message(sender_id, receiver_id, is_deleted);

-- 复合索引：会话查询
CREATE INDEX IF NOT EXISTS idx_im_conversation 
ON instant_message(conversation_id, is_deleted, sent_time DESC);

-- 单字段索引：创建时间（用于时间排序）
CREATE INDEX IF NOT EXISTS idx_im_create_time 
ON instant_message(create_time DESC);

-- 复合索引：未读消息统计
CREATE INDEX IF NOT EXISTS idx_im_unread 
ON instant_message(receiver_id, status, is_deleted);

-- 2. conversation 表索引
-- =============================================

-- 更新时间索引（用于会话列表排序）
CREATE INDEX IF NOT EXISTS idx_conv_update_time 
ON conversation(update_time DESC);

-- 创建时间索引
CREATE INDEX IF NOT EXISTS idx_conv_create_time 
ON conversation(create_time DESC);

-- 3. conversation_member 表索引
-- =============================================

-- 用户ID 索引（查询用户参与的会话）
CREATE INDEX IF NOT EXISTS idx_cm_user_id 
ON conversation_member(user_id, is_deleted);

-- 会话 ID 索引
CREATE INDEX IF NOT EXISTS idx_cm_conversation_id 
ON conversation_member(conversation_id, is_deleted);

-- 4. system_message 表索引
-- =============================================

-- 接收者 ID 索引
CREATE INDEX IF NOT EXISTS idx_sm_receiver_id 
ON system_message(receiver_id, is_read, is_deleted);

-- 消息类型索引
CREATE INDEX IF NOT EXISTS idx_sm_message_type 
ON system_message(message_type, create_time DESC);

-- 创建时间索引
CREATE INDEX IF NOT EXISTS idx_sm_create_time 
ON system_message(create_time DESC);

-- 5. check_task 表索引（查重相关）
-- =============================================

-- 论文 ID 索引
CREATE INDEX IF NOT EXISTS idx_ct_paper_id 
ON check_task(paper_id, check_status, create_time);

-- 状态索引
CREATE INDEX IF NOT EXISTS idx_ct_status 
ON check_task(check_status, start_time);

-- 6. check_result 表索引（查重结果）
-- =============================================

-- 论文 ID 索引
CREATE INDEX IF NOT EXISTS idx_cr_paper_id 
ON check_result(paper_id, status, create_time DESC);

-- 任务 ID 索引
CREATE INDEX IF NOT EXISTS idx_cr_task_id 
ON check_result(task_id, status);

-- =============================================
-- 索引创建完成验证
-- =============================================

-- 查看 instant_message 表的索引
-- SHOW INDEX FROM instant_message;

-- 查看索引大小和使用情况
-- SELECT 
--     index_name,
--     table_name,
--     non_unique,
--     seq_in_index,
--     column_name
-- FROM information_schema.statistics
-- WHERE table_schema = DATABASE()
--   AND table_name IN ('instant_message', 'conversation', 'conversation_member', 'system_message')
-- ORDER BY table_name, index_name;

-- =============================================
-- 性能优化建议
-- =============================================

-- 1. 定期分析和优化表
-- ANALYZE TABLE instant_message;
-- ANALYZE TABLE conversation;
-- ANALYZE TABLE system_message;

-- 2. 对于大数据量的表，考虑分区
-- 例如：按月份分区 instant_message 表

-- 3. 监控慢查询日志
-- 设置 MySQL 慢查询阈值为 1 秒
-- SET GLOBAL long_query_time = 1;

-- 4. 使用 EXPLAIN 分析查询计划
-- EXPLAIN SELECT * FROM instant_message 
-- WHERE sender_id = ? AND receiver_id = ? 
-- ORDER BY create_time DESC LIMIT 10;
