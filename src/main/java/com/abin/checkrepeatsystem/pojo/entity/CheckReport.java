package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 查重报告实体：对应check_report表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("check_report") // 绑定数据库表名
public class CheckReport extends BaseEntity {
    /**
     * 对应的查重任务ID（关联check_task.id，唯一）
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 对应的论文ID（关联paper_info.id，用于关联论文）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @TableField("paper_id")
    private Long paperId;

    /**
     * 报告编号（唯一，如：REPORT20240512001）
     */
    @TableField("report_no")
    private String reportNo;

    /**
     * 总相似度
     */
    @TableField("total_similarity")
    private BigDecimal totalSimilarity;

    /**
     * 重复详情（JSON格式字符串，含重复段落、相似来源、相似度）
     */
    @TableField("repeat_details")
    private String repeatDetails;

    /**
     * 报告文件存储路径
     */
    @TableField("report_path")
    private String reportPath;

    /**
     * 报告文件类型（默认pdf）
     */
    @TableField("report_type")
    private String reportType;

    /**
     * 报告生成时间
     */
    @TableField("report_generate_time")
    private LocalDateTime reportGenerateTime;
}
