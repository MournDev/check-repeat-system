package com.abin.checkrepeatsystem.teacher.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 待审核论文查询请求DTO
 */
@Data
public class ReviewQueryReq {
    /**
     * 学生姓名（模糊查询，可选）
     */
    private String studentName;

    /**
     * 论文标题（模糊查询，可选）
     */
    private String paperTitle;

    /**
     * 审核状态（可选，completed/rejected）
     */
    private String status;

    /**
     * 审核开始时间（可选）
     */
    private String startTime;

    /**
     * 审核结束时间（可选）
     */
    private String endTime;

    /**
     * 相似度范围（可选，如0-10/10-20/20-30/30-100）
     */
    private String similarityRange;

    /**
     * 当前页码（默认1）
     */
    @Min(value = 1, message = "页码不能小于1")
    private Integer currentPage = 1;

    /**
     * 每页条数（默认10，最大20）
     */
    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 20, message = "每页条数不能超过20")
    private Integer pageSize = 10;
}
