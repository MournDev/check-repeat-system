-- V3: 为 sys_notice 表添加 priority 字段，支持通知优先级分级
-- 0-普通，1-重要，2-紧急

ALTER TABLE sys_notice ADD COLUMN priority INT DEFAULT 0 COMMENT '优先级：0-普通，1-重要，2-紧急' AFTER read_time;
