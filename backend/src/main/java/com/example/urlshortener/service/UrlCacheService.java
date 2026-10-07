package com.example.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UrlCacheService {

    private static final Logger log = LoggerFactory.getLogger(UrlCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration defaultTtl;

    // Local in-memory L1 cache fallback in case Redis is down or for extra speed
    private final Map<String, String> localFallbackCache = new ConcurrentHashMap<>();

    public UrlCacheService(
            StringRedisTemplate redisTemplate,
            @Value("${app.cache.ttl-hours:24}") long ttlHours) {
        this.redisTemplate = redisTemplate;
        this.defaultTtl = Duration.ofHours(ttlHours);
    }

    private String buildKey(String shortCode) {
        return "url:" + shortCode;
    }

    public String get(String shortCode) {
        String key = buildKey(shortCode);
        try {
            String url = redisTemplate.opsForValue().get(key);
            if (url != null) {
                return url;
            }
        } catch (Exception e) {
            log.warn("Redis GET failed for key {}, falling back to local memory: {}", key, e.getMessage());
            return localFallbackCache.get(shortCode);
        }
        return localFallbackCache.get(shortCode);
    }

    public void set(String shortCode, String originalUrl) {
        String key = buildKey(shortCode);
        localFallbackCache.put(shortCode, originalUrl);
        try {
            redisTemplate.opsForValue().set(key, originalUrl, defaultTtl);
        } catch (Exception e) {
            log.warn("Redis SET failed for key {}: {}", key, e.getMessage());
        }
    }

    public void evict(String shortCode) {
        String key = buildKey(shortCode);
        localFallbackCache.remove(shortCode);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis DELETE failed for key {}: {}", key, e.getMessage());
        }
    }
}
