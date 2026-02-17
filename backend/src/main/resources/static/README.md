# Frontend (static) – Spring Boot & IntelliJ

Place your frontend files here so Spring Boot serves them at the same origin as the API.

## Required files

- `login.html` – login form, POST to `/login`, then redirect to `dashboard.html`
- `dashboard.html` – “Evaluate Risk” button, POST to `/risk/evaluate`, show result and iframe/risk alerts
- `demo-malicious.html` – demo page with intentional hidden/off-screen/cross-origin iframes; use to test iframe detection and security alerts
- `risk-agent.js` – signal capture; expose `window.RiskAgent.captureAndBuildPayload(sessionId, userId)` and `detectSuspiciousIframes()`

## API compatibility

When served by Spring Boot (same origin), use **relative URLs** so no CORS is needed:

```javascript
const API_BASE = '';   // same origin
fetch(API_BASE + '/login', { ... });
fetch(API_BASE + '/risk/evaluate', { ... });
```

- **POST /login** – body: `{ "username", "password", "referrerUrl" }` (referrerUrl optional; backend also uses HTTP `Referer` header if body is empty) → returns `{ "sessionId", "userId", "suspiciousReferrer" }`
- **POST /risk/evaluate** – body: `SignalRequest` (includes `referrerUrl`; backend falls back to `Referer` header if empty) → returns `RiskResponse` including `referrerUrl`, `suspiciousReferrer`, iframe breakdown, etc.

## Referrer monitoring (phishing/malware redirects)

If the user lands on login or risk-evaluate from a non-allowed host (e.g. phishing site), the referrer is flagged:

- **Login:** response includes `suspiciousReferrer: true`; login page shows a security notice and “Continue to dashboard” (no auto-redirect).
- **Risk evaluate:** `suspiciousReferrer` is included in the response and contributes to risk; dashboard shows a referrer alert.
- Backend uses `risk.engine.allowed-hosts` (e.g. `localhost`, `127.0.0.1`). Any other referrer host is treated as suspicious.
- Suspicious referrers are logged for Splunk/post-fact analysis.

**How to test referrer**

1. **Same-origin (allowed):** Open `http://localhost:8080/` → login → Evaluate Risk. Referrer is empty or same host → not suspicious.
2. **Simulate external referrer:** Use browser DevTools → Network, edit and resend a request and add header `Referer: https://evil-phishing.example.com/`, or use curl:
   - `curl -X POST http://localhost:8080/login -H "Content-Type: application/json" -H "Referer: https://evil.example.com/" -d '{"username":"u","password":"p"}'`
   - Expect `"suspiciousReferrer": true` in the response.
3. **Real cross-origin:** From another origin (e.g. a page on `https://example.com`) add a link to `http://localhost:8080/login.html` and click it; the browser will send that origin as referrer and the app will flag it (if that host is not in allowed-hosts).

## Run in IntelliJ

1. Open the **backend** folder (the one containing `pom.xml`) as the project in IntelliJ.
2. Run **RiskEngineApplication** (right‑click → Run, or use the green Run next to `main`).
3. Open in browser: **http://localhost:8080/** or **http://localhost:8080/login.html**
