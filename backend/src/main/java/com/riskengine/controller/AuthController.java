package com.riskengine.controller;

import com.riskengine.dto.LoginRequest;
import com.riskengine.dto.LoginResponse;
import com.riskengine.service.ReferrerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final ReferrerService referrerService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String sessionId = UUID.randomUUID().toString();
        String userId = request.getUsername() != null ? request.getUsername() : "user-" + UUID.randomUUID().toString();
        String referrerUrl = request.getReferrerUrl();
        if (referrerUrl == null || referrerUrl.isBlank()) {
            String headerReferrer = httpRequest.getHeader("Referer");
            if (headerReferrer != null && !headerReferrer.isBlank()) referrerUrl = headerReferrer;
        }
        boolean suspiciousReferrer = referrerService.isSuspicious(referrerUrl);

        LoginResponse response = LoginResponse.builder()
                .sessionId(sessionId)
                .userId(userId)
                .suspiciousReferrer(suspiciousReferrer)
                .build();

        log.atInfo().addKeyValue("event", "mock_login")
                .addKeyValue("sessionId", sessionId)
                .addKeyValue("userId", userId)
                .addKeyValue("referrerUrl", referrerUrl != null ? referrerUrl : "")
                .addKeyValue("suspiciousReferrer", suspiciousReferrer)
                .log("Mock login successful");

        if (suspiciousReferrer) {
            log.atWarn().addKeyValue("event", "suspicious_referrer_at_login")
                    .addKeyValue("referrerUrl", referrerUrl)
                    .addKeyValue("sessionId", sessionId)
                    .log("Suspicious referrer on login – consider step-up, alerting, or manual research");
        }

        return ResponseEntity.ok(response);
    }
}
