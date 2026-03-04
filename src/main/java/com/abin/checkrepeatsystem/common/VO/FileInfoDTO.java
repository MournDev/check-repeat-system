package com.abin.checkrepeatsystem.common.VO;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class FileInfoDTO {
    private String id;
    private String originalFilename;
    private String storagePath;
    private Long fileSize;
    private String fileType;
    private Date uploadTime;
}