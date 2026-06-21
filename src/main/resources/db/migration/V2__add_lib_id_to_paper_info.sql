-- V2: 为 paper_info 表添加 lib_id 字段，支持按对比库过滤比对范围
-- 对应 compare_lib.id，表示该论文属于哪个对比库

ALTER TABLE paper_info ADD COLUMN lib_id BIGINT DEFAULT NULL COMMENT '对比库ID（关联compare_lib.id）' AFTER major_id;

-- 添加索引以优化按对比库查询
CREATE INDEX idx_paper_info_lib_id ON paper_info(lib_id);
