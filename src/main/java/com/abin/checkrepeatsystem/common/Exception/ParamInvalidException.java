package com.abin.checkrepeatsystem.common.exception;

import com.abin.checkrepeatsystem.common.enums.ResultCode;
import lombok.Getter;

/**
 * 参数校验异常
 */
@Getter
public class ParamInvalidException extends BaseException {

    private final ResultCode resultCode;

    public ParamInvalidException(ResultCode resultCode, Object extraData, String message) {
        super(message, null, extraData);
        this.resultCode = resultCode;
    }
}
