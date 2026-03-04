package com.abin.checkrepeatsystem.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${avatar.storage.base-path}")
    private String avatarBasePath;

    @Value("${avatar.access.url-prefix}")
    private String avatarUrlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射头像访问路径
        String location = Paths.get(avatarBasePath).toUri().toString();
        registry.addResourceHandler(avatarUrlPrefix + "**")
                .addResourceLocations(location);
        
    }
}
