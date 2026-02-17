package com.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String sessionId;
    private String userId;
    /** True when referrer URL was not from allowed org (triggers step-up/alert/research). */
    private Boolean suspiciousReferrer;
}
