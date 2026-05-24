package com.abin.checkrepeatsystem.common.exception;

import lombok.Getter;

/**
 * 基础异常类，所有自定义业务异常的父类
 */
@Getter
public class BaseException extends RuntimeException {

    private final Object extraData;

    public BaseException(String message) {
        super(message);
        this.extraData = null;
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
        this.extraData = null;
    }

    public BaseException(String message, Throwable cause, Object extraData) {
        super(message, cause);
        this.extraData = extraData;
    }
}
