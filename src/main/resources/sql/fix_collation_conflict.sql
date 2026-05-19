-- =====================================================
-- 修复字符集冲突的脚本
-- 解决 "Illegal mix of collations" 错误
-- =====================================================

-- 1. 查看表的字符集和排序规则
SHOW CREATE TABLE sys_operation_log;
SHOW CREATE TABLE sys_operation_type;

-- 2. 修复映射表的字符集
ALTER TABLE sys_operation_type_mapping CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 3. 修改更新语句，使用 COLLATE 子句解决字符集冲突
UPDATE `sys_operation_log` log
INNER JOIN `sys_operation_type_mapping` mapping 
ON log.`operation_type` COLLATE utf8mb4_unicode_ci = mapping.`old_type` COLLATE utf8mb4_unicode_ci
SET log.`operation_type` = mapping.`new_type`
WHERE mapping.`status` = 1;

-- 4. 修复查看未匹配操作类型的语句
SELECT DISTINCT operation_type 
FROM sys_operation_log 
WHERE operation_type COLLATE utf8mb4_unicode_ci NOT IN 
(SELECT type COLLATE utf8mb4_unicode_ci FROM sys_operation_type);

-- 5. 修复更新结果统计语句
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

-- 6. 修复未匹配操作类型的查询
SELECT DISTINCT operation_type, COUNT(*) as count
FROM sys_operation_log 
WHERE operation_type COLLATE utf8mb4_unicode_ci NOT IN 
(SELECT type COLLATE utf8mb4_unicode_ci FROM sys_operation_type)
GROUP BY operation_type 
ORDER BY count DESC;

-- 7. 永久解决方案：统一两个表的字符集
-- 执行以下语句统一字符集（可选）
-- ALTER TABLE sys_operation_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- ALTER TABLE sys_operation_type CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================
-- 简化版更新脚本（使用 COLLATE 子句）
-- =====================================================

-- 简化的更新语句
UPDATE `sys_operation_log` log
INNER JOIN `sys_operation_type_mapping` mapping 
ON log.`operation_type` COLLATE utf8mb4_unicode_ci = mapping.`old_type` COLLATE utf8mb4_unicode_ci
SET log.`operation_type` = mapping.`new_type`
WHERE mapping.`status` = 1;

-- 验证更新结果
SELECT 
    (SELECT COUNT(*) FROM sys_operation_log 
     WHERE operation_type COLLATE utf8mb4_unicode_ci IN 
     (SELECT type COLLATE utf8mb4_unicode_ci FROM sys_operation_type)) AS matched_count,
    (SELECT COUNT(*) FROM sys_operation_log 
     WHERE operation_type COLLATE utf8mb4_unicode_ci NOT IN 
     (SELECT type COLLATE utf8mb4_unicode_ci FROM sys_operation_type)) AS unmatched_count;
