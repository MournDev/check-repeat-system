package com.abin.checkrepeatsystem.common.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Jackson 字符串反序列化器：对 JSON @RequestBody 中的字符串字段进行 XSS 过滤
 */
public class XssStringDeserializer extends StringDeserializer {

    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script\\b[^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data\\s*:\\s*text/html", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<iframe\\b[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object\\b[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed\\b[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<link\\b[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<form\\b[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
    };

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = super.deserialize(p, ctxt);
        return sanitize(value);
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        for (Pattern pattern : XSS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }
        return result;
    }
}
