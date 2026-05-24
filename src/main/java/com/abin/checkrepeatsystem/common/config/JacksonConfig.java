package com.abin.checkrepeatsystem.common.config;

import com.abin.checkrepeatsystem.common.security.XssStringDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 配置类
 * 处理 Long 类型序列化（避免前端精度问题）和 String 类型 XSS 过滤
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();

        SimpleModule module = new SimpleModule();

        // Long → String 序列化，避免前端精度丢失
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 注册 XSS 字符串反序列化器，覆盖 JSON @RequestBody 的 XSS 过滤
        module.addDeserializer(String.class, new XssStringDeserializer());

        objectMapper.registerModule(module);

        return objectMapper;
    }
}
