package com.abin.checkrepeatsystem.common.security;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 敏感数据脱敏序列化器
 * 支持手机号、邮箱、身份证号等常见PII字段脱敏
 */
public class SensitiveSerializer {

    /**
     * 手机号脱敏：138****1234
     */
    public static class PhoneSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null || value.length() < 7) {
                gen.writeString(value);
                return;
            }
            gen.writeString(value.substring(0, 3) + "****" + value.substring(value.length() - 4));
        }
    }

    /**
     * 邮箱脱敏：a***@example.com
     */
    public static class EmailSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null || !value.contains("@")) {
                gen.writeString(value);
                return;
            }
            int atIndex = value.indexOf('@');
            String prefix = value.substring(0, Math.min(1, atIndex));
            String domain = value.substring(atIndex);
            gen.writeString(prefix + "***" + domain);
        }
    }

    /**
     * 身份证号脱敏：110***********1234
     */
    public static class IdCardSerializer extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null || value.length() < 8) {
                gen.writeString(value);
                return;
            }
            gen.writeString(value.substring(0, 3) + "***********" + value.substring(value.length() - 4));
        }
    }
}
