package com.abin.checkrepeatsystem.student.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *享文件VO
 */
@Data
@Schema(description = "共享文件")
public class SharedFileVO {

    @Schema(description = "文件ID")
    private String id;

    @Schema(description = "文件名称")
    private String name;

    @Schema(description = "文件类型")
    private String type;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "上传者姓名")
    private String uploader;

    @Schema(description = "上传时间")
    private String uploadTime;

    @Schema(description = "文件描述")
    private String description;

    @Schema(description = "下载次数")
    private Integer downloadCount;
}