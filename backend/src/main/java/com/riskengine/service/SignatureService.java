package com.riskengine.service;

import com.riskengine.dto.SignalRequest;
import com.riskengine.util.HashUtil;
import org.springframework.stereotype.Service;

/**
 * Generates a device signature from stable client attributes (userAgent, screen, timezone).
 */
@Service
public class SignatureService {

    public String generate(SignalRequest request) {
        String userAgent = nullToEmpty(request.getUserAgent());
        String screenW = request.getScreenWidth() != null ? String.valueOf(request.getScreenWidth()) : "";
        String screenH = request.getScreenHeight() != null ? String.valueOf(request.getScreenHeight()) : "";
        String timezone = nullToEmpty(request.getTimezone());

        String payload = userAgent + screenW + screenH + timezone;
        return HashUtil.sha256Hex(payload);
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
