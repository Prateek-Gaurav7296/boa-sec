package com.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal DTO for rule-based (and future ML) scoring.
 * Normalized values: booleans as 0/1, iframe counts, rapid-click flag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedSignals {

    private int webdriverFlag;      // 0 or 1
    private int fetchOverridden;   // 0 or 1
    private int iframeHidden;      // count for scoring
    private int iframeOffscreen;   // count for scoring
    private int iframeCrossOrigin; // count for scoring
    private int iframeNotFromOrg;  // count of iframes whose URL host is not in org list
    private int pageOriginNotFromOrg; // 0 or 1: page URL host not in org list
    private int referrerNotFromOrg;   // 0 or 1: referrer URL host not in org list (phishing/malware redirect)
    private int rapidClicking;     // 0 or 1
    private int functionTampered;  // 0 or 1: Function.prototype tampering (devtools/automation)
    private int iframeMismatch;    // 0 or 1: main vs iframe UA/platform mismatch
    private int storageBlocked;    // 0 or 1: storage disabled (incognito/headless)
    private int headlessBrowser;   // 0 or 1: pluginsLength=0 and mimeTypesLength=0
    private int webdriverScriptFn; // 0 or 1: __webdriver_script_fn in document
}
