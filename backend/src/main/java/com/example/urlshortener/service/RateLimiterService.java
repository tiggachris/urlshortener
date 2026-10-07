package com.example.urlshortener.service;

import com.example.urlshortener.dto.RateLimitStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final StringRedisTemplate redisTemplate;
    private final long capacity;
    private final long refillTokens;
    private final long refillDurationSeconds;

    // In-memory fallback if Redis is temporarily unreachable
    private final Map<String, LocalBucket> localFallbackBuckets = new ConcurrentHashMap<>();

    public RateLimiterService(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.capacity:15}") long capacity,
            @Value("${app.rate-limit.refill-tokens:15}") long refillTokens,
            @Value("${app.rate-limit.refill-duration-seconds:60}") long refillDurationSeconds) {
        this.redisTemplate = redisTemplate;
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDurationSeconds = refillDurationSeconds;
    }

    public static class TokenBucketResult {
        private final boolean allowed;
        private final long tokensRemaining;
        private final long retryAfterSeconds;

        public TokenBucketResult(boolean allowed, long tokensRemaining, long retryAfterSeconds) {
            this.allowed = allowed;
            this.tokensRemaining = tokensRemaining;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getTokensRemaining() {
            return tokensRemaining;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    /**
     * Attempts to consume 1 token for client IP using Token-Bucket algorithm.
     */
    public TokenBucketResult tryConsume(String clientIp) {
        String key = "rate_limit:" + clientIp;
        long now = Instant.now().toEpochMilli();

        try {
            String tokensStr = (String) redisTemplate.opsForHash().get(key, "tokens");
            String lastRefillStr = (String) redisTemplate.opsForHash().get(key, "lastRefill");

            long currentTokens = capacity;
            long lastRefill = now;

            if (tokensStr != null && lastRefillStr != null) {
                long storedTokens = Long.parseLong(tokensStr);
                lastRefill = Long.parseLong(lastRefillStr);

                long elapsedMillis = Math.max(0, now - lastRefill);
                double refillRatePerMillis = (double) refillTokens / (refillDurationSeconds * 1000.0);
                long newTokens = (long) (elapsedMillis * refillRatePerMillis);

                currentTokens = Math.min(capacity, storedTokens + newTokens);
                if (newTokens > 0) {
                    lastRefill = now;
                }
            }

            if (currentTokens >= 1) {
                currentTokens -= 1;
                redisTemplate.opsForHash().put(key, "tokens", String.valueOf(currentTokens));
                redisTemplate.opsForHash().put(key, "lastRefill", String.valueOf(lastRefill));
                redisTemplate.expire(key, refillDurationSeconds * 2, TimeUnit.SECONDS);

                return new TokenBucketResult(true, currentTokens, 0);
            } else {
                long waitMillis = (long) ((1.0 / ((double) refillTokens / (refillDurationSeconds * 1000.0))));
                long retryAfterSec = Math.max(1, waitMillis / 1000);
                return new TokenBucketResult(false, 0, retryAfterSec);
            }
        } catch (Exception e) {
            log.warn("Redis rate limiter unavailable for IP {}, using in-memory fallback: {}", clientIp, e.getMessage());
            return tryConsumeLocal(clientIp, now);
        }
    }

    public RateLimitStatusResponse getStatus(String clientIp) {
        String key = "rate_limit:" + clientIp;
        long tokens = capacity;
        try {
            String tokensStr = (String) redisTemplate.opsForHash().get(key, "tokens");
            if (tokensStr != null) {
                tokens = Long.parseLong(tokensStr);
            }
        } catch (Exception e) {
            LocalBucket b = localFallbackBuckets.get(clientIp);
            if (b != null) tokens = b.tokens;
        }

        return RateLimitStatusResponse.builder()
                .clientIp(clientIp)
                .tokensRemaining(tokens)
                .capacity(capacity)
                .refillDurationSeconds(refillDurationSeconds)
                .resetSeconds(refillDurationSeconds)
                .build();
    }

    private synchronized TokenBucketResult tryConsumeLocal(String clientIp, long now) {
        LocalBucket bucket = localFallbackBuckets.computeIfAbsent(clientIp, k -> new LocalBucket(capacity, now));
        long elapsedMillis = Math.max(0, now - bucket.lastRefill);
        double refillRatePerMillis = (double) refillTokens / (refillDurationSeconds * 1000.0);
        long newTokens = (long) (elapsedMillis * refillRatePerMillis);

        bucket.tokens = Math.min(capacity, bucket.tokens + newTokens);
        if (newTokens > 0) {
            bucket.lastRefill = now;
        }

        if (bucket.tokens >= 1) {
            bucket.tokens -= 1;
            return new TokenBucketResult(true, bucket.tokens, 0);
        } else {
            return new TokenBucketResult(false, 0, 1);
        }
    }

    private static class LocalBucket {
        long tokens;
        long lastRefill;

        LocalBucket(long tokens, long lastRefill) {
            this.tokens = tokens;
            this.lastRefill = lastRefill;
        }
    }
}
