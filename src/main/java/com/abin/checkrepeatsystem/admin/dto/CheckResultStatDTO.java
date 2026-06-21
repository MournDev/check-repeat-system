package com.abin.checkrepeatsystem.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 论文查重结果分析DTO（按专业/年级维度）
 */
@Data
public class CheckResultStatDTO {

    @Schema(description = "分组轴（如专业名[计算机科学与技术, 软件工程,...]或年级[2021,2022,...]）")
    private List<String> groupAxis;

    @Schema(description = "各组合格人数（重复率≤阈值）")
    private List<Integer> qualifiedCount;

    @Schema(description = "各组不合格人数（重复率>阈值）")
    private List<Integer> unqualifiedCount;

    @Schema(description = "各组平均重复率（保留2位小数）")
    private List<Double> avgRepeatRate;

    @Schema(description = "总体合格率（保留2位小数，如92.30）")
    private Double totalQualifiedRate;
}
