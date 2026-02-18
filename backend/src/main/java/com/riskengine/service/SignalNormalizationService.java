package com.riskengine.service;

import com.riskengine.dto.IframeSignals;
import com.riskengine.dto.NormalizedSignals;
import com.riskengine.dto.SignalRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Normalizes raw client signals for rule-based (and future ML) consumption.
 * Boolean → 0/1, iframe counts from iframeSignals, page/iframe "not from org" flags, click interval → rapid-click.
 */
@Service
@RequiredArgsConstructor
public class SignalNormalizationService {

    private static final double RAPID_CLICK_THRESHOLD_MS = 50.0;

    @Value("${risk.engine.allowed-hosts:localhost,127.0.0.1}")
    private String allowedHostsConfig;

    private final ReferrerService referrerService;

    public NormalizedSignals normalize(SignalRequest request) {
        int webdriver = booleanToInt(request.getWebdriverFlag());
        int fetchOverridden = booleanToInt(request.getFetchOverridden());
        int iframeHidden = getIframeCount(request, IframeSignals::getHidden);
        int iframeOffscreen = getIframeCount(request, IframeSignals::getOffscreen);
        int iframeCrossOrigin = getIframeCount(request, IframeSignals::getCrossOrigin);
        int iframeNotFromOrg = getIframeCount(request, IframeSignals::getNotFromOrg);
        int pageOriginNotFromOrg = isPageOriginNotFromOrg(request);
        int referrerNotFromOrg = referrerService.isSuspicious(request.getReferrerUrl()) ? 1 : 0;
        int rapidClicking = isRapidClicking(request.getClickIntervalAvg());

        return NormalizedSignals.builder()
                .webdriverFlag(webdriver)
                .fetchOverridden(fetchOverridden)
                .iframeHidden(iframeHidden)
                .iframeOffscreen(iframeOffscreen)
                .iframeCrossOrigin(iframeCrossOrigin)
                .iframeNotFromOrg(iframeNotFromOrg)
                .pageOriginNotFromOrg(pageOriginNotFromOrg)
                .referrerNotFromOrg(referrerNotFromOrg)
                .rapidClicking(rapidClicking)
                .build();
    }

    private int isPageOriginNotFromOrg(SignalRequest request) {
        // Use backend allowed-hosts as source of truth when page origin is present
        String origin = request.getPageOrigin();
        if (origin != null && !origin.isBlank()) {
            String host = extractHostFromOrigin(origin);
            if (host != null && !host.isBlank()) {
                Set<String> allowed = Arrays.stream(allowedHostsConfig.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
                return allowed.contains(host.toLowerCase()) ? 0 : 1;
            }
        }
        // Fallback to client-reported flag when origin is missing
        return Boolean.TRUE.equals(request.getPageOriginNotFromOrg()) ? 1 : 0;
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

    private static int getIframeCount(SignalRequest request, java.util.function.ToIntFunction<IframeSignals> getter) {
        IframeSignals s = request.getIframeSignals();
        return s == null ? 0 : Math.max(0, getter.applyAsInt(s));
    }

    private static int booleanToInt(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private static int isRapidClicking(Double clickIntervalAvg) {
        if (clickIntervalAvg == null) return 0;
        return clickIntervalAvg < RAPID_CLICK_THRESHOLD_MS ? 1 : 0;
    }
}
