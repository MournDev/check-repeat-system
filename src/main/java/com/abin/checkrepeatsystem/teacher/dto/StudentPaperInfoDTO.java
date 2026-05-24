package com.abin.checkrepeatsystem.teacher.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class StudentPaperInfoDTO {
    private Long paperId;
    private String paperTitle;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fileId;
    private String fileName;
    private String fileSize;
    private String submitTime;
    private String paperStatus;
    private Double similarity;
}
