# Post-Login Runtime Risk Detection Engine – POC (Backend)

Backend for the runtime risk detection engine. Captures client signals, normalizes them, generates a device signature, applies rule-based risk scoring, and persists raw signals and decisions to PostgreSQL.

**Frontend:** Place your frontend files in `backend/src/main/resources/static/` (see that folder’s README). Spring Boot will serve them on the same origin as the API; use relative API paths (`API_BASE = ''`).

## Stack

- **Backend:** Java 21, Spring Boot 3.2
- **Database:** PostgreSQL
- No Docker, no device-specific or third-party fingerprint libraries. ML-ready design for future extension.

## Project Structure

```
risk-engine-poc/
├── backend/
│   ├── pom.xml
│   ├── mvnw
│   ├── .mvn/wrapper/
│   └── src/main/
│       ├── java/com/riskengine/
│       │   ├── RiskEngineApplication.java
│       │   ├── config/WebConfig.java
│       │   ├── controller/AuthController.java, RiskController.java
│       │   ├── dto/
│       │   ├── service/
│       │   ├── entity/
│       │   ├── repository/
│       │   └── util/HashUtil.java
│       └── resources/
│           ├── application.properties
│           ├── schema.sql
│           └── static/          ← frontend (login.html, dashboard.html, risk-agent.js)
└── README.md
```

## Database (PostgreSQL)

Create the database (tables are created by the app when using `spring.jpa.hibernate.ddl-auto=update`):

```sql
CREATE DATABASE risk_engine;
```

Reference schema (optional; app can create tables automatically):

```sql
CREATE TABLE raw_signals (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(100),
    user_id VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    signal_json JSONB
);

CREATE TABLE risk_decisions (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(100),
    user_id VARCHAR(100),
    risk_score INTEGER,
    decision VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

Configure in `backend/src/main/resources/application.properties`:

- `spring.datasource.url=jdbc:postgresql://localhost:5432/risk_engine`
- `spring.datasource.username` / `spring.datasource.password`

## API

### Endpoints

| Method | Path            | Description |
|--------|-----------------|-------------|
| POST   | `/login`        | Mock login; body `{ "username", "password" }`; returns `{ "sessionId", "userId" }`. |
| POST   | `/risk/collect` | Collect signals and evaluate risk; body `RiskCollectRequest` (3-stage fingerprint payload); returns `RiskResponse`. |
| POST   | `/risk/evaluate` | Legacy; body `SignalRequest`; returns `RiskResponse`. |

### RiskCollectRequest (POST /risk/collect)

- `timestamp`, `sessionId`, `userId`
- `stage1`: userAgent, platform, language, screen, referrer, origin, webdriver, etc.
- `stage2`: canvasHash, webglHash, audioHash, fontsHash (SHA-256)
- `stage3`: automation, functionTampered, iframeMismatch, storageWorks, cspRestricted

### RiskResponse

- `riskScore` (int), `decision` ("ALLOW" | "MFA" | "TERMINATE")
- `deviceSignature` (string), `sessionId` (string)

### Flow for `/risk/collect`

1. Map `RiskCollectRequest` → `SignalRequest` (stage1/stage2/stage3 → legacy format)
2. Store raw signals in `raw_signals`
3. Normalize signals (booleans → 0/1, counts capped at 5, click interval &lt; 50ms → rapid-click)
4. Generate device signature: SHA-256(userAgent + screenWidth + screenHeight + timezone)
5. Rule-based risk score (weights: webdriver 30, fetchOverridden 40, hiddenIframe 10 each max 50, rapidClicking 20; cap 100)
6. Decision: &lt; 30 ALLOW, 30–69 MFA, ≥ 70 TERMINATE
7. Persist row in `risk_decisions`
8. Return `RiskResponse`

### Logging

Structured key-value logging for incoming signals and risk evaluation result (event, sessionId, userId, riskScore, decision).

### Future ML

`RiskScoringService` is the extension point: replace rule-based scoring with an ML inference service.

## Run

1. Start PostgreSQL and create database `risk_engine`.
2. Set DB credentials in `backend/src/main/resources/application.properties`.
3. Run the app:
   - **IntelliJ:** Open the `backend` folder (where `pom.xml` is), then Run **RiskEngineApplication**.
   - **CLI:** From `backend/`: `./mvnw spring-boot:run` (or `mvn spring-boot:run`).

Server runs on port **8080**.

### Test from the frontend (backend already running)

1. In your browser open: **http://localhost:8080/** or **http://localhost:8080/login.html**
2. Log in with any username/password (e.g. `demo` / `demo`) and click **Login**.
3. You’ll be on the **Dashboard**. Click **Evaluate Risk**.
4. The page will call the backend and show **risk score**, **decision** (ALLOW / MFA / TERMINATE), and **device signature**.

Use **Logout** on the dashboard to return to the login page. The frontend is in `backend/src/main/resources/static/` (login.html, dashboard.html, risk-agent.js).

## Conventions

- Layered design: controller → service → repository.
- No device-specific or fingerprint libraries.
- POC-focused; minimal security config.
- Spring Data JPA for persistence.
