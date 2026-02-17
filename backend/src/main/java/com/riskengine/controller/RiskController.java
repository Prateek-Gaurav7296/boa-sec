package com.riskengine.controller;

import com.riskengine.dto.RiskResponse;
import com.riskengine.dto.SignalRequest;
import com.riskengine.service.DecisionService;
import com.riskengine.service.ReferrerService;
import com.riskengine.service.RiskScoringService;
import com.riskengine.service.SignalNormalizationService;
import com.riskengine.service.SignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/risk")
@RequiredArgsConstructor
@Slf4j
public class RiskController {

    private final SignalNormalizationService signalNormalizationService;
    private final SignatureService signatureService;
    private final RiskScoringService riskScoringService;
    private final DecisionService decisionService;
    private final ReferrerService referrerService;

    @PostMapping("/evaluate")
    public ResponseEntity<RiskResponse> evaluate(@RequestBody SignalRequest request, HttpServletRequest httpRequest) {
        if (request.getReferrerUrl() == null || request.getReferrerUrl().isBlank()) {
            String headerReferrer = httpRequest.getHeader("Referer");
            if (headerReferrer != null && !headerReferrer.isBlank()) {
                request.setReferrerUrl(headerReferrer);
            }
        }
        log.atInfo().addKeyValue("event", "incoming_signals")
                .addKeyValue("sessionId", request.getSessionId())
                .addKeyValue("userId", request.getUserId())
                .log("Incoming risk evaluation request");

        decisionService.persistRawSignals(request);
        var normalized = signalNormalizationService.normalize(request);
        String deviceSignature = signatureService.generate(request);
        int riskScore = riskScoringService.score(normalized);
        String decision = decisionService.decide(riskScore);
        decisionService.persistDecision(request.getSessionId(), request.getUserId(), riskScore, decision);

        log.atInfo().addKeyValue("event", "risk_evaluated")
                .addKeyValue("sessionId", request.getSessionId())
                .addKeyValue("riskScore", riskScore)
                .addKeyValue("decision", decision)
                .log("Risk evaluation completed");

        boolean suspiciousReferrer = referrerService.isSuspicious(request.getReferrerUrl());
        if (suspiciousReferrer) {
            log.atWarn().addKeyValue("event", "suspicious_referrer_at_risk_eval")
                    .addKeyValue("referrerUrl", request.getReferrerUrl())
                    .addKeyValue("sessionId", request.getSessionId())
                    .log("Suspicious referrer – consider step-up, alerting, or manual research");
        }

        RiskResponse response = RiskResponse.builder()
                .riskScore(riskScore)
                .decision(decision)
                .deviceSignature(deviceSignature)
                .sessionId(request.getSessionId())
                .iframeSignals(request.getIframeSignals())
                .pageOrigin(request.getPageOrigin())
                .pageOriginNotFromOrg(request.getPageOriginNotFromOrg())
                .referrerUrl(request.getReferrerUrl())
                .suspiciousReferrer(suspiciousReferrer)
                .build();

        return ResponseEntity.ok(response);
    }
}
