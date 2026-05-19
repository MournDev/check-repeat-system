package com.abin.checkrepeatsystem.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等性注解 — 用于POST/PUT接口防止重复提交
 * 客户端请求携带Idempotency-Key header，服务端基于Redis校验
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /** 幂等键有效期（秒），默认 5 分钟 */
    long ttlSeconds() default 300;

    /** 提示信息 */
    String message() default "请勿重复提交";
}
