-- 告警规则表
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_type` VARCHAR(50) NOT NULL COMMENT '规则类型(CPU/MEMORY/LOGIN_FAIL/CHECK_FAIL/STORAGE)',
    `metric_name` VARCHAR(100) DEFAULT '' COMMENT 'Micrometer指标名',
    `threshold` DOUBLE NOT NULL COMMENT '阈值',
    `duration` INT DEFAULT 60 COMMENT '持续秒数',
    `severity` VARCHAR(20) DEFAULT 'WARNING' COMMENT '严重级别(CRITICAL/WARNING/INFO)',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `notify_email` VARCHAR(200) DEFAULT '' COMMENT '通知邮箱',
    `description` VARCHAR(500) DEFAULT '' COMMENT '规则描述',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '软删除标记(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_rule_type` (`rule_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- 告警记录表
CREATE TABLE IF NOT EXISTS `alert_record` (
    `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
    `rule_id` BIGINT DEFAULT NULL COMMENT '关联规则ID',
    `rule_name` VARCHAR(100) DEFAULT '' COMMENT '规则名称（冗余）',
    `severity` VARCHAR(20) DEFAULT 'WARNING' COMMENT '严重级别(CRITICAL/WARNING/INFO)',
    `title` VARCHAR(200) NOT NULL COMMENT '告警标题',
    `message` VARCHAR(1000) DEFAULT '' COMMENT '告警详情',
    `metric_value` DOUBLE DEFAULT NULL COMMENT '触发时的指标值',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/RESOLVED/DISMISSED)',
    `trigger_time` DATETIME DEFAULT NULL COMMENT '触发时间',
    `resolve_time` DATETIME DEFAULT NULL COMMENT '解决时间',
    `resolved_by` VARCHAR(50) DEFAULT '' COMMENT '处理人',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '软删除标记(0-未删除,1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_trigger_time` (`trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- 默认告警规则种子数据
INSERT INTO `alert_rule` (`id`, `rule_name`, `rule_type`, `metric_name`, `threshold`, `duration`, `severity`, `enabled`, `description`, `create_time`, `update_time`)
VALUES
(1000000000000000001, 'CPU使用率过高', 'CPU', 'process.cpu.usage', 80, 300, 'WARNING', 1, '当CPU使用率超过80%持续5分钟时触发告警', NOW(), NOW()),
(1000000000000000002, '内存使用率过高', 'MEMORY', 'jvm.memory.usage', 85, 300, 'WARNING', 1, '当堆内存使用率超过85%持续5分钟时触发告警', NOW(), NOW()),
(1000000000000000003, '磁盘空间不足', 'STORAGE', 'disk.usage', 85, 60, 'CRITICAL', 1, '当磁盘使用率超过85%时触发告警', NOW(), NOW()),
(1000000000000000004, '频繁登录失败', 'LOGIN_FAIL', 'login.fail.count', 10, 300, 'WARNING', 1, '当5分钟内登录失败超过10次时触发告警', NOW(), NOW()),
(1000000000000000005, '查重任务失败', 'CHECK_FAIL', 'check.fail.count', 5, 300, 'INFO', 1, '当5分钟内查重失败超过5次时触发告警', NOW(), NOW());