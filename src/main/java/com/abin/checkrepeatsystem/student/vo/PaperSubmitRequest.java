package com.abin.checkrepeatsystem.student.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class PaperSubmitRequest {

    @NotBlank(message = "学科领域不能为空")
    private String subjectCode;

    @NotBlank(message = "论文标题不能为空")
    private String paperTitle;

    @NotNull(message = "学院不能为空")
    private Long collegeId;

    @NotNull(message = "专业不能为空")
    private Long majorId;

    @NotBlank(message = "论文类型不能为空")
    private String paperType;

    @NotBlank(message = "论文摘要不能为空")
    private String paperAbstract;

    @NotBlank(message = "文件ID不能为空")
    private String fileId; // 文件ID

    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;

}
