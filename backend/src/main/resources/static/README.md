# Frontend (static) – Spring Boot & IntelliJ

Place your frontend files here so Spring Boot serves them at the same origin as the API.

> **Maintenance:** Update this README whenever you change risk-agent.js, backend risk logic, or API contracts.

---

## Flow diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              USER BROWSER (dashboard / demo-malicious / etc.)            │
└─────────────────────────────────────────────────────────────────────────────────────────┘
    │
    │  1. Page loads
    ▼
┌─────────────────────┐     ┌─────────────────────┐
│ risk-agent-config.js│────▶│   window.RiskAgent   │
│ (allowed hosts)     │     │   OrgHosts set       │
└─────────────────────┘     └─────────────────────┘
    │
    │  2. risk-agent.js init (or manual Evaluate Risk click)
    ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         risk-agent.js: collectAllSignals()                               │
├──────────────┬──────────────┬──────────────┬────────────────────────────────────────────┤
│  Stage 1     │  Stage 2     │  Stage 3     │  Stage 4                                    │
│  Fast        │  Fingerprints│  Security    │  Iframe scan                                │
│  userAgent,  │  canvasHash, │  automation, │  hidden, offscreen,                         │
│  referrer,   │  webglHash,  │  functionTam-│  crossOrigin, notFromOrg                    │
│  origin,     │  audioHash,  │  pered,      │                                              │
│  webdriver   │  fontsHash   │  iframeMismatch,│                                            │
│              │              │  storageWorks │                                              │
└──────┬───────┴──────┬───────┴──────┬───────┴──────────────┬──────────────────────────────┘
       │              │              │                      │
       └──────────────┴──────────────┴──────────────────────┘
                                      │
                                      │  3. buildPayload(sessionId, userId, signals)
                                      ▼
                              ┌───────────────┐
                              │  JSON payload │
                              │  POST /risk/  │
                              │  collect      │
                              └───────┬───────┘
                                      │
══════════════════════════════════════╪═══════════════════════════════════════════════════
                                      │              BACKEND
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  RiskCollectMapper  ──▶  SignalRequest  (parse stage1, stage2, stage3, iframeSignals)    │
└─────────────────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  SignalNormalizationService  ──▶  NormalizedSignals  (booleans→0/1, thresholds)          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
    │
    ├──────────────────────────────────┐
    ▼                                  ▼
┌─────────────────────┐      ┌─────────────────────────────────────────────────────────┐
│  RiskScoringService │      │  IssueDetectionService                                    │
│  score(0–100)       │      │  detectIssues() → FlaggedIssue[] (code, description,     │
│  weighted rules     │      │  severity) for WEBDRIVER, SUSPICIOUS_IFRAMES, etc.       │
└─────────┬───────────┘      └─────────────────────────────────────────────────────────┘
          │                                         │
          ▼                                         │
┌─────────────────────┐                             │
│  DecisionService    │                             │
│  decide(score):     │                             │
│  <30  ALLOW         │                             │
│  30–69 MFA          │                             │
│  ≥70  TERMINATE     │                             │
└─────────┬───────────┘                             │
          │                                         │
          ▼                                         ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  Persist: raw_signals (signal_json)  │  risk_decisions (score, decision, flagged_issues) │
└─────────────────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  RiskResponse  { riskScore, decision, deviceSignature, flaggedIssues, suspiciousReferrer }│
└─────────────────────────────────────────────────────────────────────────────────────────┘
    │
    │  4. Response to browser
    ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│  UI displays: score, decision, flagged issues, security alerts                           │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Required files

- `/config/risk-agent-config.js` – served by backend from `risk.engine.allowed-hosts`; sets `window.RiskAgentOrgHosts`. Include **before** `risk-agent.js` on any page that evaluates risk (e.g. dashboard, demo-malicious).
- `login.html` – login form, POST to `/login`, then redirect to `dashboard.html`
- `dashboard.html` – “Evaluate Risk” button, POST to `/risk/collect`, show result and risk alerts
- `demo-malicious.html` – demo page with intentional hidden/off-screen/cross-origin iframes; use to test risk evaluation flow
- `risk-agent.js` – browser fingerprint and security signal collector; 4-stage collection; auto-sends on load; exposes `window.RiskAgent.captureAndBuildPayload(sessionId, userId)` for explicit evaluation

---

## risk-agent.js: Signal collection and compromise detection

The risk-agent collects browser and environment signals in **4 stages**, then sends them to `POST /risk/collect`. The backend uses these signals to detect compromised sessions, automation, phishing, and malicious pages.

### Stage 1 – Fast signals (`stage1`)

| Signal | Source | Purpose |
|--------|--------|---------|
| `userAgent` | `navigator.userAgent` | Browser identification; used for device fingerprinting |
| `platform` | `navigator.platform` | OS/platform; checked against iframe for mismatch |
| `language`, `languages` | `navigator` | Locale; part of device profile |
| `webdriver` | `navigator.webdriver` | **Critical** – `true` in Selenium/Puppeteer; indicates automation |
| `hardwareConcurrency`, `deviceMemory` | `navigator` | Hardware hints; part of fingerprint |
| `cookieEnabled`, `doNotTrack` | `navigator` | Privacy/restriction indicators |
| `timezone` | `Intl.DateTimeFormat().resolvedOptions().timeZone` | Timezone; part of fingerprint |
| `screen` | `screen.width`, `height`, `colorDepth`, `devicePixelRatio` | Display; part of fingerprint |
| `referrer` | `document.referrer` | Where the user came from; flags phishing redirects |
| `origin` | `location.origin` | Current page origin; flags external/phishing pages |

### Stage 2 – Fingerprint hashes (`stage2`)

All hashes are SHA-256:

| Signal | Source | Purpose |
|--------|--------|---------|
| `canvasHash` | Canvas 2D draw → `toDataURL()` | Canvas fingerprint; device stability |
| `webglHash` | WebGL vendor, renderer, extensions | GPU fingerprint |
| `audioHash` | OfflineAudioContext oscillator output | Audio stack fingerprint |
| `fontsHash` | Detected fonts via canvas `measureText()` | Installed fonts; device profile |

*These hashes support device recognition and session linking over time.*

### Stage 3 – Security / automation signals (`stage3`)

| Signal | Logic | Compromise relevance |
|--------|-------|----------------------|
| `automation.webdriver` | `navigator.webdriver === true` | **CRITICAL** – automation/bot (Selenium, Puppeteer) |
| `automation.pluginsLength` | `navigator.plugins.length` | 0 in headless Chrome → automation |
| `automation.mimeTypesLength` | `navigator.mimeTypes.length` | 0 in headless Chrome → automation |
| `automation.hasChrome` | `"chrome" in window` | Browser API presence; mismatch can indicate automation |
| `automation.hasWebdriverScriptFn` | `"__webdriver_script_fn" in document` | **CRITICAL** – Selenium/Puppeteer injection |
| `functionTampered` | `Function.prototype.toString.toString()` missing `[native code]` | DevTools/script injection or tampering |
| `iframeMismatch` | Main window vs. blank iframe: different `userAgent`/`platform` | Sandbox or automation environment |
| `storageWorks` | Tests `localStorage`, `sessionStorage`, `document.cookie` | `false` → incognito, headless, or restricted |
| `cspRestricted` | Inline script blocked by CSP | Informational – CSP present |

### Stage 4 – Iframe scan (`iframeSignals`)

Scans all `<iframe>` elements in the DOM:

| Signal | Logic | Compromise relevance |
|--------|-------|----------------------|
| `total` | Count of iframes | Baseline |
| `hidden` | `display:none`, `visibility:hidden`, opacity 0, or zero size | Clickjacking / invisible overlays |
| `offscreen` | Outside viewport (e.g. `left: -9999px`) | Concealed injection |
| `crossOrigin` | iframe `src` origin ≠ page origin | Cross-origin embedding; potential data exfil |
| `notFromOrg` | iframe host not in `RiskAgentOrgHosts` | Untrusted third-party content |
| `suspicious` | Count of iframes with any of the above | Aggregate suspicious iframe count |

### How signals drive compromise detection

The backend:

1. **Normalizes** signals (booleans → 0/1, thresholds).
2. **Scores** risk (0–100) using weighted rules.
3. **Decides** action: `ALLOW` (&lt;30), `MFA` (30–69), `TERMINATE` (≥70).
4. **Flags issues** – each signal maps to a `FlaggedIssue` (code, description, severity).

**Signal → risk weight (examples):**

| Signal | Weight | Rationale |
|--------|--------|-----------|
| `webdriver` | 30 | Automation/bot |
| `webdriverScriptFn` | 35 | Selenium/Puppeteer |
| `headlessBrowser` (plugins=0, mimeTypes=0) | 30 | Headless automation |
| `functionTampered` | 25 | Script tampering |
| `iframeMismatch` | 25 | Sandbox/automation |
| `pageOriginNotFromOrg` | 35 | Phishing / external page |
| `referrerNotFromOrg` | 30 | Phishing redirect |
| `iframeHidden`, `offscreen`, `crossOrigin`, `notFromOrg` | 10–20 each (cap 50) | Clickjacking, injection |
| `storageBlocked` | 15 | Incognito / restricted |

**Example compromise scenarios:**

| Scenario | Detected by | Typical action |
|----------|-------------|----------------|
| Selenium/Puppeteer automation | `webdriver`, `webdriverScriptFn`, `headlessBrowser` | TERMINATE |
| Script injection or DevTools tampering | `functionTampered` | MFA / TERMINATE |
| Phishing page (user on external site) | `pageOriginNotFromOrg`, `referrerNotFromOrg` | MFA / TERMINATE |
| Clickjacking / hidden iframes | `iframeSignals.hidden`, `offscreen` | MFA |
| Cross-origin data exfil risk | `iframeSignals.crossOrigin`, `notFromOrg` | MFA |
| Sandbox or virtualized environment | `iframeMismatch`, `storageBlocked` | MFA |

The response includes `flaggedIssues` (array of `{ code, description, severity }`) so the UI can show exactly which signals caused the risk.

**See [risk-agent-examples.md](./risk-agent-examples.md)** for 15 detailed scenarios (phishing, clickjacking, automation, incognito, etc.) with example signals and outcomes.

---

## API compatibility

When served by Spring Boot (same origin), use **relative URLs** so no CORS is needed:

```javascript
const API_BASE = '';   // same origin
fetch(API_BASE + '/login', { ... });
fetch(API_BASE + '/risk/collect', { ... });
```

- **POST /login** – body: `{ "username", "password", "referrerUrl" }` (referrerUrl optional; backend also uses HTTP `Referer` header if body is empty) → returns `{ "sessionId", "userId", "suspiciousReferrer" }`
- **POST /risk/collect** – body: `RiskCollectRequest` (timestamp, sessionId, userId, stage1, stage2, stage3, iframeSignals); returns `RiskResponse` including `riskScore`, `decision`, `deviceSignature`, `referrerUrl`, `suspiciousReferrer`, `flaggedIssues`, etc.

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

---

## Documentation maintenance

**Keep this README in sync when you change:**

| Change | Update in README |
|--------|------------------|
| New signals in risk-agent.js | Stage tables, flow diagram |
| New backend risk rules / weights | "How signals drive compromise detection", flow diagram |
| New API fields (request/response) | API compatibility section |
| New flagged issue codes | Example compromise scenarios (see risk-agent-examples.md) |
| New config (e.g. risk-agent-config.js) | Required files, flow diagram |
| New scenario or risk outcome | risk-agent-examples.md |
