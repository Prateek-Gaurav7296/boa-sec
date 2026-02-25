package com.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalRequest {

    private String sessionId;
    private String userId;
    private Boolean webdriverFlag;
    /** Current page origin (e.g. http://localhost:8080). Used to flag if page URL is not from org. */
    private String pageOrigin;
    /** True if page hostname is not in the allowed org host list. */
    private Boolean pageOriginNotFromOrg;
    /** Referrer URL (document.referrer). Flagged when not from org for phishing/malware redirect detection. */
    private String referrerUrl;
    /** Contains total, suspicious, hidden, offscreen, crossOrigin, notFromOrg. */
    private IframeSignals iframeSignals;
    private Boolean fetchOverridden;
    private String userAgent;
    private Integer screenWidth;
    private Integer screenHeight;
    private String timezone;
    private Double clickIntervalAvg;
    /** Function.prototype.toString was tampered (devtools/automation). */
    private Boolean functionTampered;
    /** Main window vs iframe have different UA/platform (sandbox/automation). */
    private Boolean iframeMismatch;
    /** localStorage/sessionStorage/cookies not working (incognito/headless). */
    private Boolean storageBlocked;
    /** CSP blocks inline scripts (security-positive but indicates enforced CSP). */
    private Boolean cspRestricted;
    /** plugins.length from automation detection. */
    private Integer pluginsLength;
    /** mimeTypes.length from automation detection. */
    private Integer mimeTypesLength;
    /** 'chrome' in window (browser API presence). */
    private Boolean hasChrome;
    /** __webdriver_script_fn in document (Selenium/Puppeteer). */
    private Boolean hasWebdriverScriptFn;
}
