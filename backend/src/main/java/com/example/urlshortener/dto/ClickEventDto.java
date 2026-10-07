package com.example.urlshortener.dto;

import java.time.Instant;

public class ClickEventDto {
    private Instant timestamp;
    private String browser;
    private String deviceType;
    private String referrer;
    private String maskedIp;

    public ClickEventDto() {}

    public ClickEventDto(Instant timestamp, String browser, String deviceType, String referrer, String maskedIp) {
        this.timestamp = timestamp;
        this.browser = browser;
        this.deviceType = deviceType;
        this.referrer = referrer;
        this.maskedIp = maskedIp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Instant timestamp;
        private String browser;
        private String deviceType;
        private String referrer;
        private String maskedIp;

        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder browser(String browser) { this.browser = browser; return this; }
        public Builder deviceType(String deviceType) { this.deviceType = deviceType; return this; }
        public Builder referrer(String referrer) { this.referrer = referrer; return this; }
        public Builder maskedIp(String maskedIp) { this.maskedIp = maskedIp; return this; }

        public ClickEventDto build() {
            return new ClickEventDto(timestamp, browser, deviceType, referrer, maskedIp);
        }
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
    public String getMaskedIp() { return maskedIp; }
    public void setMaskedIp(String maskedIp) { this.maskedIp = maskedIp; }
}
