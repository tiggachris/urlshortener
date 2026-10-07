package com.example.urlshortener.dto;

import java.time.Instant;

public class UrlResponse {
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private Long clickCount;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean fromCache;
    private long latencyMs;

    public UrlResponse() {}

    public UrlResponse(String shortCode, String shortUrl, String originalUrl, Long clickCount, Instant createdAt, Instant expiresAt, boolean fromCache, long latencyMs) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.originalUrl = originalUrl;
        this.clickCount = clickCount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.fromCache = fromCache;
        this.latencyMs = latencyMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String shortCode;
        private String shortUrl;
        private String originalUrl;
        private Long clickCount;
        private Instant createdAt;
        private Instant expiresAt;
        private boolean fromCache;
        private long latencyMs;

        public Builder shortCode(String shortCode) { this.shortCode = shortCode; return this; }
        public Builder shortUrl(String shortUrl) { this.shortUrl = shortUrl; return this; }
        public Builder originalUrl(String originalUrl) { this.originalUrl = originalUrl; return this; }
        public Builder clickCount(Long clickCount) { this.clickCount = clickCount; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder fromCache(boolean fromCache) { this.fromCache = fromCache; return this; }
        public Builder latencyMs(long latencyMs) { this.latencyMs = latencyMs; return this; }

        public UrlResponse build() {
            return new UrlResponse(shortCode, shortUrl, originalUrl, clickCount, createdAt, expiresAt, fromCache, latencyMs);
        }
    }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public Long getClickCount() { return clickCount; }
    public void setClickCount(Long clickCount) { this.clickCount = clickCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isFromCache() { return fromCache; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}
