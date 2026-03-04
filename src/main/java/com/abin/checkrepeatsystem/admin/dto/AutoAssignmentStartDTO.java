package com.abin.checkrepeatsystem.admin.dto;

import com.abin.checkrepeatsystem.admin.dto.AutoAssignmentConfigDTO;
import lombok.Data;
import java.io.Serializable;

/**
 * 自动分配启动请求DTO
 */
@Data
public class AutoAssignmentStartDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 算法配置
     */
    private AutoAssignmentConfigDTO config;
}