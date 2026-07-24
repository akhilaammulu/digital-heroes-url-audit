package com.digitalheroes.urlaudit.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String AUDIT_CACHE_NAME = "auditResponses";

    @Bean
    public CacheManager cacheManager(UrlAuditProperties properties) {
        LoggingCaffeineCache auditCache = new LoggingCaffeineCache(
                AUDIT_CACHE_NAME,
                Caffeine.newBuilder()
                        .expireAfterWrite(properties.cacheTtl())
                        .maximumSize(properties.cacheMaximumSize())
                        .build());

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(auditCache));
        return cacheManager;
    }
}
