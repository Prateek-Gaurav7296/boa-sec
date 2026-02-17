package com.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    private String username;
    private String password;
    /** Referrer URL (e.g. from document.referrer). Sent by client; used to detect phishing/malware redirects. */
    private String referrerUrl;
}
