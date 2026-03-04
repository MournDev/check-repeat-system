package com.abin.checkrepeatsystem.student.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量操作请求DTO
 */
@Data
public class BatchOperationRequest {
    private List<Long> paperIds;
}