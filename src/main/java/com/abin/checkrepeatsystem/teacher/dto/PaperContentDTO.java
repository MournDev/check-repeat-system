package com.abin.checkrepeatsystem.teacher.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.List;

/**
 * 论文内容响应DTO
 */
@Data
public class PaperContentDTO {
    
    /**
     * 论文ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long paperId;

    /**
     * 论文作者ID
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long studentId;

    /**
     * 论文作者姓名
     */
    private String studentName;
    
    /**
     * 论文标题
     */
    private String paperTitle;

    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 论文正文内容
     */
    private String content;
    
    /**
     * 摘要
     */
    private String abstractText;
    
    /**
     * 关键词
     */
    private List<String> keywords;
    
    /**
     * 字数统计
     */
    private Integer wordCount;
    
    /**
     * 页数
     */
    private Integer pageCount;
    
    /**
     * 文件大小
     */
    private String fileSizeDesc;
    
    /**
     * 文件类型
     */
    private String fileType;
}