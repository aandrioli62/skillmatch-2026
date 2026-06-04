# SkillMatch — Architecture

> **Course**: Progettazione di Architetture di Servizi  
> **University**: Università del Salento — Prof. Luca Mainetti — A.Y. 2025/26

## Overview

SkillMatch is a microservice-based platform that connects professionals with companies for short-term micro-projects (training, consulting, prototyping).

## Design Patterns Applied

| Pattern | Where |
|---|---|
| Microservice Architecture | Overall system decomposition |
| Client-Server | React SPA ↔ API Gateway |
| REST | All HTTP inter-service communication |
| API Gateway | Spring Cloud Gateway (port 8080) |
| Pub/Sub | RabbitMQ topic exchange `skillmatch.events` |
| Database per Service | Each service owns one logical PostgreSQL DB (or MongoDB) |
| Circuit Breaker | Resilience4j on API Gateway and inter-service calls |
| 3-Tier Architecture | controller / service / repository inside each microservice |
| Externalized Configuration | Env vars, ConfigMaps, Secrets — 12-Factor III |

## Component Diagram

```
  Browser / Mobile
       │  HTTPS
       ▼
  ┌──────────────┐
  │  API Gateway │  ← JWT validation, routing, circuit breaker
  │   :8080      │
  └──────┬───────┘
         │  HTTP (ClusterIP)
   ┌─────┴──────────────────────────────────────────┐
   │                                                │
   ▼                                                ▼
user-service     project-service    contract-service
   :8081              :8082              :8083
   │                  │
   │      payment-service  feedback-service  notification-service
   │           :8084           :8085              :8086
   │
   ▼ RabbitMQ topic exchange: skillmatch.events
   ┌─────────────────────┐
   │      RabbitMQ       │
   │  AMQP :5672         │
   │  Management :15672  │
   └─────────────────────┘
   
   ┌───────────────────────┐    ┌─────────────────┐
   │  PostgreSQL :5432     │    │  MongoDB :27017  │
   │  7 logical databases  │    │  notification-db │
   └───────────────────────┘    └─────────────────┘

   ┌──────────────────────┐
   │  Keycloak :8180      │
   │  Realm: skillmatch   │
   └──────────────────────┘
```

## Event Flow

| Event | Producer | Consumers |
|---|---|---|
| `project.published` | project-service | notification-service |
| `candidature.accepted` | project-service | contract-service, notification-service |
| `project.completed` | project-service | payment-service, contract-service |
| `payment.completed` | payment-service | feedback-service, notification-service |
| `feedback.submitted` | feedback-service | user-service |

## Infrastructure

- **Local dev**: `cd infra && docker compose up`
- **Production**: K3s on Oracle Cloud ARM VM (4 OCPU, 24 GB RAM)
- **CI/CD**: GitHub Actions — path-filtered per service → build → test → Docker buildx arm64 → push GHCR → kubectl rollout
- **Observability**: Spring Boot Actuator + Prometheus + Grafana (metrics), Loki + Promtail (logs)
