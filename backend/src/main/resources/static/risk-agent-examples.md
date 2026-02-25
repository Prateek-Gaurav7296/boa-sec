# risk-agent.js: Example Scenarios

This document describes how risk-agent.js behaves in different scenarios: what it captures, how the backend scores risk, and what action is taken.

---

## 1. Legitimate user on org domain (baseline)

**Scenario:** User opens `http://localhost:8080/dashboard.html` from the org domain. Normal browser, no automation, no iframes.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `stage1.origin` | `http://localhost:8080` |
| `stage1.referrer` | `http://localhost:8080/login.html` or empty |
| `stage1.webdriver` | `false` |
| `stage3.automation.webdriver` | `false` |
| `stage3.automation.pluginsLength` | > 0 |
| `stage3.functionTampered` | `false` |
| `stage3.iframeMismatch` | `false` |
| `stage3.storageWorks` | `true` |
| `iframeSignals` | `{ total: 0, suspicious: 0 }` |

**Risk score:** 0  
**Decision:** ALLOW  
**Action:** Normal flow, no prompts.

---

## 2. Phishing page (user on external clone)

**Scenario:** Attacker hosts a clone of your login at `https://evil-phishing.com`. User clicks link in email and lands there. risk-agent.js is embedded in the clone.

**Signals captured:**
| Signal | Value | Why it matters |
|--------|-------|----------------|
| `stage1.origin` | `https://evil-phishing.com` | Page is not from org |
| `stage1.referrer` | `https://mail.google.com` or empty | May be external |
| `pageOriginNotFromOrg` | `true` | Backend flags |
| `referrerNotFromOrg` | may be `true` | If referrer host not in allowed list |

**Risk score:** ~35–65 (page + referrer weights)  
**Decision:** MFA or TERMINATE  
**Action:** Block auto-login, show warning ("You may have reached this from an untrusted site"), require step-up auth.

---

## 3. Phishing redirect (suspicious referrer)

**Scenario:** User is on `http://localhost:8080/login.html` but arrived via a link from `https://suspicious-ads.com` (e.g. malvertising). Page origin is org, but referrer is not.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `stage1.origin` | `http://localhost:8080` |
| `stage1.referrer` | `https://suspicious-ads.com/redirect` |
| `referrerNotFromOrg` | `true` |

**Risk score:** ~30  
**Decision:** MFA  
**Action:** Show referrer alert, require MFA before proceeding.

---

## 4. Clickjacking / hidden iframes (demo-malicious)

**Scenario:** Page has hidden and offscreen iframes (e.g. invisible overlay for clickjacking or concealed injection).

**Signals captured:**
```json
"iframeSignals": {
  "total": 7,
  "hidden": 4,
  "offscreen": 2,
  "crossOrigin": 1,
  "notFromOrg": 0,
  "suspicious": 7
}
```

**Risk score:** ~50–60 (iframe contribution capped at 50)  
**Decision:** MFA  
**Action:** Treat as high risk; require step-up or block sensitive actions.

---

## 5. Cross-origin iframe (potential data exfil)

**Scenario:** Legit org page embeds an iframe from `https://untrusted-third-party.com`. Host is not in allowed list.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `iframeSignals.crossOrigin` | 1 |
| `iframeSignals.notFromOrg` | 1 |
| `iframeSignals.suspicious` | 1 |

**Risk score:** ~35 (iframe weights)  
**Decision:** MFA  
**Action:** Flag for review; possible data exfiltration or tracking.

---

## 6. Automation / bot (Selenium, Puppeteer)

**Scenario:** Script automates login or form submission using Selenium or Puppeteer. risk-agent runs in the controlled browser.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `stage1.webdriver` | `true` |
| `stage3.automation.webdriver` | `true` |
| `stage3.automation.hasWebdriverScriptFn` | `true` |
| `stage3.automation.pluginsLength` | 0 |
| `stage3.automation.mimeTypesLength` | 0 |

**Risk score:** ~95  
**Decision:** TERMINATE  
**Action:** Block login/actions, terminate session, optionally log for abuse detection.

---

## 7. Headless Chrome (no plugins)

**Scenario:** Automation uses headless Chrome. No `webdriver` flag, but plugins and mimeTypes are empty.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `stage3.automation.pluginsLength` | 0 |
| `stage3.automation.mimeTypesLength` | 0 |
| `headlessBrowser` | `true` |

**Risk score:** ~30  
**Decision:** MFA  
**Action:** Treat as automation; require human verification.

---

## 8. Script injection / DevTools tampering

**Scenario:** Attacker or user overrides `Function.prototype.toString` (e.g. via DevTools or injected script) to hide automation or alter behavior.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `stage3.functionTampered` | `true` |

**Risk score:** ~25+ (plus any other flags)  
**Decision:** MFA or TERMINATE  
**Action:** Tampering suggests evasion; require step-up or block.

---

## 9. Incognito / private browsing

**Scenario:** User is in incognito or private browsing mode.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `stage3.storageWorks` | `true` in Chrome/Firefox incognito |

**Risk score:** 0  
**Decision:** ALLOW  

**Note:** In Chrome and Firefox incognito, localStorage, sessionStorage, and cookies *still work* — they are isolated per session and cleared when the window closes. So `storageWorks` remains `true` and risk score is 0.

`storageBlocked` (+15 risk) only triggers when storage is *actually disabled*, e.g.:
- Safari with "Block All Cookies" or strict tracking prevention
- Enterprise policies that disable storage
- Browser extensions that block storage
- Some headless/automation environments with stripped storage

---

## 10. Storage actually blocked (Safari strict, enterprise policy)

**Scenario:** Storage (localStorage, sessionStorage, or cookies) is disabled by browser settings, enterprise policy, or extension.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `stage3.storageWorks` | `false` |

**Risk score:** ~15  
**Decision:** ALLOW (unless combined with other flags)  
**Action:** May indicate restricted environment; combined with automation signals, can push to MFA.

---

## 11. Sandbox / virtualized environment (iframe mismatch)

**Scenario:** Page runs in a sandbox or virtualized setup where a blank iframe reports different `userAgent` or `platform` than the main window.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `stage3.iframeMismatch` | `true` |

**Risk score:** ~25  
**Decision:** MFA  
**Action:** Environment may be non-standard or automated; require step-up.

---

## 12. Fetch API overridden (request interception)

**Scenario:** Malicious script overrides `window.fetch` to intercept or modify requests. (Note: risk-agent does not currently detect this; this would be added via client-side fetch wrapper or similar.)

**Signals captured:**
| Signal | Value |
|--------|-------|
| `fetchOverridden` | `true` (if implemented) |

**Risk score:** ~40  
**Decision:** TERMINATE  
**Action:** Indicates request tampering; block and alert.

---

## 13. Combined high-risk (phishing + hidden iframes)

**Scenario:** Phishing page that also injects hidden iframes to capture clicks or overlay content.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `pageOriginNotFromOrg` | `true` |
| `iframeSignals.hidden` | 2 |
| `iframeSignals.offscreen` | 1 |
| `referrerNotFromOrg` | `true` |

**Risk score:** ~85+  
**Decision:** TERMINATE  
**Action:** Strong phishing indicators; block and warn user.

---

## 14. Legitimate external referrer (false positive mitigation)

**Scenario:** User clicks a link from a known-good external site (e.g. `https://blog.yourcompany.com`) to `https://app.yourcompany.com`. Referrer is external but trusted.

**Mitigation:** Add `blog.yourcompany.com` to `risk.engine.allowed-hosts`. Then `referrerNotFromOrg` stays `false`.

**Risk score:** 0  
**Decision:** ALLOW  

---

## 15. CSP enforced (informational)

**Scenario:** Page has strict Content-Security-Policy that blocks inline scripts. risk-agent’s inline script test fails.

**Signals captured:**
| Signal | Value |
|--------|-------|
| `stage3.cspRestricted` | `true` |

**Risk score:** No direct weight (informational)  
**Decision:** Unchanged  
**Action:** Logged as LOW severity; indicates CSP is active (generally positive for security).

---

## Summary table

| # | Scenario | Key signals | Typical score | Decision |
|---|----------|-------------|---------------|----------|
| 1 | Legitimate user | All clean | 0 | ALLOW |
| 2 | Phishing page | pageOriginNotFromOrg | 35–65 | MFA / TERMINATE |
| 3 | Suspicious referrer | referrerNotFromOrg | ~30 | MFA |
| 4 | Clickjacking | iframeSignals.hidden, offscreen | 50–60 | MFA |
| 5 | Cross-origin iframe | crossOrigin, notFromOrg | ~35 | MFA |
| 6 | Automation (Selenium/Puppeteer) | webdriver, webdriverScriptFn | ~95 | TERMINATE |
| 7 | Headless Chrome | pluginsLength=0, mimeTypesLength=0 | ~30 | MFA |
| 8 | Script tampering | functionTampered | 25+ | MFA / TERMINATE |
| 9 | Incognito (Chrome/Firefox) | storageWorks=true; no extra risk | 0 | ALLOW |
| 10 | Storage actually blocked | storageWorks=false | ~15 | ALLOW |
| 11 | Sandbox / iframe mismatch | iframeMismatch | ~25 | MFA |
| 12 | Fetch overridden | fetchOverridden | ~40 | TERMINATE |
| 13 | Phishing + hidden iframes | Combined | 85+ | TERMINATE |
| 14 | Trusted external referrer | Allowed host | 0 | ALLOW |
| 15 | CSP enforced | cspRestricted | 0 | ALLOW |

---

*Update this document when adding new scenarios or changing risk weights. See README "Documentation maintenance" for sync rules.*
