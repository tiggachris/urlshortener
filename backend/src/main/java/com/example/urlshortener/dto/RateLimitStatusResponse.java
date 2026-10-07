package com.example.urlshortener.dto;

public class RateLimitStatusResponse {
    private String clientIp;
    private long tokensRemaining;
    private long capacity;
    private long refillDurationSeconds;
    private long resetSeconds;

    public RateLimitStatusResponse() {}

    public RateLimitStatusResponse(String clientIp, long tokensRemaining, long capacity, long refillDurationSeconds, long resetSeconds) {
        this.clientIp = clientIp;
        this.tokensRemaining = tokensRemaining;
        this.capacity = capacity;
        this.refillDurationSeconds = refillDurationSeconds;
        this.resetSeconds = resetSeconds;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String clientIp;
        private long tokensRemaining;
        private long capacity;
        private long refillDurationSeconds;
        private long resetSeconds;

        public Builder clientIp(String clientIp) { this.clientIp = clientIp; return this; }
        public Builder tokensRemaining(long tokensRemaining) { this.tokensRemaining = tokensRemaining; return this; }
        public Builder capacity(long capacity) { this.capacity = capacity; return this; }
        public Builder refillDurationSeconds(long refillDurationSeconds) { this.refillDurationSeconds = refillDurationSeconds; return this; }
        public Builder resetSeconds(long resetSeconds) { this.resetSeconds = resetSeconds; return this; }

        public RateLimitStatusResponse build() {
            return new RateLimitStatusResponse(clientIp, tokensRemaining, capacity, refillDurationSeconds, resetSeconds);
        }
    }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public long getTokensRemaining() { return tokensRemaining; }
    public void setTokensRemaining(long tokensRemaining) { this.tokensRemaining = tokensRemaining; }
    public long getCapacity() { return capacity; }
    public void setCapacity(long capacity) { this.capacity = capacity; }
    public long getRefillDurationSeconds() { return refillDurationSeconds; }
    public void setRefillDurationSeconds(long refillDurationSeconds) { this.refillDurationSeconds = refillDurationSeconds; }
    public long getResetSeconds() { return resetSeconds; }
    public void setResetSeconds(long resetSeconds) { this.resetSeconds = resetSeconds; }
}
