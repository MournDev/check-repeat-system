CREATE TABLE IF NOT EXISTS sys_backup_log (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    file_name VARCHAR(255) COMMENT '备份文件名',
    file_size BIGINT COMMENT '文件大小(字节)',
    status VARCHAR(20) NOT NULL COMMENT '备份状态(SUCCESS/FAILED)',
    start_time DATETIME COMMENT '备份开始时间',
    end_time DATETIME COMMENT '备份结束时间',
    duration_ms BIGINT COMMENT '耗时(毫秒)',
    error_message VARCHAR(1000) COMMENT '错误信息',
    backup_type VARCHAR(20) NOT NULL COMMENT '备份类型(AUTO/MANUAL)',
    is_deleted INT DEFAULT 0 COMMENT '软删除标记(0-未删除,1-已删除)',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间'
);
