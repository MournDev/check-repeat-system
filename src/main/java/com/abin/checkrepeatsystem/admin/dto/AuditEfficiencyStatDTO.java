package com.abin.checkrepeatsystem.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 教师审核效率统计DTO
 */
@Data
public class AuditEfficiencyStatDTO {

    @Schema(description = "教师轴（如[张老师, 李老师,...]）")
    private List<String> teacherAxis;

    @Schema(description = "各教师待审核任务数")
    private List<Integer> pendingAuditCount;

    @Schema(description = "各教师已审核任务数")
    private List<Integer> completedAuditCount;

    @Schema(description = "各教师平均审核耗时（分钟，保留1位小数）")
    private List<Double> avgAuditTime;

    @Schema(description = "统计周期内总待审核任务数")
    private Integer totalPendingCount;

    @Schema(description = "统计周期内总已审核任务数")
    private Integer totalCompletedCount;
}
