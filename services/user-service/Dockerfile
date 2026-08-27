# syntax=docker/dockerfile:1

# ── Build stage ────────────────────────────────────────────────────────────────
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /build

# Install parent POM into local Maven cache (non-recursive)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn install -N -B

# Resolve service dependencies (layer cache)
WORKDIR /build/services/user-service
COPY services/user-service/pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

COPY services/user-service/src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=build /build/services/user-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
