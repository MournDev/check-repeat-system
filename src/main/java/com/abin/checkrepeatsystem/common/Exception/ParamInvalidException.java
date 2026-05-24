package com.abin.checkrepeatsystem.common.exception;

import com.abin.checkrepeatsystem.common.enums.ResultCode;

/**
 * 参数校验异常
 */
public class ParamInvalidException extends BaseException {

    private final ResultCode resultCode;

    public ParamInvalidException(ResultCode resultCode, Object extraData, String message) {
        super(message, null, extraData);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
