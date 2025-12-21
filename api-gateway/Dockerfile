# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Gradle wrapper and project files
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# Make gradlew executable and build the jar
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install curl for healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# App port
EXPOSE 8080

# Copy built jar
COPY --from=build /app/build/libs/*.jar /app/app.jar

# Environment overrides for service URIs and JWT secret
ENV JAVA_OPTS=""
ENV SECURITY_JWT_SECRET="super-secret-demo-key-please-change"
ENV SERVICES_EVENT_SERVICE="http://event-service:8085"
ENV SERVICES_DRIFT_SERVICE="http://drift-correction-service:8087"
ENV SERVICES_LEDGER_SERVICE="http://shadow-ledger-service:8086"

# Run the app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

# Run the app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

