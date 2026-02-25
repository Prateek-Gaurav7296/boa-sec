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

        Boolean functionTampered = null;
        Boolean iframeMismatch = null;
        Boolean storageBlocked = null;
        Boolean cspRestricted = null;
        Integer pluginsLength = null;
        Integer mimeTypesLength = null;
        Boolean hasChrome = null;
        Boolean hasWebdriverScriptFn = null;

        Map<String, Object> stage3 = (Map<String, Object>) payload.get("stage3");
        if (stage3 != null) {
            functionTampered = getBoolean(stage3, "functionTampered");
            iframeMismatch = getBoolean(stage3, "iframeMismatch");
            Boolean storageWorks = getBoolean(stage3, "storageWorks");
            storageBlocked = storageWorks != null && !storageWorks;
            cspRestricted = getBoolean(stage3, "cspRestricted");
            Map<String, Object> automation = (Map<String, Object>) stage3.get("automation");
            if (automation != null) {
                if (Boolean.TRUE.equals(automation.get("webdriver"))) {
                    webdriverFlag = Boolean.TRUE;
                }
                pluginsLength = getInt(automation, "pluginsLength");
                mimeTypesLength = getInt(automation, "mimeTypesLength");
                hasChrome = getBoolean(automation, "hasChrome");
                hasWebdriverScriptFn = getBoolean(automation, "hasWebdriverScriptFn");
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

        IframeSignals iframeSignals = parseIframeSignals(payload);

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
                .functionTampered(functionTampered)
                .iframeMismatch(iframeMismatch)
                .storageBlocked(storageBlocked)
                .cspRestricted(cspRestricted)
                .pluginsLength(pluginsLength)
                .mimeTypesLength(mimeTypesLength)
                .hasChrome(hasChrome)
                .hasWebdriverScriptFn(hasWebdriverScriptFn)
                .build();
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    private static Boolean getBoolean(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return null;
    }

    private static int nullToZero(Integer v) {
        return v != null ? v : 0;
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

    private static IframeSignals parseIframeSignals(Map<String, Object> payload) {
        Object ifs = payload.get("iframeSignals");
        if (ifs instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) ifs;
            return IframeSignals.builder()
                    .total(nullToZero(getInt(m, "total")))
                    .suspicious(nullToZero(getInt(m, "suspicious")))
                    .hidden(nullToZero(getInt(m, "hidden")))
                    .offscreen(nullToZero(getInt(m, "offscreen")))
                    .crossOrigin(nullToZero(getInt(m, "crossOrigin")))
                    .notFromOrg(nullToZero(getInt(m, "notFromOrg")))
                    .build();
        }
        return IframeSignals.builder()
                .total(0)
                .suspicious(0)
                .hidden(0)
                .offscreen(0)
                .crossOrigin(0)
                .notFromOrg(0)
                .build();
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
