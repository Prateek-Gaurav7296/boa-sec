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
    private int rapidClicking;     // 0 or 1
}
