package com.abin.checkrepeatsystem.student.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import com.alibaba.excel.enums.poi.HorizontalAlignmentEnum;
import lombok.Data;

/**
 * 学生仪表盘统计数据Excel导出VO
 */
@Data
public class StudentDashboardExcelVO {

    @ExcelProperty(value = "指标", index = 0)
    @ColumnWidth(25)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.LEFT)
    private String metric;

    @ExcelProperty(value = "数值", index = 1)
    @ColumnWidth(15)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private String value;
}
