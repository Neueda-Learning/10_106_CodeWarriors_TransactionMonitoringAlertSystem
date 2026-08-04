# API Endpoints & Keys Documentation

This document contains all the necessary API endpoints and keys required for the backend functionality of the Transaction Monitoring System.

## 1. Internal REST API Endpoints (The "Keys" to Backend Services)

These are the core endpoints provided by the Java Spring Boot backend. The frontend uses these to communicate with the database.

### 💳 Transactions API
- **Endpoint**: `GET /api/transactions`
- **Functionality**: Fetches all banking transactions in real-time. The live simulator inserts new transactions dynamically, and this endpoint retrieves them sorted by the most recent timestamp.

### 🚨 Alerts API
- **Endpoint**: `GET /api/alerts`
- **Functionality**: Retrieves the list of all active security alerts triggered by the rule engine, including details like severity and the associated transaction ID.

- **Endpoint**: `POST /api/alerts/{alertId}/acknowledge`
- **Functionality**: Updates a specific alert's status from `OPEN` to `ACKNOWLEDGED`. Used by operators to indicate they are reviewing a flagged transaction.

- **Endpoint**: `POST /api/alerts/{alertId}/dismiss`
- **Functionality**: Updates a specific alert's status to `DISMISSED` or `CLOSED`, marking it as a false positive or fully resolved.

### 📜 Rules API
- **Endpoint**: `GET /api/rules`
- **Functionality**: Retrieves all active transaction monitoring rules (e.g., Velocity Checks, Amount Thresholds).

- **Endpoint**: `POST /api/rules`
- **Functionality**: Allows system administrators to create, update, or toggle security rules on the fly.

## 2. External API Keys (Future Enhancements)

Currently, the backend runs entirely locally via JDBC to MySQL and does not require external third-party API keys. However, the following integrations are standard for this type of system:

- **Twilio API Key**: For sending real-time SMS notifications to compliance officers when a `HIGH` severity alert is generated.
- **SendGrid API Key**: For emailing daily aggregated suspicious transaction reports.
- **Auth0 Secret Key**: For securing the REST endpoints (JWT token validation) once user authentication is implemented.
