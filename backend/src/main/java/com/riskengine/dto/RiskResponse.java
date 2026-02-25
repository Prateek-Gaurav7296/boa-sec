package com.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskResponse {

    private int riskScore;
    private String decision;
    private String deviceSignature;
    private String sessionId;
    /** Iframe counts from the page (for UI to show and alert on suspicious activity). */
    private IframeSignals iframeSignals;
    /** Current page origin; present when flagged as not from org. */
    private String pageOrigin;
    /** True when page URL host is not in the allowed org host list. */
    private Boolean pageOriginNotFromOrg;
    /** Referrer URL (for logging/alerting and Splunk post-fact analysis). */
    private String referrerUrl;
    /** True when referrer URL was not from allowed org (phishing/malware redirect). */
    private Boolean suspiciousReferrer;
    /** List of flagged issues (malicious page, automation, etc.) detected from signals. */
    private List<FlaggedIssue> flaggedIssues;
}
