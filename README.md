# Shadow Ledger System

A distributed banking backend system for processing financial events, maintaining a shadow ledger, and detecting drift between the Core Banking System (CBS) and shadow balances.

## 🏗️ Architecture

The system consists of 4 microservices:

1. **API Gateway** (Port 8080)
   - JWT-based authentication and authorization 
   - RBAC enforcement (user, auditor, admin roles)
   - Request routing to backend services
   - Distributed tracing with X-Trace-Id
   - Password storage in PostgreSQL (plain text for demo purposes)

2. **Event Service** (Port 8085)
   - Accepts financial events (debits/credits)
   - Validates and deduplicates events
   - Publishes to Kafka `transactions.raw` topic
   - Stores events in PostgreSQL for traceability

3. **Shadow Ledger Service** (Port 8086)
   - Consumes events from Kafka `transactions.raw` and `transactions.corrections`
   - Maintains immutable append-only ledger
   - Computes shadow balance using SQL window functions
   - Prevents negative balances
   - Exposes balance query API

4. **Drift and Correction Service** (Port 8087)
   - Compares CBS vs shadow balances
   - Detects mismatches (missing credits, incorrect debits)
   - Generates correction events automatically
   - Publishes corrections to Kafka `transactions.corrections` topic
   - Supports manual corrections via admin endpoint

## 🛠️ Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.2+
- **API Gateway**: Spring Cloud Gateway with Spring Web (JDBC-based)
- **Database**: PostgreSQL 16
- **Message Broker**: Apache Kafka 7.5 (Confluent Platform)
- **Security**: JWT (JJWT library), Plain text passwords (demo only - NOT for production)
- **Build Tool**: Gradle 8.x
- **Containerization**: Docker & Docker Compose (infrastructure only)
- **Monitoring**: Spring Boot Actuator, Kafka UI

## 📋 Prerequisites

- **Java 21**: Required for running the Spring Boot services locally
- **Docker Desktop or Docker Engine**: For running PostgreSQL, Kafka, Zookeeper, and Kafka UI
- **At least 4GB RAM allocated to Docker**
- **Available Ports**: 
  - 5434 (PostgreSQL)
  - 8080 (API Gateway)
  - 8085 (Event Service)
  - 8086 (Shadow Ledger Service)
  - 8087 (Drift Correction Service)
  - 8090 (Kafka UI)
  - 9092 (Kafka - external)
  - 29092 (Kafka - internal)
  - 2181 (Zookeeper)
- **curl or Insomnia/Postman**: For API testing
- **IntelliJ IDEA or VS Code**: Recommended for development

## 🚀 Quick Start

### Step 1: Start Infrastructure (PostgreSQL, Kafka, Zookeeper, Kafka UI)

```bash
cd /Users/GKAUSHIK/Desktop/ShadowLedgerSystem
docker-compose up -d
```

This starts:
- **PostgreSQL** on port 5434 (mapped from container port 5432)
- **Zookeeper** on port 2181
- **Kafka** on ports 9092 (external) and 29092 (internal)
- **Kafka UI** on port 8090

Wait for all services to be healthy:
```bash
docker-compose ps
```

### Step 2: Run Services Locally

#### Option A: Using IntelliJ IDEA (Recommended)

1. Open the project in IntelliJ IDEA
2. Run each service's main application class:
   - `api-gateway/src/main/java/com/example/Api_Gateway/ApiGatewayApplication.java`
   - `event-service/src/main/java/com/example/event_service/EventServiceApplication.java`
   - `shadow-ledger-service/src/main/java/com/example/shadow_ledger_service/ShadowLedgerServiceApplication.java`
   - `drift-correction-service/src/main/java/com/example/drift_correction_service/DriftCorrectionServiceApplication.java`

#### Option B: Using Gradle from Terminal

Open 4 separate terminal windows and run:

```bash
# Terminal 1 - Shadow Ledger Service (must start first to create Kafka topics)
cd shadow-ledger-service
./gradlew bootRun

# Terminal 2 - Event Service
cd event-service
./gradlew bootRun

# Terminal 3 - Drift Correction Service
cd drift-correction-service
./gradlew bootRun

# Terminal 4 - API Gateway
cd api-gateway
./gradlew bootRun
```

**Important**: Start Shadow Ledger Service first to ensure Kafka topics are created.

### Step 3: Verify All Services are Running

Check health endpoints:
```bash
# API Gateway
curl http://localhost:8080/actuator/health

# Event Service
curl http://localhost:8085/actuator/health

# Shadow Ledger Service
curl http://localhost:8086/actuator/health

# Drift Correction Service
curl http://localhost:8087/actuator/health
```

All should return:
```json
{"status":"UP"}
```

### Step 4: Access Kafka UI (Optional)

Open your browser and navigate to:
```
http://localhost:8090
```

You can monitor Kafka topics, messages, and consumer groups here.

### Service URLs

| Service | URL | Port | Access |
|---------|-----|------|--------|
| API Gateway | http://localhost:8080 | 8080 | Public (requires JWT) |
| Event Service | http://localhost:8085 | 8085 | Internal (direct access for monitoring only) |
| Shadow Ledger | http://localhost:8086 | 8086 | Internal (direct access for monitoring only) |
| Drift Correction | http://localhost:8087 | 8087 | Internal (direct access for monitoring only) |
| Kafka UI | http://localhost:8090 | 8090 | Public (monitoring) |
| PostgreSQL | localhost:5434 | 5434 | Database access |
| Kafka Bootstrap | localhost:9092 | 9092 | Kafka client connections |

**Note**: All client requests should go through the API Gateway (port 8080). Direct access to backend services is for monitoring and health checks only.

## 📖 API Documentation

- **[api-specs.yaml](./api-specs.yaml)** - OpenAPI 3.0 specification for all endpoints
- **[ordering-rules.md](./ordering-rules.md)** - Event ordering and immutability rules
- **[correction-strategy.md](./correction-strategy.md)** - Drift detection and correction strategy

## 🚀 Quick Start

### Option 1: Local Development (Recommended for Development)

#### 1. Start Infrastructure Only
```bash
cd /Users/GKAUSHIK/Desktop/ShadowLedgerSystem
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Zookeeper (port 2181)
- Kafka (port 9092, 29092)
- Kafka UI (port 8090)

#### 2. Run Services in IntelliJ IDEA
Open the project in IntelliJ and run each service:

1. **Shadow Ledger Service** → `ShadowLedgerServiceApplication.java`
2. **Event Service** → `EventServiceApplication.java`
3. **Drift Correction Service** → `DriftCorrectionServiceApplication.java`
4. **API Gateway** → `ApiGatewayApplication.java`

Or use Gradle:
```bash
# Terminal 1
cd shadow-ledger-service && ./gradlew bootRun

# Terminal 2
cd event-service && ./gradlew bootRun

# Terminal 3
cd drift-correction-service && ./gradlew bootRun

# Terminal 4 - API Gateway
cd api-gateway
./gradlew bootRun
```

**Important**: Start Shadow Ledger Service first to ensure Kafka topics are created.

### Step 3: Verify All Services are Running

```bash
# Build all services
docker build -t shadow/api-gateway:latest ./Api-Gateway
docker build -t shadow/event-service:latest ./event-service
docker build -t shadow/ledger-service:latest ./shadow-ledger-service
docker build -t shadow/drift-service:latest ./drift-correction-service
```

### 3. Wait for Services to be Ready
Check health endpoints:
```bash
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # Event Service
curl http://localhost:8082/actuator/health  # Shadow Ledger
curl http://localhost:8083/actuator/health  # Drift Correction
```

### 4. Run Acceptance Tests
```bash
chmod +x scripts/run-acceptance.sh
./scripts/run-acceptance.sh
```

## 📖 API Usage

### Default Test Users
The system includes demo users for testing:
- User: `user1` / Password: `password` / Role: `user`
- Auditor: `auditor1` / Password: `password` / Role: `auditor`
- Admin: `admin1` / Password: `password` / Role: `admin`

### Authentication

All API requests (except signup and login) require a valid JWT token.

#### Default Test Users

The system comes pre-configured with the following test accounts:

| User ID | Username | Password | Role |
|---------|----------|----------|------|
| `user1` | `demouser` | `password` | `user` |
| `auditor1` | `demoauditor` | `password` | `auditor` |
| `admin1` | `demoadmin` | `password` | `admin` |

**Roles:**
- `user`: Can submit events (`POST /events`) and query balances (`GET /accounts/{accountId}/shadow-balance`)
- `auditor`: Can run drift checks (`POST /drift-check`)
- `admin`: Can perform manual corrections (`POST /correct/{accountId}`)

#### 1. Sign Up (Create New Account)

**Endpoint**: `POST /auth/signup`

```bash
curl --request POST \
  --url http://localhost:8080/auth/signup \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": "newuser",
    "username": "New User",
    "password": "SecurePass123!",
    "role": "user"
  }'
```

**Response:**
```json
{
  "message": "User registered successfully"
}
```

**Note**: Both `userId` and `username` are required and must be unique.

#### 2. Login (Get JWT Token)

**Endpoint**: `POST /auth/token`

Login with userId:
```bash
curl --request POST \
  --url http://localhost:8080/auth/token \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": "user1",
    "password": "password"
  }'
```

Or login with username:
```bash
curl --request POST \
  --url http://localhost:8080/auth/token \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": "demouser",
    "password": "password"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSIsImlhdCI6MTczNTU2MTgwMCwiZXhwIjoxNzM1NTY1NDAwLCJyb2xlIjoidXNlciJ9.xyz..."
}
```

**Save this token** - you'll need it for all subsequent API calls.

**Token Details:**
- Expiration: 1 hour from issuance
- Format: Bearer token
- Contains: userId (sub), role, issued at (iat), expiration (exp)

#### 3. Logout

**Endpoint**: `POST /auth/logout`

```bash
curl --request POST \
  --url http://localhost:8080/auth/logout \
  --header 'Authorization: Bearer <JWT_TOKEN>'
```

**Response:**
```json
{
  "message": "Logged out successfully"
}
```

**Note**: The current implementation doesn't blacklist tokens server-side. The token will remain valid until expiration.

### Financial Event Operations

#### Submit Event (User Role)

**Endpoint**: `POST /events`

```bash
curl --request POST \
  --url http://localhost:8080/events \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer <JWT_TOKEN>' \
  --data '{
    "eventId": "E1001",
    "accountId": "A10",
    "type": "credit",
    "amount": 500.00,
    "timestamp": 1735561800000
  }'
```

**Event Types:**
- `credit`: Adds to account balance
- `debit`: Subtracts from account balance

**Validation Rules:**
- `eventId` must be unique (idempotency)
- `accountId` is required
- `amount` must be > 0
- `type` must be either "credit" or "debit"
- `timestamp` is required (epoch milliseconds)

**Response:**
```json
{
  "message": "Event received successfully",
  "eventId": "E1001"
}
```

#### Query Shadow Balance (User Role)

**Endpoint**: `GET /accounts/{accountId}/shadow-balance`

```bash
curl --request GET \
  --url http://localhost:8080/accounts/A10/shadow-balance \
  --header 'Authorization: Bearer <JWT_TOKEN>'
```

**Response:**
```json
{
  "accountId": "A10",
  "balance": 750.00,
  "lastEvent": "E1005"
}
```
  "lastEvent": "E1005"
}
```

### Drift Detection and Correction

#### Run Drift Check (Auditor Role)

**Endpoint**: `POST /drift-check`

Compares CBS (Core Banking System) reported balances with shadow ledger balances.

**First, login as auditor to get token:**
```bash
curl --request POST \
  --url http://localhost:8080/auth/token \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": "auditor1",
    "password": "password"
  }'
```

**Then run drift check:**
```bash
curl --request POST \
  --url http://localhost:8080/drift-check \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer <AUDITOR_JWT_TOKEN>' \
  --data '[
    {
      "accountId": "A10",
      "reportedBalance": 700.00
    },
    {
      "accountId": "A11",
      "reportedBalance": 1550.00
    }
  ]'
```

**Response:**
```json
{
  "totalAccounts": 2,
  "mismatches": 1,
  "results": [
    {
      "accountId": "A10",
      "shadowBalance": 750.00,
      "reportedBalance": 700.00,
      "difference": 50.00,
      "status": "MISMATCH",
      "mismatchType": "SHADOW_HIGHER",
      "correctionEventId": "CORR-A10-7f3a2b1c",
      "message": "Shadow balance is higher than CBS by 50.00. Correction event generated."
    },
    {
      "accountId": "A11",
      "shadowBalance": 1550.00,
      "reportedBalance": 1550.00,
      "difference": 0.00,
      "status": "MATCH",
      "message": "Balances match"
    }
  ]
}
```

**Mismatch Types:**
- `SHADOW_HIGHER`: Shadow ledger has more than CBS (generates debit correction)
- `SHADOW_LOWER`: Shadow ledger has less than CBS (generates credit correction)

When a mismatch is detected, a correction event is automatically published to Kafka topic `transactions.corrections` and consumed by the Shadow Ledger Service.

#### Manual Correction (Admin Role)

**Endpoint**: `POST /correct/{accountId}`

Allows administrators to manually generate correction events.

**First, login as admin to get token:**
```bash
curl --request POST \
  --url http://localhost:8080/auth/token \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": "admin1",
    "password": "password"
  }'
```

**Then submit manual correction:**
```bash
curl --request POST \
  --url http://localhost:8080/correct/A10 \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer <ADMIN_JWT_TOKEN>' \
  --data '{
    "type": "credit",
    "amount": 50.00,
    "reason": "Manual adjustment for reconciliation discrepancy"
  }'
```

**Response:**
```json
{
  "message": "Correction event published successfully",
  "eventId": "CORR-A10-abc123def",
  "accountId": "A10",
  "type": "credit",
  "amount": 50.00
}
```
  "message": "Correction event published successfully",
  "eventId": "CORR-A10-abc123def",
  "accountId": "A10",
  "type": "credit",
  "amount": 50.00
}
```

## 🗂️ Project Structure

```
ShadowLedgerSystem/
├── api-gateway/                      # API Gateway Service
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/Api_Gateway/
│   │       │   ├── config/           # SecurityConfig, RouteConfig, TraceIdFilter
│   │       │   ├── controller/       # AuthController, proxy controllers
│   │       │   ├── entity/           # UserAccount
│   │       │   ├── repository/       # UserAccountRepository (JDBC)
│   │       │   ├── service/          # AuthService
│   │       │   └── util/             # JwtUtil
│   │       └── resources/
│   │           ├── application.yaml  # Port 8080, DB config, JWT secret
│   │           └── schema.sql        # Users table DDL
│   ├── build.gradle
│   └── Dockerfile
│
├── event-service/                    # Event Service
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/event_service/
│   │       │   ├── config/           # KafkaProducerConfig, SecurityConfig
│   │       │   ├── controller/       # EventController
│   │       │   ├── model/            # Event
│   │       │   └── repository/       # EventRepository (JPA)
│   │       └── resources/
│   │           ├── application.yaml  # Port 8085, Kafka, DB config
│   │           └── schema.sql        # Events table DDL
│   ├── build.gradle
│   └── Dockerfile
│
├── shadow-ledger-service/            # Shadow Ledger Service
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/shadow_ledger_service/
│   │       │   ├── config/           # KafkaConsumerConfig, SecurityConfig
│   │       │   ├── controller/       # ShadowBalanceController
│   │       │   ├── model/            # LedgerEntry
│   │       │   ├── repository/       # LedgerEntryRepository (JPA + custom SQL)
│   │       │   └── service/          # LedgerEventConsumerService
│   │       └── resources/
│   │           ├── application.yaml  # Port 8086, Kafka, DB config
│   │           └── schema.sql        # Ledger table DDL
│   ├── build.gradle
│   └── Dockerfile
│
├── drift-correction-service/         # Drift and Correction Service
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/drift_correction_service/
│   │       │   ├── config/           # KafkaProducerConfig, SecurityConfig
│   │       │   ├── controller/       # DriftCheckController, CorrectionController
│   │       │   ├── model/            # CBSBalanceEntry, CorrectionEvent, DriftResult
│   │       │   ├── repository/       # LedgerEntryRepository (read-only)
│   │       │   └── service/          # DriftService
│   │       └── resources/
│   │           └── application.yaml  # Port 8087, Kafka, DB config
│   ├── build.gradle
│   └── Dockerfile
│
├── scripts/
│   └── run-acceptance.sh             # Automated acceptance tests
│
├── docker-compose.yml                # Infrastructure: PostgreSQL, Kafka, Zookeeper, Kafka UI
├── ordering-rules.md                 # Event ordering documentation
├── correction-strategy.md            # Drift correction strategy
├── api-specs.yaml                    # OpenAPI 3.0 specification
└── README.md                         # This file
```

## 📊 Database Schema

### Users Table (API Gateway)
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('user', 'auditor', 'admin')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Events Table (Event Service)
```sql
CREATE TABLE events (
    event_id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('debit', 'credit')),
    amount BIGINT NOT NULL CHECK (amount > 0),
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Ledger Table (Shadow Ledger Service)
```sql
CREATE TABLE ledger (
    event_id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('debit', 'credit')),
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 🔄 Event Flow

### Regular Event Flow
```
1. Client → API Gateway (JWT Auth + RBAC)
             ↓
2. Event Service
     - Validate event structure
     - Check for duplicate eventId
     - Store in PostgreSQL
             ↓
3. Kafka Producer → transactions.raw topic
             ↓
4. Shadow Ledger Service (Kafka Consumer)
     - Consume from transactions.raw
     - Deduplicate using eventId
     - Apply deterministic ordering (timestamp, eventId)
     - Validate no negative balances
     - Insert into immutable ledger table
     - Compute balance using SQL window function
```

### Drift Detection and Correction Flow
```
1. Auditor → API Gateway → Drift Service
     - Receives CBS balance report
     - Queries Shadow Ledger balance from PostgreSQL
     - Compares CBS vs Shadow
             ↓
2. If mismatch detected:
     - Generate correction event
     - Publish to transactions.corrections topic
             ↓
3. Shadow Ledger Service (Kafka Consumer)
     - Consumes from transactions.corrections
     - Applies same processing as regular events
     - Updates balance automatically
```

### Kafka Topics
- **transactions.raw**: Regular financial events from Event Service
- **transactions.corrections**: Correction events from Drift Service

## 🧪 Testing

### Unit and Integration Tests

Each service has its own test suite. Run tests for individual services:

```bash
# API Gateway
cd api-gateway
./gradlew test

# Event Service
cd event-service
./gradlew test

# Shadow Ledger Service
cd shadow-ledger-service
./gradlew test

# Drift Correction Service
cd drift-correction-service
./gradlew test
```

### Run All Tests
```bash
# From project root
./gradlew test
```

### Automated Acceptance Tests

The `run-acceptance.sh` script performs end-to-end testing:

1. Creates test users (user, auditor, admin)
2. Submits multiple financial events
3. Queries shadow balances
4. Performs drift detection
5. Validates correction events

```bash
# Make executable (first time only)
chmod +x scripts/run-acceptance.sh

# Run acceptance tests
./scripts/run-acceptance.sh
```

**Note**: Ensure all services are running before executing acceptance tests.

### Test Coverage

The test suites cover:
- ✅ Event validation (invalid events rejected)
- ✅ SQL window functions (balance computation)
- ✅ Drift detection (mismatch identification)
- ✅ Correction event generation
- ✅ JWT authentication and RBAC
- ✅ Kafka message production and consumption
- ✅ Idempotency (duplicate event handling)

## 📈 Monitoring and Observability

### Health Endpoints
All services expose health checks via Spring Boot Actuator:

```bash
# API Gateway
curl http://localhost:8080/actuator/health

# Event Service
curl http://localhost:8085/actuator/health

# Shadow Ledger Service
curl http://localhost:8086/actuator/health

# Drift Correction Service
curl http://localhost:8087/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### Metrics Endpoints
Access detailed metrics for each service:

```bash
# API Gateway
curl http://localhost:8080/actuator/metrics

# Event Service
curl http://localhost:8085/actuator/metrics

# Shadow Ledger Service
curl http://localhost:8086/actuator/metrics

# Drift Correction Service
curl http://localhost:8087/actuator/metrics
```

### Available Metrics
- `jvm.memory.used` - Memory usage
- `jvm.threads.live` - Active threads
- `http.server.requests` - Request count and latency
- `kafka.consumer.fetch.total` - Kafka consumer metrics
- `kafka.producer.record.send.total` - Kafka producer metrics

### Kafka UI
Monitor Kafka topics, messages, and consumer groups:

```
http://localhost:8090
```

Features:
- View all topics (transactions.raw, transactions.corrections)
- Browse messages
- Monitor consumer lag
- View broker health

### Logs
Each service logs to stdout with the following format:
```
[<timestamp>] [<trace-id>] [<log-level>] [<class-name>] - <message>
```

View logs in terminal where services are running or check IntelliJ console.

### Distributed Tracing
The API Gateway adds `X-Trace-Id` header to all requests, which is propagated through all services for request tracing.

## 🐛 Troubleshooting

### Services Not Starting

**Issue**: Service fails to start

**Solution**:
```bash
# Check if port is already in use
lsof -i :8080  # or :8085, :8086, :8087

# Check Java version
java -version  # Must be Java 21

# Clean and rebuild
./gradlew clean build
```

### Infrastructure Not Running

**Issue**: PostgreSQL or Kafka not available

**Solution**:
```bash
# Check Docker containers
docker-compose ps

# Restart infrastructure
docker-compose down
docker-compose up -d

# View container logs
docker-compose logs postgres
docker-compose logs kafka
```

### Kafka Connection Issues

**Issue**: Services can't connect to Kafka

**Solution**:
```bash
# Check Kafka is running
docker-compose ps kafka

# Check Kafka topics exist
docker exec -it shadow-kafka kafka-topics --list --bootstrap-server localhost:9092

# Create topics manually if needed
docker exec -it shadow-kafka kafka-topics --create --topic transactions.raw --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
docker exec -it shadow-kafka kafka-topics --create --topic transactions.corrections --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### Database Connection Issues

**Issue**: Can't connect to PostgreSQL

**Solution**:
```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Test connection
docker exec -it shadow-postgres psql -U postgres -d shadow_ledger

# Check if tables exist
docker exec -it shadow-postgres psql -U postgres -d shadow_ledger -c "\dt"

# View users table
docker exec -it shadow-postgres psql -U postgres -d shadow_ledger -c "SELECT user_id, username, role FROM users;"
```

### Port Already Allocated Error

**Issue**: `Bind for 0.0.0.0:5432 failed: port is already allocated`

**Solution**:
```bash
# Stop local PostgreSQL if running
brew services stop postgresql  # macOS
sudo systemctl stop postgresql  # Linux

# Or use different port in docker-compose.yml (already configured to 5434)
# The services are configured to use port 5434 externally
```

### 401 Unauthorized Error

**Issue**: Getting 401 on API calls

**Solution**:
1. Ensure you've signed up and logged in
2. Check JWT token is valid and not expired (1 hour expiration)
3. Include `Authorization: Bearer <TOKEN>` header
4. Verify token format starts with "eyJ"

### 403 Forbidden Error

**Issue**: Getting 403 on API calls

**Solution**:
1. Check user has correct role for endpoint:
   - `/events` → requires `user` role
   - `/drift-check` → requires `auditor` role
   - `/correct/{accountId}` → requires `admin` role
2. Login with correct user role
3. Verify JWT contains correct role claim

### SSL Connect Error in Insomnia

**Issue**: `Error: SSL connect error` when using https://localhost:8080

**Solution**:
Use `http://` instead of `https://`:
```
http://localhost:8080/auth/signup
```

The services are not configured with SSL certificates.

## 📚 Additional Documentation

- **[Event Ordering Rules](./ordering-rules.md)** - Deterministic ordering and immutability
- **[Correction Strategy](./correction-strategy.md)** - Drift detection and reconciliation
- **[API Specifications](./api-specs.yaml)** - OpenAPI 3.0 documentation

## 🚀 Getting Started Quick Reference

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Start services (4 separate terminals or run in IntelliJ)
cd api-gateway && ./gradlew bootRun
cd event-service && ./gradlew bootRun
cd shadow-ledger-service && ./gradlew bootRun
cd drift-correction-service && ./gradlew bootRun

# 3. Login with default user (user1/password)
curl --request POST \
  --url http://localhost:8080/auth/token \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": "user1",
    "username": "demouser",
    "password": "password"
  }'

# Response: {"token": "eyJhbGc..."}
# Copy the token from the response

# 4. Submit an event (replace <TOKEN> with actual JWT token)
curl --request POST \
  --url http://localhost:8080/events \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer <TOKEN>' \
  --data '{
    "eventId": "E1001",
    "accountId": "A10",
    "type": "credit",
    "amount": 500
  }'

# 5. Query balance
curl --request GET \
  --url http://localhost:8080/accounts/A10/shadow-balance \
  --header 'Authorization: Bearer <TOKEN>'

# 6. Login as auditor for drift check
curl --request POST \
  --url http://localhost:8080/auth/token \
  --header 'Content-Type: application/json' \
  --data '{
    "userId": "auditor1",
    "username": "demoauditor",
    "password": "password"
  }'

# 7. Run drift check with auditor token
curl --request POST \
  --url http://localhost:8080/drift-check \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Bearer <AUDITOR_TOKEN>' \
  --data '[{"accountId": "A10", "reportedBalance": 500.00}]'
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is for educational purposes as part of the Shadow Ledger System assignment.

## 👥 Authors

- **GKAUSHIK** - Shadow Ledger System Implementation

## 📞 Support

For issues, questions, or contributions:
- Check the troubleshooting section above
- Review the API documentation in `api-specs.yaml`
- Inspect service logs for detailed error messages

---

*Last Updated: December 2025*
