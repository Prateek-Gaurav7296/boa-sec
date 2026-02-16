package com.riskengine.controller;

import com.riskengine.dto.LoginRequest;
import com.riskengine.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String sessionId = UUID.randomUUID().toString();
        String userId = request.getUsername() != null ? request.getUsername() : "user-" + UUID.randomUUID().toString();

        LoginResponse response = LoginResponse.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();

        log.atInfo().addKeyValue("event", "mock_login")
                .addKeyValue("sessionId", sessionId)
                .addKeyValue("userId", userId)
                .log("Mock login successful");

        return ResponseEntity.ok(response);
    }
}
