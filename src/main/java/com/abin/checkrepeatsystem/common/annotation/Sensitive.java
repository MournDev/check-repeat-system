package com.abin.checkrepeatsystem.common.annotation;

import com.abin.checkrepeatsystem.common.security.SensitiveSerializer.*;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感数据脱敏注解
 * 标记在实体字段上，序列化时自动脱敏
 */
public @interface Sensitive {

    /**
     * 脱敏类型
     */
    SensitiveType value();

    enum SensitiveType {
        PHONE,      // 手机号：138****1234
        EMAIL,      // 邮箱：a***@example.com
        ID_CARD     // 身份证：110***********1234
    }

    /**
     * 手机号脱敏注解
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonSerialize(using = PhoneSerializer.class)
    @interface Phone {}

    /**
     * 邮箱脱敏注解
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonSerialize(using = EmailSerializer.class)
    @interface Email {}

    /**
     * 身份证号脱敏注解
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @JacksonAnnotationsInside
    @JsonSerialize(using = IdCardSerializer.class)
    @interface IdCard {}
}
