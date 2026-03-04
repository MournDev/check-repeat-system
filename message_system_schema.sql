-- 消息通信系统数据库表结构

-- 消息表
CREATE TABLE `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `conversation_id` bigint DEFAULT NULL COMMENT '会话ID',
  `message_type` varchar(20) NOT NULL COMMENT '消息类型(PRIVATE/NOTIFICATION/GROUP/BROADCAST)',
  `content_type` varchar(20) NOT NULL COMMENT '内容类型(TEXT/FILE/IMAGE/VOICE)',
  `title` varchar(200) DEFAULT NULL COMMENT '消息标题',
  `content` text COMMENT '消息内容',
  `attachments` json DEFAULT NULL COMMENT '附件列表',
  `status` varchar(20) DEFAULT 'SENT' COMMENT '消息状态(SENT/DELIVERED/READ)',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID',
  `related_type` varchar(50) DEFAULT NULL COMMENT '关联类型',
  `sent_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `delivered_time` datetime DEFAULT NULL COMMENT '送达时间',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '是否删除(0-未删除,1-已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_sent_time` (`sent_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 会话表
CREATE TABLE `conversations` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) DEFAULT NULL COMMENT '会话名称',
  `type` varchar(20) NOT NULL DEFAULT 'PRIVATE' COMMENT '会话类型(PRIVATE/GROUP)',
  `avatar` varchar(255) DEFAULT NULL COMMENT '会话头像',
  `last_message_id` bigint DEFAULT NULL COMMENT '最后一条消息ID',
  `last_active_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  `creator_id` bigint NOT NULL COMMENT '会话创建者ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '是否删除(0-未删除,1-已删除)',
  PRIMARY KEY (`id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_last_active_time` (`last_active_time`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 会话成员表
CREATE TABLE `conversation_members` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(20) DEFAULT 'MEMBER' COMMENT '成员角色(OWNER/ADMIN/MEMBER)',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `left_at` datetime DEFAULT NULL COMMENT '离开时间',
  `is_left` tinyint DEFAULT '0' COMMENT '是否已离开(0-未离开,1-已离开)',
  `unread_count` int DEFAULT '0' COMMENT '未读消息数量',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '是否删除(0-未删除,1-已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_user` (`conversation_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_is_left` (`is_left`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话成员表';

-- 插入初始数据
-- 系统通知用户（用于发送系统消息）
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `user_type`, `status`, `is_deleted`, `create_time`, `update_time`) 
VALUES (0, 'system', '', '系统通知', 'SYSTEM', 1, 0, NOW(), NOW());

-- 基础会话类型数据字典
INSERT INTO `sys_dict_data` (`dict_code`, `dict_label`, `dict_value`, `dict_type`, `status`, `create_time`) VALUES
('message_type', '私信', 'PRIVATE', 'message_type', '0', NOW()),
('message_type', '通知', 'NOTIFICATION', 'message_type', '0', NOW()),
('message_type', '群聊', 'GROUP', 'message_type', '0', NOW()),
('message_type', '广播', 'BROADCAST', 'message_type', '0', NOW()),
('content_type', '文本', 'TEXT', 'content_type', '0', NOW()),
('content_type', '文件', 'FILE', 'content_type', '0', NOW()),
('content_type', '图片', 'IMAGE', 'content_type', '0', NOW()),
('content_type', '语音', 'VOICE', 'content_type', '0', NOW()),
('message_status', '已发送', 'SENT', 'message_status', '0', NOW()),
('message_status', '已送达', 'DELIVERED', 'message_status', '0', NOW()),
('message_status', '已读', 'READ', 'message_status', '0', NOW());