#!/bin/bash

# Quick Start Script for API Gateway
# This script will set up the database and start the application

set -e

echo "🚀 API Gateway Quick Start"
echo "=========================="
echo ""

# Function to check if a command exists
command_exists() {
    command -v "$1" &> /dev/null
}

# Function to check if PostgreSQL is running
postgres_is_running() {
    if command_exists docker; then
        docker ps | grep -q "api-gateway-postgres" && return 0
    fi

    if command_exists psql; then
        psql -U postgres -c '\q' 2>/dev/null && return 0
    fi

    return 1
}

# Check for Docker
if command_exists docker && command_exists docker-compose; then
    echo "✅ Docker found"

    # Check if postgres container is running
    if docker ps | grep -q "api-gateway-postgres"; then
        echo "✅ PostgreSQL container is already running"
    else
        echo "🐘 Starting PostgreSQL container..."
        docker-compose up -d postgres

        echo "⏳ Waiting for PostgreSQL to be ready (15 seconds)..."
        sleep 15

        echo "✅ PostgreSQL container started"
    fi

    echo ""
    echo "📊 Database Status:"
    docker exec api-gateway-postgres psql -U shadow_user -d shadow_ledger -c "SELECT version();" 2>/dev/null || echo "⚠️  Database may still be initializing..."

elif command_exists psql; then
    echo "✅ PostgreSQL found locally"

    # Try to create database if it doesn't exist
    echo "🔍 Checking/creating database 'shadow_ledger'..."
    createdb -U postgres shadow_ledger 2>/dev/null || psql -U postgres -c "CREATE DATABASE shadow_ledger;" 2>/dev/null || echo "⚠️  Database may already exist"

    echo "✅ Database ready"

else
    echo "❌ Neither Docker nor local PostgreSQL found!"
    echo ""
    echo "Please install one of:"
    echo "  • Docker Desktop: https://www.docker.com/products/docker-desktop"
    echo "  • PostgreSQL: brew install postgresql@16"
    echo ""
    exit 1
fi

echo ""
echo "🎉 Database is ready!"
echo ""
echo "Now you can:"
echo "  1. Run the application from IntelliJ"
echo "  2. Or run: ./gradlew bootRun"
echo ""
echo "📝 Check DATABASE_SETUP.md for more details"

