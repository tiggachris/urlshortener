package com.example.urlshortener.dto;

import java.util.List;
import java.util.Map;

public class AnalyticsResponse {
    private String shortCode;
    private String originalUrl;
    private String shortUrl;
    private long totalClicks;
    private Map<String, Long> clicksByBrowser;
    private Map<String, Long> clicksByDevice;
    private Map<String, Long> topReferrers;
    private List<ClickEventDto> recentClicks;

    public AnalyticsResponse() {}

    public AnalyticsResponse(String shortCode, String originalUrl, String shortUrl, long totalClicks, Map<String, Long> clicksByBrowser, Map<String, Long> clicksByDevice, Map<String, Long> topReferrers, List<ClickEventDto> recentClicks) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.totalClicks = totalClicks;
        this.clicksByBrowser = clicksByBrowser;
        this.clicksByDevice = clicksByDevice;
        this.topReferrers = topReferrers;
        this.recentClicks = recentClicks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String shortCode;
        private String originalUrl;
        private String shortUrl;
        private long totalClicks;
        private Map<String, Long> clicksByBrowser;
        private Map<String, Long> clicksByDevice;
        private Map<String, Long> topReferrers;
        private List<ClickEventDto> recentClicks;

        public Builder shortCode(String shortCode) { this.shortCode = shortCode; return this; }
        public Builder originalUrl(String originalUrl) { this.originalUrl = originalUrl; return this; }
        public Builder shortUrl(String shortUrl) { this.shortUrl = shortUrl; return this; }
        public Builder totalClicks(long totalClicks) { this.totalClicks = totalClicks; return this; }
        public Builder clicksByBrowser(Map<String, Long> clicksByBrowser) { this.clicksByBrowser = clicksByBrowser; return this; }
        public Builder clicksByDevice(Map<String, Long> clicksByDevice) { this.clicksByDevice = clicksByDevice; return this; }
        public Builder topReferrers(Map<String, Long> topReferrers) { this.topReferrers = topReferrers; return this; }
        public Builder recentClicks(List<ClickEventDto> recentClicks) { this.recentClicks = recentClicks; return this; }

        public AnalyticsResponse build() {
            return new AnalyticsResponse(shortCode, originalUrl, shortUrl, totalClicks, clicksByBrowser, clicksByDevice, topReferrers, recentClicks);
        }
    }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }
    public long getTotalClicks() { return totalClicks; }
    public void setTotalClicks(long totalClicks) { this.totalClicks = totalClicks; }
    public Map<String, Long> getClicksByBrowser() { return clicksByBrowser; }
    public void setClicksByBrowser(Map<String, Long> clicksByBrowser) { this.clicksByBrowser = clicksByBrowser; }
    public Map<String, Long> getClicksByDevice() { return clicksByDevice; }
    public void setClicksByDevice(Map<String, Long> clicksByDevice) { this.clicksByDevice = clicksByDevice; }
    public Map<String, Long> getTopReferrers() { return topReferrers; }
    public void setTopReferrers(Map<String, Long> topReferrers) { this.topReferrers = topReferrers; }
    public List<ClickEventDto> getRecentClicks() { return recentClicks; }
    public void setRecentClicks(List<ClickEventDto> recentClicks) { this.recentClicks = recentClicks; }
}
