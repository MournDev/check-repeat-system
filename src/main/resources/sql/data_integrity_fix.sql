-- =============================================
-- 查重系统数据完整性检查与修复脚本
-- 创建时间：2026-03-05
-- 说明：检查并修复数据库表的数据完整性问题
-- =============================================

-- 1. 检查学院表数据
-- =============================================
SELECT 'college' AS table_name, COUNT(*) AS record_count FROM college WHERE is_deleted = 0
UNION ALL
SELECT 'major', COUNT(*) FROM major WHERE is_deleted = 0
UNION ALL
SELECT 'sys_user', COUNT(*) FROM sys_user WHERE is_deleted = 0 AND status = 1;

-- 2. 检查论文信息表的完整性
-- =============================================
-- 检查缺失学院信息的论文
SELECT COUNT(*) AS papers_missing_college 
FROM paper_info pi 
LEFT JOIN college c ON pi.college_id = c.id 
WHERE pi.is_deleted = 0 AND (pi.college_id IS NULL OR c.id IS NULL);

-- 检查缺失专业信息的论文
SELECT COUNT(*) AS papers_missing_major 
FROM paper_info pi 
LEFT JOIN major m ON pi.major_id = m.id 
WHERE pi.is_deleted = 0 AND (pi.major_id IS NULL OR m.id IS NULL);

-- 检查缺失导师的论文
SELECT COUNT(*) AS papers_missing_teacher 
FROM paper_info 
WHERE is_deleted = 0 AND teacher_id IS NULL;

-- 3. 检查查重任务与结果的完整性
-- =============================================
-- 检查已完成但缺失查重结果的任务
SELECT COUNT(*) AS completed_tasks_without_result
FROM check_task ct
LEFT JOIN check_result cr ON ct.id = cr.task_id
WHERE ct.check_status = 'completed' 
  AND ct.is_deleted = 0
  AND (cr.id IS NULL OR cr.status = 0);

-- 检查查重任务与论文信息的一致性
SELECT COUNT(*) AS tasks_with_mismatched_paper
FROM check_task ct
INNER JOIN paper_info pi ON ct.paper_id = pi.id
WHERE ct.is_deleted = 0 
  AND pi.is_deleted = 0
  AND ct.check_rate != pi.similarity_rate;

-- 4. 检查论文状态的一致性
-- =============================================
-- 检查查重完成但未设置标记的论文
SELECT COUNT(*) AS papers_check_completed_not_set
FROM paper_info 
WHERE similarity_rate IS NOT NULL 
  AND similarity_rate >= 0
  AND (check_completed IS NULL OR check_completed = 0)
  AND is_deleted = 0;

-- 检查状态不一致的论文
SELECT 
  paper_status,
  check_completed,
  similarity_rate,
  COUNT(*) as count
FROM paper_info
WHERE is_deleted = 0
GROUP BY paper_status, check_completed, similarity_rate
ORDER BY paper_status, check_completed;

-- 5. 修复数据完整性问题
-- =============================================

-- 修复 1: 为缺失查重完成标记的论文设置标记
UPDATE paper_info 
SET check_completed = 1,
    update_time = NOW()
WHERE similarity_rate IS NOT NULL 
  AND similarity_rate >= 0
  AND (check_completed IS NULL OR check_completed = 0)
  AND is_deleted = 0;

-- 修复 2: 统一查重任务与论文信息的相似度
UPDATE paper_info pi
INNER JOIN check_task ct ON pi.id = ct.paper_id
SET pi.similarity_rate = ct.check_rate,
    pi.update_time = NOW()
WHERE ct.check_status = 'completed'
  AND ct.is_deleted = 0
  AND pi.is_deleted = 0
  AND (ct.check_rate != pi.similarity_rate OR pi.similarity_rate IS NULL);

-- 修复 3: 为已完成的查重任务补充 check_result 记录
INSERT INTO check_result (
    task_id, paper_id, repeat_rate, check_source, 
    check_time, word_count, status, create_by, update_by, create_time, update_time
)
SELECT 
    ct.id,
    ct.paper_id,
    ct.check_rate,
    'LOCAL',
    ct.end_time,
    pi.word_count,
    1,
    ct.create_by,
    ct.update_by,
    ct.create_time,
    ct.update_time
FROM check_task ct
INNER JOIN paper_info pi ON ct.paper_id = pi.id
LEFT JOIN check_result cr ON ct.id = cr.task_id
WHERE ct.check_status = 'completed'
  AND ct.is_deleted = 0
  AND pi.is_deleted = 0
  AND (cr.id IS NULL OR cr.status = 0);

-- 修复 4: 更新论文状态为一致的逻辑
-- 已完成查重且合格的论文状态应为 AUDITING（待审核）
UPDATE paper_info
SET paper_status = 'AUDITING',
    check_result = '合格',
    update_time = NOW()
WHERE check_completed = 1
  AND similarity_rate <= 20  -- 假设阈值为 20%
  AND paper_status NOT IN ('AUDITING', 'COMPLETED')
  AND is_deleted = 0;

-- 已完成查重但不合格的论文状态应为 REJECTED（审核不通过）
UPDATE paper_info
SET paper_status = 'REJECTED',
    check_result = '不合格',
    update_time = NOW()
WHERE check_completed = 1
  AND similarity_rate > 20  -- 假设阈值为 20%
  AND paper_status != 'REJECTED'
  AND is_deleted = 0;

-- 6. 验证修复结果
-- =============================================
-- 验证查重任务与结果的一致性
SELECT 
    'completed_tasks' AS category,
    COUNT(*) AS total_count
FROM check_task 
WHERE check_status = 'completed' AND is_deleted = 0
UNION ALL
SELECT 
    'with_check_result',
    COUNT(*)
FROM check_task ct
INNER JOIN check_result cr ON ct.id = cr.task_id
WHERE ct.check_status = 'completed' AND ct.is_deleted = 0 AND cr.status = 1;

-- 验证论文状态的一致性
SELECT 
    paper_status,
    check_completed,
    CASE 
        WHEN similarity_rate <= 20 THEN 'PASS'
        ELSE 'FAIL'
    END AS result_category,
    COUNT(*) as count
FROM paper_info
WHERE is_deleted = 0
GROUP BY paper_status, check_completed, 
    CASE 
        WHEN similarity_rate <= 20 THEN 'PASS'
        ELSE 'FAIL'
    END
ORDER BY paper_status, check_completed;

-- 7. 性能优化建议
-- =============================================
-- 为常用查询字段添加索引（如果尚未存在）
CREATE INDEX IF NOT EXISTS idx_paper_status ON paper_info(paper_status, is_deleted);
CREATE INDEX IF NOT EXISTS idx_paper_check_completed ON paper_info(check_completed, is_deleted);
CREATE INDEX IF NOT EXISTS idx_check_task_status ON check_task(check_status, is_deleted);
CREATE INDEX IF NOT EXISTS idx_check_result_task ON check_result(task_id, status);

-- 分析表统计信息
ANALYZE TABLE paper_info;
ANALYZE TABLE check_task;
ANALYZE TABLE check_result;
