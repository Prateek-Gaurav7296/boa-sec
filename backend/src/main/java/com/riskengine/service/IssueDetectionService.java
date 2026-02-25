package com.riskengine.service;

import com.riskengine.dto.FlaggedIssue;
import com.riskengine.dto.NormalizedSignals;
import com.riskengine.dto.SignalRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects and flags malicious page / automation / bot signals based on risk-agent.js collected data.
 */
@Service
@Slf4j
public class IssueDetectionService {

    private static final String SEVERITY_CRITICAL = "CRITICAL";
    private static final String SEVERITY_HIGH = "HIGH";
    private static final String SEVERITY_MEDIUM = "MEDIUM";
    private static final String SEVERITY_LOW = "LOW";

    /**
     * Analyzes normalized signals and returns all flagged issues for display/auditing.
     */
    public List<FlaggedIssue> detectIssues(NormalizedSignals normalized, SignalRequest request) {
        List<FlaggedIssue> issues = new ArrayList<>();

        if (normalized.getWebdriverFlag() == 1) {
            issues.add(FlaggedIssue.builder()
                    .code("WEBDRIVER")
                    .description("navigator.webdriver is true – automation/bot environment (Selenium, Puppeteer)")
                    .severity(SEVERITY_CRITICAL)
                    .build());
        }

        if (normalized.getWebdriverScriptFn() == 1) {
            issues.add(FlaggedIssue.builder()
                    .code("WEBDRIVER_SCRIPT_FN")
                    .description("__webdriver_script_fn detected in document – Selenium/Puppeteer automation")
                    .severity(SEVERITY_CRITICAL)
                    .build());
        }

        if (normalized.getFunctionTampered() == 1) {
            issues.add(FlaggedIssue.builder()
                    .code("FUNCTION_TAMPERED")
                    .description("Function.prototype.toString tampered – devtools or script injection")
                    .severity(SEVERITY_HIGH)
                    .build());
        }

        if (normalized.getIframeMismatch() == 1) {
            issues.add(FlaggedIssue.builder()
                    .code("IFRAME_MISMATCH")
                    .description("Main window vs iframe have different userAgent/platform – sandbox or automation")
                    .severity(SEVERITY_HIGH)
                    .build());
        }

        if (normalized.getHeadlessBrowser() == 1) {
            issues.add(FlaggedIssue.builder()
                    .code("HEADLESS_BROWSER")
                    .description("plugins.length=0 and mimeTypes.length=0 – headless Chrome or automation")
                    .severity(SEVERITY_HIGH)
                    .build());
        }

        if (normalized.getStorageBlocked() == 1) {
            issues.add(FlaggedIssue.builder()
                    .code("STORAGE_BLOCKED")
                    .description("localStorage/sessionStorage/cookies not working – incognito or restricted environment")
                    .severity(SEVERITY_MEDIUM)
                    .build());
        }

        if (normalized.getPageOriginNotFromOrg() == 1) {
            issues.add(FlaggedIssue.builder()
                    .code("PAGE_ORIGIN_NOT_FROM_ORG")
                    .description("Page origin host not in allowed org list – possible phishing or external page")
                    .severity(SEVERITY_HIGH)
                    .build());
        }

        if (normalized.getReferrerNotFromOrg() == 1) {
            issues.add(FlaggedIssue.builder()
                    .code("SUSPICIOUS_REFERRER")
                    .description("Referrer URL not from allowed org – possible phishing or malware redirect")
                    .severity(SEVERITY_HIGH)
                    .build());
        }

        if (normalized.getFetchOverridden() == 1) {
            issues.add(FlaggedIssue.builder()
                    .code("FETCH_OVERRIDDEN")
                    .description("fetch API overridden – possible request interception or tampering")
                    .severity(SEVERITY_HIGH)
                    .build());
        }

        if (normalized.getRapidClicking() == 1) {
            issues.add(FlaggedIssue.builder()
                    .code("RAPID_CLICKING")
                    .description("Abnormally rapid click intervals – possible automation")
                    .severity(SEVERITY_MEDIUM)
                    .build());
        }

        if (normalized.getIframeHidden() > 0 || normalized.getIframeOffscreen() > 0) {
            issues.add(FlaggedIssue.builder()
                    .code("SUSPICIOUS_IFRAMES")
                    .description(String.format("Hidden/offscreen iframes detected (hidden=%d, offscreen=%d)", 
                            normalized.getIframeHidden(), normalized.getIframeOffscreen()))
                    .severity(SEVERITY_MEDIUM)
                    .build());
        }

        if (normalized.getIframeCrossOrigin() > 0 || normalized.getIframeNotFromOrg() > 0) {
            issues.add(FlaggedIssue.builder()
                    .code("CROSS_ORIGIN_IFRAMES")
                    .description(String.format("Cross-origin or non-org iframes (crossOrigin=%d, notFromOrg=%d)", 
                            normalized.getIframeCrossOrigin(), normalized.getIframeNotFromOrg()))
                    .severity(SEVERITY_MEDIUM)
                    .build());
        }

        if (Boolean.TRUE.equals(request.getCspRestricted())) {
            issues.add(FlaggedIssue.builder()
                    .code("CSP_RESTRICTED")
                    .description("Content-Security-Policy restricts inline scripts – enforced CSP (informational)")
                    .severity(SEVERITY_LOW)
                    .build());
        }

        return issues;
    }
}
