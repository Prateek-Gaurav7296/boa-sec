package com.riskengine.service;

import com.riskengine.dto.IframeSignals;
import com.riskengine.dto.SignalRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RiskCollectMapper {

    @Value("${risk.engine.allowed-hosts:localhost,127.0.0.1}")
    private String allowedHostsConfig;

    @SuppressWarnings("unchecked")
    public SignalRequest toSignalRequest(Map<String, Object> payload) {
        if (payload == null) return null;

        String sessionId = getString(payload, "sessionId");
        String userId = getString(payload, "userId");
        String userAgent = null;
        String pageOrigin = null;
        String referrerUrl = null;
        String timezone = null;
        Integer screenWidth = null;
        Integer screenHeight = null;
        Boolean webdriverFlag = Boolean.FALSE;
        Boolean pageOriginNotFromOrg = null;

        Map<String, Object> stage1 = (Map<String, Object>) payload.get("stage1");
        if (stage1 != null) {
            userAgent = getString(stage1, "userAgent");
            pageOrigin = getString(stage1, "origin");
            referrerUrl = getString(stage1, "referrer");
            timezone = getString(stage1, "timezone");
            Object wd = stage1.get("webdriver");
            webdriverFlag = Boolean.TRUE.equals(wd);
            Map<String, Object> screen = (Map<String, Object>) stage1.get("screen");
            if (screen != null) {
                screenWidth = getInt(screen, "width");
                screenHeight = getInt(screen, "height");
            }
        }

        Map<String, Object> stage3 = (Map<String, Object>) payload.get("stage3");
        if (stage3 != null) {
            Map<String, Object> automation = (Map<String, Object>) stage3.get("automation");
            if (automation != null && Boolean.TRUE.equals(automation.get("webdriver"))) {
                webdriverFlag = Boolean.TRUE;
            }
        }

        if (pageOrigin != null && !pageOrigin.isBlank()) {
            String host = extractHostFromOrigin(pageOrigin);
            if (host != null && !host.isBlank()) {
                Set<String> allowed = Arrays.stream(allowedHostsConfig.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
                pageOriginNotFromOrg = !allowed.contains(host.toLowerCase());
            }
        }

        IframeSignals iframeSignals = IframeSignals.builder()
                .total(0)
                .suspicious(0)
                .hidden(0)
                .offscreen(0)
                .crossOrigin(0)
                .notFromOrg(0)
                .build();

        return SignalRequest.builder()
                .sessionId(sessionId)
                .userId(userId)
                .webdriverFlag(webdriverFlag)
                .pageOrigin(pageOrigin)
                .pageOriginNotFromOrg(pageOriginNotFromOrg)
                .referrerUrl(referrerUrl)
                .iframeSignals(iframeSignals)
                .fetchOverridden(Boolean.FALSE)
                .userAgent(userAgent)
                .screenWidth(screenWidth)
                .screenHeight(screenHeight)
                .timezone(timezone)
                .clickIntervalAvg(null)
                .build();
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    private static Integer getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String extractHostFromOrigin(String origin) {
        if (origin == null) return null;
        origin = origin.trim();
        int i = origin.indexOf("://");
        if (i >= 0) origin = origin.substring(i + 3);
        int j = origin.indexOf(':');
        if (j >= 0) origin = origin.substring(0, j);
        int k = origin.indexOf('/');
        if (k >= 0) origin = origin.substring(0, k);
        return origin.isEmpty() ? null : origin;
    }
}
