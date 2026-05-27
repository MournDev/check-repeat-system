-- 安全添加备份启停开关列
SELECT COUNT(*) INTO @col1 FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_param' AND COLUMN_NAME = 'backup_enabled';
SET @sql = IF(@col1 = 0,
    'ALTER TABLE system_param ADD COLUMN backup_enabled INT DEFAULT 1 COMMENT ''是否启用自动备份(0-禁用,1-启用)''',
    'SELECT 1 AS skip');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 安全添加备份保留天数列
SELECT COUNT(*) INTO @col2 FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_param' AND COLUMN_NAME = 'backup_retention_days';
SET @sql = IF(@col2 = 0,
    'ALTER TABLE system_param ADD COLUMN backup_retention_days INT DEFAULT 30 COMMENT ''备份文件保留天数''',
    'SELECT 1 AS skip');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
