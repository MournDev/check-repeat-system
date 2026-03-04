package com.abin.checkrepeatsystem.student.dto;

import lombok.Data;

/**
 * 论文撤回请求DTO
 */
@Data
public class PaperWithdrawRequest {
    private Long paperId;
    private String reason; // 撤回原因
}