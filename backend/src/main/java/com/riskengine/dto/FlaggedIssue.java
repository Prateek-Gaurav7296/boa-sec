package com.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single flagged issue detected from client signals (malicious page, automation, etc.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaggedIssue {
    /** Unique issue code for programmatic handling. */
    private String code;
    /** Human-readable description. */
    private String description;
    /** Severity: LOW, MEDIUM, HIGH, CRITICAL. */
    private String severity;
}
