package com.riskengine.service;

import com.riskengine.dto.SignalRequest;
import com.riskengine.entity.RawSignal;
import com.riskengine.entity.RiskDecisionLog;
import com.riskengine.repository.RawSignalRepository;
import com.riskengine.repository.RiskDecisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DecisionService {

    private final RawSignalRepository rawSignalRepository;
    private final RiskDecisionRepository riskDecisionRepository;

    public String decide(int riskScore) {
        if (riskScore < 30) return "ALLOW";
        if (riskScore < 70) return "MFA";
        return "TERMINATE";
    }

    public void persistRawSignals(SignalRequest request) {
        Map<String, Object> signalJson = toSignalMap(request);
        RawSignal entity = RawSignal.builder()
                .sessionId(request.getSessionId())
                .userId(request.getUserId())
                .timestamp(Instant.now())
                .signalJson(signalJson)
                .build();
        rawSignalRepository.save(entity);
    }

    public void persistDecision(String sessionId, String userId, int riskScore, String decision) {
        RiskDecisionLog log = RiskDecisionLog.builder()
                .sessionId(sessionId)
                .userId(userId)
                .riskScore(riskScore)
                .decision(decision)
                .createdAt(Instant.now())
                .build();
        riskDecisionRepository.save(log);
    }

    private static Map<String, Object> toSignalMap(SignalRequest r) {
        Map<String, Object> m = new HashMap<>();
        m.put("sessionId", r.getSessionId());
        m.put("userId", r.getUserId());
        m.put("webdriverFlag", r.getWebdriverFlag());
        m.put("pageOrigin", r.getPageOrigin());
        m.put("pageOriginNotFromOrg", r.getPageOriginNotFromOrg());
        m.put("referrerUrl", r.getReferrerUrl());
        if (r.getIframeSignals() != null) {
            Map<String, Object> iframe = new HashMap<>();
            iframe.put("total", r.getIframeSignals().getTotal());
            iframe.put("suspicious", r.getIframeSignals().getSuspicious());
            iframe.put("hidden", r.getIframeSignals().getHidden());
            iframe.put("offscreen", r.getIframeSignals().getOffscreen());
            iframe.put("crossOrigin", r.getIframeSignals().getCrossOrigin());
            iframe.put("notFromOrg", r.getIframeSignals().getNotFromOrg());
            m.put("iframeSignals", iframe);
        }
        m.put("fetchOverridden", r.getFetchOverridden());
        m.put("userAgent", r.getUserAgent());
        m.put("screenWidth", r.getScreenWidth());
        m.put("screenHeight", r.getScreenHeight());
        m.put("timezone", r.getTimezone());
        m.put("clickIntervalAvg", r.getClickIntervalAvg());
        return m;
    }
}
