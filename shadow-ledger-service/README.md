# Shadow Ledger Service

A Spring Boot service for maintaining an immutable shadow ledger of financial transactions, consuming events from Kafka.

## Architecture

- **Spring Boot 4.0** with Java 21
- **Apache Kafka** for event streaming
- **PostgreSQL** for persistent storage
- **Custom JSON Deserializer** (Spring Kafka 4.0 compatible)

## Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Gradle 8+ (included via wrapper)

## Running with Docker Compose

### 1. Start All Services

```bash
docker-compose up --build
```

This will start:
- **PostgreSQL** on port `5434` (host) / `5432` (container)
- **Zookeeper** on port `2181`
- **Kafka** on port `9092` (host) / `29092` (Docker network)
- **Kafka UI** on port `8090` - http://localhost:8090
- **Shadow Ledger Service** on port `8086`

### 2. View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f shadow-ledger-service
```

### 3. Stop Services

```bash
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## Running Locally (for Development)

### 1. Start Infrastructure Only

```bash
# Start only Postgres, Kafka, Zookeeper, and Kafka UI
docker-compose up postgres zookeeper kafka kafka-ui
```

### 2. Run the Application

```bash
./gradlew bootRun
```

Or with custom properties:
```bash
KAFKA_BOOTSTRAP=localhost:9092 \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/shadow_ledger \
./gradlew bootRun
```

## Configuration

### Environment Variables

| Variable | Description | Default (Docker) | Default (Local) |
|----------|-------------|------------------|-----------------|
| `KAFKA_BOOTSTRAP` | Kafka bootstrap servers | `kafka:29092` | `localhost:9092` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://postgres:5432/shadow_ledger` | `jdbc:postgresql://localhost:5434/shadow_ledger` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` | `postgres` |
| `SERVER_PORT` | Application port | `8086` | `8086` |

### Kafka Topics

- `transactions.raw` - Raw transaction events
- `transactions.corrections` - Correction transaction events

Topics are auto-created by Kafka.

## API Endpoints

### Health Check
```bash
curl http://localhost:8086/actuator/health
```

### Get Account Balance
```bash
curl http://localhost:8086/api/shadow/balance/{accountId}
```

### Metrics
```bash
curl http://localhost:8086/actuator/metrics
```

## Kafka Integration

### Message Format

The service expects JSON messages in the following format:

```json
{
  "eventId": "evt-123",
  "accountId": "acc-456",
  "type": "credit",
  "amount": 100.50,
  "timestamp": 1702656000000
}
```

### Producing Test Messages

Using Kafka CLI:
```bash
# Access Kafka container
docker exec -it shadow-kafka bash

# Produce a message
kafka-console-producer --bootstrap-server localhost:9092 --topic transactions.raw

# Then paste JSON (one per line):
{"eventId":"evt-001","accountId":"acc-123","type":"credit","amount":100.00,"timestamp":1702656000000}
```

Using Kafka UI:
1. Open http://localhost:8090
2. Navigate to Topics → `transactions.raw`
3. Click "Produce Message"
4. Paste JSON payload

## Database Schema

The service creates a `ledger` table:

```sql
CREATE TABLE ledger (
    event_id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Troubleshooting

### Application won't connect to Kafka

**Running in Docker:**
- Ensure `KAFKA_BOOTSTRAP=kafka:29092` (Docker internal network)
- Check: `docker logs shadow-kafka`

**Running locally:**
- Ensure `KAFKA_BOOTSTRAP=localhost:9092` (host machine)
- Verify Kafka is accessible: `nc -zv localhost 9092`

### Kafka UI shows cluster offline

- Update docker-compose.yml: `KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092`
- Restart: `docker-compose restart kafka-ui`

### Database connection errors

- Verify PostgreSQL is running: `docker ps | grep postgres`
- Check connection: `psql -h localhost -p 5434 -U postgres -d shadow_ledger`

### JsonDeserializer deprecation warnings

This project uses a custom `CustomJsonDeserializer` that replaces Spring Kafka's deprecated `JsonDeserializer` (deprecated in Spring Boot 4.0). This is production-ready and future-proof.

## Development

### Build

```bash
./gradlew clean build
```

### Run Tests

```bash
./gradlew test
```

### Create Docker Image

```bash
docker build -t shadow-ledger-service:latest .
```

## Monitoring

- **Kafka UI**: http://localhost:8090
- **Application Health**: http://localhost:8086/actuator/health
- **Application Metrics**: http://localhost:8086/actuator/metrics

## License

[Your License Here]

