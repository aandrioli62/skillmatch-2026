# CLAUDE-SERVICES.md — Service Implementation Details

## Shared Dependencies (all Spring Boot services)

```xml
<!-- Core -->
spring-boot-starter-web
spring-boot-starter-data-jpa          (or spring-boot-starter-data-mongodb for Notification)
spring-boot-starter-validation
spring-boot-starter-actuator
spring-boot-starter-security
spring-boot-starter-oauth2-resource-server

<!-- Messaging -->
spring-boot-starter-amqp              (RabbitMQ)

<!-- Resilience -->
spring-cloud-starter-circuitbreaker-resilience4j

<!-- Database -->
postgresql                             (driver)
flyway-core                            (migrations)

<!-- Documentation -->
springdoc-openapi-starter-webmvc-ui

<!-- Testing -->
spring-boot-starter-test
testcontainers (postgres, rabbitmq)

<!-- Utilities -->
lombok
mapstruct                              (DTO mapping)
```

## application.yml Template (per service)

```yaml
spring:
  application:
    name: ${SERVICE_NAME}
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}
    username: ${DB_USERNAME:skillmatch}
    password: ${DB_PASSWORD:secret}
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway handles schema
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/skillmatch}
          jwk-set-uri: ${KEYCLOAK_JWK_URI:http://localhost:8180/realms/skillmatch/protocol/openid-connect/certs}

server:
  port: ${SERVER_PORT:8081}

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always

resilience4j:
  circuitbreaker:
    instances:
      default:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
```

## RabbitMQ Configuration (shared pattern)

```java
@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE = "skillmatch.events";

    @Bean
    public TopicExchange skillmatchExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // Each service declares its own queues and bindings
    // Example for Notification Service:
    @Bean
    public Queue projectPublishedQueue() {
        return new Queue("notification.project.published", true);
    }

    @Bean
    public Binding projectPublishedBinding(Queue projectPublishedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(projectPublishedQueue).to(exchange).with("project.published");
    }
}
```

## Event Payload Standard

```json
{
  "eventId": "uuid",
  "eventType": "project.published",
  "timestamp": "2026-01-15T10:30:00Z",
  "source": "project-service",
  "data": {
    "projectId": "uuid",
    "companyId": "uuid",
    "title": "Consulenza UI/UX",
    "requiredSkills": ["ui-design", "figma"]
  }
}
```

## Security Config Template (Resource Server)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/projects/**").hasRole("COMPANY")
                .requestMatchers(HttpMethod.POST, "/api/v1/candidatures/**").hasRole("PROFESSIONAL")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter()))
            );
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter conv = new JwtGrantedAuthoritiesConverter();
        conv.setAuthoritiesClaimName("realm_access.roles");
        conv.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter jwtConv = new JwtAuthenticationConverter();
        jwtConv.setJwtGrantedAuthoritiesConverter(conv);
        return jwtConv;
    }
}
```

## Database Schemas

### User Service (user-db)

```sql
-- V1__create_users.sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('PROFESSIONAL', 'COMPANY', 'ADMIN')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'VALIDATED', 'SUSPENDED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE professional_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    bio TEXT,
    payment_account VARCHAR(255),
    reputation_level VARCHAR(20) DEFAULT 'JUNIOR' CHECK (reputation_level IN ('JUNIOR', 'AFFIDABILE', 'TOP_PERFORMER')),
    avg_rating NUMERIC(3,2) DEFAULT 0,
    total_reviews INT DEFAULT 0
);

CREATE TABLE company_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id),
    company_name VARCHAR(255) NOT NULL,
    vat_number VARCHAR(50),
    address TEXT,
    contact_person VARCHAR(200)
);

CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) UNIQUE NOT NULL,
    category VARCHAR(100)
);

CREATE TABLE user_skills (
    user_id UUID NOT NULL REFERENCES users(id),
    skill_id UUID NOT NULL REFERENCES skills(id),
    certification_url VARCHAR(500),
    PRIMARY KEY (user_id, skill_id)
);

CREATE TABLE portfolio_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Project Service (project-db)

```sql
-- V1__create_projects.sql
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration_days INT,
    budget NUMERIC(10,2),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','OPEN','ASSIGNED','IN_PROGRESS','COMPLETED','CLOSED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE project_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    skill_name VARCHAR(100) NOT NULL,
    min_reputation_level VARCHAR(20)
);

CREATE TABLE candidatures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id),
    professional_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','REJECTED','WITHDRAWN')),
    cover_letter TEXT,
    applied_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, professional_id)
);
```

### Contract Service (contract-db)

```sql
-- V1__create_contracts.sql
CREATE TABLE contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    professional_id UUID NOT NULL,
    company_id UUID NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    commission_rate NUMERIC(5,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PENDING_SIGNATURES','ACTIVE','COMPLETED','CANCELLED')),
    terms TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    signed_at TIMESTAMP
);
```

### Payment Service (payment-db)

```sql
-- V1__create_transactions.sql
CREATE TABLE commission_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rate_percentage NUMERIC(5,2) NOT NULL DEFAULT 8.00,
    effective_from TIMESTAMP NOT NULL DEFAULT NOW(),
    set_by_admin_id UUID
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL,
    company_id UUID NOT NULL,
    professional_id UUID NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    commission_amount NUMERIC(10,2) NOT NULL,
    net_amount NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED' CHECK (status IN ('INITIATED','PROCESSING','COMPLETED','FAILED','REFUNDED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    company_id UUID NOT NULL,
    total NUMERIC(10,2) NOT NULL,
    commission NUMERIC(10,2) NOT NULL,
    professional_fee NUMERIC(10,2) NOT NULL,
    pdf_url VARCHAR(500),
    issued_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Feedback Service (feedback-db)

```sql
-- V1__create_feedbacks.sql
CREATE TABLE feedbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    reviewee_id UUID NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(project_id, reviewer_id, reviewee_id)
);
```

## Docker Compose (local development)

```yaml
# infra/docker-compose.yml
version: "3.9"

services:
  # --- Identity ---
  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    command: start-dev --http-port=8180
    environment:
      KC_DB: postgres
      KC_DB_URL_HOST: postgres
      KC_DB_URL_DATABASE: identitydb
      KC_DB_USERNAME: skillmatch
      KC_DB_PASSWORD: secret
      KC_HOSTNAME: identity
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8180:8180"
    depends_on:
      - postgres

  # --- Single PostgreSQL instance, multiple logical DBs ---
  postgres:
    image: postgres:16
    environment:
      POSTGRES_USER: skillmatch
      POSTGRES_PASSWORD: secret
      POSTGRES_DB: identitydb
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./init-databases.sql:/docker-entrypoint-initdb.d/init-databases.sql

  # --- MongoDB for Notification Service ---
  mongodb:
    image: mongo:7
    ports:
      - "27017:27017"
    volumes:
      - mongodb-data:/data/db

  # --- Message Broker ---
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  # --- API Gateway ---
  api-gateway:
    build:
      context: ../services/api-gateway
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      KEYCLOAK_ISSUER_URI: http://keycloak:8180/realms/skillmatch
      KEYCLOAK_JWK_URI: http://keycloak:8180/realms/skillmatch/protocol/openid-connect/certs
    depends_on:
      - keycloak

  # --- Business Services ---
  user-service:
    build:
      context: ../services/user-service
      dockerfile: Dockerfile
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: userdb
      DB_USERNAME: skillmatch
      DB_PASSWORD: secret
      RABBITMQ_HOST: rabbitmq
      KEYCLOAK_ISSUER_URI: http://keycloak:8180/realms/skillmatch
      KEYCLOAK_JWK_URI: http://keycloak:8180/realms/skillmatch/protocol/openid-connect/certs
      JAVA_OPTS: "-Xmx256m -Xms128m"
    depends_on:
      - postgres
      - rabbitmq
      - keycloak

  project-service:
    build:
      context: ../services/project-service
      dockerfile: Dockerfile
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: projectdb
      DB_USERNAME: skillmatch
      DB_PASSWORD: secret
      RABBITMQ_HOST: rabbitmq
      KEYCLOAK_ISSUER_URI: http://keycloak:8180/realms/skillmatch
      KEYCLOAK_JWK_URI: http://keycloak:8180/realms/skillmatch/protocol/openid-connect/certs
      JAVA_OPTS: "-Xmx256m -Xms128m"
    depends_on:
      - postgres
      - rabbitmq
      - keycloak

  contract-service:
    build:
      context: ../services/contract-service
      dockerfile: Dockerfile
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: contractdb
      DB_USERNAME: skillmatch
      DB_PASSWORD: secret
      RABBITMQ_HOST: rabbitmq
      KEYCLOAK_ISSUER_URI: http://keycloak:8180/realms/skillmatch
      KEYCLOAK_JWK_URI: http://keycloak:8180/realms/skillmatch/protocol/openid-connect/certs
      JAVA_OPTS: "-Xmx256m -Xms128m"
    depends_on:
      - postgres
      - rabbitmq
      - keycloak

  payment-service:
    build:
      context: ../services/payment-service
      dockerfile: Dockerfile
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: paymentdb
      DB_USERNAME: skillmatch
      DB_PASSWORD: secret
      RABBITMQ_HOST: rabbitmq
      KEYCLOAK_ISSUER_URI: http://keycloak:8180/realms/skillmatch
      KEYCLOAK_JWK_URI: http://keycloak:8180/realms/skillmatch/protocol/openid-connect/certs
      JAVA_OPTS: "-Xmx256m -Xms128m"
    depends_on:
      - postgres
      - rabbitmq
      - keycloak

  feedback-service:
    build:
      context: ../services/feedback-service
      dockerfile: Dockerfile
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: feedbackdb
      DB_USERNAME: skillmatch
      DB_PASSWORD: secret
      RABBITMQ_HOST: rabbitmq
      KEYCLOAK_ISSUER_URI: http://keycloak:8180/realms/skillmatch
      KEYCLOAK_JWK_URI: http://keycloak:8180/realms/skillmatch/protocol/openid-connect/certs
      JAVA_OPTS: "-Xmx256m -Xms128m"
    depends_on:
      - postgres
      - rabbitmq
      - keycloak

  notification-service:
    build:
      context: ../services/notification-service
      dockerfile: Dockerfile
    environment:
      MONGODB_URI: mongodb://mongodb:27017/notificationdb
      RABBITMQ_HOST: rabbitmq
      KEYCLOAK_ISSUER_URI: http://keycloak:8180/realms/skillmatch
      KEYCLOAK_JWK_URI: http://keycloak:8180/realms/skillmatch/protocol/openid-connect/certs
      JAVA_OPTS: "-Xmx256m -Xms128m"
    depends_on:
      - mongodb
      - rabbitmq
      - keycloak

volumes:
  postgres-data:
  mongodb-data:
```

## PostgreSQL Init Script (creates all logical databases)

```sql
-- infra/init-databases.sql (mounted in docker-entrypoint-initdb.d/)
-- identitydb is created automatically via POSTGRES_DB env var

CREATE DATABASE userdb;
CREATE DATABASE projectdb;
CREATE DATABASE contractdb;
CREATE DATABASE paymentdb;
CREATE DATABASE feedbackdb;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE userdb TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE projectdb TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE contractdb TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO skillmatch;
GRANT ALL PRIVILEGES ON DATABASE feedbackdb TO skillmatch;
```

## Oracle Cloud VM Setup Script

```bash
#!/bin/bash
# infra/scripts/setup-oracle-vm.sh — Run once on fresh Ubuntu 22.04 ARM VM

set -e

# 1. Update system
sudo apt update && sudo apt upgrade -y

# 2. Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 3. Install K3s (lightweight Kubernetes)
curl -sfL https://get.k3s.io | sh -s - \
  --write-kubeconfig-mode 644 \
  --disable traefik  # We'll use nginx-ingress instead (optional)

# 4. Verify
kubectl get nodes
kubectl get pods -A

# 5. Install Nginx Ingress Controller (optional, if traefik disabled)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.9.4/deploy/static/provider/cloud/deploy.yaml

# 6. Install cert-manager for Let's Encrypt TLS (optional)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml

# 7. Create namespace
kubectl create namespace skillmatch

# 8. Configure GHCR pull secret (for private images)
kubectl create secret docker-registry ghcr-secret \
  --namespace skillmatch \
  --docker-server=ghcr.io \
  --docker-username=YOUR_GITHUB_USER \
  --docker-password=YOUR_GITHUB_PAT

# 9. Open firewall ports (Oracle Cloud Security List must also be configured)
sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT

echo "VM setup complete. Deploy with: kubectl apply -f infra/k8s/"
```

## Kubernetes Deployment Template

```yaml
# infra/k8s/user-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: skillmatch
  labels:
    app: user-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      imagePullSecrets:
        - name: ghcr-secret
      containers:
        - name: user-service
          image: ghcr.io/YOUR_USER/skillmatch-user-service:latest
          ports:
            - containerPort: 8081
          env:
            - name: DB_HOST
              value: postgres-service
            - name: DB_NAME
              value: userdb
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: password
            - name: RABBITMQ_HOST
              value: rabbitmq-service
            - name: KEYCLOAK_ISSUER_URI
              valueFrom:
                configMapKeyRef:
                  name: skillmatch-config
                  key: keycloak-issuer-uri
            - name: JAVA_OPTS
              value: "-Xmx256m -Xms128m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 45
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 5
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "384Mi"
              cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: skillmatch
spec:
  selector:
    app: user-service
  ports:
    - port: 8081
      targetPort: 8081
  type: ClusterIP
```

## Kubernetes ConfigMap & Secrets

```yaml
# infra/k8s/config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: skillmatch-config
  namespace: skillmatch
data:
  keycloak-issuer-uri: "http://keycloak-service:8180/realms/skillmatch"
  keycloak-jwk-uri: "http://keycloak-service:8180/realms/skillmatch/protocol/openid-connect/certs"
  rabbitmq-host: "rabbitmq-service"
---
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
  namespace: skillmatch
type: Opaque
stringData:
  username: skillmatch
  password: secret
```

## GitHub Actions CI/CD Template (Monorepo with path filter)

```yaml
# .github/workflows/user-service.yml
name: User Service CI/CD

on:
  push:
    branches: [main]
    paths:
      - 'services/user-service/**'
  pull_request:
    branches: [main]
    paths:
      - 'services/user-service/**'

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository_owner }}/skillmatch/user-service
  SERVICE_PATH: services/user-service

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ${{ env.SERVICE_PATH }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build and Test
        run: ./mvnw verify
      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results-user-service
          path: ${{ env.SERVICE_PATH }}/target/surefire-reports/

  docker-build-push:
    needs: build-and-test
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4

      - name: Set up QEMU (for ARM cross-compilation)
        uses: docker/setup-qemu-action@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push (linux/arm64)
        uses: docker/build-push-action@v5
        with:
          context: ${{ env.SERVICE_PATH }}
          platforms: linux/arm64
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}

  deploy:
    needs: docker-build-push
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Oracle Cloud VM via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.ORACLE_VM_HOST }}
          username: ubuntu
          key: ${{ secrets.ORACLE_VM_SSH_KEY }}
          script: |
            kubectl set image deployment/user-service \
              user-service=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }} \
              -n skillmatch
            kubectl rollout status deployment/user-service -n skillmatch --timeout=120s
```

> **Nota**: Creare un file `.yml` per ogni servizio: `user-service.yml`, `project-service.yml`, ecc. L'unica differenza tra le pipeline è il valore di `SERVICE_PATH`, `IMAGE_NAME` e il nome del deployment Kubernetes.

## Use Cases (minimum 3 per stakeholder)

### Professional
1. UC-P1: Registrazione e completamento profilo con competenze e portfolio
2. UC-P2: Candidatura a un progetto aperto
3. UC-P3: Visualizzazione storico collaborazioni e feedback ricevuti

### Company
1. UC-C1: Pubblicazione di un nuovo progetto con requisiti
2. UC-C2: Selezione candidato e stipula micro-contratto digitale
3. UC-C3: Pagamento e ricezione fattura unica

### Admin
1. UC-A1: Validazione iscrizione di un professionista
2. UC-A2: Configurazione percentuale di commissione
3. UC-A3: Monitoraggio transazioni e gestione segnalazioni/sospensioni
