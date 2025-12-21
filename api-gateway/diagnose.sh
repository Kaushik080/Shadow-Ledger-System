#!/bin/bash

echo "=========================================="
echo "API Gateway Diagnostics"
echo "=========================================="
echo ""

echo "1. Checking if PostgreSQL is running..."
PG_5432=$(lsof -i :5432 2>/dev/null)
PG_5434=$(lsof -i :5434 2>/dev/null)

if [ -n "$PG_5432" ]; then
    echo "✓ PostgreSQL is running on port 5432"
    echo "$PG_5432"
elif [ -n "$PG_5434" ]; then
    echo "✓ PostgreSQL is running on port 5434"
    echo "$PG_5434"
    echo ""
    echo "⚠ WARNING: Your PostgreSQL is on port 5434, but the default is 5432."
    echo "   Run the application with: DB_PORT=5434 ./gradlew bootRun"
else
    echo "✗ PostgreSQL is NOT running"
    echo "   Start it with: docker-compose up -d postgres"
fi

echo ""
echo "2. Checking if API Gateway is running..."
API_8080=$(lsof -i :8080 2>/dev/null)
API_8085=$(lsof -i :8085 2>/dev/null)

if [ -n "$API_8080" ]; then
    echo "✓ API Gateway is running on port 8080"
    echo "$API_8080"
elif [ -n "$API_8085" ]; then
    echo "✓ API Gateway is running on port 8085"
    echo "$API_8085"
else
    echo "✗ API Gateway is NOT running"
    echo "   Start it with: ./gradlew bootRun"
fi

echo ""
echo "3. Checking Docker containers..."
DOCKER_PS=$(docker ps 2>/dev/null)
if [ $? -eq 0 ]; then
    echo "$DOCKER_PS" | grep -E "CONTAINER|postgres|api-gateway" || echo "No relevant containers running"
else
    echo "✗ Docker is not accessible or not running"
fi

echo ""
echo "4. Testing API Gateway health..."
if [ -n "$API_8080" ]; then
    HEALTH=$(curl -s http://localhost:8080/actuator/health 2>/dev/null)
    if [ -n "$HEALTH" ]; then
        echo "✓ API Gateway health check successful:"
        echo "$HEALTH"
    else
        echo "✗ API Gateway health check failed"
    fi
elif [ -n "$API_8085" ]; then
    HEALTH=$(curl -s http://localhost:8085/actuator/health 2>/dev/null)
    if [ -n "$HEALTH" ]; then
        echo "✓ API Gateway health check successful:"
        echo "$HEALTH"
    else
        echo "✗ API Gateway health check failed"
    fi
else
    echo "⊘ Skipped (API Gateway not running)"
fi

echo ""
echo "=========================================="
echo "Summary:"
echo "=========================================="

if [ -z "$PG_5432" ] && [ -z "$PG_5434" ]; then
    echo "⚠ START POSTGRESQL: docker-compose up -d postgres"
fi

if [ -z "$API_8080" ] && [ -z "$API_8085" ]; then
    echo "⚠ START API GATEWAY: ./gradlew bootRun"
fi

if [ -n "$PG_5434" ]; then
    echo "⚠ SET DB_PORT: DB_PORT=5434 ./gradlew bootRun"
fi

echo ""
echo "To test signup:"
echo 'curl -X POST http://localhost:8080/auth/signup \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{"userId":"user123","username":"John Doe","password":"SecurePass123!","role":"user"}'"'"
echo ""

