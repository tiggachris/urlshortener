package com.example.urlshortener.service;

import com.example.urlshortener.dto.AnalyticsResponse;
import com.example.urlshortener.dto.ClickEventDto;
import com.example.urlshortener.entity.ClickEvent;
import com.example.urlshortener.entity.UrlMapping;
import com.example.urlshortener.exception.ResourceNotFoundException;
import com.example.urlshortener.repository.ClickEventRepository;
import com.example.urlshortener.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public AnalyticsService(ClickEventRepository clickEventRepository, UrlRepository urlRepository) {
        this.clickEventRepository = clickEventRepository;
        this.urlRepository = urlRepository;
    }

    @Async("analyticsExecutor")
    @Transactional
    public void recordClick(String shortCode, String clientIp, String userAgent, String referrer) {
        try {
            // Increment overall click counter on mapping
            urlRepository.incrementClickCount(shortCode);

            // Parse device and browser
            String browser = parseBrowser(userAgent);
            String deviceType = parseDevice(userAgent);
            String cleanReferrer = cleanReferrer(referrer);

            ClickEvent clickEvent = ClickEvent.builder()
                    .shortCode(shortCode)
                    .ipAddress(clientIp)
                    .userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
                    .browser(browser)
                    .deviceType(deviceType)
                    .referrer(cleanReferrer.length() > 500 ? cleanReferrer.substring(0, 500) : cleanReferrer)
                    .build();

            clickEventRepository.save(clickEvent);
            log.debug("Recorded click event for {}: browser={}, device={}", shortCode, browser, deviceType);
        } catch (Exception e) {
            log.error("Failed to record click for {}: {}", shortCode, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL code not found: " + shortCode));

        long totalClicks = clickEventRepository.countByShortCode(shortCode);

        // Group by browser
        Map<String, Long> clicksByBrowser = new HashMap<>();
        for (Object[] row : clickEventRepository.countClicksByBrowser(shortCode)) {
            clicksByBrowser.put((String) row[0], (Long) row[1]);
        }

        // Group by device
        Map<String, Long> clicksByDevice = new HashMap<>();
        for (Object[] row : clickEventRepository.countClicksByDeviceType(shortCode)) {
            clicksByDevice.put((String) row[0], (Long) row[1]);
        }

        // Top referrers
        Map<String, Long> topReferrers = new HashMap<>();
        for (Object[] row : clickEventRepository.countClicksByReferrer(shortCode)) {
            topReferrers.put((String) row[0], (Long) row[1]);
        }

        // Recent clicks
        List<ClickEventDto> recentClicks = clickEventRepository.findTop20ByShortCodeOrderByTimestampDesc(shortCode)
                .stream()
                .map(e -> ClickEventDto.builder()
                        .timestamp(e.getTimestamp())
                        .browser(e.getBrowser())
                        .deviceType(e.getDeviceType())
                        .referrer(e.getReferrer())
                        .maskedIp(maskIp(e.getIpAddress()))
                        .build())
                .collect(Collectors.toList());

        return AnalyticsResponse.builder()
                .shortCode(shortCode)
                .originalUrl(mapping.getOriginalUrl())
                .shortUrl(baseUrl + "/" + shortCode)
                .totalClicks(totalClicks)
                .clicksByBrowser(clicksByBrowser)
                .clicksByDevice(clicksByDevice)
                .topReferrers(topReferrers)
                .recentClicks(recentClicks)
                .build();
    }

    private String parseBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/") && !ua.contains("edg/")) return "Chrome";
        if (ua.contains("safari/") && !ua.contains("chrome/")) return "Safari";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("opera/") || ua.contains("opr/")) return "Opera";
        if (ua.contains("curl") || ua.contains("k6")) return "Benchmark/Bot";
        return "Other";
    }

    private String parseDevice(String userAgent) {
        if (userAgent == null) return "Desktop";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "Mobile";
        if (ua.contains("ipad") || ua.contains("tablet")) return "Tablet";
        if (ua.contains("k6") || ua.contains("curl")) return "Load Test";
        return "Desktop";
    }

    private String cleanReferrer(String referrer) {
        if (referrer == null || referrer.isBlank() || referrer.contains("localhost") || referrer.contains("127.0.0.1")) {
            return "Direct";
        }
        try {
            URI uri = URI.create(referrer);
            String host = uri.getHost();
            if (host != null) {
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (Exception ignored) {}
        return referrer;
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isBlank() || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1") || ip.startsWith("127.")) {
            return "127.0.0.***";
        }
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".***.***";
            }
        }
        return ip.substring(0, Math.min(ip.length(), 6)) + "***";
    }
}
