# FDB Pay — Issues & Solutions

This document tracks every issue encountered while building and running FDB Pay from scratch, along with the detailed solutions applied.

---

## 1. `shared/pom.xml` uses wrong parent POM

### Issue
`backend/shared/pom.xml` had `<parent>` set to `spring-boot-starter-parent:3.3.2` directly:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.2</version>
    <relativePath/>
</parent>
```

All other service POMs (auth-service, wallet-service, etc.) use `com.fdbpay:fdb-pay-parent:1.0.0-SNAPSHOT` as their parent. The `fdb-pay-parent` inherits from `spring-boot-starter-parent` and adds project-wide managed dependencies (Spring Cloud BOM, JJWT version, shared library version).

Because `shared/pom.xml` bypassed `fdb-pay-parent`, it could not resolve `${jjwt.version}` or other properties defined only in the root POM. Maven resolved these properties to their literal string values (e.g. `${jjwt.version}` remained unresolved), causing the build to fail.

### Solution
Changed `shared/pom.xml` to use `fdb-pay-parent` as its parent, with a `relativePath` pointing to the root POM:

```xml
<parent>
    <groupId>com.fdbpay</groupId>
    <artifactId>fdb-pay-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

This gives `shared` access to all properties and dependency management declared in the root POM. The `<relativePath>` is necessary because Maven resolves reactor parents by relative path rather than by searching the local repository.

---

## 2. `spring-boot-starter-kafka` artifact does not exist in Maven Central

### Issue
All service POMs declared a dependency on `spring-boot-starter-kafka` without a `<version>` tag, expecting the version to come from the `spring-boot-dependencies` BOM (inherited through `spring-boot-starter-parent → spring-boot-dependencies`).

However, the artifact `org.springframework.boot:spring-boot-starter-kafka` was **removed from Maven Central** for all versions prior to 4.0.0. The `spring-boot-dependencies:3.3.2` BOM no longer lists this artifact in its `<dependencyManagement>` section. When Maven tried to resolve the dependency, it failed with:

```
'dependencies.dependency.version' for org.springframework.boot:spring-boot-starter-kafka:jar is missing.
```

Verification confirmed:
```
$ curl -sI https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-kafka/3.3.2/spring-boot-starter-kafka-3.3.2.pom
HTTP/2 404
```

Only versions ≥ 4.0.0-M1 exist under this groupId/artifactId. The old versions were moved/removed when this starter was split into a separate project.

### Solution
Replaced `spring-boot-starter-kafka` with `spring-kafka` in every POM. The `spring-kafka` artifact provides the core Spring Kafka integration and **is** managed by `spring-boot-dependencies:3.3.2` (at version 3.2.2). Also added `kafka-clients` (managed at version 3.7.1) for completeness.

**Changes made in 19 POMs** (root pom's dependency management + 18 service/shared POMs):

| Before | After |
|---|---|
| `org.springframework.boot:spring-boot-starter-kafka` | `org.springframework.kafka:spring-kafka` |
| — | `org.apache.kafka:kafka-clients` |

The root POM's `<dependencyManagement>` now explicitly manages both artifacts:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
    <version>${spring-kafka.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>${kafka.version}</version>
</dependency>
```

---

## 3. `spring-boot-dependencies` BOM not inherited through parent chain

### Issue
Even after fixing the kafka artifact name, Maven could not resolve the version for `spring-kafka` (and other managed dependencies) in child modules. The error persisted:

```
'dependencies.dependency.version' for org.springframework.kafka:spring-kafka:jar is missing.
```

The root POM (`fdb-pay-parent`) correctly inherits from `spring-boot-starter-parent:3.3.2`, which in turn inherits from `spring-boot-dependencies:3.3.2`. The `spring-boot-dependencies` BOM includes `<dependencyManagement>` entries for `spring-kafka` at version 3.2.2 and `kafka-clients` at version 3.7.1.

However, Maven's reactor model building does not always resolve transitive `<dependencyManagement>` from grandparent POMs when the immediate parent is a reactor module. Child modules that declared `<parent>com.fdbpay:fdb-pay-parent:1.0.0-SNAPSHOT</parent>` could not see the managed versions from `spring-boot-dependencies`.

This was confirmed because even after running `mvn install -N` to install the parent POM into the local Maven repository, the error persisted.

### Solution
Two changes:

**a) Import `spring-boot-dependencies` BOM directly** in the root POM's `<dependencyManagement>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-dependencies</artifactId>
    <version>3.3.2</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

This makes all managed versions from `spring-boot-dependencies` directly available to every module in the reactor, without relying on transitive parent-chain resolution.

**b) Added explicit version properties** for artifacts that weren't resolving:

```xml
<properties>
    <spring-kafka.version>3.2.2</spring-kafka.version>
    <kafka.version>3.7.1</kafka.version>
</properties>
```

These are used in the `<dependencyManagement>` entry for `spring-kafka` and `kafka-clients`, ensuring that even if the BOM import fails, the versions are hard-coded.

---

## 4. `shared` module fails Spring Boot repackage goal

### Issue
The root POM configures the `spring-boot-maven-plugin` globally:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
</plugin>
```

When Maven runs `mvn clean package`, the plugin attempts to repackage the `shared` JAR as an executable Spring Boot fat-jar. Because `shared` is a library (not a Spring Boot application with a `main` class), the repackage goal fails with:

```
Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:3.3.2:repackage (repackage) on project shared: Execution repackage of goal ...: Unable to find main class
```

### Solution
Added `<skip>true</skip>` to the `spring-boot-maven-plugin` configuration in `shared/pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <skip>true</skip>
            </configuration>
        </plugin>
    </plugins>
</build>
```

This tells the plugin to skip the repackage goal for the shared module, while other services continue to get the fat-jar repackaging.

---

## 5. Missing `spring-boot-starter-webflux` in services using `WebClient`

### Issue
Several services use `org.springframework.web.reactive.function.client.WebClient` for making HTTP requests but did not declare `spring-boot-starter-webflux` as a dependency. This caused compilation failures:

```
package org.springframework.web.reactive.function.client does not exist
cannot find symbol: class WebClient
```

The services affected:
- auth-service
- agent-service
- corporate-service
- promotions-service
- remittance-service
- reporting-service
- dispute-service
- transfer-service

`bill-payment-service` already had `spring-boot-starter-webflux` declared and compiled successfully.

### Solution
Added the following dependency block to each affected service's `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

No `<version>` tag is needed because the version is managed by the `spring-boot-dependencies` BOM (imported in the root POM). The `spring-boot-starter-webflux` artifact is listed in `spring-boot-dependencies:3.3.2` with version `3.3.2`.

---

## 6. Missing `Pageable` import in `corporate-service`

### Issue
`corporate-service/src/main/java/com/fdbpay/corporate/service/impl/ApprovalServiceImpl.java` and `PayrollServiceImpl.java` use `Pageable` in method signatures but never imported it:

```java
public ApiResponse<?> getPendingApprovals(UUID approverId, Pageable pageable) {
```

The files imported `Page` and `PageRequest` from `org.springframework.data.domain` but omitted `Pageable`. This caused:

```
cannot find symbol: class Pageable
```

### Solution
Added the missing import to both files:

```java
import org.springframework.data.domain.Pageable;
```

---

## 7. Missing `EnumUtils` from Apache Commons Lang3 in `bill-payment-service`

### Issue
`AirtimeTopupServiceImpl.java` uses `EnumUtils.isValidEnum()` but neither imported it nor had `commons-lang3` as a Maven dependency:

```java
if (!EnumUtils.isValidEnum(AirtimeProvider.class, provider.name())) {
```

Resulted in:
```
cannot find symbol: variable EnumUtils
```

### Solution
**a)** Added `commons-lang3` dependency to `bill-payment-service/pom.xml`:

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
</dependency>
```

No version is needed because `commons-lang3` is managed by `spring-boot-dependencies`.

**b)** Added the import to `AirtimeTopupServiceImpl.java`:

```java
import org.apache.commons.lang3.EnumUtils;
```

---

## 8. `Long.multiply(BigDecimal)` — type mismatch errors

### Issue
Two files called `.multiply()` on a `Long` variable, but `multiply()` is only available on `BigDecimal`:

- `agent-service/CommissionServiceImpl.java` line 47: `amount.multiply(rate)` — `amount` is `Long`
- `wallet-service/SavingsServiceImpl.java` line 267: `pocket.getCurrentAmount().multiply(...)` — `getCurrentAmount()` returns `Long`

Resulted in:
```
cannot find symbol: method multiply(java.math.BigDecimal)
  location: variable amount of type java.lang.Long
```

### Solution
Converted the `Long` to `BigDecimal` before calling `multiply()`:

```java
// Before (broken):
amount.multiply(rate)

// After (fixed):
BigDecimal.valueOf(amount).multiply(rate)
```

Similarly for the wallet-service case:
```java
BigDecimal.valueOf(pocket.getCurrentAmount()).multiply(rate)
```

---

## 9. `List.orElseThrow()` — method not available on `List`

### Issue
`merchant-service/InvoiceController.java` line 28 calls `.orElseThrow()` directly on a `List<Merchant>`, but `List` does not have this method. This is a common mistake — `orElseThrow` belongs to `Optional`, not `List`.

```
cannot find symbol: method orElseThrow(()->new Re[...]erId))
  location: interface java.util.List<com.fdbpay.merchant.service.model.Merchant>
```

### Solution
Converted the `List` to a `Stream` and used `findFirst()` to get an `Optional`, then chained `orElseThrow()`:

```java
// Before (broken):
merchantRepository.findByMerchantId(merchantId)
    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));

// After (fixed):
merchantRepository.findByMerchantId(merchantId)
    .stream()
    .findFirst()
    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
```

This assumes `findByMerchantId()` returns a `List<Merchant>` (which is the standard Spring Data JPA return type for `findBy*` methods).

---

## 10. `WebClient.RequestBodySpec.queryParam()` — wrong method chain

### Issue
`agent-service/CommissionServiceImpl.java` line 148 tried to call `.queryParam()` on `WebClient.RequestBodySpec`, but this method is not available on that type. In Spring WebFlux, `.queryParam()` should be applied during URI construction, not after the request body spec is obtained.

```
cannot find symbol: method queryParam(java.lang.String,java.util.UUID)
  location: interface org.springframework.web.reactive.function.client.WebClient.RequestBodySpec
```

### Solution
Restructured the WebClient call to apply `queryParam()` inside the `.uri(uriBuilder -> ...)` lambda:

```java
// Before (broken):
webClient.post()
    .uri("/path")
    .queryParam("key", value)  // Not available on RequestBodySpec
    .bodyValue(body)
    .retrieve()
    ...

// After (fixed):
webClient.post()
    .uri(uriBuilder -> uriBuilder
        .path("/path")
        .queryParam("key", value)
        .build())
    .bodyValue(body)
    .retrieve()
    ...
```

The `uri(Function<UriBuilder, URI>)` overload allows building the complete URI including query parameters before the request body specification.

---

## 11. Shared library beans not scanned — `JwtTokenProvider` not found

### Issue
`auth-service` and `api-gateway` depend on `JwtTokenProvider` from the `shared` library (package `com.fdbpay.shared.config`). At runtime, Spring Boot failed to find the bean:

```
Parameter 2 of constructor in com.fdbpay.auth.service.impl.AuthServiceImpl required a bean of type 'com.fdbpay.shared.config.JwtTokenProvider' that could not be found.
```

Spring Boot's `@SpringBootApplication` annotation enables component scanning only for the package of the main class and its sub-packages (`com.fdbpay.auth` and `com.fdbpay.apigateway` respectively). Beans in `com.fdbpay.shared.config` are never discovered.

### Solution
Added `scanBasePackages` to the `@SpringBootApplication` annotation in both applications:

**AuthServiceApplication.java:**
```java
@SpringBootApplication(scanBasePackages = {"com.fdbpay.auth", "com.fdbpay.shared"})
```

**ApiGatewayApplication.java:**
```java
@SpringBootApplication(scanBasePackages = {"com.fdbpay.apigateway", "com.fdbpay.shared"})
```

This tells Spring Boot to scan both the service's own package and the shared library's package for components, configurations, and beans. A more production-grade approach would be to register the shared library as an auto-configuration using `spring.factories` or `@AutoConfiguration`, but `scanBasePackages` is sufficient for getting started.

---

## 12. API Gateway route for auth-service needs path rewriting

### Issue
The API Gateway routes `/v1/auth/**` to the auth-service via:

```yaml
- id: auth-service
  uri: lb://auth-service
  predicates:
    - Path=/v1/auth/**
```

However, the auth-service controller is mapped to `@RequestMapping("/auth")` — not `/v1/auth`. The gateway forwards the full path as-is, so the auth-service receives `/v1/auth/register` and `/v1/auth/login`, which don't match the controller's mapping of `/auth/register` and `/auth/login`.

The gateway returned `503 Service Unavailable` because the route could not match, even though Eureka showed both services as `UP`.

### Solution
Added a `RewritePath` filter to the auth-service route to strip the `/v1` prefix:

```yaml
- id: auth-service
  uri: lb://auth-service
  predicates:
    - Path=/v1/auth/**
  filters:
    - RewritePath=/v1/auth/(?<segment>.*), /auth/${segment}
```

This regex-based filter captures everything after `/v1/auth/` and rewrites the path to `/auth/<captured-segment>`. For example:
- `/v1/auth/register` → `/auth/register`
- `/v1/auth/login` → `/auth/login`

The frontend's nginx already proxies `/v1/*` requests to the API Gateway, so this fix makes the entire chain work: browser → nginx → api-gateway → auth-service.

---

## 13. `bitnami/kafka:3.7` Docker image not found

### Issue
The docker-compose.yml used `bitnami/kafka:3.7` as the Kafka image. This tag (and all Bitnami Kafka tags) were unavailable in the Docker registry:

```
Error response from daemon: failed to resolve reference "docker.io/bitnami/kafka:3.7": docker.io/bitnami/kafka:3.7: not found
```

The `bitnami/kafka:latest` tag was also unavailable. This may be due to registry changes or image deprecation.

### Solution
Switched to `apache/kafka:3.7.1` — the official Apache Kafka Docker image. The environment variables and configuration differ from the Bitnami image:

| Aspect | bitnami/kafka | apache/kafka |
|--------|---------------|--------------|
| Env vars | `KAFKA_CFG_*` | `KAFKA_*` |
| KRaft cluster ID | Not needed (auto-generated) | Required via `CLUSTER_ID` env |
| Health check | `kafka-topics.sh` path differs | `/opt/kafka/bin/kafka-topics.sh` |

Updated configuration:

```yaml
kafka:
  image: apache/kafka:3.7.1
  environment:
    CLUSTER_ID: 4L6g3nShT-eMCtK--X86sw
    KAFKA_NODE_ID: 0
    KAFKA_PROCESS_ROLES: controller,broker
    KAFKA_CONTROLLER_QUORUM_VOTERS: 0@kafka:9093
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
    KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
```

---

## 14. Port conflicts with existing Docker containers

### Issue
Multiple ports were already in use by other projects running on the same host:

| Port | Service | Conflicting Container |
|------|---------|----------------------|
| 6379 | Redis | `khmer-real-estate-redis-1` |
| 5432 | Postgres | Host postgres (system service) + `khmer-real-estate-postgres-1` |
| 5433 | Postgres (mapped) | `khmer-real-estate-postgres-1` (mapped to 5433) |
| 8080 | API Gateway | `khmer-real-estate-api-1` |
| 3000 | Frontend | `khmer-real-estate-client-1` |

Docker failed with:
```
Error response from daemon: driver failed programming external connectivity on endpoint ...
Bind for 0.0.0.0:6379 failed: port is already allocated
```

### Solution
**a)** Stopped conflicting containers from the other project:
```bash
docker stop <container-id>
```

**b)** Removed the host port mapping for Postgres (containers communicate over the Docker network, so external port exposure is optional):
```yaml
# Before:
ports:
  - "5432:5432"

# After: (no ports block)
```

**c)** For the API Gateway and Frontend, recreated containers with `docker compose rm -f <service>` followed by `docker compose up -d <service>` to ensure they joined the correct Docker network (containers created during port conflicts sometimes start without a network).

---

## 15. API Gateway container starts without a network

### Issue
When Docker Compose fails to bind a host port, it still creates the container but does not attach it to the Docker network. This resulted in:

```
$ docker inspect fdb_pay-api-gateway-1 --format '{{json .NetworkSettings.Networks}}'
{}
```

The container could not resolve any service names (`redis`, `eureka-server`, etc.), causing DNS failures:

```
java.net.UnknownHostException: eureka-server: Temporary failure in name resolution
io.netty.resolver.dns.DnsNameResolverTimeoutException: query 'redis' via UDP timed out
```

### Solution
Removed the container and let Docker Compose recreate it:
```bash
docker compose rm -f api-gateway
docker compose up -d api-gateway
```

This ensures the container is properly attached to the `fdb_pay_default` network and can resolve service names.

---

## 16. `shared` library POMs not copied in Docker build

### Issue
The Dockerfile was modified to build only a subset of services (`shared,api-gateway,auth-service,notification-service,eureka-server`), but the root `pom.xml` declares all 20 modules. Maven requires all declared module directories to exist during POM parsing:

```
Child module /build/wallet-service of /build/pom.xml does not exist
Child module /build/transfer-service of /build/pom.xml does not exist
... (15 more)
```

The POM validation fails before any module is built.

### Solution
Two changes:

**a)** Keep all `COPY pom.xml` lines in the Dockerfile so that Maven can parse the complete module list:
```dockerfile
COPY wallet-service/pom.xml wallet-service/pom.xml
COPY transfer-service/pom.xml transfer-service/pom.xml
# ... (all 20 module POMs)
```

**b)** Use `-pl` with `-am` to build only the needed modules:
```dockerfile
RUN mvn clean package -pl shared,api-gateway,auth-service,notification-service,eureka-server -am -DskipTests -B
```

The `-pl` flag restricts the build to these 5 modules. The `-am` ("also make") flag ensures that dependencies of these modules (like `shared`) are also built. Modules not in `-pl` are parsed but not compiled.

---

## Summary of files modified

| File | Change |
|------|--------|
| `backend/pom.xml` | Added `spring-boot-dependencies` BOM import, explicit `spring-kafka` + `kafka-clients` versions, version properties |
| `backend/shared/pom.xml` | Changed parent to `fdb-pay-parent`, added spring-boot-maven-plugin skip |
| All 18 service `pom.xml` | Replaced `spring-boot-starter-kafka` with `spring-kafka` |
| `backend/api-gateway/pom.xml` | Added Lombok dependency |
| 6 service `pom.xml` | Added `spring-boot-starter-webflux` |
| `backend/bill-payment-service/pom.xml` | Added `commons-lang3` |
| `backend/api-gateway/src/main/resources/application.yml` | Added `RewritePath` filter for auth-service |
| `backend/auth-service/src/main/java/.../AuthServiceApplication.java` | Added `scanBasePackages` |
| `backend/api-gateway/src/main/java/.../ApiGatewayApplication.java` | Added `scanBasePackages` |
| `backend/corporate-service/src/main/java/.../ApprovalServiceImpl.java` | Added `Pageable` import |
| `backend/corporate-service/src/main/java/.../PayrollServiceImpl.java` | Added `Pageable` import |
| `backend/bill-payment-service/src/main/java/.../AirtimeTopupServiceImpl.java` | Added `EnumUtils` import |
| `backend/agent-service/src/main/java/.../CommissionServiceImpl.java` | Fixed `Long.multiply` and `WebClient.queryParam` |
| `backend/wallet-service/src/main/java/.../SavingsServiceImpl.java` | Fixed `Long.multiply` |
| `backend/merchant-service/src/main/java/.../InvoiceController.java` | Fixed `List.orElseThrow` to `stream().findFirst().orElseThrow` |
| `docker-compose.yml` | Changed Kafka image to `apache/kafka:3.7.1`, removed postgres port mapping |
| `backend/Dockerfile` | Added all POM copies, updated build command with `-pl` |
