package com.abin.checkrepeatsystem.admin.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import com.alibaba.excel.enums.poi.HorizontalAlignmentEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志Excel导出VO
 */
public class OperationLogExcelVO {

    @ExcelProperty(value = "ID", index = 0)
    @ColumnWidth(15)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private Long id;

    @ExcelProperty(value = "操作人", index = 1)
    @ColumnWidth(20)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.LEFT)
    private String username;

    @ExcelProperty(value = "操作类型", index = 2)
    @ColumnWidth(25)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.LEFT)
    private String operationType;

    @ExcelProperty(value = "操作描述", index = 3)
    @ColumnWidth(50)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.LEFT)
    private String description;

    @ExcelProperty(value = "IP地址", index = 4)
    @ColumnWidth(20)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private String ipAddress;

    @ExcelProperty(value = "操作时间", index = 5)
    @ColumnWidth(25)
    @ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER)
    private LocalDateTime operationTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }
}
