package com.abin.checkrepeatsystem.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(
                buildCache("dictData", 10, 200),
                buildCache("dictLabel", 10, 500),
                buildCache("subjectTree", 30, 10),
                buildCache("paperDict", 10, 10),
                buildCache("adminColleges", 30, 10),
                buildCache("adminMajors", 30, 10),
                buildCache("adminGrades", 30, 10),
                buildCache("adminMajorMap", 30, 10)
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, int expireMinutes, int maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .build());
    }
}
