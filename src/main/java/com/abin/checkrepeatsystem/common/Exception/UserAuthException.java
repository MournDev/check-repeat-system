package com.abin.checkrepeatsystem.common.exception;

import com.abin.checkrepeatsystem.common.enums.ResultCode;
import lombok.Getter;

/**
 * 用户认证异常（JWT过期、签名错误、Token无效等）
 */
@Getter
public class UserAuthException extends BaseException {

    private final ResultCode resultCode;

    public UserAuthException(String message) {
        super(message);
        this.resultCode = ResultCode.NOT_LOGIN;
    }

    public UserAuthException(String message, Throwable cause) {
        super(message, cause);
        this.resultCode = ResultCode.NOT_LOGIN;
    }

    public UserAuthException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public UserAuthException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }
}
