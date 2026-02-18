# Referrer Monitoring – Change Log

**Date:** 2026-02-16  
**Feature:** Monitor referrer URL on login and risk-evaluate to detect phishing/malware redirects. Flag suspicious referrers for step-up, alerting, and post-fact analysis (e.g. Splunk).

---

## Summary

When a user is redirected to the bank login from a malicious source (phishing/malware), the browser sends the source URL in the `Referer` header. We now:

- Check referrer on **login** and **risk evaluate**.
- Treat any referrer whose host is **not** in the allowed-org list as suspicious.
- Return `suspiciousReferrer` and surface it in the UI; log for Splunk/post-fact analysis.
- Use both request-body `referrerUrl` and HTTP `Referer` header (header used when body is empty).

---

## Backend Changes

### New

- **`ReferrerService`**  
  - `isSuspicious(referrerUrl)` – returns true when referrer is non-empty and its host is not in `risk.engine.allowed-hosts` (e.g. `localhost`, `127.0.0.1`).

### Modified

- **`AuthController`**
  - `POST /login` accepts optional `referrerUrl` in body.
  - If `referrerUrl` is null/blank, uses HTTP `Referer` header.
  - Calls `ReferrerService.isSuspicious(referrerUrl)` and includes `suspiciousReferrer` in `LoginResponse`.
  - Logs suspicious referrer at info level.

- **`RiskController`**
  - `POST /risk/evaluate`: if `SignalRequest.referrerUrl` is null/blank, sets it from HTTP `Referer` header.
  - Response already includes `referrerUrl` and `suspiciousReferrer` (from existing risk pipeline).

- **`SignalRequest`** (DTO)  
  - Already had `referrerUrl`; no schema change.

- **`SignalNormalizationService`**  
  - Uses `ReferrerService` to set `referrerNotFromOrg` in `NormalizedSignals` from `request.getReferrerUrl()`.

- **`RiskScoringService`**  
  - Already scores `referrerNotFromOrg` (e.g. +30 points).

- **`DecisionService`**  
  - Already passes referrer into response and persistence; logs suspicious referrer.

- **`RiskResponse`** (DTO)  
  - Already includes `referrerUrl` and `suspiciousReferrer`.

- **`LoginRequest` / `LoginResponse`**  
  - Already include `referrerUrl` and `suspiciousReferrer` (added in this feature).

### Configuration

- **`application.properties`**  
  - `risk.engine.allowed-hosts=localhost,127.0.0.1` – same list used for page origin, iframe “not from org,” and **referrer**. Any other referrer host is suspicious.

---

## Frontend Changes

- **`login.html`**
  - Sends `referrerUrl: document.referrer` in the login request body.
  - If response has `suspiciousReferrer: true`, shows a security notice and “Continue to dashboard” (no auto-redirect).

- **`risk-agent.js`**
  - Includes `referrerUrl: document.referrer` in the payload built for `POST /risk/evaluate`.

- **`dashboard.html`**
  - Shows referrer in the risk breakdown (referrer URL and “Suspicious referrer” alert when applicable).
  - Breakdown section is shown when referrer/referrer alert is present even if there are no iframe signals.

---

## API Contract

- **POST /login**  
  - Body: `{ "username", "password", "referrerUrl" }` (optional).  
  - Backend may use `Referer` header if `referrerUrl` is empty.  
  - Response: `{ "sessionId", "userId", "suspiciousReferrer" }`.

- **POST /risk/evaluate**  
  - Body: `SignalRequest` including `referrerUrl` (optional).  
  - Backend may use `Referer` header if `referrerUrl` is empty.  
  - Response: `RiskResponse` includes `referrerUrl`, `suspiciousReferrer`, and rest of risk breakdown.

---

## How to Test

1. **Allowed referrer (normal)**  
   Open `http://localhost:8080/` → login → Evaluate Risk. Referrer is same-origin or empty → `suspiciousReferrer: false`.

2. **Suspicious referrer via header (e.g. curl)**  
   ```bash
   curl -X POST http://localhost:8080/login \
     -H "Content-Type: application/json" \
     -H "Referer: https://evil-phishing.example.com/" \
     -d '{"username":"u","password":"p"}'
   ```  
   Expect `"suspiciousReferrer": true` in the response.

3. **Real cross-origin**  
   From a page on another origin (e.g. `https://example.com`), add a link to `http://localhost:8080/login.html` and click it. Browser sends that origin as referrer → app flags it and shows the login security notice.

4. **Dashboard**  
   After login with a suspicious referrer (or after risk evaluate with one), dashboard shows the referrer and “Suspicious referrer” in the breakdown.

---

## Splunk / Post-Fact Analysis

- Suspicious referrers are logged by the backend (login and risk-evaluate flows).
- Stored data (e.g. `RawSignal`, `RiskDecisionLog`) can include referrer where applicable; logs can be ingested into Splunk to run alerts or reports on suspicious referrers.

---

## Files Touched (Reference)

| Area   | File |
|--------|------|
| Backend | `controller/AuthController.java`, `controller/RiskController.java` |
| Backend | `service/ReferrerService.java`, `service/SignalNormalizationService.java` |
| Backend | `dto/LoginRequest.java`, `dto/LoginResponse.java`, `dto/SignalRequest.java`, `dto/RiskResponse.java`, `dto/NormalizedSignals.java` |
| Backend | `service/RiskScoringService.java`, `service/DecisionService.java` |
| Config | `application.properties` (allowed-hosts) |
| Static | `login.html`, `dashboard.html`, `risk-agent.js` |
| Docs  | `src/main/resources/static/README.md` |
