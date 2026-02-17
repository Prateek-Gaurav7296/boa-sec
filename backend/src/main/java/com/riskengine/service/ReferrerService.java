package com.riskengine.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Checks if a referrer URL is suspicious (e.g. user redirected from phishing/malware).
 * Used at login and during risk evaluation. Referrer host must be in allowed org list
 * or referrer empty (direct access); otherwise flagged for step-up/alerting/research.
 */
@Service
public class ReferrerService {

    @Value("${risk.engine.allowed-hosts:localhost,127.0.0.1}")
    private String allowedHostsConfig;

    /**
     * True if the referrer URL is non-empty and its host is not in the allowed org list.
     * Empty/blank referrer is not flagged (direct access or referrer policy stripped it).
     */
    public boolean isSuspicious(String referrerUrl) {
        if (referrerUrl == null || referrerUrl.isBlank()) return false;
        String host = extractHostFromUrl(referrerUrl.trim());
        if (host == null || host.isEmpty()) return false;
        Set<String> allowed = Arrays.stream(allowedHostsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        return !allowed.contains(host.toLowerCase());
    }

    /** Extract hostname from a full URL (e.g. https://evil.com/path → evil.com). */
    public static String extractHostFromUrl(String url) {
        if (url == null) return null;
        int i = url.indexOf("://");
        if (i >= 0) url = url.substring(i + 3);
        int j = url.indexOf(':');
        if (j >= 0) url = url.substring(0, j);
        int k = url.indexOf('/');
        if (k >= 0) url = url.substring(0, k);
        int q = url.indexOf('?');
        if (q >= 0) url = url.substring(0, q);
        return url.isEmpty() ? null : url;
    }
}
