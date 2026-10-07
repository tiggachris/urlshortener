# Multi-stage Dockerfile: Unified Frontend + Backend in a Single Service

# Stage 1: Build Frontend
FROM node:22-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend ./
RUN npm run build

# Stage 2: Build Spring Boot Backend with Embedded Frontend Static Assets
FROM eclipse-temurin:21-jdk-alpine AS backend-builder
WORKDIR /app/backend
COPY backend/mvnw backend/mvnw.cmd backend/pom.xml ./
COPY backend/.mvn .mvn
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B
COPY backend/src ./src
COPY --from=frontend-builder /app/backend/src/main/resources/static ./src/main/resources/static
RUN ./mvnw clean package -DskipTests

# Stage 3: Production JRE Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=backend-builder /app/backend/target/urlshortener-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
