-- V4: 为 sys_notice 表添加 related_id 和 related_type 字段

ALTER TABLE sys_notice ADD COLUMN related_id BIGINT DEFAULT NULL COMMENT '相关业务ID' AFTER priority;
ALTER TABLE sys_notice ADD COLUMN related_type VARCHAR(50) DEFAULT NULL COMMENT '相关业务类型' AFTER related_id;
