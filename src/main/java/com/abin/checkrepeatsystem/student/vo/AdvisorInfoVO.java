package com.abin.checkrepeatsystem.student.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *师信息VO
 */
@Data
@Schema(description = "导师信息")
public class AdvisorInfoVO {

    @Schema(description = "导师ID")
    private String id;

    @Schema(description = "导师姓名")
    private String name;

    @Schema(description = "导师职称")
    private String title;

    @Schema(description = "研究领域")
    private String researchField;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "电话")
    private String phone;

    @Schema(description = "办公室")
    private String office;

    @Schema(description = "办公时间")
    private String officeHours;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "指导学生数")
    private Integer studentCount;

    @Schema(description = "所属学院")
    private String college;
}