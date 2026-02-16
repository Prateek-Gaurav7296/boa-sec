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
    /** Replaces legacy hiddenIframeCount; contains total, suspicious, hidden, offscreen, crossOrigin. */
    private IframeSignals iframeSignals;
    private Boolean fetchOverridden;
    private String userAgent;
    private Integer screenWidth;
    private Integer screenHeight;
    private String timezone;
    private Double clickIntervalAvg;
}
