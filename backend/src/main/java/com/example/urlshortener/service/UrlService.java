package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.UrlResponse;
import com.example.urlshortener.entity.UrlMapping;
import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.util.Base62Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    private final UrlRepository urlRepository;
    private final UrlCacheService urlCacheService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UrlService(UrlRepository urlRepository, UrlCacheService urlCacheService) {
        this.urlRepository = urlRepository;
        this.urlCacheService = urlCacheService;
    }

    @Transactional
    public UrlResponse shortenUrl(ShortenRequest request) {
        String originalUrl = normalizeAndValidateUrl(request.getOriginalUrl());

        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            shortCode = request.getCustomAlias().trim();
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new InvalidUrlException("Custom alias '" + shortCode + "' is already in use.");
            }
        } else {
            UrlMapping tempMapping = UrlMapping.builder()
                    .originalUrl(originalUrl)
                    .shortCode("temp_" + System.nanoTime())
                    .clickCount(0L)
                    .build();
            tempMapping = urlRepository.save(tempMapping);

            shortCode = Base62Encoder.encode(tempMapping.getId() + 100000L);
            tempMapping.setShortCode(shortCode);
            urlRepository.save(tempMapping);
        }

        Instant expiresAt = null;
        if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
            expiresAt = Instant.now().plus(request.getExpiresInDays(), ChronoUnit.DAYS);
        }

        UrlMapping finalMapping;
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            finalMapping = UrlMapping.builder()
                    .originalUrl(originalUrl)
                    .shortCode(shortCode)
                    .clickCount(0L)
                    .expiresAt(expiresAt)
                    .build();
            finalMapping = urlRepository.save(finalMapping);
        } else {
            finalMapping = urlRepository.findByShortCode(shortCode).orElseThrow();
            finalMapping.setExpiresAt(expiresAt);
            finalMapping = urlRepository.save(finalMapping);
        }

        urlCacheService.set(shortCode, originalUrl);

        return buildUrlResponse(finalMapping, false, 0);
    }

    @Transactional(readOnly = true)
    public UrlResponse getOriginalUrl(String shortCode) {
        long startTime = System.nanoTime();

        // 1. Try Redis cache first
        String cachedUrl = urlCacheService.get(shortCode);
        String resolvedShortUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl + "/" + shortCode : "/" + shortCode;
        if (cachedUrl != null) {
            long latencyMs = (System.nanoTime() - startTime) / 1_000_000;
            return UrlResponse.builder()
                    .shortCode(shortCode)
                    .originalUrl(cachedUrl)
                    .shortUrl(resolvedShortUrl)
                    .fromCache(true)
                    .latencyMs(Math.max(0, latencyMs))
                    .build();
        }

        // 2. Cache miss -> query DB
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL '" + shortCode + "' not found"));

        if (mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(Instant.now())) {
            throw new ResourceNotFoundException("Short URL '" + shortCode + "' has expired");
        }

        urlCacheService.set(shortCode, mapping.getOriginalUrl());

        long latencyMs = (System.nanoTime() - startTime) / 1_000_000;
        return buildUrlResponse(mapping, false, latencyMs);
    }

    private UrlResponse buildUrlResponse(UrlMapping mapping, boolean fromCache, long latencyMs) {
        String resolvedShortUrl = (baseUrl != null && !baseUrl.isBlank()) 
                ? baseUrl + "/" + mapping.getShortCode() 
                : "/" + mapping.getShortCode();
        return UrlResponse.builder()
                .shortCode(mapping.getShortCode())
                .shortUrl(resolvedShortUrl)
                .originalUrl(mapping.getOriginalUrl())
                .clickCount(mapping.getClickCount())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .fromCache(fromCache)
                .latencyMs(latencyMs)
                .build();
    }

    private String normalizeAndValidateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new InvalidUrlException("URL cannot be empty");
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        try {
            URI uri = URI.create(trimmed);
            if (uri.getHost() == null || !uri.getHost().contains(".")) {
                throw new InvalidUrlException("Invalid domain name: " + trimmed);
            }
            return trimmed;
        } catch (Exception e) {
            throw new InvalidUrlException("Malformed URL: " + e.getMessage());
        }
    }
}
