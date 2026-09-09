# Multi-stage Dockerfile for FactoryOS Backend

# Stage 1: Build JAR with Maven & Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper & POM
COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -B

# Copy source code and build
COPY src src
RUN mvn clean package -DskipTests -B

# Stage 2: One-shot Flyway Database Migration
FROM eclipse-temurin:21-jre-alpine AS migration
WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar
ENTRYPOINT ["java", "-Dspring.main.web-application-type=none", "-Dspring.flyway.enabled=true", "-jar", "app.jar"]

# Stage 3: Runtime Backend Service
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S factoryos && adduser -S factoryos -G factoryos
USER factoryos:factoryos

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
