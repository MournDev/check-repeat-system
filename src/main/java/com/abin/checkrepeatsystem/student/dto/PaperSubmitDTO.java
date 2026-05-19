package com.abin.checkrepeatsystem.student.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论文提交版本DTO（用于前端版本列表展示）
 */
@Data
@ApiModel(description = "论文提交版本DTO")
public class PaperSubmitDTO {

    @ApiModelProperty(value = "提交记录ID")
    private Long id;

    @ApiModelProperty(value = "提交版本号")
    private Integer submitVersion;

    @ApiModelProperty(value = "文件ID")
    private Long fileId;

    @ApiModelProperty(value = "提交时间")
    private LocalDateTime submitTime;

    @ApiModelProperty(value = "备注")
    private String remark;
}