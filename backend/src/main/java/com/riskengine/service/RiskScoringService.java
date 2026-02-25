package com.riskengine.service;

import com.riskengine.dto.NormalizedSignals;
import org.springframework.stereotype.Service;

/**
 * Rule-based risk scoring. Includes weights for webdriver, automation signals, page origin,
 * referrer, iframe indicators, and new malicious-page signals from risk-agent.js.
 * Total cap 100.
 *
 * Future: replace rule-based scoring with ML inference service.
 */
@Service
public class RiskScoringService {

    private static final int WEIGHT_WEBDRIVER = 30;
    private static final int WEIGHT_WEBDRIVER_SCRIPT_FN = 35;
    private static final int WEIGHT_FETCH_OVERRIDDEN = 40;
    private static final int WEIGHT_PAGE_NOT_FROM_ORG = 35;
    private static final int WEIGHT_REFERRER_NOT_FROM_ORG = 30;
    private static final int WEIGHT_IFRAME_HIDDEN = 10;
    private static final int WEIGHT_IFRAME_OFFSCREEN = 15;
    private static final int WEIGHT_IFRAME_CROSS_ORIGIN = 20;
    private static final int WEIGHT_IFRAME_NOT_FROM_ORG = 15;
    private static final int IFRAME_CONTRIBUTION_CAP = 50;
    private static final int WEIGHT_RAPID_CLICKING = 20;
    private static final int WEIGHT_FUNCTION_TAMPERED = 25;
    private static final int WEIGHT_IFRAME_MISMATCH = 25;
    private static final int WEIGHT_HEADLESS_BROWSER = 30;
    private static final int WEIGHT_STORAGE_BLOCKED = 15;
    private static final int MAX_SCORE = 100;

    public int score(NormalizedSignals normalized) {
        int score = 0;
        score += normalized.getWebdriverFlag() * WEIGHT_WEBDRIVER;
        score += normalized.getWebdriverScriptFn() * WEIGHT_WEBDRIVER_SCRIPT_FN;
        score += normalized.getFetchOverridden() * WEIGHT_FETCH_OVERRIDDEN;
        score += normalized.getPageOriginNotFromOrg() * WEIGHT_PAGE_NOT_FROM_ORG;
        score += normalized.getReferrerNotFromOrg() * WEIGHT_REFERRER_NOT_FROM_ORG;
        score += normalized.getFunctionTampered() * WEIGHT_FUNCTION_TAMPERED;
        score += normalized.getIframeMismatch() * WEIGHT_IFRAME_MISMATCH;
        score += normalized.getHeadlessBrowser() * WEIGHT_HEADLESS_BROWSER;
        score += normalized.getStorageBlocked() * WEIGHT_STORAGE_BLOCKED;

        int iframeScore = normalized.getIframeHidden() * WEIGHT_IFRAME_HIDDEN
                + normalized.getIframeOffscreen() * WEIGHT_IFRAME_OFFSCREEN
                + normalized.getIframeCrossOrigin() * WEIGHT_IFRAME_CROSS_ORIGIN
                + normalized.getIframeNotFromOrg() * WEIGHT_IFRAME_NOT_FROM_ORG;
        score += Math.min(iframeScore, IFRAME_CONTRIBUTION_CAP);

        score += normalized.getRapidClicking() * WEIGHT_RAPID_CLICKING;
        return Math.min(score, MAX_SCORE);
    }
}
