# How to Write a Spring Boot Microservice — FDB Pay Edition

This guide uses the **promotions-service** as the reference because it covers every pattern used across the project: JPA, Redis caching, Kafka producer + consumer, inter-service HTTP calls (WebClient), Flyway migrations, Eureka service discovery, API documentation, and the shared library.

We will walk every file top-to-bottom, explain what each piece does and why it exists, and then show how the service communicates with others.

---

## Table of Contents

1. [Monorepo Overview](#1-monorepo-overview)
2. [Folder Structure of promotions-service](#2-folder-structure)
3. [Parent POM — Dependency Management](#3-parent-pom)
4. [The Shared Library](#4-the-shared-library)
5. [Step-by-Step Service Construction](#5-step-by-step)
   - [5a. pom.xml](#5a-pomxml)
   - [5b. application.yml](#5b-applicationyml)
   - [5c. Flyway Migration](#5c-flyway-migration)
   - [5d. JPA Entity (Model)](#5d-jpa-entity-model)
   - [5e. Enums](#5e-enums)
   - [5f. DTOs — Request & Response](#5f-dtos)
   - [5g. Repository](#5g-repository)
   - [5h. Service Interface](#5h-service-interface)
   - [5i. Service Implementation](#5i-service-implementation)
   - [5j. Controller (REST)](#5j-controller)
   - [5k. Configuration Classes](#5k-configuration-classes)
   - [5l. Kafka Consumer](#5l-kafka-consumer)
   - [5m. Application Entry Point](#5m-application-entry-point)
6. [OOP Concepts Used](#6-oop-concepts)
7. [Inter-Service Communication](#7-inter-service-communication)
   - [7a. Async via Kafka](#7a-async-via-kafka)
   - [7b. Sync via WebClient](#7b-sync-via-webclient)
   - [7c. Via API Gateway](#7c-via-api-gateway)
8. [Redis Caching Deep-Dive](#8-redis-caching)
9. [Exception Handling](#9-exception-handling)
10. [Docker & Deployment](#10-docker--deployment)
11. [Service Discovery (Eureka)](#11-service-discovery)
12. [API Documentation (SpringDoc)](#12-api-documentation)
13. [Summary — Putting It All Together](#13-summary)

---

## 1. Monorepo Overview

FDB Pay uses a **multi-module Maven monorepo** at `backend/`:

```
backend/
  pom.xml                  <-- Parent POM (fdb-pay-parent)
  shared/                  <-- Shared library JAR
  api-gateway/             <-- Spring Cloud Gateway
  eureka-server/           <-- Service Discovery
  auth-service/
  wallet-service/
  transfer-service/
  merchant-service/
  bill-payment-service/
  agent-service/
  corporate-service/
  notification-service/
  fraud-risk-service/
  reporting-service/
  kyc-service/
  settlement-service/
  dispute-service/
  audit-service/
  remittance-service/
  promotions-service/      <-- OUR REFERENCE SERVICE
  support-service/
```

Each service is a **standalone Spring Boot application** that can run independently, but they share:
- The `shared` library JAR (common exception classes, DTOs, Kafka event schemas, Redis config)
- Infrastructure (PostgreSQL, Redis, Kafka) defined in `docker-compose.yml`
- Service discovery via Eureka

---

## 2. Folder Structure

Here is the complete directory tree of `promotions-service/`:

```
promotions-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/fdbpay/promotions/service/
    │   │   ├── PromotionsServiceApplication.java       ← Entry point (main class)
    │   │   ├── config/
    │   │   │   └── WebClientConfig.java                 ← Spring config for inter-service HTTP
    │   │   ├── consumer/
    │   │   │   └── PromotionEventConsumer.java          ← Kafka listener
    │   │   ├── controller/
    │   │   │   └── PromotionController.java             ← REST endpoints
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   │   ├── CreatePromotionRequest.java
    │   │   │   │   ├── ApplyPromotionRequest.java
    │   │   │   │   └── RedeemCashbackRequest.java
    │   │   │   └── response/
    │   │   │       ├── PromotionResponse.java
    │   │   │       ├── PromotionUsageResponse.java
    │   │   │       ├── PromotionValidationResponse.java
    │   │   │       ├── CashbackWalletResponse.java
    │   │   │       └── CashbackTransactionResponse.java
    │   │   ├── model/
    │   │   │   ├── Promotion.java                       ← JPA entity
    │   │   │   ├── PromotionUsage.java
    │   │   │   ├── CashbackWallet.java
    │   │   │   ├── CashbackTransaction.java
    │   │   │   └── enums/
    │   │   │       ├── PromotionType.java
    │   │   │       ├── PromotionStatus.java
    │   │   │       ├── FundingType.java
    │   │   │       └── CashbackTxnType.java
    │   │   ├── repository/
    │   │   │   ├── PromotionRepository.java
    │   │   │   ├── PromotionUsageRepository.java
    │   │   │   ├── CashbackWalletRepository.java
    │   │   │   └── CashbackTransactionRepository.java
    │   │   └── service/
    │   │       ├── PromotionsService.java               ← Interface
    │   │       └── impl/
    │   │           └── PromotionsServiceImpl.java       ← Implementation
    │   └── resources/
    │       ├── application.yml                          ← All configuration
    │       └── db/migration/
    │           └── V1__init_promotions_schema.sql        ← Flyway migration
    └── test/
        └── java/...
```

**Package naming convention:** `com.fdbpay.<service-name>.service`.  
Every service follows this convention so the `shared` library (whose packages start with `com.fdbpay.shared`) can be found at compile time and Spring's component scanning can find all beans.

---

## 3. Parent POM (`backend/pom.xml`)

This is the foundation of every service. It declares the **Spring Boot parent**, common properties, and dependency versions that all child modules inherit.

```xml
<!-- backend/pom.xml (excerpts) -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.2</version>                     <!-- Single source of Spring Boot truth -->
</parent>

<groupId>com.fdbpay</groupId>
<artifactId>fdb-pay-parent</artifactId>
<packaging>pom</packaging>                       <!-- "pom" = multi-module aggregator -->

<properties>
    <java.version>21</java.version>              <!-- JDK 21 -->
    <spring-cloud.version>2023.0.3</spring-cloud.version>
    <shared.version>1.0.0-SNAPSHOT</shared.version>
    <springdoc.version>2.6.0</springdoc.version>
</properties>

<dependencyManagement>                           <!-- Central version control -->
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type><scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fdbpay</groupId>
            <artifactId>shared</artifactId>
            <version>${shared.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Why this matters:** Every service's `pom.xml` declares `<parent>com.fdbpay:fdb-pay-parent</parent>` and inherits `java.version`, `spring-cloud.version`, and all dependency versions. This avoids hardcoding versions in each service. The `shared` library version is defined once here.

---

## 4. The Shared Library (`backend/shared/`)

The `shared` library is a **plain JAR** (not a Spring Boot executable) that every service imports. It packages code that would otherwise be duplicated across services.

### What goes in shared?

| Artifact | Purpose |
|----------|---------|
| `ApiResponse<T>` | Generic JSON response envelope: `{success, data, meta, error}` |
| `BusinessException` | Base runtime exception with error code |
| `ResourceNotFoundException` | Pre-configured "not found" exception |
| `GlobalExceptionHandler` | `@RestControllerAdvice` — catches exceptions from **all** services |
| `ErrorCodes` | String constants like `"INSUFFICIENT_BALANCE"`, `"VALIDATION_ERROR"` |
| `TransactionEvent` | Kafka event schema for transaction-completed events |
| `NotificationEvent` | Kafka event schema for notification events |
| `CacheConfig` | `@Configuration` with `RedisCacheManager` + `RedisTemplate` beans |
| `InsufficientBalanceException` | Specific business exception |

### How services include it

```xml
<dependency>
    <groupId>com.fdbpay</groupId>
    <artifactId>shared</artifactId>
    <version>${shared.version}</version>
</dependency>
```

### How Spring finds shared beans

The `@SpringBootApplication` annotation in each service must scan the shared package:

```java
@SpringBootApplication(scanBasePackages = {"com.fdbpay.promotions.service", "com.fdbpay.shared"})
```

Without `"com.fdbpay.shared"` in `scanBasePackages`, the shared `CacheConfig`, `GlobalExceptionHandler`, etc. would never be discovered.

---

## 5. Step-by-Step Service Construction

### 5a. pom.xml

**File:** `promotions-service/pom.xml`

```xml
<parent>
    <groupId>com.fdbpay</groupId>
    <artifactId>fdb-pay-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>

<artifactId>promotions-service</artifactId>

<dependencies>
    <!-- 1. WEB — makes this a REST service (embedded Tomcat) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 2. JPA — connects to PostgreSQL via Hibernate ORM -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- 3. REDIS — for caching and RedisTemplate -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- 4. VALIDATION — @Valid / @NotBlank / @Positive on request DTOs -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- 5. KAFKA — send and receive async events -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- 6. POSTGRESQL DRIVER — runtime only (not needed at compile time) -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- 7. FLYWAY — database migration tool -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- 8. SHARED LIBRARY — common code -->
    <dependency>
        <groupId>com.fdbpay</groupId>
        <artifactId>shared</artifactId>
        <version>${shared.version}</version>
    </dependency>

    <!-- 9. LOMBOK — @Getter, @Setter, @Builder, etc. (compile-time only) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 10. JWT — token parsing/validation -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- 11. SPRINGDOC — auto-generates OpenAPI 3 spec -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>

    <!-- 12. ACTUATOR — health checks, metrics -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- 13. EUREKA CLIENT — registers with service discovery -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>

    <!-- 14. WEBFLUX — provides WebClient for inter-service HTTP calls -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

**What each starter provides:**

| Starter | What it pulls in |
|---------|-----------------|
| `spring-boot-starter-web` | Tomcat, Spring MVC, Jackson JSON |
| `spring-boot-starter-data-jpa` | Hibernate ORM, `JpaRepository` interface, connection pool (HikariCP) |
| `spring-boot-starter-data-redis` | Redis connection factory, `RedisTemplate`, `@Cacheable` support |
| `spring-boot-starter-validation` | Hibernate Validator (JSR-380 Bean Validation implementation) |
| `spring-kafka` | `KafkaTemplate` (send), `@KafkaListener` (receive) |
| `spring-boot-starter-webflux` | Reactive `WebClient` (for making HTTP calls to other services) |
| `spring-cloud-starter-netflix-eureka-client` | Eureka client — auto-registers, discovers other services |
| `spring-boot-starter-actuator` | `/actuator/health`, `/actuator/info` endpoints |

---

### 5b. application.yml

**File:** `promotions-service/src/main/resources/application.yml`

This is the **entire configuration** of the service. Every property is explained below.

```yaml
server:
  port: 8096                   # Port this service listens on inside the container

spring:
  application:
    name: promotions-service   # Eureka service ID (used by gateway routes: lb://promotions-service)

  # ── DATABASE ──────────────────────────────────────────────
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:fdbpay_promotions}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:                     # HikariCP connection pool tuning
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000      # 5 minutes
      connection-timeout: 20000 # 20 seconds

  # ── JPA / HIBERNATE ────────────────────────────────────────
  jpa:
    hibernate:
      ddl-auto: validate        # NEVER "update" in production. Flyway manages schema.
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 25        # Batch inserts/updates for performance
        order_inserts: true     # Group inserts by table
        order_updates: true

  # ── FLYWAY ─────────────────────────────────────────────────
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true   # Apply migrations to an existing non-empty DB

  # ── REDIS ──────────────────────────────────────────────────
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000             # 2 seconds connection timeout

  cache:
    type: redis                 # Use Redis as the cache backend
    redis:
      time-to-live: 900000      # Default TTL: 15 minutes (in milliseconds)
      cache-null-values: false  # Don't cache null results

  # ── KAFKA ──────────────────────────────────────────────────
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: promotions-service    # Kafka consumer group (services in same group share load)
      auto-offset-reset: earliest      # Start reading from earliest message if no offset stored
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.fdbpay.shared.*"  # Security: only deserialize known types
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                 # Wait for all replicas to acknowledge
      retries: 3
      properties:
        max.block.ms: 5000      # Max time to block when sending

# ── EUREKA ──────────────────────────────────────────────────
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
  instance:
    prefer-ip-address: true     # Register with IP, not hostname

# ── JWT ─────────────────────────────────────────────────────
jwt:
  secret: ${JWT_SECRET:...}
  expiration-ms: 900000

# ── SPRINGDOC ──────────────────────────────────────────────
springdoc:
  api-docs:
    path: /api-docs             # OpenAPI JSON at GET /api-docs
  swagger-ui:
    path: /swagger-ui.html      # Swagger UI at GET /swagger-ui.html

# ── LOGGING ─────────────────────────────────────────────────
logging:
  level:
    com.fdbpay.promotions: INFO
    org.springframework.kafka: WARN
```

**Key patterns:**

- **Environment variable substitution with defaults:** `${DB_HOST:localhost}` means "use the env var `DB_HOST`, or fall back to `localhost`". In Docker, env vars like `DB_HOST=postgres` override these.
- **`ddl-auto: validate`** — Hibernate checks that JPA entities match the database schema but never alters it. Schema changes are done via Flyway migrations only. This is the **production-safe** setting.
- **Kafka `spring.json.trusted.packages`** — A security measure. Jackson will only deserialize messages from these packages into Java objects, preventing deserialization attacks.

---

### 5c. Flyway Migration

**File:** `src/main/resources/db/migration/V1__init_promotions_schema.sql`

Flyway applies SQL migrations in version order. The naming convention is:

```
V<version>__<description>.sql
```

- `V` = versioned migration (applied once, never changed)
- Version numbers must be unique and sequential
- Description is human-readable (uses double underscores)

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- Enables gen_random_uuid()

CREATE TABLE promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- Auto-generate UUID primary key
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    type VARCHAR(30) NOT NULL,                       -- Stored as string (EnumType.STRING)
    funding_type VARCHAR(20) NOT NULL,
    merchant_id UUID,                                -- Nullable FK to another service's table
    discount_value BIGINT NOT NULL,
    max_discount BIGINT,
    min_transaction_amount BIGINT,
    max_usage_total INT NOT NULL DEFAULT 0,
    max_usage_per_user INT NOT NULL DEFAULT 0,
    usage_count INT NOT NULL DEFAULT 0,
    start_date TIMESTAMPTZ NOT NULL,                 -- TIMESTAMPTZ = timestamp with time zone
    end_date TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    promo_code VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- Performance indexes
CREATE INDEX idx_promotions_status ON promotions(status);
CREATE INDEX idx_promotions_merchant_id ON promotions(merchant_id);
CREATE UNIQUE INDEX idx_promotions_promo_code ON promotions(promo_code) WHERE promo_code IS NOT NULL;
CREATE INDEX idx_promotions_active_dates ON promotions(status, start_date, end_date);

CREATE TABLE promotion_usages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id UUID NOT NULL REFERENCES promotions(id),  -- FK to promotions
    user_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    discount_applied BIGINT NOT NULL DEFAULT 0,
    cashback_amount BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Foreign key indexes (improve JOIN performance)
CREATE INDEX idx_promotion_usages_promotion ON promotion_usages(promotion_id);
CREATE INDEX idx_promotion_usages_user ON promotion_usages(user_id, promotion_id);
CREATE INDEX idx_promotion_usages_transaction ON promotion_usages(transaction_id);
```

**Why Flyway over `ddl-auto: update`?**

- Every schema change is **versioned and repeatable** across all environments (dev/staging/prod)
- You can see the full history of schema changes in `db/migration/`
- Team members all get the same schema
- Never accidentally drops a column in production

---

### 5d. JPA Entity (Model)

**File:** `model/Promotion.java`

This is the canonical example of a JPA entity in this project.

```java
@Getter                         // Lombok: generates getters for all fields
@Setter                         // Lombok: generates setters
@NoArgsConstructor              // Lombok: JPA needs a no-arg constructor
@AllArgsConstructor             // Lombok: constructor with all args (for builder)
@Builder                        // Lombok: builder pattern: Promotion.builder().title(...).build()
@Entity                         // JPA: this class maps to a database table
@Table(name = "promotions")     // Specifies the table name (default = class name)
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // Auto-generate UUID via Hibernate
    private UUID id;                                 // Primary key — always UUID in this project

    @Column(nullable = false, length = 200)          // NOT NULL, VARCHAR(200)
    private String title;

    @Column(length = 1000)                           // VARCHAR(1000), nullable by default
    private String description;

    @Enumerated(EnumType.STRING)                     // Store enum as string in DB, not ordinal number
    @Column(nullable = false, length = 30)
    private PromotionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingType fundingType;

    private UUID merchantId;                         // Simple UUID field (no FK constraint in JPA)

    @Column(nullable = false)
    private Long discountValue;

    private Long maxDiscount;                        // Nullable wrapper type, not primitive

    @Builder.Default                                // When using builder, default to 0
    @Column(nullable = false)
    private Integer maxUsageTotal = 0;

    @Column(nullable = false)
    private OffsetDateTime startDate;                // TIMESTAMPTZ in PostgreSQL

    @Column(nullable = false)
    private OffsetDateTime endDate;

    @Column(nullable = false, updatable = false)     // Set once, never updated
    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    // ── Lifecycle Callbacks ──────────────────────────────────
    @PrePersist                                      // Runs BEFORE the first INSERT
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate                                       // Runs BEFORE each UPDATE
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
```

**Key JPA annotations reference:**

| Annotation | Purpose |
|-----------|---------|
| `@Entity` | Marks the class as a JPA entity (maps to a DB table) |
| `@Table(name = "...")` | Overrides the default table name |
| `@Id` | Marks the primary key field |
| `@GeneratedValue(strategy = GenerationType.UUID)` | Auto-generates UUID primary keys |
| `@Column(nullable = false, length = 100)` | Column constraints |
| `@Enumerated(EnumType.STRING)` | Store enum as a readable string (NOT ordinal) |
| `@Builder.Default` | Sets a default value in the Lombok builder pattern |
| `@PrePersist` | Lifecycle hook — runs before the first INSERT |
| `@PreUpdate` | Lifecycle hook — runs before each UPDATE |

**Why `OffsetDateTime` and not `LocalDateTime`?**

`OffsetDateTime` stores the timezone offset (e.g., `+06:30` for Myanmar). `TIMESTAMPTZ` in PostgreSQL stores the timezone-aware instant. This ensures timestamps are correct regardless of the server's timezone.

**Why `Long` (wrapper) and not `long` (primitive)?**

Wrapper types (`Long`, `Integer`) can be `null` in Java, which maps to `NULL` in the database. Primitives default to `0`, making it impossible to distinguish "unset" from "zero". For `discountValue`, which is always required, `long` would also work — but the convention here uses wrappers consistently.

---

### 5e. Enums

**File:** `model/enums/PromotionType.java`

```java
public enum PromotionType {
    FIXED_DISCOUNT,
    PERCENTAGE_DISCOUNT,
    CASHBACK,
    BOGO,
    COUPON_CODE
}
```

```java
public enum PromotionStatus {
    DRAFT, ACTIVE, PAUSED, EXPIRED
}
```

```java
public enum FundingType {
    MERCHANT, BANK
}
```

```java
public enum CashbackTxnType {
    EARNED, REDEEMED, EXPIRED
}
```

Enums are simple Java `enum` types. Because of `@Enumerated(EnumType.STRING)` on the entity field, the **name** (e.g., `"CASHBACK"`) is stored in the database, not an integer ordinal. This makes the database readable and lets you reorder enum constants without breaking existing data.

**Where enums are used:**
- In the entity: `promotion.setType(PromotionType.CASHBACK)`
- In the controller's request DTO: `@NotNull PromotionType type`
- In the database: stored as `VARCHAR` strings like `'CASHBACK'`
- In JSON API responses: serialized as strings like `"CASHBACK"`

---

### 5f. DTOs — Request & Response

**Request DTOs** define the expected JSON body for POST/PUT endpoints.  
**Response DTOs** define the JSON structure returned to the client.

#### CreatePromotionRequest

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreatePromotionRequest {

    @NotBlank(message = "Title is required")        // Validation: must not be null/empty/blank
    private String title;

    private String description;                     // No annotation = optional

    @NotNull(message = "Promotion type is required")
    private PromotionType type;

    @NotNull(message = "Funding type is required")
    private FundingType fundingType;

    private UUID merchantId;

    @Positive(message = "Discount value must be positive")
    private Long discountValue;

    private Long maxDiscount;
    private Long minTransactionAmount;
    private Integer maxUsageTotal;
    private Integer maxUsagePerUser;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private String promoCode;
}
```

The `@NotBlank`, `@NotNull`, `@Positive` annotations are from `jakarta.validation.constraints`. When a request body is annotated with `@Valid` in the controller, Spring automatically validates these constraints before the controller method runs. If validation fails, the `GlobalExceptionHandler` catches `MethodArgumentNotValidException` and returns a 400 with field-level error messages.

#### ApplyPromotionRequest

```java
public class ApplyPromotionRequest {
    @NotBlank
    private String promoCode;

    @Positive
    private Long transactionAmount;
}
```

#### RedeemCashbackRequest

```java
public class RedeemCashbackRequest {
    @Positive
    private Long amount;
}
```

#### PromotionResponse (Response DTO)

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromotionResponse {
    private UUID id;
    private String title;
    private String description;
    private PromotionType type;
    private FundingType fundingType;
    private UUID merchantId;
    private Long discountValue;
    private Long maxDiscount;
    private Long minTransactionAmount;
    private Integer maxUsageTotal;
    private Integer maxUsagePerUser;
    private Integer usageCount;
    private Integer remainingUses;         // Computed field (not in entity)
    private Boolean isActive;              // Computed field (not in entity)
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private PromotionStatus status;
    private String promoCode;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
```

**Why separate DTOs from entities?**

The entity (`Promotion`) has database-specific fields (`@Id`, lifecycle hooks) and JPA annotations. The response DTO (`PromotionResponse`) has **computed fields** (`remainingUses`, `isActive`) that don't exist in the database. Returning the entity directly would:
1. Expose internal fields you don't want to share
2. Cause lazy-loading issues if the Hibernate session is closed
3. Include JPA metadata in your JSON
4. Not allow computed/presentational fields

The mapping from entity → DTO is done manually in the service layer (in `mapToResponse()`).

**Why `Boolean` (wrapper) for `isActive` instead of `boolean`?**

Wrapper `Boolean` can be `null` in the JSON response. If a client sees `null`, they know the value wasn't computed. Primitive `boolean` would default to `false`, which is misleading.

**DTO naming convention:**

| Type | Package | Example |
|------|---------|---------|
| Request DTOs | `dto/request/` | `CreatePromotionRequest.java` |
| Response DTOs | `dto/response/` | `PromotionResponse.java` |

---

### 5g. Repository

**PromotionRepository**

```java
@Repository                                          // Spring stereotype — enables exception translation
public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    // Spring Data JPA derives the query from the method name!
    List<Promotion> findByStatus(PromotionStatus status);                                // WHERE status = ?

    List<Promotion> findByMerchantId(UUID merchantId);                                    // WHERE merchant_id = ?

    Optional<Promotion> findByPromoCode(String promoCode);                                // WHERE promo_code = ?  (unique)
                                                                                          // Optional = may return null

    List<Promotion> findByStatusAndStartDateBeforeAndEndDateAfter(                        // WHERE status = ?
            PromotionStatus status, OffsetDateTime now1, OffsetDateTime now2);            //   AND start_date < ?
                                                                                          //   AND end_date > ?
}
```

**Spring Data JPA Query Methods:**

The method name IS the query. Spring Data JPA parses the method name at startup and generates the `WHERE` clause automatically.

| Method name part | SQL equivalent |
|-----------------|----------------|
| `findBy` | `SELECT ... WHERE` |
| `Status` | `status = ?` |
| `And` | `AND` |
| `StartDateBefore` | `start_date < ?` |
| `EndDateAfter` | `end_date > ?` |
| `OrderByCreatedAtDesc` | `ORDER BY created_at DESC` |

**Rules for query methods:**
- Return type can be `Entity`, `Optional<Entity>`, `List<Entity>`, `Page<Entity>`, etc.
- Property names in the method must match entity field names (camelCase maps to snake_case)
- Supported keywords: `And`, `Or`, `Between`, `LessThan`, `GreaterThan`, `IsNull`, `IsNotNull`, `Like`, `In`, `OrderBy`, `Top`, `First`, etc.

**PromotionUsageRepository — custom query**

```java
@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, UUID> {

    // Method-based query
    List<PromotionUsage> findByUserIdAndPromotionId(UUID userId, UUID promotionId);

    // Custom JPQL query (when method name isn't enough)
    @Query("SELECT COUNT(pu) FROM PromotionUsage pu WHERE pu.userId = :userId AND pu.promotionId = :promotionId")
    long countByUserIdAndPromotionId(@Param("userId") UUID userId, @Param("promotionId") UUID promotionId);
}
```

Use `@Query` when the method name would be too long or you need database-specific SQL. Here, the `@Query` counts rows instead of fetching them, which is more efficient than `findByUserIdAndPromotionId().size()`.

**CashbackWalletRepository**

```java
@Repository
public interface CashbackWalletRepository extends JpaRepository<CashbackWallet, UUID> {
    Optional<CashbackWallet> findByUserId(UUID userId);    // Each user has one wallet
}
```

**CashbackTransactionRepository**

```java
@Repository
public interface CashbackTransactionRepository extends JpaRepository<CashbackTransaction, UUID> {
    Page<CashbackTransaction> findByCashbackWalletIdOrderByCreatedAtDesc(
            UUID cashbackWalletId, Pageable pageable);     // Paginated query
}
```

Note the `Pageable` parameter — Spring automatically adds `LIMIT` and `OFFSET` and returns a `Page` object with `totalElements`, `totalPages`, etc.

---

### 5h. Service Interface

**File:** `service/PromotionsService.java`

```java
public interface PromotionsService {

    PromotionResponse createPromotion(CreatePromotionRequest request);               // C - Create

    List<PromotionResponse> getActivePromotions(UUID userId);                        // R - Read (list)

    PromotionValidationResponse validatePromoCode(String promoCode, Long amount, UUID userId);  // Custom operation

    PromotionUsageResponse applyPromotion(UUID userId, ApplyPromotionRequest request,
                                          UUID transactionId);                      // Custom operation

    CashbackWalletResponse getCashbackWallet(UUID userId);                           // R - Read

    CashbackWalletResponse redeemCashback(UUID userId, RedeemCashbackRequest request); // Custom operation

    Page<PromotionResponse> getMyPromotions(UUID userId, int page, int size);        // R - Read (paginated)

    void deactivatePromotion(UUID promotionId);                                      // U - Update (status change)
}
```

**Why an interface?**

- **Loose coupling:** The controller depends on the interface, not the concrete implementation. You can swap implementations without changing the controller.
- **Testing:** You can mock the interface in unit tests.
- **AOP proxies:** Spring creates a JDK dynamic proxy for the interface, enabling `@Transactional` and other annotation-driven behavior.
- **Documentation:** The interface serves as a contract — it clearly defines what the service does without revealing how.

---

### 5i. Service Implementation

**File:** `service/impl/PromotionsServiceImpl.java`

This is where all business logic lives. Every important pattern is annotated.

```java
@Slf4j                                          // Lombok: creates `log` field (SLF4J logger)
@Service                                        // Spring stereotype — makes this a service bean
@RequiredArgsConstructor                        // Lombok: creates constructor for all `final` fields
@Transactional                                  // All public methods run in a DB transaction
public class PromotionsServiceImpl implements PromotionsService {

    // ── Dependencies injected via constructor (thanks to @RequiredArgsConstructor) ──
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final CashbackWalletRepository cashbackWalletRepository;
    private final CashbackTransactionRepository cashbackTransactionRepository;
    private final WebClient.Builder webClientBuilder;    // For calling wallet-service

    private static final String WALLET_SERVICE_BASE = "http://wallet-service/wallet";

    // ── CREATE ─────────────────────────────────────────────────────────────────
    @Override
    @Transactional                                      // Runs in a DB transaction
    public PromotionResponse createPromotion(CreatePromotionRequest request) {
        // 1. Map request → entity using builder pattern
        Promotion promotion = Promotion.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .fundingType(request.getFundingType())
                .merchantId(request.getMerchantId())
                .discountValue(request.getDiscountValue())
                .maxDiscount(request.getMaxDiscount())
                .minTransactionAmount(request.getMinTransactionAmount())
                .maxUsageTotal(request.getMaxUsageTotal() != null ? request.getMaxUsageTotal() : 0)
                .maxUsagePerUser(request.getMaxUsagePerUser() != null ? request.getMaxUsagePerUser() : 0)
                .usageCount(0)
                .startDate(request.getStartDate() != null ? request.getStartDate() : OffsetDateTime.now())
                .endDate(request.getEndDate() != null ? request.getEndDate() : OffsetDateTime.now().plusDays(30))
                .status(PromotionStatus.ACTIVE)
                .promoCode(request.getPromoCode())
                .build();

        // 2. Save to database
        promotion = promotionRepository.save(promotion);
        log.info("Promotion created: id={}, title={}", promotion.getId(), promotion.getTitle());

        // 3. Map entity → response DTO
        return mapToResponse(promotion);
    }

    // ── READ (list with filtering) ──────────────────────────────────────────────
    @Override
    public List<PromotionResponse> getActivePromotions(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        return promotionRepository
                .findByStatusAndStartDateBeforeAndEndDateAfter(PromotionStatus.ACTIVE, now, now)
                .stream()
                .filter(p -> p.getMerchantId() == null || (userId != null && p.getMerchantId().equals(userId)))
                .map(this::mapToResponse)
                .toList();                              // Java 16+ .toList() instead of .collect(Collectors.toList())
    }

    // ── VALIDATION (pure logic, no side effects) ─────────────────────────────────
    @Override
    public PromotionValidationResponse validatePromoCode(String promoCode, Long amount, UUID userId) {
        Promotion promotion = promotionRepository.findByPromoCode(promoCode).orElse(null);
        if (promotion == null)
            return invalid("Invalid promo code");

        if (promotion.getStatus() != PromotionStatus.ACTIVE)
            return invalid("Promotion is not active");

        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate()))
            return invalid("Promotion has expired or not yet started");

        if (promotion.getMaxUsageTotal() > 0 && promotion.getUsageCount() >= promotion.getMaxUsageTotal())
            return invalid("Promotion usage limit reached");

        if (promotion.getMaxUsagePerUser() > 0) {
            long userUsageCount = promotionUsageRepository.countByUserIdAndPromotionId(userId, promotion.getId());
            if (userUsageCount >= promotion.getMaxUsagePerUser())
                return invalid("You have reached the per-user usage limit");
        }

        if (promotion.getMinTransactionAmount() != null && amount < promotion.getMinTransactionAmount())
            return invalid("Minimum transaction amount is " + promotion.getMinTransactionAmount());

        Long discount = calculateDiscount(promotion, amount);
        return PromotionValidationResponse.builder().valid(true).discount(discount)
                .message("Promotion applied successfully").build();
    }

    // ── APPLY (write operation) ──────────────────────────────────────────────────
    @Override
    @Transactional
    public PromotionUsageResponse applyPromotion(UUID userId, ApplyPromotionRequest request, UUID transactionId) {
        Promotion promotion = promotionRepository.findByPromoCode(request.getPromoCode())
                .orElseThrow(() -> new ResourceNotFoundException("Promotion", request.getPromoCode()));

        PromotionValidationResponse validation = this.validatePromoCode(
                request.getPromoCode(), request.getTransactionAmount(), userId);
        if (!validation.isValid())
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, validation.getMessage());

        Long discount = validation.getDiscount();
        Long cashbackAmount = 0L;

        if (promotion.getType() == PromotionType.CASHBACK) {
            cashbackAmount = discount;
            discount = 0L;
        }

        // Create usage record
        PromotionUsage usage = PromotionUsage.builder()
                .promotionId(promotion.getId()).userId(userId)
                .transactionId(transactionId)
                .discountApplied(discount).cashbackAmount(cashbackAmount)
                .build();
        usage = promotionUsageRepository.save(usage);

        // Increment usage counter
        promotion.setUsageCount(promotion.getUsageCount() + 1);
        promotionRepository.save(promotion);

        // If cashback, credit the user's cashback wallet
        if (cashbackAmount > 0)
            creditCashbackWallet(userId, cashbackAmount, promotion.getId(), transactionId);

        return PromotionUsageResponse.builder()...build();
    }

    // ── DISCOUNT CALCULATION (switch expression) ─────────────────────────────────
    private Long calculateDiscount(Promotion promotion, Long amount) {
        return switch (promotion.getType()) {                   // Java 17+ switch expression
            case FIXED_DISCOUNT -> {
                Long discount = promotion.getDiscountValue();
                if (promotion.getMaxDiscount() != null && discount > promotion.getMaxDiscount())
                    discount = promotion.getMaxDiscount();
                yield Math.min(discount, amount);               // yield returns a value from a switch branch
            }
            case PERCENTAGE_DISCOUNT -> {
                BigDecimal percentage = BigDecimal.valueOf(promotion.getDiscountValue())
                        .divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);
                Long discount = BigDecimal.valueOf(amount)
                        .multiply(percentage).setScale(0, RoundingMode.HALF_UP).longValue();
                if (promotion.getMaxDiscount() != null && discount > promotion.getMaxDiscount())
                    discount = promotion.getMaxDiscount();
                yield Math.min(discount, amount);
            }
            case CASHBACK -> {
                // Same calculation as percentage but returned as cashback amount
                ...
            }
            default -> 0L;
        };
    }

    // ── INTER-SERVICE CALL via WebClient ─────────────────────────────────────────
    private void creditCashbackWallet(UUID userId, Long amount, UUID promotionId, UUID transactionId) {
        CashbackWallet wallet = cashbackWalletRepository.findByUserId(userId)
                .orElseGet(() -> createCashbackWallet(userId));  // Create if not exists
        wallet.setBalance(wallet.getBalance() + amount);
        wallet.setTotalEarned(wallet.getTotalEarned() + amount);
        cashbackWalletRepository.save(wallet);

        CashbackTransaction earnedTxn = CashbackTransaction.builder()
                .cashbackWalletId(wallet.getId())
                .type(CashbackTxnType.EARNED).amount(amount)
                .promotionId(promotionId).transactionId(transactionId)
                .description("Cashback earned from promotion")
                .build();
        cashbackTransactionRepository.save(earnedTxn);
    }

    // ── ENTITY → DTO MAPPING ─────────────────────────────────────────────────────
    private PromotionResponse mapToResponse(Promotion promotion) {
        OffsetDateTime now = OffsetDateTime.now();
        boolean isActive = promotion.getStatus() == PromotionStatus.ACTIVE
                && now.isAfter(promotion.getStartDate())
                && now.isBefore(promotion.getEndDate());
        int remainingUses = promotion.getMaxUsageTotal() > 0
                ? promotion.getMaxUsageTotal() - promotion.getUsageCount()
                : -1;   // -1 means unlimited

        return PromotionResponse.builder()
                .id(promotion.getId())
                .title(promotion.getTitle())
                .type(promotion.getType())
                .status(promotion.getStatus())
                .isActive(isActive)
                .remainingUses(remainingUses)
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .createdAt(promotion.getCreatedAt())
                .build();
    }
}
```

**Named constants (`private static final`):**

```java
private static final String WALLET_SERVICE_BASE = "http://wallet-service/wallet";
```

The Eureka service name (`wallet-service`) is used as the hostname, not an IP or Docker container name. Eureka's `@LoadBalanced` WebClient resolves this to the actual instance address automatically.

**The `@RequiredArgsConstructor` pattern:**

This Lombok annotation generates a constructor with one parameter for each `final` field. Spring then uses this constructor for dependency injection:

```java
// Lombok generates this:
public PromotionsServiceImpl(
    PromotionRepository promotionRepository,
    PromotionUsageRepository promotionUsageRepository,
    ...
    WebClient.Builder webClientBuilder
) { ... }
```

This is the **recommended way** to do dependency injection — the constructor makes dependencies explicit, testable, and prevents circular references.

---

### 5j. Controller (REST)

**File:** `controller/PromotionController.java`

```java
@RestController                                          // @Controller + @ResponseBody on every method
@RequestMapping("/promotions")                              // Base URL for all endpoints
@RequiredArgsConstructor                                   // Constructor injection
public class PromotionController {

    private final PromotionsService promotionsService;      // Only dependency — the service interface

    // ── POST /promotions ────────────────────────────────────────────────────────
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)                     // Returns 201, not 200
    public ApiResponse<PromotionResponse> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request) {  // @Valid triggers Bean Validation
        return ApiResponse.success(promotionsService.createPromotion(request));
    }

    // ── GET /promotions/active ─────────────────────────────────────────────────
    @GetMapping("/active")
    public ApiResponse<List<PromotionResponse>> getActivePromotions(
            @RequestParam(required = false) UUID userId) {   // Optional query param
        return ApiResponse.success(promotionsService.getActivePromotions(userId));
    }

    // ── POST /promotions/validate ──────────────────────────────────────────────
    @PostMapping("/validate")
    public ApiResponse<PromotionValidationResponse> validatePromoCode(
            @RequestParam String promoCode,                 // Required query param
            @RequestParam Long amount,
            @RequestParam UUID userId) {
        return ApiResponse.success(promotionsService.validatePromoCode(promoCode, amount, userId));
    }

    // ── POST /promotions/apply ─────────────────────────────────────────────────
    @PostMapping("/apply")
    public ApiResponse<PromotionUsageResponse> applyPromotion(
            @RequestParam UUID userId,
            @Valid @RequestBody ApplyPromotionRequest request) {
        return ApiResponse.success(promotionsService.applyPromotion(
                userId, request, UUID.randomUUID()));        // Generate transactionId for tracking
    }

    // ── GET /promotions/cashback-wallet ────────────────────────────────────────
    @GetMapping("/cashback-wallet")
    public ApiResponse<CashbackWalletResponse> getCashbackWallet(
            @RequestParam UUID userId) {
        return ApiResponse.success(promotionsService.getCashbackWallet(userId));
    }

    // ── GET /promotions/my (paginated) ─────────────────────────────────────────
    @GetMapping("/my")
    public ApiResponse<Page<PromotionResponse>> getMyPromotions(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,     // Default to page 0
            @RequestParam(defaultValue = "20") int size) {   // Default to 20 per page
        return ApiResponse.success(promotionsService.getMyPromotions(userId, page, size));
    }

    // ── DELETE /promotions/{id} ─────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deactivatePromotion(
            @PathVariable UUID id) {
        promotionsService.deactivatePromotion(id);
        return ApiResponse.success(null);                   // Returns {success: true, data: null}
    }
}
```

**Key patterns in controllers:**

| Annotation | Purpose |
|-----------|---------|
| `@RestController` | Every method returns JSON (no view resolution) |
| `@RequestMapping("/promotions")` | Base path for all endpoints |
| `@GetMapping`, `@PostMapping`, `@DeleteMapping` | HTTP method + path |
| `@PathVariable` | Extract value from URL path, e.g., `{id}` → `UUID id` |
| `@RequestParam` | Extract query parameter, optional with `required=false` or `defaultValue` |
| `@RequestBody` | Deserialize JSON request body |
| `@Valid` | Validate the request body using Bean Validation annotations |
| `@ResponseStatus(HttpStatus.CREATED)` | Override default 200 status |

**The response wrapper:**

Every controller method returns `ApiResponse<T>`:

```java
return ApiResponse.success(data);
// Produces: { "success": true, "data": {...}, "meta": { "requestId": "...", "timestamp": "..." } }
```

The `ApiResponse` is defined in the shared library and provides a consistent JSON envelope across all 16 services. The client always checks `success` and then reads either `data` or `error`.

---

### 5k. Configuration Classes

**File:** `config/WebClientConfig.java`

```java
@Configuration                                          // Marks this as a Spring configuration class
public class WebClientConfig {

    @Bean
    @LoadBalanced                                        // Integrates with Eureka — resolves service names
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();                      // Creates a reusable WebClient builder
    }
}
```

**What `@LoadBalanced` does:**

When you inject `WebClient.Builder` into a service and make a request like:

```java
webClientBuilder.build()
    .post()
    .uri("http://wallet-service/wallet/credit")   // "wallet-service" is a Eureka service ID
    .bodyValue(payload)
    .retrieve()
    .bodyToMono(Map.class)
    .block();
```

The `@LoadBalanced` interceptor intercepts the `wallet-service` hostname, looks it up in Eureka, gets the actual IP:port of a healthy instance, and routes the request there. Without `@LoadBalanced`, you'd get a DNS resolution error for `wallet-service`.

**Alternatives to WebClient:**

Some services use `RestTemplate` (the older Spring approach). `WebClient` is the **reactive, non-blocking** alternative recommended by Spring for new code. In this project, we call `.block()` on it to make it synchronous, but it can also be used fully reactive.

---

### 5l. Kafka Consumer

**File:** `consumer/PromotionEventConsumer.java`

```java
@Slf4j
@Component                                              // Spring stereotype — simple bean
@RequiredArgsConstructor
public class PromotionEventConsumer {

    private final PromotionRepository promotionRepository;
    private final PromotionsService promotionsService;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;  // Sends notification events

    @KafkaListener(topics = "txn.completed", groupId = "promotions-service")  // <-- THE CONSUMER
    public void handleTransactionCompleted(TransactionEvent event) {
        log.info("Processing transaction for cashback eligibility: txnId={}, amount={}",
                event.getTransactionId(), event.getAmount());

        // Guard — skip invalid events
        if (event.getReceiverUserId() == null || event.getAmount() == null) return;

        OffsetDateTime now = OffsetDateTime.now();

        // Find all active cashback promotions
        List<Promotion> cashbackPromotions = promotionRepository
                .findByStatusAndStartDateBeforeAndEndDateAfter(PromotionStatus.ACTIVE, now, now)
                .stream()
                .filter(p -> p.getType() == PromotionType.CASHBACK)
                .toList();

        // Auto-apply cashback promotions
        for (Promotion promotion : cashbackPromotions) {
            if (promotion.getMinTransactionAmount() != null
                    && event.getAmount() < promotion.getMinTransactionAmount()) continue;

            try {
                promotionsService.applyPromotion(
                        event.getReceiverUserId(),
                        new ApplyPromotionRequest(promotion.getPromoCode(), event.getAmount()),
                        event.getTransactionId());
                log.info("Auto-applied cashback: promotionId={}, userId={}",
                        promotion.getId(), event.getReceiverUserId());
            } catch (Exception e) {
                log.warn("Failed to auto-apply cashback: {}", e.getMessage());
                // Graceful — one promotion failure doesn't break others
            }
        }
    }
}
```

**How Kafka messaging works in this project:**

1. **Producer:** Another service (e.g., `wallet-service` or `transfer-service`) publishes a `TransactionEvent` to the `"txn.completed"` Kafka topic after a successful transaction.
2. **Consumer:** `PromotionEventConsumer.handleTransactionCompleted()` listens to that topic. When a transaction completes, the consumer automatically checks if any active cashback promotions apply and credits the user's cashback wallet.
3. **Notification producer:** After processing, the consumer could send a `NotificationEvent` via `kafkaTemplate.send(...)` to notify the user (pattern 1: listen, process, produce).

**Key Kafka concepts:**

| Concept | How it works here |
|---------|------------------|
| **Topic** | `"txn.completed"` — a named channel. Services publish to and subscribe from topics. |
| **Producer** | Sends `TransactionEvent` objects (serialized as JSON) to a topic |
| **Consumer** | `@KafkaListener(topics = "...")` — a method that receives events from a topic |
| **Group ID** | `"promotions-service"` — consumers with the same group ID share the workload. Each message goes to one consumer in the group. |
| **Auto-offset-reset** | `earliest` — if this is the first time the consumer group runs, start reading from the oldest messages (not "latest", which would skip pre-existing messages) |

---

### 5m. Application Entry Point

**File:** `PromotionsServiceApplication.java`

```java
package com.fdbpay.promotions.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.fdbpay.promotions.service", "com.fdbpay.shared"})
@EnableDiscoveryClient                                  // Register with Eureka
public class PromotionsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromotionsServiceApplication.class, args);
    }
}
```

**What `@SpringBootApplication` does (it's a combination of three annotations):**

| Annotation inside `@SpringBootApplication` | Purpose |
|--------------------------------------------|---------|
| `@Configuration` | Marks the class as a source of bean definitions |
| `@EnableAutoConfiguration` | Auto-configures beans based on dependencies (e.g., if `spring-boot-starter-web` is on classpath, configure Tomcat and Spring MVC) |
| `@ComponentScan` | Scans the package for `@Component`, `@Service`, `@Repository`, `@Controller` beans |

**Why `scanBasePackages` is needed:**

Without it, Spring only scans `com.fdbpay.promotions.service` and its sub-packages. The `shared` library's classes live in `com.fdbpay.shared`, so they would be missed. The `scanBasePackages` tells Spring to scan both packages, which picks up `CacheConfig`, `GlobalExceptionHandler`, etc.

---

## 6. OOP Concepts Used

Every OOP principle is demonstrated in this microservice:

### 6a. Encapsulation

Fields are `private`, accessed only through getters/setters generated by Lombok:

```java
public class Promotion {
    @Getter @Setter
    private String title;          // External code uses promotion.getTitle() / promotion.setTitle()
}
```

### 6b. Abstraction

The `PromotionsService` **interface** defines the contract (WHAT the service does). The `PromotionsServiceImpl` **class** provides the implementation (HOW it does it). The controller depends only on the interface:

```java
// Controller depends on the INTERFACE, not the implementation
private final PromotionsService promotionsService;     // ← interface

// Spring injects PromotionsServiceImpl at runtime
```

This means:
- You can change the implementation without changing the controller
- You can create a mock implementation for unit tests
- The interface acts as documentation

### 6c. Inheritance

- `PromotionsServiceImpl implements PromotionsService` — interface implementation
- `BusinessException extends RuntimeException` — exception hierarchy
- `ResourceNotFoundException extends BusinessException` — specialized exception
- Every repository `extends JpaRepository<Entity, UUID>` — inherits CRUD methods (`save`, `findById`, `findAll`, `deleteById`, etc.)

### 6d. Polymorphism

- `List<PromotionResponse>` — the list interface is used regardless of the actual implementation (ArrayList, etc.)
- `ApiResponse.success(data)` — the static factory method returns `ApiResponse<T>` parameterized with any type
- `switch (promotion.getType())` — enum-based polymorphism. Each `PromotionType` constant has a different discount calculation behavior, all handled by the same `calculateDiscount` method

### 6e. Composition over Inheritance

The service class **has** repositories (composition) rather than **being** a repository (inheritance):

```java
// Composition: the service HAS repositories
public class PromotionsServiceImpl implements PromotionsService {
    private final PromotionRepository promotionRepository;        // has-a
    private final PromotionUsageRepository promotionUsageRepository; // has-a
    private final CashbackWalletRepository cashbackWalletRepository; // has-a
    private final CashbackTransactionRepository cashbackTransactionRepository; // has-a
    private final WebClient.Builder webClientBuilder;               // has-a
}
```

This is more flexible than extending a base class because you can compose with exactly the dependencies you need.

### 6f. Dependency Injection (DI)

Spring's **Inversion of Control (IoC)** container creates all beans and injects them where needed. The `@RequiredArgsConstructor` generates a constructor that Spring uses for injection:

```java
// Spring calls this constructor automatically
public PromotionsServiceImpl(
    PromotionRepository promotionRepository,     // ← Spring creates and injects this
    WebClient.Builder webClientBuilder            // ← Spring creates and injects this
) { ... }
```

No `new` keyword is ever used for dependencies. Benefits:
- **Loose coupling:** classes don't create their own dependencies
- **Testability:** you can pass mocks in unit tests
- **Lifecycle management:** Spring handles singleton scoping

---

## 7. Inter-Service Communication

Microservices need to talk to each other. This project uses three communication patterns:

### 7a. Async via Kafka (Event-Driven)

```
                     Kafka Topic
                 ┌────────────────┐
                 │ txn.completed   │
                 └────────────────┘
                        ▲
                        │ produces
                ┌───────┴───────┐
                │               │
    transfer-service      wallet-service
    (publishes when     (publishes when
     transfer done)      wallet credited)
                        │
                        │ consumes
                 ┌──────┴──────┐
                 │ promotions- │
                 │ service     │
                 │ (auto-apply │
                 │ cashback)   │
                 └─────────────┘
```

**Producer example (in wallet-service or transfer-service):**

```java
@Service
public class SomeService {
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void processTransaction(...) {
        // ... do business logic ...
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(txnId)
                .amount(amount)
                .receiverUserId(userId)
                .timestamp(OffsetDateTime.now())
                .build();
        kafkaTemplate.send("txn.completed", event);  // Fire-and-forget
    }
}
```

**Consumer example (in promotions-service):**

```java
@Component
public class PromotionEventConsumer {
    @KafkaListener(topics = "txn.completed")
    public void handle(TransactionEvent event) {
        // Process asynchronously — runs in its own thread
        // Does not block the original transaction
    }
}
```

**Why async (Kafka) over sync (HTTP)?**

- **Decoupling:** The transaction service doesn't need to know about promotions. It just publishes events.
- **Resilience:** If promotions-service is down, the transaction still completes. Events queue up in Kafka and are processed when promotions-service recovers.
- **Scalability:** Multiple instances of promotions-service can consume from the same topic in parallel.
- **Extensibility:** Adding a new service that reacts to transactions (e.g., fraud detection) requires zero changes to the transaction service — just add a new consumer.

### 7b. Sync via WebClient (HTTP)

```
POST /wallet/credit
┌──────────────┐     HTTP      ┌──────────────┐
│ promotions-  │ ──────────►   │ wallet-      │
│ service      │ ◄──────────   │ service      │
│              │   200 OK      │              │
└──────────────┘              └──────────────┘
```

**Caller (promotions-service):**

```java
WebClient webClient = webClientBuilder.build();
webClient.post()
    .uri("http://wallet-service/wallet/credit")        // Eureka service ID
    .bodyValue(Map.of(
        "userId", userId.toString(),
        "amount", amount,
        "description", "Cashback redemption",
        "txnId", redeemTxn.getId().toString()
    ))
    .retrieve()                                         // Send request
    .bodyToMono(Map.class)                              // Parse response as Map
    .block();                                           // Wait for response (synchronous)
```

The `@LoadBalanced` annotation on the `WebClient.Builder` bean replaces `wallet-service` with the actual IP:port from Eureka. Without this, you'd need to hardcode IPs or use service-specific URLs.

**Why sync (HTTP) over async?**

- The cashback redemption **needs an immediate response** to confirm success
- The user is waiting for a response on their phone
- If the wallet service is down, the promotion service should fail immediately so the user knows

### 7c. Via API Gateway

The gateway (Spring Cloud Gateway at `api-gateway/`) provides a **single public entry point** for all services:

```
Client ──► nginx (port 3000) ──► api-gateway (port 8080) ──► service
```

**How gateway routing works:**

```yaml
# api-gateway/application.yml
spring.cloud.gateway.routes:
  - id: promotions-service
    uri: lb://promotions-service        # Eureka load-balanced URI
    predicates:
      - Path=/v1/promotions/**          # Match incoming path
    filters:
      - StripPrefix=1                   # Remove /v1 prefix before forwarding
```

When a client calls `GET /v1/promotions/active`:
1. The gateway matches the path `/v1/promotions/**`
2. The `StripPrefix=1` filter removes `/v1` → forwards to `promotions-service` as `GET /promotions/active`
3. Eureka resolves `lb://promotions-service` to one of the running instances

---

## 8. Redis Caching Deep-Dive

While `promotions-service` uses Redis via the shared `CacheConfig`, `wallet-service` has richer caching patterns. Here's how caching works throughout the project:

### Auto-configuration: The shared `CacheConfig`

In `backend/shared/src/main/java/com/fdbpay/shared/config/CacheConfig.java`:

```java
@Configuration
public class CacheConfig {

    // CacheManager — for @Cacheable / @CacheEvict annotations
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());       // Handle OffsetDateTime
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(..., ObjectMapper.DefaultTyping.NON_FINAL);

        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))               // Default TTL: 15 minutes
                .serializeKeysWith(StringRedisSerializer)
                .serializeValuesWith(serializer);

        return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
    }

    // RedisTemplate — for direct Redis operations
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // ... configures key/value serializers ...
        return template;
    }
}
```

**Why `JavaTimeModule` is critical:** Without it, `OffsetDateTime` fields would be serialized as arrays `[2026, 7, 30, 10, 30, 0, 0, ...]` instead of ISO strings `"2026-07-30T10:30:00+06:30"`. The `JavaTimeModule` and `disable(WRITE_DATES_AS_TIMESTAMPS)` fix this.

### @Cacheable — Read-through caching

```java
@Service
public class WalletServiceImpl implements WalletService {

    @Override
    @Cacheable(value = "wallets", key = "#userId")   // Cache result by userId
    public WalletResponse getWallet(UUID userId) {
        // First call: queries DB, stores result in Redis
        // Subsequent calls: returns from Redis (skips DB)
        return findWallet(userId);
    }

    @Override
    @CacheEvict(value = "wallets", key = "#userId")  // Remove from cache on update
    @Transactional
    public WalletResponse updateWallet(UUID userId, ...) {
        // Update DB, then evict stale cache entry
    }
}
```

**Cache behavior:**

| Scenario | What happens |
|----------|-------------|
| First `getWallet(x)` | Query DB → store in Redis with key `wallets::x` → return result |
| Second `getWallet(x)` | Return from Redis (method body never executes) |
| `updateWallet(x)` | Update DB → remove `wallets::x` from Redis |
| After update, `getWallet(x)` | Cache miss → query DB → re-cache |

### RedisTemplate — For idempotency checks

```java
@Service
public class WalletServiceImpl implements WalletService {
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void processTransaction(CreateTransactionRequest request) {
        String idempotencyKey = "txn:" + request.getIdempotencyKey();

        // Check if this request was already processed
        if (redisTemplate.hasKey(idempotencyKey)) {
            throw new BusinessException(ErrorCodes.DUPLICATE_TRANSACTION, "Duplicate transaction");
        }

        // Mark as processing (TTL = 24 hours)
        redisTemplate.opsForValue().set(idempotencyKey, "PROCESSING", Duration.ofHours(24));

        // ... process transaction ...

        // Mark as completed
        redisTemplate.opsForValue().set(idempotencyKey, "COMPLETED", Duration.ofHours(24));
    }
}
```

Without this pattern, network retries or client double-clicks could create duplicate wallets or transactions.

---

## 9. Exception Handling

**The shared `GlobalExceptionHandler`** catches all exceptions from all services:

```java
@Slf4j
@RestControllerAdvice                                         // Applies to all @RestController's
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)                 // Catches BusinessException + subclasses
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: {} - {}", ex.getCode(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)   // Catches @Valid failures
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            errors.put(((FieldError) error).getField(), error.getDefaultMessage());
        });
        // Returns 400 with field-level errors
    }

    @ExceptionHandler(Exception.class)                        // Catch-all for unexpected errors
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unexpected exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }
}
```

**Exception hierarchy:**

```
RuntimeException
  └── BusinessException (code + message)
        ├── ResourceNotFoundException ("Wallet not found with id: ...")
        ├── InsufficientBalanceException
        └── (any custom exception with an error code)
```

**How the service throws exceptions:**

```java
// In service implementation:
throw new ResourceNotFoundException("Promotion", request.getPromoCode());
// → 400 with code="USER_NOT_FOUND", message="Promotion not found with id: ABC123"

throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Invalid promo code");
// → 400 with code="VALIDATION_ERROR", message="Invalid promo code"
```

The exception handler catches these and returns a consistent JSON error response that the frontend can parse uniformly.

---

## 10. Docker & Deployment

### Multi-stage Dockerfile

```dockerfile
# backend/Dockerfile

# STAGE 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY promotions-service/pom.xml promotions-service/pom.xml
# Copy ALL POMs first for dependency resolution optimization
COPY .../*/pom.xml .../

# Download dependencies (cached unless POMs change)
RUN mvn dependency:go-offline -B || true

# Copy all source code
COPY shared/ shared/
COPY promotions-service/ promotions-service/

# Build all modules
RUN mvn clean package -pl promotions-service -am -DskipTests -B

# STAGE 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /build/promotions-service/target/*.jar /app/promotions-service.jar

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -sf http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java -jar /app/${SERVICE_NAME}.jar"]
```

**Why multi-stage?**
- Stage 1 (builder) has Maven + JDK (heavy, ~700MB)
- Stage 2 (runtime) has only the JRE (light, ~200MB)
- The final image is small and has no build tools

**Why the `SERVICE_NAME` env var?**
The same Docker image (`fdbpay-backend:latest`) is used for ALL 18 backend services. The `SERVICE_NAME` environment variable determines which JAR to launch:

```yaml
# docker-compose.yml
promotions-service:
    image: fdbpay-backend:latest           # Same image for all services
    environment:
      SERVICE_NAME: promotions-service     # Which JAR to run
      SERVER_PORT: "8096"
```

This is a key optimization — one Docker build produces one image that can start any service.

### docker-compose.yml

```yaml
services:
  # ── Infrastructure ──
  postgres:
    image: postgres:16-alpine
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./docker/postgres-init.sql:/docker-entrypoint-initdb.d/init.sql  # Creates databases

  redis:
    image: redis:7-alpine

  kafka:
    image: apache/kafka:3.7.1

  # ── Backend Build ──
  backend-build:
    build:
      context: ./backend
      dockerfile: Dockerfile
    image: fdbpay-backend:latest
    profiles: ["build"]

  # ── Backend Services ──
  eureka-server:
    image: fdbpay-backend:latest
    environment: { SERVICE_NAME: eureka-server }
    ports: ["8761:8761"]

  api-gateway:
    image: fdbpay-backend:latest
    environment:
      SERVICE_NAME: api-gateway
      EUREKA_URL: http://eureka-server:8761/eureka/
    ports: ["8080:8080"]
    depends_on: [eureka-server]

  promotions-service:
    image: fdbpay-backend:latest
    environment:
      SERVICE_NAME: promotions-service
      SERVER_PORT: "8096"
      DB_HOST: postgres
      REDIS_HOST: redis
      EUREKA_URL: http://eureka-server:8761/eureka/
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    ports: ["8096:8096"]
    depends_on:
      postgres: { condition: service_healthy }
      redis:    { condition: service_healthy }
      kafka:    { condition: service_healthy }
```

---

## 11. Service Discovery (Eureka)

**Eureka Server** (at `eureka-server/`) is a service registry. Each microservice registers itself on startup and deregisters on shutdown.

**How registration happens automatically:**

1. `@EnableDiscoveryClient` on the main class tells Spring to register with Eureka
2. `eureka.client.service-url.defaultZone` tells it where Eureka is
3. The service's `spring.application.name` becomes its **Eureka service ID** (e.g., `promotions-service`)

**Why Eureka matters:**

- **Load balancing:** The `@LoadBalanced` WebClient can distribute requests across multiple instances of the same service.
- **Resilience:** If an instance fails, Eureka removes it from the registry within 30 seconds (heartbeat timeout). Clients automatically route to healthy instances.
- **Decoupling:** Services refer to each other by logical name (`promotions-service`) rather than by IP address (`192.168.1.5:8096`).

---

## 12. API Documentation (SpringDoc)

SpringDoc OpenAPI auto-generates an OpenAPI 3 specification from your controller code.

### What it generates

- **Endpoint paths** from `@RequestMapping`, `@GetMapping`, etc.
- **Request parameters** from `@RequestParam`, `@PathVariable`
- **Request bodies** from `@RequestBody` DTOs
- **Response schemas** from the return type of each controller method
- **Validation rules** from `@NotBlank`, `@NotNull`, `@Positive`, etc.

### Configuration

```yaml
springdoc:
  api-docs:
    path: /api-docs         # JSON at GET /api-docs
  swagger-ui:
    path: /swagger-ui.html  # UI at GET /swagger-ui.html
```

### How it's aggregated

The API Gateway has routes for each service's OpenAPI spec:

```yaml
springdoc.swagger-ui.urls:
  - name: promotions
    url: /v3/api-docs/promotions
```

And a proxy route:

```yaml
- id: swagger-promotions
  uri: lb://promotions-service
  predicates:
    - Path=/swagger-proxy/promotions/**
```

This lets you see ALL 16 services' APIs from a single Swagger UI at `/swagger-ui.html`.

---

## 13. Why Each `.java` File Exists

This section lists **every `.java` file** in `promotions-service` and explains its role, why it's required, and what happens if you omit it.

### Application Entry Point

| File | Why it's required | What happens without it |
|------|-------------------|------------------------|
| `PromotionsServiceApplication.java` | Contains `main()` method. `@SpringBootApplication` bootstraps all Spring beans, auto-configuration, and component scanning. `@EnableDiscoveryClient` registers with Eureka. | **No `.java` file means no JAR can be built.** This is the entry point the JVM calls via `java -jar`. Without it, nothing starts. |

### Config Layer

| File | Why it's required | What happens without it |
|------|-------------------|------------------------|
| `config/WebClientConfig.java` | Creates a `WebClient.Builder` bean annotated with `@LoadBalanced`. This is the **only place** that configures how this service makes HTTP calls to other services. Without `@LoadBalanced`, the Eureka service name (`wallet-service`) would fail to resolve because there's no DNS record for it. | Any HTTP call to another service (e.g., `wallet-service/wallet/credit` in `redeemCashback()`) would throw `UnknownHostException`. You'd have to hardcode IP addresses, which breaks in Docker and scaling. |

### Controller Layer

| File | Why it's required | What happens without it |
|------|-------------------|------------------------|
| `controller/PromotionController.java` | **The REST API layer.** Maps incoming HTTP requests to service method calls. Handles parameter extraction (`@RequestParam`, `@PathVariable`), request body deserialization (`@RequestBody`), validation triggering (`@Valid`), and HTTP status codes. Wraps results in `ApiResponse`. | Clients have no way to call the service. Without a controller, there are zero HTTP endpoints. The service compiles but is unreachable. |

### Service Layer

| File | Why it's required | What happens without it |
|------|-------------------|------------------------|
| `service/PromotionsService.java` | **The contract (interface).** Defines all business operations the service provides. Enables loose coupling — the controller depends on this interface, not the implementation. Enables Spring AOP proxies (`@Transactional`, `@Cacheable` work on interfaces). Enables mocking in unit tests. | Without the interface, the controller would depend directly on the implementation class. This makes it impossible to swap implementations, harder to test, and prevents Spring from creating proper AOP proxies (JDK dynamic proxies require an interface). |
| `service/impl/PromotionsServiceImpl.java` | **The business logic implementation.** Contains all logic: validation, discount calculation, database operations, inter-service calls. `@Transactional` ensures DB operations are atomic. Orchestrates the 4 repositories and WebClient. | Without this file, the service interface has no implementation. Spring would fail at startup with `NoSuchBeanDefinitionException` — no bean of type `PromotionsService` exists. |

### Model Layer (JPA Entities)

| File | Why it's required | What happens without it |
|------|-------------------|------------------------|
| `model/Promotion.java` | **Core domain entity.** Maps to the `promotions` database table. Every promotion CRUD operation needs this class to read/write rows. Hibernate uses it for INSERT, SELECT, UPDATE queries. Without it, `PromotionRepository` cannot function. | `PromotionRepository` (which extends `JpaRepository<Promotion, UUID>`) would fail to compile — the generic type `Promotion` doesn't exist. No promotions can be stored or retrieved. |
| `model/PromotionUsage.java` | **Tracks usage history.** Maps to `promotion_usages`. Records which user used which promotion on which transaction, along with discount/cashback amounts. Required for enforcing `maxUsagePerUser` and `maxUsageTotal` limits. | The `countByUserIdAndPromotionId()` query in `PromotionUsageRepository` would fail. The usage limit enforcement in `validatePromoCode()` would have to be removed or done without persistence (losing accuracy after service restart). |
| `model/CashbackWallet.java` | **Per-user cashback balance.** Maps to `cashback_wallets`. Tracks earned, redeemed, and current balance for each user. Without it, cashback could not be accumulated or tracked. | The `redeemCashback()` method checks `wallet.getBalance()` — without this entity, there's no way to know how much cashback a user has. Cashback redemptions would be impossible. |
| `model/CashbackTransaction.java` | **Audit trail for cashback.** Maps to `cashback_transactions`. Records every cashback earn/redeem/expire event. Required for showing the user their cashback history and for auditing. | Without it, users could not see their cashback history. Auditors could not verify cashback was correctly calculated. The `CashbackTransactionRepository` would fail to compile. |

### Enums

| File | Why it's required | What happens without it |
|------|-------------------|------------------------|
| `model/enums/PromotionType.java` | Defines the valid promotion types: `FIXED_DISCOUNT`, `PERCENTAGE_DISCOUNT`, `CASHBACK`, `BOGO`, `COUPON_CODE`. Used in the `calculateDiscount()` switch expression to determine how to compute the discount. | The `Promotion.type` field (annotated `@Enumerated(EnumType.STRING)`) needs an enum type. Without it, you'd need to use a raw `String` field, losing type safety — you could accidentally set `type = "pizza"` at compile time. The switch expression in `calculateDiscount()` would be impossible. |
| `model/enums/PromotionStatus.java` | Defines lifecycle states: `DRAFT`, `ACTIVE`, `PAUSED`, `EXPIRED`. Used in queries (`findByStatus`) and validation (`promotion.getStatus() != ACTIVE`). | Same as above — you'd use a raw `String`. A typo like `"ACTIVE"` vs `"ACTVE"` would silently match zero rows in queries. |
| `model/enums/FundingType.java` | Defines who funds the promotion: `MERCHANT` or `BANK`. Required field in `CreatePromotionRequest`. | The `@NotNull FundingType fundingType` field in the request DTO would have no type. Validation would need custom string checking. |
| `model/enums/CashbackTxnType.java` | Defines cashback transaction types: `EARNED`, `REDEEMED`, `EXPIRED`. Used in `CashbackTransaction.type` field. | Same type-safety issue as other enums. The consumer logic that creates `EARNED` transactions could accidentally use an invalid value. |

### DTO Layer (Request)

| File | Why it's required | What happens without it |
|------|-------------------|------------------------|
| `dto/request/CreatePromotionRequest.java` | Defines the JSON structure for `POST /promotions`. Carries `@NotBlank`, `@NotNull`, `@Positive` validation annotations that Spring validates at the controller boundary. | The `createPromotion()` controller method would need to accept raw `Map<String, Object>` and manually validate and extract every field. More code, more bugs, no compile-time safety. |
| `dto/request/ApplyPromotionRequest.java` | Defines JSON structure for `POST /promotions/apply`. Contains `promoCode` and `transactionAmount` with validation. | Same as above — manual extraction from `Map`. |
| `dto/request/RedeemCashbackRequest.java` | Defines JSON structure for `POST /promotions/cashback-redeem`. Contains `amount` with `@Positive`. | Same as above. |

### DTO Layer (Response)

| File | Why it's required | What happens without it |
|------|-------------------|------------------------|
| `dto/response/PromotionResponse.java` | Defines the JSON shape of a promotion returned to the client. Contains **computed fields** (`remainingUses`, `isActive`) that don't exist in the `Promotion` entity. Without this DTO, you'd either expose JPA-internal fields or would need to strip them manually from every response. | Returning the `Promotion` entity directly would expose `createdAt`, `updatedAt`, and all JPA fields in the JSON. Clients would see internal structure. Computed fields like `isActive` couldn't be added. |
| `dto/response/PromotionUsageResponse.java` | Defines JSON shape for promotion usage records. Isolates API contract from entity structure. | Same leaky-abstraction problem. |
| `dto/response/PromotionValidationResponse.java` | Defines JSON structure for promo code validation result (`valid`, `discount`, `message`). This is a **computed result** — it doesn't map to any database table. | Without this DTO, you'd return a raw `Map<String, Object>` with no type safety or documentation. |
| `dto/response/CashbackWalletResponse.java` | Defines JSON shape of cashback wallet. Contains only fields relevant to the client (not internal DB fields). | Same leaky-abstraction problem. |
| `dto/response/CashbackTransactionResponse.java` | Defines JSON shape of cashback transaction history. Used if you add a "view cashback history" endpoint later. | Without it, the endpoint would return entity objects with internal fields exposed. |

### Repository Layer

| File | Why it's required | What happens without it |
|------|-------------------|------------------------|
| `repository/PromotionRepository.java` | **The data access object for promotions.** Extends `JpaRepository<Promotion, UUID>` which provides `save()`, `findById()`, `findAll()`, `deleteById()` automatically. Also declares custom query methods: `findByStatus()`, `findByPromoCode()`, `findByStatusAndStartDateBeforeAndEndDateAfter()`. | The service layer has no way to read/write promotions from the database. All promotion operations would fail. |
| `repository/PromotionUsageRepository.java` | Data access for usage tracking. Provides `countByUserIdAndPromotionId()` via `@Query` — a custom JPQL query that counts rows instead of fetching them (more efficient for limit checking). | The per-user usage limit check in `validatePromoCode()` would need to fetch all rows and count them in Java, which is slower and uses more memory. |
| `repository/CashbackWalletRepository.java` | Data access for cashback wallets. Provides `findByUserId()` which returns `Optional<CashbackWallet>` — either the existing wallet or empty (to create a new one). | Without it, the service can't look up or create cashback wallets. Cashback credits and redemptions would be impossible. |
| `repository/CashbackTransactionRepository.java` | Data access for cashback audit trail. Provides paginated query `findByCashbackWalletIdOrderByCreatedAtDesc()`. | Without it, there's no way to query the cashback transaction history for display or audit. |

### Kafka Consumer Layer

| File | Why it's required | What happens without it |
|------|-------------------|------------------------|
| `consumer/PromotionEventConsumer.java` | **Listens to `txn.completed` Kafka topic.** When another service publishes a transaction completion event, this consumer auto-applies cashback promotions. This is how the service reacts to events **without the caller knowing about promotions at all**. | Cashback would never be auto-applied. Users would have to manually apply promotions (which most won't do). The entire "earn cashback automatically when you transact" feature would be missing. |

### What each file says about your architecture

| If you have this file... | It means your service needs to... |
|--------------------------|----------------------------------|
| `Application.java` | Start up (always required) |
| `config/*.java` | Configure some infrastructure (WebClient, Redis, security, etc.) |
| `controller/*.java` | Expose REST endpoints (always required for a REST API service) |
| `service/*Service.java` | Define a business contract (always required) |
| `service/impl/*ServiceImpl.java` | Implement business logic (always required) |
| `model/*.java` | Persist data in a database |
| `model/enums/*.java` | Use constrained string values (type-safe alternatives to raw strings) |
| `dto/request/*.java` | Accept structured input with validation |
| `dto/response/*.java` | Return structured output without exposing internals |
| `repository/*.java` | Query the database via Spring Data JPA |
| `consumer/*.java` | React to events from other services asynchronously |

## 14. Summary — Putting It All Together

Here's the complete lifecycle of a request through the promotions-service:

```
CLIENT REQUEST
     │
     ▼
┌────────────────────────────┐
│ nginx (port 3000)           │  Routes /v1/promotions/... to api-gateway
│ if /api/v1/ fails,          │
│ falls to SPA                │
└──────────┬─────────────────┘
           │
           ▼
┌────────────────────────────┐
│ api-gateway (port 8080)    │  Matches /v1/promotions/**, StripPrefix=1
│ Eureka resolves            │  → lb://promotions-service/promotions/**
│ promotions-service         │
└──────────┬─────────────────┘
           │
           ▼
┌────────────────────────────┐
│ AuthFilter                 │  Validates JWT token from Authorization header
│ (in gateway)               │  Returns 401 if invalid/missing
└──────────┬─────────────────┘
           │
           ▼
┌────────────────────────────┐
│ PromotionController        │  @RestController, @RequestMapping("/promotions")
│ Receives request, calls    │  Parameters extracted via @RequestParam
│ PromotionsService          │  Body validated via @Valid
└──────────┬─────────────────┘
           │
           ▼
┌────────────────────────────┐
│ PromotionsServiceImpl      │  Business logic
│                            │
│ For reads:                 │
│   PromotionRepository      │  → Spring Data JPA → PostgreSQL
│   @Cacheable (Redis)       │  → Cache hit = skip DB
│                            │
│ For writes:                │
│   @Transactional            │  → All-or-nothing DB operations
│   PromotionRepository.save │  → INSERT/UPDATE
│                            │
│ For cashback:              │
│   WebClient (HTTP)         │  → wallet-service (sync)
│   KafkaTemplate (events)   │  → notification-service (async)
└──────────┬─────────────────┘
           │
           ▼
┌────────────────────────────┐
│ ApiResponse.success(data)  │  Wraps result in consistent JSON envelope
└────────────────────────────┘
```

**Architecture principles to remember:**

1. **Controller is thin** — just marshals HTTP request/response. All logic is in the service.
2. **Service has the logic** — coordinates repositories, calls other services, manages transactions.
3. **Repository is Spring Data JPA** — write interfaces, not queries.
4. **Model is the JPA entity** — maps 1:1 with a database table.
5. **DTOs are the contract** — what the API sends and receives. Never expose entities directly.
6. **Shared library prevents duplication** — common exception handling, response format, event schemas, cache config.
7. **Flyway manages the database** — no `ddl-auto: update` in production.
8. **Kafka for async, WebClient for sync** — choose based on whether the caller needs an immediate response.
9. **Eureka for service discovery** — services refer to each other by logical name.
10. **Same Docker image for all services** — the `SERVICE_NAME` env var picks which JAR to run.
