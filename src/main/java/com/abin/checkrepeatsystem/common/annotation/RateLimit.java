package com.abin.checkrepeatsystem.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解 — 基于Redis滑动窗口实现
 * 可用于登录、忘记密码、文件上传等敏感接口
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 时间窗口内的最大请求数 */
    int maxRequests() default 10;

    /** 时间窗口长度（秒） */
    int windowSeconds() default 60;

    /** 限流键前缀 */
    String keyPrefix() default "rate_limit";

    /** 提示信息 */
    String message() default "请求过于频繁，请稍后重试";
}
