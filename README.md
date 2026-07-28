# Payment Processing Service

A microservice that orchestrates PayPal payment transactions as part of the [payment-integration-system](https://github.com/chandan-howale/payment-integration-system).

---

## Overview

This service acts as the **payment orchestrator** in the payment-integration-system. It manages the full payment lifecycle — creating transactions, initiating PayPal orders, and capturing payments. It communicates with the **paypal-provider-service** for PayPal API operations, uses **MySQL** for transaction persistence, and registers with **Netflix Eureka** for service discovery.

### System Architecture

```
                                    ┌───────────────────────────────┐
                                    │      Netflix Eureka           │
                                    │     (Service Registry)        │
                                    │                               │
                                    │  payment-processing-service   │
                                    │   registers on startup        │
                                    └─────────────▲─────────────────┘
                                                  │ discover
                                                  │
┌──────────────────────────────┐                  │
│  payment-processing-service  │──────────────────┤
│       (This Service)         │   calls          │
│                              │                  │
│  • Orchestrates payments     │                  │
│  • Manages status state      │                  │
│    machine                   │                  │
│  • Persists to MySQL         │                  │
│  • Circuit breaker on HTTP   │                  │
└──────────┬───────────────────┘                  │
           │                                      │
           │                                      │
           ▼                                      │
┌──────────────────────────────┐                  │
│         MySQL                │                  │
│     (payments database)      │                  │
└──────────────────────────────┘                  │
                                                  │
                                    ┌─────────────┴────────────────┐
                                    │  paypal-provider-service     │
                                    │   (PayPal API Wrapper)       │
                                    │                              │
                                    │  • Creates PayPal orders     │
                                    │  • Captures PayPal orders    │
                                    └─────────────┬────────────────┘
                                                  │
                                                  ▼
                                    ┌──────────────────────────────┐
                                    │     PayPal Sandbox API       │
                                    │   (Orders + OAuth + URLs)    │
                                    └──────────────────────────────┘
```

**How it works:**
1. **payment-processing-service** registers itself with **Eureka** on startup, making it discoverable.
2. When a payment is initiated, this service calls **paypal-provider-service** via Eureka to create/capture PayPal orders.
3. **MySQL** stores all transaction records with full status history.
4. **Resilience4j Circuit Breaker** protects against failures when calling paypal-provider-service.

### Payment Flow — Step by Step

```
Step 1: Create Payment
────────────────────────────────────────────────────────────────

  Client
    │
    │  POST /v1/payments
    │  { "userId": 1, "paymentMethodId": 1, "providerId": 1,
    │    "paymentTypeId": 1, "amount": 10.00, "currency": "USD",
    │    "merchantTransactionReference": "ORDER-123" }
    ▼
  payment-processing-service
    │
    │  ┌─────────────────────────────────────────────────┐
    │  │ 1. Map request to TransactionDto                │
    │  │ 2. Generate unique txnReference (UUID)          │
    │  │ 3. Delegate to PaymentStatusFactory             │
    │  │    → CreatedStatusProcessor                     │
    │  │ 4. Insert transaction into MySQL (status=1)     │
    │  └─────────────────────────────────────────────────┘
    │
    ▼
  Response:
  { "txnReference": "a1b2c3d4-...", "txnStatusId": 1 }


Step 2: Initiate Payment
────────────────────────────────────────────────────────────────

  Client
    │
    │  POST /v1/payments/{txnReference}/initiate
    │  { "successUrl": "https://...", "cancelUrl": "https://..." }
    ▼
  payment-processing-service
    │
    │  ┌──────────────────────────────────────────────────┐
    │  │ 1. Fetch transaction from MySQL                  │
    │  │ 2. Update status → Initiated (status=2)          │
    │  │ 3. Prepare HttpRequest for PayPal Create Order   │
    │  │ 4. Call paypal-provider-service via Eureka       │
    │  │    (protected by Circuit Breaker)                │
    │  │ 5. Parse response → get orderId + redirectUrl    │
    │  │ 6. Update status → Pending (status=3)            │
    │  │    Store providerReference (PayPal orderId)      │
    │  └──────────────────────────────────────────────────┘
    │
    ▼
  Response:
  { "txnReference": "a1b2c3d4-...", "txnStatusId": 3,
    "providerReference": "5O190127TN364715T",
    "redirectUrl": "https://sandbox.paypal.com/..." }


Step 3: User Approves on PayPal (external browser flow)

Step 4: Capture Payment
────────────────────────────────────────────────────────────────

  Client
    │
    │  POST /v1/payments/{txnReference}/capture
    ▼
  payment-processing-service
    │
    │  ┌──────────────────────────────────────────────────┐
    │  │ 1. Fetch transaction from MySQL                  │
    │  │ 2. Update status → Approved (status=4)           │
    │  │ 3. Prepare HttpRequest for PayPal Capture Order  │
    │  │ 4. Call paypal-provider-service via Eureka       │
    │  │ 5. Parse response → verify COMPLETED             │
    │  │ 6. Update status → Success (status=5)            │
    │  └──────────────────────────────────────────────────┘
    │
    ▼
  Response:
  { "txnReference": "a1b2c3d4-...", "txnStatusId": 5 }
```

### Transaction Status State Machine

```
Created (1) → Initiated (2) → Pending (3) → Approved (4) → Success (5)
                                                                │
                                                           Failed (6)
```

| Status | ID | Description |
|---|---|---|
| **Created** | 1 | Transaction record inserted into DB |
| **Initiated** | 2 | Status updated; PayPal Create Order API called |
| **Pending** | 3 | PayPal order created; `providerReference` and `redirectUrl` stored |
| **Approved** | 4 | Status updated before calling PayPal Capture Order API |
| **Success** | 5 | Capture confirmed completed |
| **Failed** | 6 | Set **only** when Create Order fails. Capture failures are left for reconciliation |

> **Note:** Capture failures are intentionally NOT marked as `Failed` (status=6). Since the user already approved, a failed capture is left for a reconciliation system to handle.

---

## Tech Stack

| Technology | Purpose |
|---|---|
| **Spring Boot 3.4.2** | Application framework |
| **Java 17** | Language runtime |
| **Netflix Eureka Client** | Service discovery & registration |
| **Spring Cloud Circuit Breaker (Resilience4j)** | Fault tolerance for external HTTP calls |
| **MySQL** | Transaction persistence (raw JDBC) |
| **NamedParameterJdbcTemplate** | Data access layer (no JPA/Hibernate) |
| **ModelMapper** | Object mapping (Entity ↔ DTO, STRICT strategy) |
| **Apache HttpClient 5** | Connection-pooled HTTP client |
| **AWS Secrets Manager** | Credential management (dev/prod profiles) |
| **Micrometer + Brave** | Distributed tracing |
| **Spring Boot Actuator** | Health checks and monitoring |
| **Lombok** | Boilerplate code reduction |
| **Gson** | JSON serialization/deserialization |
| **Maven** | Build tool |

---

## REST API Endpoints

### Payment Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/v1/payments` | Create a new payment transaction (status=1: Created) |
| `POST` | `/v1/payments/{txnReference}/initiate` | Initiate payment via PayPal (status 1→2→3) |
| `POST` | `/v1/payments/{txnReference}/capture` | Capture an approved payment (status 3→4→5) |

### Actuator

| Endpoint | Description |
|---|---|
| `/actuator/health` | Application health check |
| `/actuator/circuitbreakers` | Circuit breaker state (Resilience4j) |

---

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **MySQL** running locally with the `payments` database created
- **Eureka Service Registry** running (`localhost:8761`)
- **PayPal Provider Service** running (`localhost:8083`)
- **Docker** (optional) — For running MySQL locally

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/chandan-howale/payment-processing-service.git
cd payment-processing-service/payment-processing-eureka-sr-impl
```

### 2. Set Up Environment Variables

Copy the example env file and add your credentials:

```bash
cp .env.example .env
```

Edit `.env` with your database credentials:

```env
DB_URL=jdbc:mysql://localhost:3306/payments?allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=payments
DB_PASSWORD=your_password_here
```

### 3. Set Up MySQL Database

Run the migration scripts to create the database schema and seed data:

```bash
# Connect to MySQL and run the scripts
mysql -u root -p < src/main/resources/db/migration/v1__paymentdb_setup_ddl.sql
mysql -u root -p < src/main/resources/db/migration/v2__paymentdb_masterdata_dml.sql
```

> **Tip:** You can use **DBeaver** — a free, open-source GUI tool — to visualize the database schema, run SQL scripts, inspect table data, and manage connections. Connect to MySQL using the credentials from your `.env` file.

### 4. Start Dependencies

Make sure these services are running before starting:

```bash
# 1. Eureka Service Registry (port 8761)
# 2. PayPal Provider Service (port 8083)
# 3. MySQL (port 3306)
```

### 5. Run the Application

#### Option A: Using IntelliJ IDEA (Recommended)

1. Open the project folder in IntelliJ IDEA.
2. Navigate to `src/main/java/com/chandan/payments/PaymentProcessingServiceApplication.java`.
3. Right-click → **Run 'PaymentProcessingServiceApplication'**.
4. The service starts on **port 8082**.

> **Note for IntelliJ users:** If Lombok annotations aren't working, enable annotation processing:
> `Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing`

#### Option B: Using VS Code

1. Install extensions: **Extension Pack for Java** (Microsoft) + **Spring Boot Extension Pack**.
2. Open the project folder in VS Code.
3. Press `F5` or go to **Run → Start Debugging**.
4. The service starts on **port 8082**.

#### Option C: Using Maven (Command Line)

```bash
# Build the project first
mvn clean install -DskipTests

# Run with local profile (default)
mvn spring-boot:run
```

The service starts on **port 8082**.

### 6. Verify

```bash
# Check health
curl http://localhost:8082/actuator/health

# Create a test payment
curl -X POST http://localhost:8082/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "paymentMethodId": 1,
    "providerId": 1,
    "paymentTypeId": 1,
    "amount": 10.00,
    "currency": "USD",
    "merchantTransactionReference": "ORDER-TEST-001"
  }'
```

---

## Configuration Profiles

| Profile | Database | Eureka | PayPal Provider | Circuit Breaker |
|---|---|---|---|---|
| **local** (default) | From `.env` file | `localhost:8761` | `localhost:8083` | Enabled |
| **dev** | AWS Secrets Manager | Eureka cluster | Via Eureka discovery | Enabled |
| **prod** | AWS Secrets Manager | Eureka cluster | Via Eureka discovery | Enabled |

Switch profiles using Maven:

```bash
mvn spring-boot:run -Pdev
```

### Circuit Breaker Configuration (local)

| Setting | Value | Description |
|---|---|---|
| `failureRateThreshold` | 40% | Open circuit when 40% of calls fail |
| `minimumNumberOfCalls` | 5 | Minimum calls before evaluating |
| `slidingWindowSize` | 10 | Count-based sliding window |
| `waitDurationInOpenState` | 60s | Wait before transitioning to half-open |
| `permittedNumberOfCallsInHalfOpenState` | 2 | Test calls in half-open state |

---

## Project Structure

```
payment-processing-eureka-sr-impl/
├── src/main/java/com/chandan/payments/
│   ├── controller/              # REST controllers (PaymentController)
│   ├── service/
│   │   ├── interfaces/          # PaymentService, TransactionStatusProcessor
│   │   ├── impl/
│   │   │   ├── PaymentServiceImpl           # Core orchestration logic
│   │   │   └── statusprocessors/            # One per status (Created, Initiated, etc.)
│   │   ├── helper/              # PPCreateOrderHelper, PPCaptureOrderHelper
│   │   ├── PaymentStatusService.java        # Delegates to correct processor
│   │   └── PaymentStatusFactory.java        # Maps statusId → processor
│   ├── dao/
│   │   ├── interfaces/          # TransactionDao
│   │   └── impl/                # TransactionDaoImpl (raw JDBC)
│   ├── http/                    # HttpServiceEngine, HttpRequest, Circuit Breaker
│   ├── entity/                  # TransactionEntity
│   ├── dto/                     # TransactionDto
│   ├── pojo/                    # Request/Response POJOs
│   ├── paypalprovider/          # PayPal API request/response models
│   ├── config/                  # AppConfig (RestClient, ModelMapper)
│   ├── constant/                # ErrorCodeEnum
│   ├── exception/               # ProcessingServiceException, GlobalExceptionHandler
│   └── util/                    # JsonUtil
├── src/main/resources/
│   ├── application.properties
│   ├── application-local.properties
│   ├── application-dev.properties
│   └── db/migration/
│       ├── v1__paymentdb_setup_ddl.sql      # Database schema
│       └── v2__paymentdb_masterdata_dml.sql # Seed data
├── src/test/java/               # Unit tests for all layers
└── pom.xml
```

---

## Error Handling

All errors return a consistent JSON response:

```json
{
  "errorCode": "20002",
  "errorMessage": "PayPal-provider service is currently unavailable. Please try again later."
}
```

Error codes follow the `20000`–`20009` range:

| Code | Description |
|---|---|
| 20000 | Generic error — Something went wrong |
| 20001 | Resource not found — Invalid URL |
| 20002 | PayPal provider service unavailable |
| 20003 | Payment status not found |
| 20004 | Unknown PayPal provider error |
| 20005 | Error updating transaction details |

---

## Key Design Decisions

- **Strategy Pattern for Status Processing** — Each transaction status has its own `TransactionStatusProcessor` implementation. `PaymentStatusFactory` maps `statusId` → processor via switch statement. Adding a new status requires: a new processor class, a new case in the factory, and a new entry in `v2__paymentdb_masterdata_dml.sql`.

- **Circuit Breaker on External Calls** — `HttpServiceEngine.makeHttpCall()` is protected by Resilience4j `@CircuitBreaker`. If the paypal-provider-service is down, the fallback throws `ProcessingServiceException` instead of hanging.

- **No ORM — Raw JDBC** — Uses `NamedParameterJdbcTemplate` for all database operations. SQL is written inline in `TransactionDaoImpl`. This gives full control over queries without JPA overhead.

- **ModelMapper with STRICT Strategy** — Converts between `TransactionEntity` ↔ `TransactionDto` ↔ request/response POJOs. STRICT matching prevents silent field mismatches; skip-nulls prevents overwriting valid values.

- **Capture Failure ≠ Failed Status** — When PayPal capture fails, the transaction is NOT marked as `Failed` (status=6). Since the user already approved, a failed capture requires reconciliation — a separate system handles this.

- **Load Balanced RestClient** — The `RestClient` bean is annotated with `@LoadBalanced`, allowing Eureka service names in URLs (e.g., `http://paypal-provider-service/payments`) instead of hardcoded host:port.

---

## Testing

```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=PaymentServiceImplTest

# Run a single test method
mvn test -Dtest=PaymentServiceImplTest#testCreatePayment
```

> **Note:** Tests use Mockito to mock dependencies (DAO, HTTP engine, helpers). MySQL and external services are not required for unit tests.

---

## Related Services

This service is part of the [payment-integration-system](https://github.com/chandan-howale/payment-integration-system):

| Service | Repository | Port | Purpose |
|---|---|---|---|
| **Eureka Service Registry** | [eureka-service-registry](https://github.com/chandan-howale/eureka-service-registry) | 8761 | Service discovery |
| **PayPal Provider Service** | [paypal-provider-service](https://github.com/chandan-howale/paypal-provider-service) | 8083 | PayPal API wrapper |
| **Payment Processing Service** | [This repository](https://github.com/chandan-howale/payment-processing-service) | 8082 | Payment orchestration |

---

## License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

---

## Author

**Chandan Howale** — [GitHub](https://github.com/chandan-howale)

Built as part of the [payment-integration-system](https://github.com/chandan-howale/payment-integration-system) project.
