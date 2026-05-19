package com.abin.checkrepeatsystem.student.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

// 最新论文DTO
@Data
public class LatestPaperDTO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String title;
    private String status;// SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED
    private LocalDateTime submitTime;
    private LocalDateTime approveTime;
    private String advisorName;
    private String allocationStatus;    // 分配确认状态: pending/confirmed/rejected
    private LocalDateTime allocationTime; // 分配时间
    private String feedback;
    private Integer wordCount;
    private Integer pageCount;
    private BigDecimal similarity;      // 查重率
}
