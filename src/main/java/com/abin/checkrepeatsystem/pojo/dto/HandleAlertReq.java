package com.abin.checkrepeatsystem.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HandleAlertReq {

    @NotNull(message = "告警ID不能为空")
    private Long alertId;

    @NotBlank(message = "操作类型不能为空")
    private String action;

    private String remark;
}
