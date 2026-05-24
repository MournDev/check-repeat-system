package com.abin.checkrepeatsystem.common.exception;

/**
 * 用户认证异常
 */
public class UserAuthException extends BaseException {

    public UserAuthException(String message) {
        super(message);
    }

    public UserAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
