# ── Build stage ────────────────────────────────────────────────────────────────
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /build

# Install parent POM into local Maven cache (non-recursive)
COPY pom.xml .
RUN mvn install -N -B

# Resolve service dependencies (layer cache)
WORKDIR /build/services/payment-service
COPY services/payment-service/pom.xml .
RUN mvn dependency:go-offline -B

COPY services/payment-service/src ./src
RUN mvn package -DskipTests -B

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=build /build/services/payment-service/target/*.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
