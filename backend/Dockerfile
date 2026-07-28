FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY wallet-service/pom.xml wallet-service/pom.xml
COPY transfer-service/pom.xml transfer-service/pom.xml
COPY merchant-service/pom.xml merchant-service/pom.xml
COPY bill-payment-service/pom.xml bill-payment-service/pom.xml
COPY agent-service/pom.xml agent-service/pom.xml
COPY corporate-service/pom.xml corporate-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY fraud-risk-service/pom.xml fraud-risk-service/pom.xml
COPY reporting-service/pom.xml reporting-service/pom.xml
COPY kyc-service/pom.xml kyc-service/pom.xml
COPY settlement-service/pom.xml settlement-service/pom.xml
COPY dispute-service/pom.xml dispute-service/pom.xml
COPY audit-service/pom.xml audit-service/pom.xml
COPY remittance-service/pom.xml remittance-service/pom.xml
COPY promotions-service/pom.xml promotions-service/pom.xml
COPY support-service/pom.xml support-service/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml

RUN mvn dependency:go-offline -B || true

COPY shared/ shared/
COPY api-gateway/ api-gateway/
COPY auth-service/ auth-service/
COPY wallet-service/ wallet-service/
COPY transfer-service/ transfer-service/
COPY merchant-service/ merchant-service/
COPY bill-payment-service/ bill-payment-service/
COPY agent-service/ agent-service/
COPY corporate-service/ corporate-service/
COPY notification-service/ notification-service/
COPY fraud-risk-service/ fraud-risk-service/
COPY reporting-service/ reporting-service/
COPY kyc-service/ kyc-service/
COPY settlement-service/ settlement-service/
COPY dispute-service/ dispute-service/
COPY audit-service/ audit-service/
COPY remittance-service/ remittance-service/
COPY promotions-service/ promotions-service/
COPY support-service/ support-service/
COPY eureka-server/ eureka-server/

RUN mvn clean package -DskipTests -B -T 2C

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app

COPY --from=builder /build/eureka-server/target/*.jar /app/eureka-server.jar
COPY --from=builder /build/api-gateway/target/*.jar /app/api-gateway.jar
COPY --from=builder /build/auth-service/target/*.jar /app/auth-service.jar
COPY --from=builder /build/wallet-service/target/*.jar /app/wallet-service.jar
COPY --from=builder /build/transfer-service/target/*.jar /app/transfer-service.jar
COPY --from=builder /build/merchant-service/target/*.jar /app/merchant-service.jar
COPY --from=builder /build/bill-payment-service/target/*.jar /app/bill-payment-service.jar
COPY --from=builder /build/agent-service/target/*.jar /app/agent-service.jar
COPY --from=builder /build/corporate-service/target/*.jar /app/corporate-service.jar
COPY --from=builder /build/notification-service/target/*.jar /app/notification-service.jar
COPY --from=builder /build/fraud-risk-service/target/*.jar /app/fraud-risk-service.jar
COPY --from=builder /build/reporting-service/target/*.jar /app/reporting-service.jar
COPY --from=builder /build/kyc-service/target/*.jar /app/kyc-service.jar
COPY --from=builder /build/settlement-service/target/*.jar /app/settlement-service.jar
COPY --from=builder /build/dispute-service/target/*.jar /app/dispute-service.jar
COPY --from=builder /build/audit-service/target/*.jar /app/audit-service.jar
COPY --from=builder /build/remittance-service/target/*.jar /app/remittance-service.jar
COPY --from=builder /build/promotions-service/target/*.jar /app/promotions-service.jar
COPY --from=builder /build/support-service/target/*.jar /app/support-service.jar

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -sf http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java -jar /app/${SERVICE_NAME}.jar"]
