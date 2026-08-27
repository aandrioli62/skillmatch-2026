# syntax=docker/dockerfile:1

# ── Build stage ────────────────────────────────────────────────────────────────
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /build

# Install parent POM into local Maven cache (non-recursive)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn install -N -B

# Resolve service dependencies (layer cache)
WORKDIR /build/services/api-gateway
COPY services/api-gateway/pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

COPY services/api-gateway/src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=build /build/services/api-gateway/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
