ALTER TABLE check_task ADD COLUMN check_rule_id BIGINT DEFAULT NULL COMMENT '查重规则ID（关联check_rule.id）';
CREATE INDEX idx_check_task_rule_id ON check_task(check_rule_id);
