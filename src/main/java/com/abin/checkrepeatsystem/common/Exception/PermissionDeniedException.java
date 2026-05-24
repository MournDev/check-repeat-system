package com.abin.checkrepeatsystem.common.exception;

import com.abin.checkrepeatsystem.common.enums.ResultCode;
import lombok.Getter;

/**
 * 权限拒绝异常
 */
@Getter
public class PermissionDeniedException extends BaseException {

    private final ResultCode resultCode;
    private final Long userId;
    private final String operation;

    public PermissionDeniedException(ResultCode resultCode, Long userId, String operation, String message) {
        super(message);
        this.resultCode = resultCode;
        this.userId = userId;
        this.operation = operation;
    }
}
