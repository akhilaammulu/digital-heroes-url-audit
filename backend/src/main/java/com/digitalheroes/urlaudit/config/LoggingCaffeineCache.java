package com.digitalheroes.urlaudit.config;

import com.digitalheroes.urlaudit.util.RequestIdUtils;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCache;

@Slf4j
public class LoggingCaffeineCache extends CaffeineCache {

    public LoggingCaffeineCache(String name, Cache<Object, Object> cache) {
        super(name, cache);
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper value = super.get(key);
        String event = value == null ? "cache_miss" : "cache_hit";
        log.atInfo()
                .addKeyValue("event", event)
                .addKeyValue("requestId", RequestIdUtils.current())
                .addKeyValue("cacheName", getName())
                .addKeyValue("key", key)
                .log(event);
        return value;
    }
}
