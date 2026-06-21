package com.abin.checkrepeatsystem.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

/**
 * 密码复杂度校验注解
 * 要求：8-50位，至少包含大写字母、小写字母、数字、特殊字符中的3种
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {})
@Pattern(
        regexp = "^(?![a-zA-Z]+$)(?![a-z0-9]+$)(?![A-Z0-9]+$)(?![a-z\\W]+$)(?![A-Z\\W]+$)(?![0-9\\W]+$).{8,50}$",
        message = "密码长度8-50位，且至少包含大写字母、小写字母、数字、特殊字符中的3种"
)
public @interface ValidPassword {
    String message() default "密码长度8-50位，且至少包含大写字母、小写字母、数字、特殊字符中的3种";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}