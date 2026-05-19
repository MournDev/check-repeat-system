package com.abin.checkrepeatsystem.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class AlertRuleBatchSaveReq {

    @NotEmpty(message = "规则列表不能为空")
    @Valid
    private List<AlertRuleDTO> rules;
}
