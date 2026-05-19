package com.abin.checkrepeatsystem.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alert_record")
public class AlertRecord extends BaseEntity {

    private Long ruleId;
    private String ruleName;
    private String severity;
    private String title;
    private String message;
    private Double metricValue;
    private String status;
    private LocalDateTime triggerTime;
    private LocalDateTime resolveTime;
    private String resolvedBy;
}