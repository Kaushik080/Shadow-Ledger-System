# Configuration Summary - Shadow Ledger Service

## ✅ Changes Made to Fix Kafka Connection Issues

### 1. **Application Configuration (application.yaml)**
   - **Kafka Bootstrap Server**: Changed to `kafka:29092` (Docker internal network)
   - **Database URL**: Made configurable via environment variables
   - **Logging**: Set to DEBUG for Kafka troubleshooting
   
   ```yaml
   kafka:
     bootstrap-servers: ${KAFKA_BOOTSTRAP:kafka:29092}
   ```

### 2. **Custom JSON Deserializer (CustomJsonDeserializer.java)**
   - Created to replace deprecated Spring Kafka JsonDeserializer
   - Uses Jackson ObjectMapper directly
   - Auto-registers JavaTimeModule for Java 8 date/time support
   - Includes error handling and logging

### 3. **Docker Configuration**
   - **docker-compose.yml**: Complete setup with all services
   - **Dockerfile**: Optimized multi-stage build with curl for healthchecks
   - **.dockerignore**: Excludes unnecessary files from build

### 4. **Kafka Consumer Config (KafkaConsumerConfig.java)**
   - Uses `CustomJsonDeserializer<LedgerEvent>` wrapped with `ErrorHandlingDeserializer`
   - Configures timeouts and polling intervals
   - Type-safe configuration

### 5. **LedgerEvent Model (LedgerEvent.java)**
   - Added `@JsonIgnoreProperties(ignoreUnknown = true)`
   - Added `@JsonProperty` annotations for explicit mapping

## 🔧 Docker Network Configuration

### Kafka Listeners
```yaml
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,PLAINTEXT_HOST://0.0.0.0:9092
```

- **kafka:29092** - For containers in the same Docker network (shadow-net)
- **localhost:9092** - For host machine access

### Service Network
All services are on the `shadow-net` bridge network:
- postgres (container: postgres:5432, host: localhost:5434)
- zookeeper (container: zookeeper:2181, host: localhost:2181)
- kafka (container: kafka:29092, host: localhost:9092)
- kafka-ui (uses: kafka:29092)
- shadow-ledger-service (uses: kafka:29092, postgres:5432)

## 🚀 How to Run

### Option 1: Quick Start (Recommended)
```bash
chmod +x quick-start.sh
./quick-start.sh
```

### Option 2: Manual Docker Compose
```bash
# Build and start
docker-compose up --build -d

# View logs
docker-compose logs -f shadow-ledger-service

# Stop
docker-compose down
```

### Option 3: Local Development
```bash
# Start infrastructure only
docker-compose up postgres zookeeper kafka kafka-ui -d

# Run app locally
KAFKA_BOOTSTRAP=localhost:9092 ./gradlew bootRun
```

## 🔍 Verification Steps

1. **Check all containers are running:**
   ```bash
   docker ps
   ```
   Expected: 5 containers (postgres, zookeeper, kafka, kafka-ui, shadow-ledger-service)

2. **Check Kafka UI:**
   - Open http://localhost:8090
   - Verify "shadow-ledger-cluster" shows as "online"

3. **Check application health:**
   ```bash
   curl http://localhost:8086/actuator/health
   ```
   Expected: `{"status":"UP"}`

4. **Check logs:**
   ```bash
   docker-compose logs shadow-ledger-service
   ```
   Look for: "Started ShadowLedgerServiceApplication"

## 🐛 Troubleshooting

### Issue: Kafka connection refused
**Solution:** 
- If running in Docker: Use `kafka:29092`
- If running on host: Use `localhost:9092`

### Issue: Database connection error
**Solution:**
- If running in Docker: Use `postgres:5432`
- If running on host: Use `localhost:5434`

### Issue: Kafka UI shows offline
**Solution:**
```bash
docker-compose restart kafka kafka-ui
# Or rebuild
docker-compose up --build kafka kafka-ui -d
```

### Issue: Application fails to start
**Solution:**
```bash
# Check logs
docker-compose logs shadow-ledger-service

# Rebuild with no cache
docker-compose build --no-cache shadow-ledger-service
docker-compose up -d shadow-ledger-service
```

## 📊 Testing the Setup

### 1. Produce a test message to Kafka
```bash
# Access Kafka container
docker exec -it shadow-kafka bash

# Create a test message
kafka-console-producer --bootstrap-server localhost:9092 --topic transactions.raw

# Paste this JSON:
{"eventId":"test-001","accountId":"acc-123","type":"credit","amount":100.00,"timestamp":1702656000000}
```

### 2. Check if message was consumed
```bash
# Check application logs
docker-compose logs shadow-ledger-service | grep "Received raw transaction"

# Check database
docker exec -it shadow-postgres psql -U postgres -d shadow_ledger -c "SELECT * FROM ledger;"
```

### 3. Query balance via API
```bash
curl http://localhost:8086/api/shadow/balance/acc-123
```

## 📝 Key Points

1. **Always use Docker internal hostnames** when services communicate within Docker network
2. **Use localhost** only when accessing from your host machine
3. **Kafka has two listeners**: one for Docker (29092) and one for host (9092)
4. **CustomJsonDeserializer** resolves Spring Boot 4.0 deprecation warnings
5. **Environment variables** make the app flexible for Docker and local development

## 🎯 Next Steps

1. Run `./quick-start.sh` to start everything
2. Open Kafka UI at http://localhost:8090
3. Produce test messages to `transactions.raw` topic
4. Check application logs to see messages being consumed
5. Query the API to verify data is persisted

---

**Status**: ✅ All configuration issues resolved. Ready to run!

