# Frontend (static) – Spring Boot & IntelliJ

Place your frontend files here so Spring Boot serves them at the same origin as the API.

## Required files

- `login.html` – login form, POST to `/login`, then redirect to `dashboard.html`
- `dashboard.html` – “Evaluate Risk” button, POST to `/risk/evaluate`, show result
- `risk-agent.js` – signal capture; expose `window.RiskAgent.captureAndBuildPayload(sessionId, userId)`

## API compatibility

When served by Spring Boot (same origin), use **relative URLs** so no CORS is needed:

```javascript
const API_BASE = '';   // same origin
fetch(API_BASE + '/login', { ... });
fetch(API_BASE + '/risk/evaluate', { ... });
```

- **POST /login** – body: `{ "username", "password" }` → returns `{ "sessionId", "userId" }`
- **POST /risk/evaluate** – body: `SignalRequest` (sessionId, userId, webdriverFlag, hiddenIframeCount, fetchOverridden, userAgent, screenWidth, screenHeight, timezone, clickIntervalAvg) → returns `{ "riskScore", "decision", "deviceSignature", "sessionId" }`

## Run in IntelliJ

1. Open the **backend** folder (the one containing `pom.xml`) as the project in IntelliJ.
2. Run **RiskEngineApplication** (right‑click → Run, or use the green Run next to `main`).
3. Open in browser: **http://localhost:8080/** or **http://localhost:8080/login.html**
