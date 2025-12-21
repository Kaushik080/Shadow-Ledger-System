n the i#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Shadow Ledger Service - Quick Start${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}Error: Docker is not running. Please start Docker Desktop.${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 1: Building and starting all services...${NC}"
docker-compose up --build -d

echo ""
echo -e "${YELLOW}Step 2: Waiting for services to be healthy...${NC}"
sleep 5

# Wait for PostgreSQL
echo -n "Waiting for PostgreSQL..."
for i in {1..30}; do
    if docker exec shadow-postgres pg_isready -U postgres > /dev/null 2>&1; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# Wait for Kafka
echo -n "Waiting for Kafka..."
for i in {1..30}; do
    if docker exec shadow-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# Wait for Shadow Ledger Service
echo -n "Waiting for Shadow Ledger Service..."
for i in {1..60}; do
    if curl -f http://localhost:8086/actuator/health > /dev/null 2>&1; then
        echo -e " ${GREEN}✓${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All services are up and running!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "📊 ${YELLOW}Kafka UI:${NC}           http://localhost:8090"
echo -e "🏥 ${YELLOW}Health Check:${NC}       http://localhost:8086/actuator/health"
echo -e "📈 ${YELLOW}Metrics:${NC}            http://localhost:8086/actuator/metrics"
echo -e "🗄️  ${YELLOW}PostgreSQL:${NC}         localhost:5434"
echo ""
echo -e "${YELLOW}View logs:${NC}           docker-compose logs -f"
echo -e "${YELLOW}Stop services:${NC}       docker-compose down"
echo -e "${YELLOW}Clean restart:${NC}       docker-compose down -v && ./quick-start.sh"
echo ""
echo -e "${GREEN}Happy coding! 🚀${NC}"

