# Risk Engine POC – Setup Guide

This guide is for anyone setting up the project for the first time, including those new to software development. It explains what each part of the project does, how to set up the database, and how to verify that everything works.

**Important:** Complete [Section 1](#1-environment--version-requirements) and [Section 2](#2-verify-your-environment-do-this-first) first. That way you fix any Java, Maven, or database version issues before running the app.

---

## Table of contents

1. [Environment & version requirements](#1-environment--version-requirements)
2. [Verify your environment (do this first)](#2-verify-your-environment-do-this-first)
3. [What this project does](#3-what-this-project-does)
4. [Prerequisites](#4-prerequisites)
5. [Project structure and file reference](#5-project-structure-and-file-reference)
6. [Database setup](#6-database-setup)
7. [Application configuration](#7-application-configuration)
8. [Running the application](#8-running-the-application)
9. [Checks after setup](#9-checks-after-setup)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Environment & version requirements

Use the versions below. Mismatched versions can cause build or runtime errors, so check these **before** you run the project.

### 1.1 Required versions (from this project)

| Component | Required version | Where it is defined |
|-----------|------------------|----------------------|
| **Java** | **21** | `backend/pom.xml` → `<java.version>21</java.version>` |
| **Spring Boot** | **3.2.0** | `backend/pom.xml` → `<version>3.2.0</version>` in parent |
| **PostgreSQL (server)** | **12 or higher** (14+ recommended) | Not in repo; you install it. App uses standard SQL and JSONB. |
| **PostgreSQL JDBC driver** | Managed by Spring Boot (e.g. 42.6.x) | Brought in by `pom.xml`; no need to set manually. |
| **Maven** (if not using wrapper) | **3.6+** | Optional; use `./mvnw` so you don’t need to install Maven. |

### 1.2 How to check each component

Run these in a terminal and compare with the required versions.

**Java**

```bash
java -version
```

- **Expected:** Something like `openjdk version "21.x.x"` or `java version "21.x.x"`. The important part is **21**.
- **If you see 17 or 11:** Install Java 21 and make sure your `PATH` uses it (or set `JAVA_HOME` to the Java 21 install).

**Java compiler (optional check)**

```bash
javac -version
```

- **Expected:** `javac 21.x.x` (must be 21, not lower).

**Maven (only if you use `mvn` instead of `./mvnw`)**

```bash
mvn -version
```

- **Expected:** Apache Maven 3.6 or higher, and **Java version: 21** in the same output.
- **If you use `./mvnw spring-boot:run`:** You can skip installing Maven; the wrapper will use a compatible version.

**PostgreSQL server**

```bash
psql --version
```

- **Expected:** `psql (PostgreSQL) 12.x` or higher (14, 15, 16, etc.).
- **Alternative (if psql is not in PATH):** Open pgAdmin or any PostgreSQL client and check the server version in the UI (e.g. “Server status” or “About”).

### 1.3 Summary table: command vs what you need

| What you need | Command to check | You’re good if |
|---------------|-------------------|----------------|
| Java 21 | `java -version` | First line shows version **21** |
| Maven (optional) | `mvn -version` | 3.6+ and Java 21 listed |
| PostgreSQL | `psql --version` or server UI | Version **12 or higher** |

---

## 2. Verify your environment (do this first)

Before installing anything else or editing config, run these checks. Fix any failure before moving on.

**Step 1 – Java**

```bash
java -version
```

- If you don’t see **21**, install Java 21 (e.g. from [Adoptium](https://adoptium.net/) or your OS package manager) and run `java -version` again.

**Step 2 – PostgreSQL installed and running**

- **Option A:** Run `psql --version`. If the command is not found, install PostgreSQL.
- **Option B:** Open pgAdmin (or another client). If you can’t connect to a PostgreSQL server, install and start PostgreSQL.

**Step 3 – Project builds (optional but recommended)**

From the project root, go to the backend and run:

```bash
cd backend
./mvnw clean compile
```

- **Windows:** `mvnw.cmd clean compile`
- **Expected:** `BUILD SUCCESS` at the end. If you see “Java version” or “release version” errors, your Java is not 21 or not the one used by the wrapper.

Once all steps pass, continue with [Section 3](#3-what-this-project-does) and the rest of the guide.

---

## 3. What this project does

This is a **Post-Login Runtime Risk Detection Engine** (proof of concept). In simple terms:

- A user “logs in” through a simple web page.
- The browser collects some signals (e.g. screen size, timezone, whether certain APIs were tampered with, iframes).
- These signals are sent to the server, which scores the session for risk (low / medium / high).
- The server decides: **ALLOW** (continue), **MFA** (ask for extra verification), or **TERMINATE** (block).
- Raw signals and decisions are stored in a **PostgreSQL** database for analysis.

No real authentication or production security is implemented; this is a POC to demonstrate the flow and data model.

---

## 4. Prerequisites

Before you start, you need:

| Requirement | Purpose | How to check |
|-------------|---------|--------------|
| **Java 21** | Backend runs on Java | Run `java -version`; you should see version 21 (see [Section 1](#1-environment--version-requirements)). |
| **PostgreSQL 12+** | Database for signals and decisions | Run `psql --version` or use pgAdmin; ensure the server is running (see [Database setup](#6-database-setup)). |
| **A code editor or IDE** (e.g. IntelliJ IDEA, VS Code, Cursor) | To open and run the project | Optional if you only use the command line. |
| **A web browser** | To use the login page and dashboard | Any modern browser (Chrome, Firefox, Edge, Safari). |

If Java or PostgreSQL is not installed or the version is wrong, fix that first using [Section 1](#1-environment--version-requirements) and [Section 2](#2-verify-your-environment-do-this-first).

---

## 5. Project structure and file reference

Below is what each folder and important file is for. You do not need to change most of these; they are listed so you know where things live.

### 5.1 Root of the project

| Path | Type | Purpose |
|------|------|---------|
| `README.md` | Doc | Short overview, API summary, and how to run the app. |
| `SETUP.md` | Doc | This file: detailed setup and checks. |
| `backend/` | Folder | Contains the entire Java application and the web UI. |

---

### 5.2 Backend root (`backend/`)

| Path | Type | Purpose |
|------|------|---------|
| `pom.xml` | Config | Maven project file: lists Java version (21), Spring Boot version, and libraries (e.g. PostgreSQL driver, JPA). Do not change unless you know Maven. |
| `mvnw` | Script | Maven Wrapper script for Unix/Mac. Lets you run Maven (e.g. `./mvnw spring-boot:run`) without installing Maven globally. |
| `mvnw.cmd` | Script | Same as `mvnw` but for Windows. |
| `.mvn/wrapper/` | Folder | Contains Maven Wrapper settings and jar so the project can build without a global Maven install. |
| `.gitignore` | Config | Tells Git which files not to track (e.g. `target/`, build outputs). |
| `src/` | Folder | All source code and resources (Java, config, static web files). |

---

### 5.3 Java source code (`backend/src/main/java/com/riskengine/`)

| Path | Type | Purpose |
|------|------|---------|
| `RiskEngineApplication.java` | Java | Main entry point. Spring Boot starts the application from here. You run this class to start the server. |
| **config/** | Folder | Configuration for the web application. |
| `config/WebConfig.java` | Java | Configures CORS (so the browser can call the API from different origins) and redirects the root URL `/` to the login page. |
| **controller/** | Folder | REST API endpoints. |
| `controller/AuthController.java` | Java | Handles **POST /login**. Accepts username/password (mock), returns a session ID and user ID. No real authentication. |
| `controller/RiskController.java` | Java | Handles **POST /risk/collect** and **POST /risk/evaluate**. Receives browser signals (RiskCollectRequest or SignalRequest), runs the risk pipeline (normalize → signature → score → decision), saves to DB, returns risk score and decision. |
| **dto/** | Folder | Data Transfer Objects: the shapes of JSON requests and responses. |
| `dto/LoginRequest.java` | Java | Request body for login: `username`, `password`. |
| `dto/LoginResponse.java` | Java | Response from login: `sessionId`, `userId`. |
| `dto/SignalRequest.java` | Java | Request body for risk evaluation: `sessionId`, `userId`, `iframeSignals`, `webdriverFlag`, `fetchOverridden`, `userAgent`, screen size, `timezone`, `clickIntervalAvg`, etc. |
| `dto/IframeSignals.java` | Java | Nested object inside `SignalRequest`: iframe counts (`total`, `suspicious`, `hidden`, `offscreen`, `crossOrigin`) from the browser. |
| `dto/RiskResponse.java` | Java | Response from risk evaluation: `riskScore`, `decision`, `deviceSignature`, `sessionId`. |
| `dto/NormalizedSignals.java` | Java | Internal use: normalized values (e.g. 0/1 flags, iframe counts) used only for scoring inside the server. |
| **service/** | Folder | Business logic. |
| `service/SignalNormalizationService.java` | Java | Converts raw signals into normalized form (e.g. booleans to 0/1, rapid-click detection from click interval). |
| `service/SignatureService.java` | Java | Builds a device signature (SHA-256 of userAgent + screen size + timezone) for identification. |
| `service/RiskScoringService.java` | Java | Rule-based risk scoring using weights (e.g. webdriver, fetch override, iframe counts). Score is capped at 100. |
| `service/DecisionService.java` | Java | Converts risk score into ALLOW / MFA / TERMINATE; persists raw signals and risk decisions to the database. |
| **entity/** | Folder | JPA entities: one class per database table. |
| `entity/RawSignal.java` | Java | Maps to table `raw_signals`: stores each risk-evaluation request’s payload as JSON. |
| `entity/RiskDecisionLog.java` | Java | Maps to table `risk_decisions`: stores each risk score and decision per session/user. |
| **repository/** | Folder | Spring Data JPA repositories: used to read/write the entities. |
| `repository/RawSignalRepository.java` | Java | Saves and queries `RawSignal` (raw_signals table). |
| `repository/RiskDecisionRepository.java` | Java | Saves and queries `RiskDecisionLog` (risk_decisions table). |
| **util/** | Folder | Small utilities. |
| `util/HashUtil.java` | Java | SHA-256 hashing used for the device signature. |

---

### 5.4 Resources and configuration (`backend/src/main/resources/`)

| Path | Type | Purpose |
|------|------|---------|
| `application.properties` | Config | **Main config file.** Server port (8080), database URL, username, password, and JPA settings. You **must** set the correct database username and password here (see [Application configuration](#7-application-configuration)). |
| `schema.sql` | SQL | Reference SQL for the two tables. The app can create tables automatically; this file is for documentation or manual creation. |
| **static/** | Folder | Static web files served by Spring Boot. This is the “frontend” (HTML + JS). |

---

### 5.5 Frontend – static files (`backend/src/main/resources/static/`)

| Path | Type | Purpose |
|------|------|---------|
| `index.html` | HTML | Redirects to the login page. Opening `http://localhost:8080/` lands here, then redirects to `login.html`. |
| `login.html` | HTML | Login page: username and password fields, “Login” button. On submit, calls **POST /login**, stores `sessionId` and `userId` in the browser, then redirects to the dashboard. |
| `dashboard.html` | HTML | After login: shows session info, “Evaluate Risk” button, and area for risk result (score, decision, device signature). Calls **POST /risk/collect** with data from `risk-agent.js`. |
| `risk-agent.js` | JavaScript | 3-stage browser fingerprint collector (fast signals, canvas/WebGL/audio/fonts hashes, anti-tampering). Auto-sends on load; exposes `RiskAgent.captureAndBuildPayload(sessionId, userId)` for **POST /risk/collect**. |
| `README.md` | Doc | Short note on placing frontend files and using the API when served from Spring Boot. |

---

## 6. Database setup

The application uses **PostgreSQL** and expects a database named **risk_engine**. Tables can be created automatically by the application when it starts.

### 6.1 Install and start PostgreSQL

- **Windows:** Install from [postgresql.org](https://www.postgresql.org/download/windows/) and start the service (e.g. from Services or pgAdmin).
- **macOS:** e.g. `brew install postgresql` then `brew services start postgresql`.
- **Linux:** Use your package manager (e.g. `apt install postgresql`, `sudo systemctl start postgresql`).

Ensure the PostgreSQL server is running before the next steps.

### 6.2 Create the database

You need a database named **risk_engine**. You can create it in any of these ways:

**Option A – Command line (psql)**

1. Open a terminal.
2. Connect to PostgreSQL (replace `postgres` with your PostgreSQL username if different):
   - macOS/Linux: `psql -U postgres`
   - Windows: `psql -U postgres` (or use “SQL Shell” from the Start menu).
3. Run:
   ```sql
   CREATE DATABASE risk_engine;
   ```
4. Optionally verify:
   ```sql
   \l
   ```
   You should see `risk_engine` in the list.
5. Exit: `\q`

**Option B – pgAdmin (or another GUI)**

1. Open pgAdmin and connect to your PostgreSQL server.
2. Right‑click “Databases” → “Create” → “Database”.
3. Set name to **risk_engine**, then save.

### 6.3 Tables (optional – app can create them)

The application is configured with `spring.jpa.hibernate.ddl-auto=update`, so it will **create or update** the tables when it starts. You do **not** have to run any SQL by hand.

If you prefer to create tables manually (e.g. for a strict production-like setup), you can run the statements in **`backend/src/main/resources/schema.sql`** once against the `risk_engine` database:

- **raw_signals** – stores each risk-evaluation request (session_id, user_id, timestamp, signal_json).
- **risk_decisions** – stores each risk result (session_id, user_id, risk_score, decision, created_at).

For the POC, letting the application create tables is enough.

### 6.4 Note on database user and password

The application will connect to PostgreSQL using a **username** and **password**. You will set these in `application.properties` (see next section). Ensure this user has permission to create and use the `risk_engine` database (usually the same user you used to create the database, e.g. `postgres`, or a dedicated app user you create).

---

## 7. Application configuration

The only file you **must** edit for a normal setup is the application config.

### 7.1 Open the config file

- Path: **`backend/src/main/resources/application.properties`**

### 7.2 Set database username and password

Find these lines and set them to your PostgreSQL username and password:

```properties
spring.datasource.username=postgres
spring.datasource.password=your_actual_password
```

- Replace `postgres` with your PostgreSQL username if different.
- Replace `your_actual_password` with the real password (leave empty only if your PostgreSQL user really has no password).

### 7.3 Optional: change database URL or port

If PostgreSQL is not on the same machine or not on the default port, adjust:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/risk_engine
```

- Change `localhost` to your database host if needed.
- Change `5432` if your PostgreSQL listens on another port.
- Keep `risk_engine` as the database name unless you created a different one.

### 7.4 Leave the rest as-is for the POC

- `server.port=8080` – the app and the web UI will be at `http://localhost:8080`.
- Other properties (JPA, logging) can stay as they are for this POC.

---

## 8. Running the application

### 8.1 From the command line

1. Open a terminal.
2. Go to the **backend** folder (where `pom.xml` is):
   ```bash
   cd path/to/risk-engine-poc/backend
   ```
3. Start the application:
   - **Mac/Linux:** `./mvnw spring-boot:run`
   - **Windows:** `mvnw.cmd spring-boot:run`
   - If Maven is installed globally: `mvn spring-boot:run`
4. Wait until you see a line similar to: “Started RiskEngineApplication” and no errors. The server will listen on port **8080**.

### 8.2 From IntelliJ IDEA (or similar IDE)

1. **File → Open** and select the **backend** folder (the one that contains `pom.xml`).
2. Let the IDE import the Maven project and resolve dependencies.
3. In the project tree, open `src/main/java/com/riskengine/RiskEngineApplication.java`.
4. Run it (e.g. right‑click → Run ‘RiskEngineApplication’, or green Run button next to `main`).
5. Wait until the console shows that the application has started (e.g. “Started RiskEngineApplication”).

---

## 9. Checks after setup

Do these checks in order. If any step fails, see [Troubleshooting](#10-troubleshooting).

### 9.1 Check 1: Application starts without errors

**What to do:** Start the application (command line or IDE) and watch the console.

**Expected:**

- No “Failed to configure a DataSource” or “Connection refused” errors.
- A line like: `Started RiskEngineApplication in X.XXX seconds`.
- No stack trace.

**If it fails:** Usually a database problem: wrong URL, username, password, or PostgreSQL not running. Fix `application.properties` and ensure PostgreSQL is up and the database `risk_engine` exists.

---

### 9.2 Check 2: Database tables exist (optional but recommended)

**What to do:** After the first successful start, inspect the database.

**Using psql:**

```bash
psql -U postgres -d risk_engine -c "\dt"
```

**Expected:** You should see two tables: **raw_signals** and **risk_decisions**.

**Using pgAdmin:** Connect to `risk_engine` and look at the “Tables” list; the same two tables should be there.

**If they are missing:** Ensure the app started successfully at least once and that `spring.jpa.hibernate.ddl-auto=update` is present in `application.properties`. If you use `validate` instead, you must create the tables yourself using `schema.sql`.

---

### 9.3 Check 3: Login page loads in the browser

**What to do:**

1. Open a browser.
2. Go to: **http://localhost:8080/** or **http://localhost:8080/login.html**

**Expected:**

- The login page appears (username and password fields, “Login” button).
- No “Cannot reach server” or connection errors.

**If it fails:** The application is not running or not on port 8080. Start it and ensure no other program is using port 8080.

---

### 9.4 Check 4: Login works (mock)

**What to do:**

1. On the login page, enter any username and password (e.g. `demo` / `demo`).
2. Click **Login**.

**Expected:**

- The page redirects to the dashboard (URL like `http://localhost:8080/dashboard.html`).
- The dashboard shows something like “Session: …” and an “Evaluate Risk” button.

**If it fails:** e.g. “Failed to fetch” or “Login failed” – the browser cannot call the API. Ensure you are using `http://localhost:8080` (not opening the HTML file from disk with `file://`). Check the browser console (F12 → Console) for errors.

---

### 9.5 Check 5: Risk evaluation works

**What to do:**

1. On the dashboard, click **Evaluate Risk**.
2. Wait a moment.

**Expected:**

- A result section appears with:
  - **Risk score** (a number 0–100),
  - **Decision** (ALLOW, MFA, or TERMINATE),
  - **Device signature** (a long hex string).

**If it fails:** Check the browser console and network tab (F12 → Network) for failed requests to `/risk/collect`. Ensure the backend is still running and that there are no errors in the server console.

---

### 9.6 Check 6: Data is stored in the database

**What to do:** After at least one successful “Evaluate Risk”:

**Using psql:**

```bash
psql -U postgres -d risk_engine -c "SELECT id, session_id, user_id, risk_score, decision FROM risk_decisions ORDER BY id DESC LIMIT 5;"
```

**Expected:** One or more rows with recent `risk_score` and `decision` values.

**Optional – raw signals:**

```bash
psql -U postgres -d risk_engine -c "SELECT id, session_id, user_id FROM raw_signals ORDER BY id DESC LIMIT 5;"
```

**Expected:** Rows corresponding to each risk evaluation you ran.

**If no rows:** The app might not be connected to the correct database, or the requests might be failing before persistence. Re-check Check 5 and the server logs.

---

### 9.7 Check 7: API from command line (optional)

**What to do:** Call the login API with curl (or similar):

```bash
curl -X POST http://localhost:8080/login -H "Content-Type: application/json" -d "{\"username\":\"test\",\"password\":\"test\"}"
```

**Expected:** JSON response like: `{"sessionId":"...","userId":"test"}`.

**If it fails:** Server not running, wrong port, or firewall blocking localhost.

---

## 10. Troubleshooting

| Problem | What to try |
|--------|-------------|
| **“Failed to configure a DataSource” / “Connection refused”** | PostgreSQL is not running, or URL/username/password in `application.properties` is wrong. Verify with `psql` or pgAdmin that you can connect to the same host, port, database, and user. |
| **“Port 8080 already in use”** | Another program is using 8080. Stop it or change `server.port` in `application.properties` (e.g. to 8081) and use `http://localhost:8081` in the browser. |
| **“Failed to fetch” on login or Evaluate Risk** | You are likely opening the page from disk (`file://`). Always use **http://localhost:8080/login.html** (or the port you set). |
| **Login works but Evaluate Risk shows an error** | Open browser DevTools (F12) → Console and Network. Check the `/risk/collect` request (status code and response body). Check server logs for exceptions. |
| **Tables not created** | Ensure `spring.jpa.hibernate.ddl-auto=update` is in `application.properties` and the app has started at least once without DB connection errors. If you use `validate`, create tables manually from `schema.sql`. |
| **Java version error / “release version 21 not supported”** | The project requires **Java 21**. Run `java -version` and `javac -version`; if they show 17 or 11, install Java 21 and set `JAVA_HOME` (see [Section 1](#1-environment--version-requirements)). |
| **Maven/Wrapper errors** | From the **backend** folder run `./mvnw spring-boot:run` (or `mvnw.cmd` on Windows). If that fails, install Maven 3.6+ and run `mvn spring-boot:run`. Ensure Maven is using Java 21 (`mvn -version`). |

---

## Quick reference

- **Application config:** `backend/src/main/resources/application.properties` (set DB username and password).
- **Database name:** `risk_engine`.
- **Tables:** `raw_signals`, `risk_decisions` (created by the app if `ddl-auto=update`).
- **Run app:** From `backend/`: `./mvnw spring-boot:run` or run `RiskEngineApplication` in IDE.
- **Open in browser:** http://localhost:8080/ or http://localhost:8080/login.html
- **API:** POST `/login` (username, password) and POST `/risk/collect` (signals). See README for full API details.

---

*This setup guide is part of the Risk Engine POC. For API details and high-level design, see **README.md**.*
