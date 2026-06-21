package com.abin.checkrepeatsystem.student.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import com.alibaba.excel.enums.poi.HorizontalAlignmentEnum;
import lombok.Data;

/**
 * 学生论文列表Excel导出VO
 */
@Data
public class StudentPaperExcelVO {

    @ExcelProperty(value = "论文标题", index = 0)
    @ColumnWidth(40)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.LEFT)
    private String paperTitle;

    @ExcelProperty(value = "论文类型", index = 1)
    @ColumnWidth(15)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private String paperType;

    @ExcelProperty(value = "状态", index = 2)
    @ColumnWidth(12)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private String status;

    @ExcelProperty(value = "相似度(%)", index = 3)
    @ColumnWidth(15)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private String similarityRate;

    @ExcelProperty(value = "提交时间", index = 4)
    @ColumnWidth(20)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private String submitTime;

    @ExcelProperty(value = "审核时间", index = 5)
    @ColumnWidth(20)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private String reviewTime;

    @ExcelProperty(value = "审核意见", index = 6)
    @ColumnWidth(40)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.LEFT)
    private String reviewOpinion;
}
