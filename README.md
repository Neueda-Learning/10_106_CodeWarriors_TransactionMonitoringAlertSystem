# Transaction Monitoring & Alert System

Welcome to the CodeWarriors Transaction Monitoring System. This repository contains the complete stack for a real-time banking transaction monitoring and anomaly detection dashboard.

## 🏗️ Project Architecture

The project is divided into two main components:
1. **Frontend**: A Single Page Application (SPA) built with Vanilla JavaScript, HTML5, and Bootstrap 5. It serves as the operator's dashboard for viewing live transactions and resolving alerts.
2. **Backend**: A Java Spring Boot application utilizing Spring Web and Spring JDBC (connected to a MySQL database). It exposes REST API endpoints for the frontend to consume.

---

## 🚀 Getting Started

### 1. Database Setup
Ensure you have a local MySQL server running on port `3306`.
- **Username**: `root`
- **Password**: `n3u3da!`
- The Spring Boot application will automatically execute `schema.sql` and `seed_data.sql` to initialize the `transactions` database upon startup.

### 2. Running the Backend
1. Open the `Backend/transactions` project in your Java IDE.
2. Run `TransactionsApplication.java`.
3. The server will start on `http://localhost:8085`.
4. *Note: A Live Transaction Simulator is currently enabled and will generate 5 random transactions every 60 seconds to populate the live dashboard.*

### 3. Running the Frontend
The frontend requires no build steps. 
1. Navigate to the `Frontend` directory.
2. Open `index.html` in any modern web browser.
3. The dashboard will automatically fetch live data from the backend if it is running. (If the backend is offline, the frontend will elegantly fall back to mock data).

---

## 🔗 API Endpoints & Interfaces (API "Keys")

This section documents all the internal REST API endpoints (interfaces) that connect our Frontend to the Backend, as well as placeholders for external API keys if third-party services are integrated in the future.

### Internal REST API Endpoints

These are the core endpoints that the backend provides and the frontend consumes. 

#### 1. Transactions API
- **Endpoint**: `GET /api/transactions`
- **Status**: ✅ Implemented
- **Usage**: Fetches all bank transactions, sorted by the most recent. The frontend polls this every 60 seconds for the live dashboard.

#### 2. Alerts API (Pending Backend Implementation)
- **Endpoint**: `GET /api/alerts`
- **Status**: ⏳ Pending (Frontend currently uses mock data)
- **Usage**: Fetches all active security alerts triggered by the rule engine.

#### 3. Alert Lifecycle API (Pending Backend Implementation)
- **Endpoint**: `POST /api/alerts/{alertId}/acknowledge`
- **Status**: ⏳ Pending
- **Usage**: Changes an alert status from `OPEN` to `ACKNOWLEDGED`.
- **Endpoint**: `POST /api/alerts/{alertId}/dismiss`
- **Status**: ⏳ Pending
- **Usage**: Closes an alert as a false positive.

#### 4. Rules API (Pending Backend Implementation)
- **Endpoint**: `GET /api/rules`
- **Status**: ⏳ Pending
- **Usage**: Fetches the list of active monitoring rules (e.g., Velocity Check, Amount Threshold).
- **Endpoint**: `POST /api/rules`
- **Status**: ⏳ Pending
- **Usage**: Allows operators to create or update transaction monitoring rules.

---

### 🔑 External API Keys (Future Integrations)

Currently, the application is fully self-contained and **does not require any paid or external API keys** to run. However, as the project scales, the following API keys might be integrated by the team:

1. **Twilio API Key** (Optional)
   - *Purpose*: To send real-time SMS alerts to operators when a `HIGH` severity alert is triggered.
2. **SendGrid / SMTP API Key** (Optional)
   - *Purpose*: To send daily email summaries of flagged transactions to the compliance team.
3. **Auth0 / JWT Secret Key** (Optional)
   - *Purpose*: Currently, the REST APIs are unprotected. A secret key will be required once Spring Security is implemented to issue tokens for operator login.

---

## 👨‍💻 Team Collaboration Notes
- **Frontend State**: The UI is fully built out. The dashboard, charts, and transaction filters are dynamically tied to the `GET /api/transactions` endpoint. 
- **Backend State**: The foundation is laid. The `Transactions` module is complete with a live simulator. The next priority is for the backend team to build out the `Alerts` and `Rules` controllers matching the endpoints listed above so the frontend can swap out its remaining mock data.
