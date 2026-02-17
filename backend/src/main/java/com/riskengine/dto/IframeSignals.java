package com.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Iframe detection signals from the client (risk-agent.js detectSuspiciousIframes).
 * Used for clickjacking / hidden injection / cross-origin risk scoring.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IframeSignals {

    private int total;
    private int suspicious;
    private int hidden;
    private int offscreen;
    private int crossOrigin;
    /** Count of iframes whose src host is not in the allowed org host list. */
    private int notFromOrg;
}
