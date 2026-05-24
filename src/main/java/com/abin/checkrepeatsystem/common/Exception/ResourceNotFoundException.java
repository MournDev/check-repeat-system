package com.abin.checkrepeatsystem.common.exception;

import com.abin.checkrepeatsystem.common.enums.ResultCode;
import lombok.Getter;

/**
 * 资源未找到异常
 */
@Getter
public class ResourceNotFoundException extends BaseException {

    private final ResultCode resultCode;
    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(ResultCode resultCode, String resourceType, Object resourceId, String message) {
        super(message);
        this.resultCode = resultCode;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
        this.resourceType = null;
        this.resourceId = null;
    }
}
