package com.abin.checkrepeatsystem.common.exception;

import com.abin.checkrepeatsystem.common.enums.ResultCode;
import lombok.Getter;

/**
 * 业务异常，关联ResultCode枚举统一响应
 */
@Getter
public class BusinessException extends BaseException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode, String message) {
        super(message, null, null);
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message, Object extraData) {
        super(message, null, extraData);
        this.resultCode = resultCode;
    }
}
