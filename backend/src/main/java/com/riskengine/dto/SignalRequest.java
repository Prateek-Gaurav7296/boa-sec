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
}
