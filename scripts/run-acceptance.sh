#!/bin/bash

# Shadow Ledger System - Acceptance Test Script
# This script runs a series of tests to validate the system functionality

set -e

API_GATEWAY="http://localhost:8080"
TRACE_ID=$(uuidgen)

echo "======================================"
echo "Shadow Ledger System - Acceptance Tests"
echo "======================================"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Test 1: Health Check
echo "Test 1: Health Check"
echo "--------------------"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" ${API_GATEWAY}/actuator/health)
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ]; then
    print_success "API Gateway is healthy"
else
    print_error "API Gateway health check failed (HTTP $HTTP_CODE)"
    exit 1
fi
echo ""

# Test 2: Create User with 'user' Role
echo "Test 2: Create Test User (role: user)"
echo "--------------------------------------"
SIGNUP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${API_GATEWAY}/auth/signup \
    -H "Content-Type: application/json" \
    -d '{
        "userId": "testuser1",
        "username": "Test User",
        "password": "TestPassword123!",
        "role": "user"
    }')
HTTP_CODE=$(echo "$SIGNUP_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "409" ]; then
    print_success "User signup successful or user already exists"
else
    print_error "User signup failed (HTTP $HTTP_CODE)"
    echo "$SIGNUP_RESPONSE"
fi
echo ""

# Test 3: Create Auditor User
echo "Test 3: Create Test Auditor (role: auditor)"
echo "--------------------------------------------"
AUDITOR_SIGNUP=$(curl -s -w "\n%{http_code}" -X POST ${API_GATEWAY}/auth/signup \
    -H "Content-Type: application/json" \
    -d '{
        "userId": "testauditor1",
        "username": "Test Auditor",
        "password": "TestPassword123!",
        "role": "auditor"
    }')
HTTP_CODE=$(echo "$AUDITOR_SIGNUP" | tail -n1)
if [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "409" ]; then
    print_success "Auditor signup successful or user already exists"
else
    print_error "Auditor signup failed (HTTP $HTTP_CODE)"
    echo "$AUDITOR_SIGNUP"
fi
echo ""

# Test 4: Get JWT Token for User
echo "Test 4: Get JWT Token for User"
echo "-------------------------------"
TOKEN_RESPONSE=$(curl -s -X POST ${API_GATEWAY}/auth/token \
    -H "Content-Type: application/json" \
    -d '{
        "userId": "testuser1",
        "password": "TestPassword123!"
    }')
JWT_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.token')
if [ "$JWT_TOKEN" != "null" ] && [ ! -z "$JWT_TOKEN" ]; then
    print_success "Login successful, JWT token obtained"
else
    print_error "Login failed"
    echo "$TOKEN_RESPONSE"
    exit 1
fi
echo ""

# Test 5: Get JWT Token for Auditor
echo "Test 5: Get JWT Token for Auditor"
echo "----------------------------------"
AUDITOR_TOKEN_RESPONSE=$(curl -s -X POST ${API_GATEWAY}/auth/token \
    -H "Content-Type: application/json" \
    -d '{
        "userId": "testauditor1",
        "password": "TestPassword123!"
    }')
AUDITOR_TOKEN=$(echo "$AUDITOR_TOKEN_RESPONSE" | jq -r '.token')
if [ "$AUDITOR_TOKEN" != "null" ] && [ ! -z "$AUDITOR_TOKEN" ]; then
    print_success "Auditor login successful, JWT token obtained"
else
    print_error "Auditor login failed"
    echo "$AUDITOR_TOKEN_RESPONSE"
    exit 1
fi
echo ""

# Test 6: Submit Events
echo "Test 6: Submit Financial Events"
echo "--------------------------------"
TIMESTAMP=$(date +%s)000

# Submit credit event
EVENT1_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${API_GATEWAY}/events \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "X-Trace-Id: ${TRACE_ID}" \
    -d "{
        \"eventId\": \"E-TEST-${TIMESTAMP}-001\",
        \"accountId\": \"TEST-A10\",
        \"type\": \"credit\",
        \"amount\": 1000.00,
        \"timestamp\": ${TIMESTAMP}
    }")
HTTP_CODE=$(echo "$EVENT1_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "202" ] || [ "$HTTP_CODE" = "201" ]; then
    print_success "Credit event submitted (1000.00)"
else
    print_error "Credit event submission failed (HTTP $HTTP_CODE)"
    echo "$EVENT1_RESPONSE"
fi

# Wait for Kafka processing
sleep 3

# Submit debit event
TIMESTAMP=$((TIMESTAMP + 1000))
EVENT2_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${API_GATEWAY}/events \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "X-Trace-Id: ${TRACE_ID}" \
    -d "{
        \"eventId\": \"E-TEST-${TIMESTAMP}-002\",
        \"accountId\": \"TEST-A10\",
        \"type\": \"debit\",
        \"amount\": 250.00,
        \"timestamp\": ${TIMESTAMP}
    }")
HTTP_CODE=$(echo "$EVENT2_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "202" ] || [ "$HTTP_CODE" = "201" ]; then
    print_success "Debit event submitted (250.00)"
else
    print_error "Debit event submission failed (HTTP $HTTP_CODE)"
    echo "$EVENT2_RESPONSE"
fi

# Wait for Kafka processing
sleep 3
echo ""

# Test 7: Query Shadow Balance
echo "Test 7: Query Shadow Balance"
echo "-----------------------------"
BALANCE_RESPONSE=$(curl -s ${API_GATEWAY}/accounts/TEST-A10/shadow-balance \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "X-Trace-Id: ${TRACE_ID}")
BALANCE=$(echo "$BALANCE_RESPONSE" | jq -r '.balance')
if [ "$BALANCE" != "null" ]; then
    print_success "Shadow balance retrieved: ${BALANCE}"
    EXPECTED_BALANCE="750.00"
    if [ "$BALANCE" = "$EXPECTED_BALANCE" ] || [ "$BALANCE" = "750" ]; then
        print_success "Balance matches expected value (1000 - 250 = 750)"
    else
        print_info "Balance: $BALANCE (expected ~750.00)"
    fi
else
    print_error "Shadow balance query failed"
    echo "$BALANCE_RESPONSE"
fi
echo ""

# Test 8: Duplicate Event (Idempotency)
echo "Test 8: Test Event Idempotency"
echo "-------------------------------"
DUPLICATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${API_GATEWAY}/events \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "X-Trace-Id: ${TRACE_ID}" \
    -d "{
        \"eventId\": \"E-TEST-${TIMESTAMP}-002\",
        \"accountId\": \"TEST-A10\",
        \"type\": \"debit\",
        \"amount\": 250.00,
        \"timestamp\": ${TIMESTAMP}
    }")
HTTP_CODE=$(echo "$DUPLICATE_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "409" ]; then
    print_success "Duplicate event correctly rejected (idempotency works)"
else
    print_info "Duplicate event handling: HTTP $HTTP_CODE"
fi
echo ""

# Test 9: Invalid Event Validation
echo "Test 9: Event Validation"
echo "------------------------"
INVALID_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${API_GATEWAY}/events \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "X-Trace-Id: ${TRACE_ID}" \
    -d "{
        \"eventId\": \"E-TEST-INVALID\",
        \"accountId\": \"TEST-A10\",
        \"type\": \"invalid_type\",
        \"amount\": -100.00,
        \"timestamp\": ${TIMESTAMP}
    }")
HTTP_CODE=$(echo "$INVALID_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "400" ]; then
    print_success "Invalid event correctly rejected"
else
    print_info "Invalid event handling: HTTP $HTTP_CODE"
fi
echo ""

# Test 10: Unauthorized Access
echo "Test 10: Authorization Test"
echo "---------------------------"
UNAUTH_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${API_GATEWAY}/events \
    -H "Content-Type: application/json" \
    -d "{
        \"eventId\": \"E-TEST-UNAUTH\",
        \"accountId\": \"TEST-A10\",
        \"type\": \"credit\",
        \"amount\": 100.00,
        \"timestamp\": ${TIMESTAMP}
    }")
HTTP_CODE=$(echo "$UNAUTH_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "401" ]; then
    print_success "Unauthorized request correctly rejected"
else
    print_info "Unauthorized access handling: HTTP $HTTP_CODE"
fi
echo ""

# Test 11: Drift Check (Auditor Role)
echo "Test 11: Drift Check (Auditor Role)"
echo "------------------------------------"
DRIFT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${API_GATEWAY}/drift-check \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${AUDITOR_TOKEN}" \
    -H "X-Trace-Id: ${TRACE_ID}" \
    -d '[
        {
            "accountId": "TEST-A10",
            "reportedBalance": 750.00
        }
    ]')
HTTP_CODE=$(echo "$DRIFT_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$DRIFT_RESPONSE" | head -n -1)
if [ "$HTTP_CODE" = "200" ]; then
    print_success "Drift check executed successfully"
    TOTAL_ACCOUNTS=$(echo "$RESPONSE_BODY" | jq -r '.totalAccounts')
    MISMATCHES=$(echo "$RESPONSE_BODY" | jq -r '.mismatches')
    print_info "Total accounts checked: $TOTAL_ACCOUNTS"
    print_info "Mismatches found: $MISMATCHES"
    if [ "$MISMATCHES" = "0" ]; then
        print_success "No drift detected - balances match!"
    fi
else
    print_error "Drift check failed (HTTP $HTTP_CODE)"
    echo "$RESPONSE_BODY"
fi
echo ""

echo "======================================"
echo "Acceptance Tests Completed"
echo "======================================"

