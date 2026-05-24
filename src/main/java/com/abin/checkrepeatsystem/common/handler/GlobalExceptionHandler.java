package com.abin.checkrepeatsystem.common.handler;

import com.abin.checkrepeatsystem.common.exception.BaseException;
import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.Result;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：基于ResultCode枚举统一响应格式
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // -------------------------- 1. 自定义业务异常（优先级最高） --------------------------
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<?>> handleBusinessException(BusinessException e) {
        ResultCode resultCode = e.getResultCode();
        // 日志：打印异常信息（包含错误码、额外数据）
        log.info("业务异常：code={}, msg={}, extraData={}",
                resultCode.getCode(), e.getMessage(), e.getExtraData(), e);

        // 构造统一Result响应（直接从枚举获取错误码、HTTP状态码）
        Result<?> result = Result.error(
                resultCode.getCode(),    // 业务错误码（如 40001）
                e.getMessage(),          // 错误信息（自定义或枚举默认）
                e.getExtraData()         // 额外数据（错误参数、资源ID等）
        );

        // 返回：HTTP状态码与枚举绑定（如 400、404）
        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }

    // -------------------------- 2. 框架参数类型异常（参考原有写法） --------------------------
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<String>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配：{}", e.getMessage(), e);
        // 关联参数类型错误的ResultCode
        ResultCode resultCode = ResultCode.PARAM_TYPE_ERROR;
        String customMsg = String.format("参数【%s】类型错误，需传入【%s】类型",
                e.getName(), e.getRequiredType().getSimpleName());

        // 构造统一Result响应
        Result<String> result = Result.error(
                resultCode.getCode(),
                customMsg,
                null
        );

        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }

    // -------------------------- 3. @Valid 请求体校验失败 --------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", errors);
        ResultCode resultCode = ResultCode.PARAM_ERROR;
        Result<String> result = Result.error(resultCode.getCode(), errors, null);
        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }

    // -------------------------- 4. 约束校验失败（@Validated on path/query params） --------------------------
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<String>> handleConstraintViolation(ConstraintViolationException e) {
        String errors = e.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", errors);
        ResultCode resultCode = ResultCode.PARAM_ERROR;
        Result<String> result = Result.error(resultCode.getCode(), errors, null);
        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }

    // -------------------------- 5. 认证异常 --------------------------
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<?>> handleBaseException(BaseException e) {
        log.warn("认证/基础异常：{}", e.getMessage());
        ResultCode resultCode = ResultCode.NOT_LOGIN;
        Result<?> result = Result.error(resultCode, e.getMessage());
        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }

    // -------------------------- 6. 权限拒绝 --------------------------
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<?>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限拒绝：{}", e.getMessage());
        ResultCode resultCode = ResultCode.PERMISSION_NO_ACCESS;
        Result<?> result = Result.error(resultCode, "权限不足，无法执行此操作");
        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }

    // -------------------------- 7. 文件大小超限 --------------------------
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<?>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("文件大小超限：{}", e.getMessage());
        ResultCode resultCode = ResultCode.PARAM_ERROR;
        Result<?> result = Result.error(resultCode, "文件大小超过限制（最大50MB）");
        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }

    // -------------------------- 8. 请求体解析失败 --------------------------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        ResultCode resultCode = ResultCode.PARAM_FORMAT_ERROR;
        Result<?> result = Result.error(resultCode, "请求体格式错误，请检查JSON格式");
        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }

    // -------------------------- 9. HTTP方法不支持 --------------------------
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<?>> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("HTTP方法不支持：{}", e.getMessage());
        ResultCode resultCode = ResultCode.PARAM_ERROR;
        Result<?> result = Result.error(resultCode, "请求方法" + e.getMethod() + "不支持，支持：" + e.getSupportedHttpMethods());
        return new ResponseEntity<>(result, HttpStatus.METHOD_NOT_ALLOWED);
    }

    // -------------------------- 10. 非法参数 --------------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<?>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("非法参数：{}", e.getMessage());
        ResultCode resultCode = ResultCode.PARAM_ERROR;
        Result<?> result = Result.error(resultCode, e.getMessage());
        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }

    // -------------------------- 12. 资源未找到（Spring Boot 3） --------------------------
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<?>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源未找到：{}", e.getMessage());
        ResultCode resultCode = ResultCode.RESOURCE_NOT_FOUND;
        Result<?> result = Result.error(resultCode);
        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }

    // -------------------------- 13. 系统异常（兜底处理） --------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<?>> handleSystemException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        // 关联系统错误的ResultCode
        ResultCode resultCode = ResultCode.SYSTEM_ERROR;

        Result<?> result = Result.error(
                resultCode.getCode(),
                resultCode.getDefaultMsg(),  // 使用枚举默认信息，避免暴露技术细节
                null
        );

        return new ResponseEntity<>(result, resultCode.getHttpStatus());
    }
}