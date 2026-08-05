# Transaction Monitoring - End-to-End Setup

This project has a Spring Boot backend and a static frontend dashboard.

## Backend API Routes

### Accounts
- `GET /accounts`
- `GET /accounts/{id}`
- `POST /accounts`
- `PUT /accounts/{id}`
- `DELETE /accounts/{id}`

### Bank Transactions
- `GET /transactions`
- `GET /transactions/{id}`
- `POST /transactions`
- `PUT /transactions/{id}`
- `DELETE /transactions/{id}`

### Alerts
- `GET /alerts`
- `GET /alerts/{id}`
- `POST /alerts`
- `PUT /alerts/{id}/status`
- `DELETE /alerts/{id}`

### Rules
- `GET /rules`
- `GET /rules/{id}`
- `POST /rules`
- `PUT /rules/{id}`
- `DELETE /rules/{id}`

## Local Run Guide

1. Start MySQL and ensure `root` credentials in `application.properties` are valid.
2. Start backend on port `8085`.
3. Serve frontend from a local static server.

### Start Backend

```powershell
Set-Location "C:\10_106_CodeWarriors_TransactionMonitoring\Backend\transactions"
.\mvnw.cmd spring-boot:run
```

### Run Tests

```powershell
Set-Location "C:\10_106_CodeWarriors_TransactionMonitoring\Backend\transactions"
.\mvnw.cmd test -DskipTests=false
```

### Start Frontend Static Server (Python example)

```powershell
Set-Location "C:\10_106_CodeWarriors_TransactionMonitoring\Frontend"
python -m http.server 5500
```

Open: `http://localhost:5500/index.html`

## Notes

- Backend CORS is enabled for local integration.
- SQL schema and seed data are loaded at startup from:
  - `schema.sql`
  - `seed_data.sql`
