package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert_rule")
public class AlertRule extends BaseEntity {

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    @NotBlank(message = "指标名称不能为空")
    private String metricName;

    @NotNull(message = "阈值不能为空")
    @DecimalMin(value = "0", message = "阈值不能为负数")
    private Double threshold;

    private Integer duration;

    @NotBlank(message = "严重级别不能为空")
    private String severity;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    private String notifyEmail;
    private String description;
}