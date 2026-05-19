-- =====================================================
-- 审核草稿表 (review_draft)
-- 用于存储教师的审核草稿，支持暂存审核意见
-- =====================================================

CREATE TABLE IF NOT EXISTS `review_draft` (
    `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
    `paper_id` bigint NOT NULL COMMENT '论文ID（关联paper_info.id）',
    `teacher_id` bigint NOT NULL COMMENT '教师ID（关联sys_user.id）',
    `review_status` varchar(50) DEFAULT NULL COMMENT '审核状态（approve-审核通过，reject-审核不通过，modify-需要修改，defer-暂缓审核）',
    `review_opinion` text DEFAULT NULL COMMENT '审核意见',
    `review_attach` varchar(500) DEFAULT NULL COMMENT '审核附件路径',
    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `is_deleted` tinyint(1) DEFAULT 0 COMMENT '软删除标记（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_paper_teacher` (`paper_id`, `teacher_id`, `is_deleted`),
    KEY `idx_teacher` (`teacher_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审核草稿表';

-- =====================================================
-- 说明：
-- 1. id: 使用雪花算法生成，确保分布式环境下唯一性
-- 2. paper_id + teacher_id + is_deleted: 联合索引，确保每个教师对每篇论文只有一个草稿
-- 3. teacher_id + is_deleted: 联合索引，用于查询某个教师的所有草稿
-- 4. 软删除: 使用is_deleted字段标记删除，而非物理删除
-- =====================================================