package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ShortenRequest {

    @NotBlank(message = "Original URL must not be blank")
    private String originalUrl;

    @Pattern(regexp = "^[a-zA-Z0-9-_]{3,20}$|^$", message = "Custom alias must be between 3 and 20 alphanumeric characters, dashes, or underscores")
    private String customAlias;

    private Integer expiresInDays;

    public ShortenRequest() {}

    public ShortenRequest(String originalUrl, String customAlias, Integer expiresInDays) {
        this.originalUrl = originalUrl;
        this.customAlias = customAlias;
        this.expiresInDays = expiresInDays;
    }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public String getCustomAlias() { return customAlias; }
    public void setCustomAlias(String customAlias) { this.customAlias = customAlias; }
    public Integer getExpiresInDays() { return expiresInDays; }
    public void setExpiresInDays(Integer expiresInDays) { this.expiresInDays = expiresInDays; }
}
