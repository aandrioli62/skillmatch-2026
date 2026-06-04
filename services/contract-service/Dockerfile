# ── Build stage ────────────────────────────────────────────────────────────────
FROM --platform=linux/arm64 maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM --platform=linux/arm64 eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
