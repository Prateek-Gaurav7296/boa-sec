package com.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskResponse {

    private int riskScore;
    private String decision;
    private String deviceSignature;
    private String sessionId;
}
