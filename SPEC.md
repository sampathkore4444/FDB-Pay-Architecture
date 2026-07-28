# FDB Pay — End-to-End Product & Technical Specification

**Version:** 1.0
**Date:** July 2026
**Classification:** Internal — FDB Bank Myanmar

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Goals & Objectives](#2-goals--objectives)
3. [User Personas](#3-user-personas)
4. [Feature Specification](#4-feature-specification)
5. [System Architecture](#5-system-architecture)
   - [Technology Stack](#53-technology-stack)
6. [Detailed Service Design](#6-detailed-service-design)
7. [Database Design](#7-database-design)
8. [API Specification](#8-api-specification)
9. [End-to-End Payment Flows](#9-end-to-end-payment-flows)
10. [Security & Compliance](#10-security--compliance)
11. [Infrastructure & Deployment](#11-infrastructure--deployment)
12. [Integrations](#12-integrations)
13. [Non-Functional Requirements](#13-non-functional-requirements)
14. [Error Handling & Resilience](#14-error-handling--resilience)
15. [Analytics & Reporting](#15-analytics--reporting)
16. [Phased Rollout Plan](#16-phased-rollout-plan)
17. [Glossary](#17-glossary)

---

## 1. Executive Summary

**FDB Pay** is a digital payment platform built by FDB Bank to serve Myanmar's rapidly growing mobile-first economy. It enables consumers, merchants, and agents to send, receive, and manage money through a unified ecosystem spanning a mobile app, web portal, USSD, and POS integrations.

The platform supports peer-to-peer (P2P) transfers, merchant payments (QR & POS), bill payments, airtime top-ups, salary disbursements, cross-border remittances, and merchant settlement — all backed by FDB Bank's core banking infrastructure.

### Key Differentiators

- **Bank-backed trust**: Regulated by the Central Bank of Myanmar (CBM), deposits insured under Myanmar's banking framework.
- **Offline-first**: USSD fallback ensures accessibility in low-connectivity areas across all 7 states and 7 regions.
- **Multi-rail**: Supports mobile money interoperability (MPGS, Myanmar Payment Union), bank-to-bank (RTGS), and card networks.
- **Agent network**: Leverages FDB's existing branch and agent footprint for cash-in / cash-out (CICO).

---

## 2. Goals & Objectives

| # | Goal | Success Metric | Target |
|---|------|---------------|--------|
| G1 | Drive digital payment adoption in Myanmar | Monthly Active Users (MAU) | 500K within 12 months |
| G2 | Onboard merchants for QR & POS payments | Registered merchants | 10,000 within 12 months |
| G3 | Reduce cash dependency for everyday transactions | Digital transaction volume | 30% of FDB retail transactions digital within 18 months |
| G4 | Enable interoperability with Myanmar's payment ecosystem | Connected schemes | MPU, MPGS, CBM RTGS, mobile money operators |
| G5 | Generate new revenue streams | Net payment revenue | Break-even within 24 months |
| G6 | Serve unbanked/underbanked via agent-assisted onboarding | Agent-assisted accounts opened | 100K within 12 months |

---

## 3. User Personas

### 3.1 Consumer (P2P / Payer)

- **Profile**: Myanmar mobile user, ages 18–55, smartphone or feature phone.
- **Needs**: Send money to family/friends, pay bills, top up airtime, QR payments at shops.
- **Channels**: Mobile app (Android/iOS), USSD, web.

### 3.2 Merchant

- **Profile**: Small-to-medium business (tea shops, groceries, restaurants, market vendors).
- **Needs**: Accept digital payments, receive daily settlement, view transaction history, manage staff tills.
- **Channels**: Merchant app, web dashboard, POS terminal, QR code display.

### 3.3 Agent (CICO)

- **Profile**: FDB Bank branch staff or authorized agent shop owners.
- **Needs**: Facilitate cash-in/cash-out, earn commissions, manage float.
- **Channels**: Agent app, USSD.

### 3.4 Corporate / Payroll Client

- **Profile**: Businesses disbursing salaries, vendors making bulk payments.
- **Needs**: Bulk disbursement, payroll integration, reconciliation reports, API access.
- **Channels**: Corporate web portal, API.

### 3.5 Administrator (Internal)

- **Profile**: FDB Bank operations, compliance, product teams.
- **Needs**: Monitor transactions, manage risk, configure products, generate reports, handle disputes.
- **Channels**: Admin web dashboard.

---

## 4. Feature Specification

### 4.1 Consumer Features

| Feature | Description | Priority |
|---------|-------------|----------|
| **Registration & KYC** | Phone-number-based registration, tiered KYC (basic → enhanced → full), NRC verification | P0 |
| **Account Wallet** | Digital wallet in MMK (and future multi-currency), top-up via bank transfer, CICO, card | P0 |
| **P2P Transfer** | Send money to any FDB Pay user or mobile number, instant or scheduled | P0 |
| **QR Payment (Scan & Pay)** | Scan merchant QR code, enter PIN, payment settled instantly to merchant | P0 |
| **Bill Payment** | Electricity (MEP, SEPE, ZP, EP), water, internet, TV subscription | P0 |
| **Airtime Top-Up** | MPT, Ooredoo, Mytel, Telenor (now Atom) | P0 |
| **Merchant Directory** | Browse/search nearby merchants, view offers | P1 |
| **Transaction History** | Full history with filters, search, export to PDF/CSV | P0 |
| **Promotions & Cashback** | Merchant-funded and bank-funded offers, coupon codes | P1 |
| **Savings Pockets** | Sub-wallets for goal-based saving, earn interest (future) | P2 |
| **Remittance (Inbound)** | Receive international remittances via partner corridors | P1 |
| **Request Money** | Send payment request via SMS/link with QR | P2 |
| **Scheduled Payments** | Recurring transfers and bill payments | P1 |
| **Multi-Language** | Myanmar (Burmese), English interface | P0 |

### 4.2 Merchant Features

| Feature | Description | Priority |
|---------|-------------|----------|
| **Merchant Registration** | Business registration with license upload, UBS/MRB verification | P0 |
| **QR Code Generation** | Static QR (merchant display) and dynamic QR (per-transaction) | P0 |
| **POS Terminal** | Sound-box / Bluetooth POS for audio confirmation | P1 |
| **Settlement** | T+1 settlement to FDB merchant bank account, configurable schedules | P0 |
| **Merchant Dashboard** | Transaction reports, settlement history, refund management | P0 |
| **Multi-Staff Till** | Sub-accounts for staff with role-based access | P1 |
| **Invoices** | Generate and send digital invoices to customers | P2 |
| **Inventory Integration** | Future: link to POS/inventory systems via API | P3 |

### 4.3 Agent Features

| Feature | Description | Priority |
|---------|-------------|----------|
| **Agent Registration** | KYC-verified agent onboarding with FDB authorization | P0 |
| **Cash-In** | Customer deposits cash, agent credits customer wallet | P0 |
| **Cash-Out** | Customer withdraws cash via agent, agent debits customer wallet | P0 |
| **Float Management** | Agent tracks own float balance, rebalancing requests | P0 |
| **Commission Tracking** | Real-time commission balance, withdrawal history | P1 |
| **QR for Agent** | Customer scans agent QR for CICO transactions | P1 |

### 4.4 Corporate Features

| Feature | Description | Priority |
|---------|-------------|----------|
| **Bulk Disbursement** | Upload CSV, disburse salaries/payments to thousands of wallets | P0 |
| **Payroll API** | RESTful API for ERP/payroll system integration | P1 |
| **Multi-Approval Workflow** | Maker-checker approval for large disbursements | P1 |
| **Reconciliation Reports** | Automated daily/monthly reconciliation files | P0 |
| **Dedicated Account Manager** | Support channel for corporate clients | P1 |

### 4.5 Admin Features

| Feature | Description | Priority |
|---------|-------------|----------|
| **Transaction Monitoring** | Real-time dashboard, alerts, velocity checks | P0 |
| **KYC Management** | Review/approve/reject KYC documents, tier upgrades | P0 |
| **Merchant Management** | Onboard, activate, suspend merchants | P0 |
| **Dispute Resolution** | Chargeback/dispute workflow, evidence upload, resolution tracking | P0 |
| **AML/CFT Screening** | sanctions screening, suspicious transaction reports (STR) to FIU | P0 |
| **Configuration** | Fee schedules, limits, promotions, system parameters | P0 |
| **Audit Trail** | Immutable log of all admin actions | P0 |
| **Reporting** | Transaction volumes, revenue, merchant performance, compliance reports | P0 |

---

## 5. System Architecture

### 5.1 High-Level Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           CLIENT LAYER                                  │
│  ┌──────────┐  ┌──────────┐  ┌─────────┐  ┌─────────┐  ┌───────────┐  │
│  │ Mobile   │  │ Web      │  │ USSD    │  │ POS     │  │ Corporate │  │
│  │ App      │  │ Portal   │  │ Gateway │  │Terminal │  │ Portal    │  │
│  └────┬─────┘  └────┬─────┘  └────┬────┘  └────┬────┘  └─────┬─────┘  │
│       └──────────────┴─────────────┴────────────┴─────────────┘         │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │ HTTPS / WSS / SMPP
┌──────────────────────────┴───────────────────────────────────────────────┐
│                        API GATEWAY / BFF                                │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐                   │
│  │ Rate Limiter│  │ Auth (JWT)   │  │ Request      │                   │
│  │ & Throttle  │  │ & Session    │  │ Router       │                   │
│  └─────────────┘  └──────────────┘  └──────────────┘                   │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │
┌──────────────────────────┴───────────────────────────────────────────────┐
│                        CORE SERVICES (Kubernetes)                       │
│                                                                         │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐           │
│  │ Auth &     │ │ Wallet     │ │ Transfer   │ │ Merchant   │           │
│  │ Identity   │ │ Service    │ │ Service    │ │ Service    │           │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘           │
│                                                                         │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐           │
│  │ Bill Pay   │ │ Notification│ │ KYC &      │ │ Fraud &    │           │
│  │ Service    │ │ Service    │ │ Compliance │ │ Risk       │           │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘           │
│                                                                         │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐           │
│  │ Agent      │ │ Corporate  │ │ Promotion  │ │ Settlement │           │
│  │ Service    │ │ Service    │ │ Engine     │ │ & Clearing │           │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘           │
│                                                                         │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐                           │
│  │ Reporting  │ │ Notification│ │ Config     │                           │
│  │ & Analytics│ │ (Push/SMS) │ │ Service    │                           │
│  └────────────┘ └────────────┘ └────────────┘                           │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │
┌──────────────────────────┴───────────────────────────────────────────────┐
│                     MESSAGING & EVENT LAYER                              │
│  ┌──────────────────────────────────────────────────────────────┐       │
│  │              Apache Kafka (Event Bus)                        │       │
│  │  Topics: txn.* │ merchant.* │ kyc.* │ settlement.* │ audit.*│       │
│  └──────────────────────────────────────────────────────────────┘       │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │
┌──────────────────────────┴───────────────────────────────────────────────┐
│                         DATA LAYER                                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│  │PostgreSQL│ │ MongoDB  │ │ Redis    │ │ Elastic  │ │ S3/MinIO │     │
│  │ (OLTP)   │ │ (KYC/doc)│ │ (Cache/  │ │ Search   │ │ (Files/  │     │
│  │          │ │          │ │  Session)│ │ (Logs)   │ │  Docs)   │     │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │
┌──────────────────────────┴───────────────────────────────────────────────┐
│                     EXTERNAL INTEGRATIONS                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│  │FDB Core  │ │ MPU/MPGS │ │ CBM RTGS │ │ Biller   │ │ Mobile   │     │
│  │Banking   │ │ (Card    │ │ (Inter-  │ │ APIs     │ │ Money    │     │
│  │(CBS)     │ │ Network) │ │ bank)    │ │          │ │Operators │     │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │
│                                                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                  │
│  │ SMS      │ │ KYC/NRC  │ │Sanctions │ │Corridor  │                  │
│  │ Providers│ │ Verify   │ │ Screening│ │ Partners │                  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘                  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Architecture Principles

| Principle | Description |
|-----------|-------------|
| **Microservices** | Each bounded context is an independent, deployable service |
| **Event-Driven** | Asynchronous communication via Kafka for decoupling and auditability |
| **CQRS** | Separate read/write models for high-throughput services (Wallet, Transfer) |
| **Idempotency** | All payment operations are idempotent via client-supplied idempotency keys |
| **Zero Trust** | mTLS between services, JWT validation at gateway, least-privilege IAM |
| **Observability** | Distributed tracing (OpenTelemetry), centralized logging (ELK), metrics (Prometheus/Grafana) |
| **Offline Resilience** | USSD as fallback, local queue on POS, retry with exponential backoff |

### 5.3 Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Backend Framework** | Java 21 + Spring Boot 3 | Primary service implementation |
| **Frontend (Mobile)** | Kotlin (Android), Swift (iOS) | Native mobile apps |
| **Frontend (Web)** | React | Admin, merchant, corporate dashboards |
| **API Protocol** | REST (JSON), gRPC (internal) | External & internal communication |
| **Database (OLTP)** | PostgreSQL 16 | Primary transactional data store (wallets, ledgers, users) |
| **Database (Document)** | MongoDB 7 | KYC documents, unstructured data |
| **Cache / Session** | Redis 7 (Cluster) | Session store, rate limiting, balance cache, OTP |
| **Search / Logs** | Elasticsearch 8 | Full-text search, centralized logging, audit trail |
| **Object Storage** | MinIO (S3-compatible) | KYC files, reports, exports |
| **Event Streaming** | Apache Kafka 3 | Async inter-service communication, event sourcing |
| **Stream Processing** | Apache Flink | Real-time fraud detection, velocity aggregation |
| **Container Runtime** | Docker | Application packaging |
| **Orchestration** | Kubernetes (on-premise) | Service deployment, scaling, self-healing |
| **Service Mesh** | Istio | mTLS, traffic management, observability |
| **CI/CD** | GitLab CI + ArgoCD | Build pipelines, GitOps deployment |
| **Monitoring** | Prometheus + Grafana | Metrics collection, dashboards, alerting |
| **Logging** | Fluentd → Elasticsearch → Kibana | Centralized log aggregation |
| **Tracing** | Jaeger (OpenTelemetry) | Distributed request tracing |
| **Secrets Management** | HashiCorp Vault | Secure secret storage and rotation |
| **HSM** | Thales Luna HSM | Cryptographic key management (PCI DSS) |
| **WAF** | ModSecurity + Nginx | Web application firewall |
| **Load Balancer** | Nginx / HAProxy | External traffic routing, SSL termination |
| **DNS** | CoreDNS | Internal Kubernetes service discovery |
| **Backup** | Velero (K8s), pgBackRest (PG) | Cluster and database backup/restore |
| **SMS Providers** | MPT Bulk SMS, Twilio, Unifone | Transactional and promotional SMS |
| **Push Notifications** | FCM (Android), APNs (iOS) | Mobile push delivery |

---

## 6. Detailed Service Design

### 6.1 Auth & Identity Service

**Responsibility:** User registration, authentication, session management, PIN management.

| Component | Detail |
|-----------|--------|
| Registration | Phone number + OTP verification → basic account creation |
| Login | Phone number + MPIN (4-6 digit) or biometric (app) |
| Session | JWT access token (15 min) + refresh token (7 days), stored in Redis |
| PIN | Bcrypt-hashed MPIN, separate PIN for transaction authorization |
| Device Mgmt | Trusted device registration, device fingerprinting, max 3 devices |
| 2FA | OTP for sensitive operations (high-value transfer, profile change) |
| Token Service | OAuth2 token issuance for third-party API consumers |

### 6.2 Wallet Service

**Responsibility:** Wallet lifecycle, balance management, ledger, top-up, withdrawal.

```
┌──────────────────────────────────────────┐
│            WALLET SERVICE                │
│                                          │
│  ┌──────────┐     ┌──────────────────┐   │
│  │ Wallet   │────▶│ Double-Entry     │   │
│  │ Manager  │     │ Ledger           │   │
│  └──────────┘     └──────────────────┘   │
│       │                                  │
│  ┌────┴─────┐     ┌──────────────────┐   │
│  │ Balance  │     │ Hold Management  │   │
│  │ Cache    │     │ (Reserve/Frozen) │   │
│  └──────────┘     └──────────────────┘   │
└──────────────────────────────────────────┘
```

**Wallet States:**
- `ACTIVE` — Normal operation
- `SUSPENDED` — Temporarily frozen (compliance/risk)
- `CLOSED` — Permanently closed

**Balance Types:**
- `AVAILABLE` — Usable for transactions
- `HELD` — Reserved for pending authorizations (e.g., QR payment in progress)
- `FROZEN` — Admin/compliance freeze
- `TOTAL` = Available + Held + Frozen

**Top-Up Channels:**
1. Bank transfer (FDB account → wallet, via CBS integration)
2. Cash-in at agent
3. Card top-up (debit/credit via MPU/MPGS)
4. Salary credit (corporate disbursement)

**Withdrawal Channels:**
1. Cash-out at agent
2. Bank transfer (wallet → FDB account)
3. Merchant settlement (auto-debit)

### 6.3 Transfer Service

**Responsibility:** P2P transfers, inter-wallet transfers, scheduling, routing.

**Transfer Types:**

| Type | Source → Destination | Speed | Fee |
|------|-------------------|-------|-----|
| Internal P2P | FDB Wallet → FDB Wallet | Instant | Free / nominal |
| Internal Bank | FDB Wallet → FDB Bank Account | Instant | Free |
| Interbank | FDB Wallet → Other Bank | RTGS timing | Per schedule |
| QR Merchant | Customer Wallet → Merchant Wallet | Instant | Merchant fee |
| Scheduled | Wallet → Any (scheduled) | On trigger date | Per type |
| Bulk | Corporate → Multiple Wallets | Batch processed | Volume discount |

**Routing Engine:**
```
Transfer Request → Routing Engine → Select Rail
                    │
                    ├─ Same FDB Wallet → Direct ledger entry (no external call)
                    ├─ FDB Bank Account → CBS API call
                    ├─ MPU/MPGS → Card network routing
                    ├─ CBM RTGS → Interbank settlement
                    └─ Mobile Money → Partner API
```

### 6.4 Merchant Service

**Responsibility:** Merchant lifecycle, QR generation, settlement, analytics.

**Merchant Onboarding Flow:**
```
Application → Document Upload → UBS/MRB Verification → Compliance Check
    → Approval → Activation → QR Kit Dispatch (optional)
```

**Settlement Engine:**
- Default: T+1 settlement (next business day before 14:00 MM time)
- Configurable: Same-day (T+0) for premium merchants
- Process: Aggregate daily transactions → Generate settlement file → CBS credit entry
- Reconciliation: Automated matching of settlement entries against transaction log

**QR Code Standards:**
- Static QR: Contains merchant ID + FDB Pay branding
- Dynamic QR: Includes amount, reference, expiry
- Format: EMVCo QRIS-compatible (for future interoperability)

### 6.5 Bill Payment Service

**Responsibility:** Connect to billers, process payments, handle confirmations.

**Supported Billers (Phase 1):**

| Biller Category | Providers | Payment Method |
|-----------------|-----------|----------------|
| Electricity | MEPCO, SEPE, ZP, EP, CBE | Account number + Amount |
| Water | Yangon City, Mandalay City | Account number |
| Internet | Myanma Posts & Telecom, 5BB, GTV | Account number |
| TV | MyTV, CANAL+ | Subscription number |
| Mobile Postpaid | MPT, Ooredoo | Phone number |

**Bill Payment Flow:**
```
User enters biller → System fetches bill (if API available) → User confirms amount
    → Debit wallet → Credit biller account → Return confirmation receipt
```

### 6.6 Fraud & Risk Service

**Responsibility:** Real-time fraud detection, transaction risk scoring, AML/CFT compliance.

**Risk Engine Components:**

| Layer | Mechanism | Action |
|-------|-----------|--------|
| **Pre-Auth** | Velocity checks, device fingerprint, geo-location | Block / Step-up auth |
| **Real-Time ML** | Anomaly detection model (transaction pattern, amount, time) | Flag / Block / Allow |
| **Post-Auth** | Batch analysis, network analysis | Alert / Freeze / Report |
| **Compliance** | Sanctions screening (UN, OFAC, EU lists), PEP checks | Block / Report to FIU |

**Transaction Limits (Tiered KYC):**

| Tier | KYC Level | Daily Limit | Monthly Limit | Per-Txn Limit |
|------|-----------|-------------|---------------|---------------|
| Basic | Phone + OTP | MMK 500,000 | MMK 5,000,000 | MMK 200,000 |
| Enhanced | NRC + Photo | MMK 5,000,000 | MMK 50,000,000 | MMK 2,000,000 |
| Full | In-branch KYC | MMK 50,000,000 | MMK 500,000,000 | MMK 20,000,000 |
| Corporate | Business KYC | Configurable | Configurable | Configurable |

### 6.7 Notification Service

**Responsibility:** Multi-channel notifications (Push, SMS, Email, In-App).

| Event | Push | SMS | Email | In-App |
|-------|------|-----|-------|--------|
| Transaction sent | ✓ | ✓ | — | ✓ |
| Transaction received | ✓ | ✓ | — | ✓ |
| KYC status change | ✓ | ✓ | ✓ | ✓ |
| Login from new device | ✓ | ✓ | — | — |
| Settlement received | — | ✓ | ✓ | ✓ |
| Promotional | ✓ | Optional | Optional | ✓ |
| Bill payment reminder | ✓ | ✓ | — | ✓ |

**SMS Providers (Primary + Fallback):**
1. MPT Bulk SMS
2. Twilio (international fallback)
3. Unifone (local)

### 6.8 Settlement & Clearing Service

**Responsibility:** Merchant settlement, inter-bank clearing, reconciliation.

**Merchant Settlement Flow:**
```
End of Business Day
    → Aggregate merchant transactions
    → Deduct platform fees & commissions
    → Generate settlement file (CSV + CBS format)
    → Submit to CBS for merchant account credit
    → Generate settlement confirmation
    → Notify merchant
```

**Interbank Clearing:**
- FDB Pay participates in CBM's Myanmar Payment Union (MPU) clearing
- RTGS integration for high-value interbank transfers
- Daily netting for card-present transactions

---

## 7. Database Design

### 7.1 Core Tables (PostgreSQL)

```sql
-- Wallets
CREATE TABLE wallets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    currency        VARCHAR(3) DEFAULT 'MMK',
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    balance_total   BIGINT DEFAULT 0,  -- stored in smallest unit (kyat)
    balance_held    BIGINT DEFAULT 0,
    balance_frozen  BIGINT DEFAULT 0,
    daily_limit     BIGINT DEFAULT 500000,
    monthly_limit   BIGINT DEFAULT 5000000,
    kyc_tier        VARCHAR(20) DEFAULT 'BASIC',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    version         BIGINT DEFAULT 0  -- optimistic locking
);

-- Ledger (Double-Entry)
CREATE TABLE ledger_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id       UUID NOT NULL REFERENCES wallets(id),
    type            VARCHAR(20) NOT NULL,  -- CREDIT / DEBIT
    amount          BIGINT NOT NULL,
    balance_after   BIGINT NOT NULL,
    txn_id          UUID NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_ledger_wallet_date ON ledger_entries(wallet_id, created_at DESC);

-- Transactions
CREATE TABLE transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(64) UNIQUE NOT NULL,
    type            VARCHAR(30) NOT NULL,  -- P2P, QR_MERCHANT, BILL_PAY, TOPUP, etc.
    status          VARCHAR(20) NOT NULL,  -- PENDING, COMPLETED, FAILED, REVERSED
    sender_wallet   UUID REFERENCES wallets(id),
    receiver_wallet UUID REFERENCES wallets(id),
    amount          BIGINT NOT NULL,
    fee             BIGINT DEFAULT 0,
    currency        VARCHAR(3) DEFAULT 'MMK',
    description     TEXT,
    metadata        JSONB,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    failure_reason  TEXT
);

CREATE INDEX idx_txn_sender ON transactions(sender_wallet, created_at DESC);
CREATE INDEX idx_txn_receiver ON transactions(receiver_wallet, created_at DESC);
CREATE INDEX idx_txn_status ON transactions(status, created_at);

-- Users
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(15) UNIQUE NOT NULL,
    name            VARCHAR(100),
    email           VARCHAR(150),
    nrc_number      VARCHAR(30),
    status          VARCHAR(20) DEFAULT 'PENDING',
    kyc_tier        VARCHAR(20) DEFAULT 'NONE',
    pin_hash        VARCHAR(200),
    pin_attempts    INT DEFAULT 0,
    pin_locked_until TIMESTAMPTZ,
    referral_code   VARCHAR(10) UNIQUE,
    referred_by     UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Merchants
CREATE TABLE merchants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    business_name   VARCHAR(200) NOT NULL,
    business_type   VARCHAR(50),
    business_license VARCHAR(50),
    tax_id          VARCHAR(30),
    settlement_acct VARCHAR(30),  -- FDB bank account for settlement
    settlement_type VARCHAR(20) DEFAULT 'T1',  -- T0, T1, T7
    fee_schedule    VARCHAR(30) DEFAULT 'STANDARD',
    status          VARCHAR(20) DEFAULT 'PENDING',
    category        VARCHAR(50),
    address         TEXT,
    latitude        DECIMAL(10, 8),
    longitude       DECIMAL(11, 8),
    qr_static_url   TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Agent Float Accounts
CREATE TABLE agent_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    wallet_id       UUID NOT NULL REFERENCES wallets(id),
    float_balance   BIGINT DEFAULT 0,
    commission_bal  BIGINT DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    daily_limit     BIGINT DEFAULT 50000000,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Settlement Records
CREATE TABLE settlements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     UUID NOT NULL REFERENCES merchants(id),
    period_start    TIMESTAMPTZ NOT NULL,
    period_end      TIMESTAMPTZ NOT NULL,
    gross_amount    BIGINT NOT NULL,
    fees            BIGINT NOT NULL,
    net_amount      BIGINT NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING',
    settled_at      TIMESTAMPTZ,
    settlement_ref  VARCHAR(50),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Audit Log (Append-only)
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor_id        UUID NOT NULL,
    actor_type      VARCHAR(20) NOT NULL,  -- USER, MERCHANT, ADMIN, SYSTEM
    action          VARCHAR(50) NOT NULL,
    resource_type   VARCHAR(50) NOT NULL,
    resource_id     VARCHAR(50),
    details         JSONB,
    ip_address      INET,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Partition audit_log by month for performance
-- CREATE TABLE audit_log_y2026m07 PARTITION OF audit_log
--     FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
```

### 7.2 Redis Structure

```
# Session
session:{user_id}          → JSON { access_token, refresh_token, device_id, expires_at }

# Rate Limiting
rl:{endpoint}:{user_id}    → COUNT (sliding window)

# OTP
otp:{phone}                → { code, attempts, expires_at }

# Wallet Balance Cache
wallet:balance:{wallet_id} → { available, held, frozen, version }

# Idempotency
idempotency:{key}          → { status, response, ttl }
```

### 7.3 MongoDB Collections (KYC Documents)

```javascript
// kyc_documents
{
    _id: ObjectId,
    user_id: UUID,
    tier: "ENHANCED" | "FULL",
    documents: [
        {
            type: "NRC_FRONT" | "NRC_BACK" | "SELFIE" | "PROOF_OF_ADDRESS",
            file_url: "s3://fdbpay-kyc/...",
            uploaded_at: ISODate,
            verified: Boolean,
            verified_by: UUID,
            rejection_reason: String
        }
    ],
    status: "PENDING" | "VERIFIED" | "REJECTED",
    submitted_at: ISODate,
    reviewed_at: ISODate
}
```

---

## 8. API Specification

### 8.1 Base Configuration

- **Base URL:** `https://api.fdbpay.com.mm/v1`
- **Auth:** Bearer JWT (Consumer/Merchant/Admin), API Key + Secret (Corporate)
- **Format:** JSON (request & response)
- **Versioning:** URL-based (`/v1/`, `/v2/`)

### 8.2 Core Endpoints

#### Authentication

```
POST   /auth/register          — Register new user
POST   /auth/login             — Login (phone + PIN)
POST   /auth/otp/send          — Send OTP to phone
POST   /auth/otp/verify        — Verify OTP
POST   /auth/pin/set           — Set/Change MPIN
POST   /auth/pin/reset         — Reset MPIN (via OTP)
POST   /auth/token/refresh     — Refresh access token
POST   /auth/logout            — Invalidate session
```

#### Wallet

```
GET    /wallet                  — Get wallet details + balance
GET    /wallet/ledger           — Transaction ledger (paginated)
GET    /wallet/ledger/export    — Export ledger as CSV
POST   /wallet/topup            — Initiate top-up
POST   /wallet/withdraw         — Cash-out request
GET    /wallet/limits           — Get current limits & usage
```

#### Transfers

```
POST   /transfer                — Initiate transfer (P2P, QR, bill, etc.)
GET    /transfer/{id}           — Get transfer status
POST   /transfer/{id}/confirm   — Confirm with PIN/OTP
POST   /transfer/{id}/cancel    — Cancel pending transfer
GET    /transfer/history        — Transfer history (paginated, filterable)
POST   /transfer/schedule       — Schedule recurring transfer
DELETE /transfer/schedule/{id}  — Cancel scheduled transfer
```

#### Merchant

```
POST   /merchant/register       — Register as merchant
GET    /merchant/profile        — Get merchant profile
PUT    /merchant/profile        — Update merchant profile
GET    /merchant/transactions   — Merchant transaction list
GET    /merchant/settlements    — Settlement history
POST   /merchant/qr/generate    — Generate QR code (static/dynamic)
GET    /merchant/settlements/{id}/detail — Settlement detail
POST   /merchant/refund         — Initiate refund
```

#### Bill Payment

```
GET    /bills/categories        — List biller categories
GET    /bills/billers           — List billers (filter by category)
GET    /bills/billers/{id}/lookup?account={n}  — Fetch bill details
POST   /bills/pay               — Pay bill
GET    /bills/history           — Bill payment history
POST   /bills/setup-auto        — Setup auto-pay
```

#### Corporate

```
POST   /corp/bulk-disburse      — Upload bulk disbursement file
GET    /corp/bulk-disburse/{id} — Check bulk disbursement status
GET    /corp/reconciliation     — Download reconciliation file
POST   /corp/payroll/schedule   — Schedule payroll run
```

#### Admin

```
GET    /admin/dashboard         — Overview metrics
GET    /admin/transactions      — All transactions (with filters)
GET    /admin/users             — User management
PUT    /admin/users/{id}/status — Suspend/activate user
GET    /admin/merchants         — Merchant management
PUT    /admin/merchants/{id}/status — Approve/suspend merchant
GET    /admin/kyc/pending       — Pending KYC reviews
PUT    /admin/kyc/{id}/review   — Approve/reject KYC
GET    /admin/disputes          — Dispute management
POST   /admin/disputes/{id}/resolve — Resolve dispute
GET    /admin/aml/alerts        — AML screening alerts
POST   /admin/aml/{id}/action   — Action on AML alert
```

### 8.3 Standard Response Format

```json
{
    "success": true,
    "data": { },
    "meta": {
        "request_id": "req_abc123",
        "timestamp": "2026-07-27T10:30:00Z",
        "pagination": {
            "page": 1,
            "per_page": 20,
            "total": 150,
            "total_pages": 8
        }
    },
    "error": null
}
```

### 8.4 Error Response Format

```json
{
    "success": false,
    "data": null,
    "error": {
        "code": "INSUFFICIENT_BALANCE",
        "message": "Wallet balance is insufficient for this transaction.",
        "details": {
            "available_balance": 50000,
            "requested_amount": 100000
        }
    }
}
```

---

## 9. End-to-End Payment Flows

### 9.1 P2P Transfer (Same Platform)

```
┌────────┐    ┌─────────┐    ┌──────────┐    ┌─────────┐    ┌──────────┐
│ Sender │    │ API GW  │    │ Transfer │    │ Wallet  │    │ Receiver │
│  App   │    │         │    │ Service  │    │ Service │    │   App    │
└───┬────┘    └────┬────┘    └────┬─────┘    └────┬────┘    └────┬─────┘
    │              │              │                │              │
    │ 1. POST      │              │                │              │
    │ /transfer    │              │                │              │
    │─────────────▶│              │                │              │
    │              │ 2. Route     │                │              │
    │              │─────────────▶│                │              │
    │              │              │                │              │
    │              │              │ 3. Debit sender│              │
    │              │              │───────────────▶│              │
    │              │              │                │              │
    │              │              │ 4. Credit      │              │
    │              │              │   receiver     │              │
    │              │              │───────────────▶│              │
    │              │              │                │              │
    │              │              │ 5. Emit event  │              │
    │              │              │────┐           │              │
    │              │              │    │ (Kafka)   │              │
    │              │              │◀───┘           │              │
    │              │              │                │              │
    │              │ 6. Response  │                │              │
    │              │◀─────────────│                │              │
    │ 7. Success   │              │                │              │
    │◀─────────────│              │                │              │
    │              │              │                │ 8. Push +    │
    │              │              │                │    SMS       │
    │              │              │                │─────────────▶│
```

**Steps:**
1. Sender initiates transfer via app with recipient phone/wallet ID, amount, PIN.
2. API Gateway authenticates, rate-limits, routes to Transfer Service.
3. Transfer Service validates → calls Wallet Service to debit sender (with lock).
4. Wallet Service credits receiver's wallet (same DB transaction via saga).
5. Transfer Service emits `txn.completed` event to Kafka.
6. Returns success response to sender.
7. Notification Service sends push + SMS to both sender and receiver.

### 9.2 QR Merchant Payment

```
┌────────┐  ┌────────┐  ┌────────┐  ┌─────────┐  ┌────────┐  ┌──────────┐
│Customer│  │  API   │  │Merchant│  │ Wallet  │  │Merchant│  │ Sound-   │
│  App   │  │Gateway │  │Service │  │ Service │  │ Wallet │  │  Box     │
└───┬────┘  └───┬────┘  └───┬────┘  └────┬────┘  └───┬────┘  └────┬─────┘
    │           │           │            │            │            │
    │ 1. Scan QR│           │            │            │            │
    │ 2. Enter  │           │            │            │            │
    │    Amount │           │            │            │            │
    │ 3. POST   │           │            │            │            │
    │  /transfer│           │            │            │            │
    │──────────▶│           │            │            │            │
    │           │ 4. Lookup │            │            │            │
    │           │  merchant │            │            │            │
    │           │──────────▶│            │            │            │
    │           │◀──────────│            │            │            │
    │           │           │            │            │            │
    │ 5. Auth   │           │            │            │            │
    │  (PIN)    │           │            │            │            │
    │──────────▶│           │            │            │            │
    │           │ 6. Debit  │            │            │            │
    │           │  customer │            │            │            │
    │           │──────────▶│────────────▶            │            │
    │           │           │            │            │            │
    │           │ 7. Credit │            │            │            │
    │           │  merchant │            │            │            │
    │           │──────────▶│────────────┼───────────▶│            │
    │           │           │            │            │            │
    │           │           │ 8. Notify  │            │            │
    │           │           │  merchant  │            │   Audio    │
    │           │           │────────────────────────────────────▶│
    │           │           │            │            │  "Payment  │
    │           │           │            │            │  received   │
    │           │           │            │            │  MMK 5000" │
    │ 9. Success│           │            │            │            │
    │◀──────────│           │            │            │            │
```

**Steps:**
1. Customer scans merchant QR code (static or dynamic with amount).
2. Customer enters amount (if static QR) and confirms.
3. Customer enters MPIN for authorization.
4. Transfer Service validates merchant, checks customer balance.
5. Wallet Service debits customer, credits merchant wallet.
6. Notification Service triggers sound-box at merchant to announce payment amount.
7. Both parties receive confirmation notification.

### 9.3 Agent Cash-In

```
┌──────────┐  ┌────────┐  ┌────────┐  ┌─────────┐  ┌──────────┐
│ Customer │  │ Agent  │  │  API   │  │ Agent   │  │ Customer │
│ (at shop)│  │  App   │  │Gateway │  │Service  │  │  Wallet  │
└────┬─────┘  └───┬────┘  └───┬────┘  └────┬────┘  └────┬─────┘
     │            │           │            │            │
     │ 1. Give   │           │            │            │
     │   cash to  │           │            │            │
     │   agent    │           │            │            │
     │───────────▶│           │            │            │
     │            │           │            │            │
     │            │ 2. Agent  │            │            │
     │            │  scans    │            │            │
     │            │  customer │            │            │
     │            │  QR /     │            │            │
     │            │  enters # │            │            │
     │            │           │            │            │
     │            │ 3. POST   │            │            │
     │            │ /agent/   │            │            │
     │            │ cash-in   │            │            │
     │            │──────────▶│            │            │
     │            │           │ 4. Verify │            │
     │            │           │  agent +  │            │
     │            │           │  customer │            │
     │            │           │──────────▶│            │
     │            │           │            │            │
     │            │ 5. Agent  │            │            │
     │            │  confirms │            │            │
     │            │  amount + │            │            │
     │            │  PIN      │            │            │
     │            │──────────▶│            │            │
     │            │           │ 6. Debit  │            │
     │            │           │  agent    │            │
     │            │           │  float    │            │
     │            │           │──────────▶│            │
     │            │           │            │            │
     │            │           │ 7. Credit │            │
     │            │           │  customer │            │
     │            │           │  wallet   │            │
     │            │           │───────────────────────▶│
     │            │           │            │            │
     │            │ 8. Success│            │            │
     │            │◀──────────│            │            │
     │ 9. Confirmed          │            │            │
     │◀──────────│           │            │            │
```

### 9.4 Bill Payment

```
┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌──────────┐
│  User  │  │  API   │  │  Bill  │  │ Wallet │  │  Biller  │
│  App   │  │Gateway │  │  Pay   │  │Service │  │   API    │
└───┬────┘  └───┬────┘  └───┬────┘  └───┬────┘  └────┬─────┘
    │           │           │           │            │
    │ 1. Select │           │           │            │
    │  biller + │           │           │            │
    │  account  │           │           │            │
    │──────────▶│           │           │            │
    │           │ 2. Route  │           │            │
    │           │──────────▶│           │            │
    │           │           │           │            │
    │           │           │ 3. Lookup │            │
    │           │           │  bill via │            │
    │           │           │  biller   │            │
    │           │           │  API      │            │
    │           │           │───────────────────────▶│
    │           │           │◀───────────────────────│
    │           │           │           │            │
    │ 4. Show   │           │           │            │
    │  bill     │           │           │            │
    │  details  │           │           │            │
    │◀──────────│           │           │            │
    │           │           │           │            │
    │ 5. Confirm│           │           │            │
    │  + PIN    │           │           │            │
    │──────────▶│           │           │            │
    │           │ 6. Debit  │           │            │
    │           │──────────▶│           │            │
    │           │           │           │            │
    │           │           │ 7. Pay to │            │
    │           │           │  biller   │            │
    │           │           │───────────────────────▶│
    │           │           │◀───────────────────────│
    │           │           │           │            │
    │ 8. Success│           │           │            │
    │◀──────────│           │           │            │
```

### 9.5 Merchant Settlement

```
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ Scheduler│  │Settlement│  │ Merchant │  │   CBS    │  │ Merchant │
│ (Cron)   │  │ Service  │  │ Service  │  │  (Core)  │  │ Bank Acct│
└────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
     │             │             │             │             │
     │ 1. Trigger  │             │             │             │
     │   (14:00    │             │             │             │
     │    daily)   │             │             │             │
     │────────────▶│             │             │             │
     │             │             │             │             │
     │             │ 2. Query    │             │             │
     │             │  completed  │             │             │
     │             │  txns for   │             │             │
     │             │  merchant   │             │             │
     │             │────────────▶│             │             │
     │             │◀────────────│             │             │
     │             │             │             │             │
     │             │ 3. Calculate│             │             │
     │             │  net = gross│             │             │
     │             │  - fees     │             │             │
     │             │             │             │             │
     │             │ 4. Submit   │             │             │
     │             │  credit to  │             │             │
     │             │  CBS        │             │             │
     │             │────────────────────────▶│             │
     │             │             │             │             │
     │             │             │             │ 5. Credit   │
     │             │             │             │  merchant   │
     │             │             │             │  account    │
     │             │             │             │────────────▶│
     │             │◀────────────────────────│             │
     │             │             │             │             │
     │             │ 6. Mark     │             │             │
     │             │  settled    │             │             │
     │             │             │             │             │
     │             │ 7. Notify   │             │             │
     │             │  merchant   │             │             │
     │             │────────────▶│             │             │
```

### 9.6 Salary Disbursement (Corporate Bulk)

```
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│Corporate │  │ Corporate│  │ Transfer │  │  Wallet  │
│  Portal  │  │ Service  │  │ Service  │  │ Service  │
└────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
     │             │             │             │
     │ 1. Upload   │             │             │
     │  CSV file   │             │             │
     │  (phone,    │             │             │
     │   amount)   │             │             │
     │────────────▶│             │             │
     │             │             │             │
     │             │ 2. Validate │             │
     │             │  rows       │             │
     │             │  (dups,     │             │
     │             │   limits)   │             │
     │             │             │             │
     │ 3. Maker-   │             │             │
     │  checker    │             │             │
     │  approval   │             │             │
     │◀────────────│             │             │
     │             │             │             │
     │ 4. Approved │             │             │
     │────────────▶│             │             │
     │             │             │             │
     │             │ 5. For each │             │
     │             │  row:       │             │
     │             │────────────▶│             │
     │             │             │ 6. Debit   │
     │             │             │  corp      │
     │             │             │  wallet    │
     │             │             │────────────▶│
     │             │             │             │
     │             │             │ 7. Credit  │
     │             │             │  employee  │
     │             │             │  wallet    │
     │             │             │────────────▶│
     │             │             │             │
     │ 8. Summary  │             │             │
     │  report     │             │             │
     │◀────────────│             │             │
```

---

## 10. Security & Compliance

### 10.1 Security Measures

| Layer | Measure |
|-------|---------|
| **Transport** | TLS 1.3 for all external communication; mTLS between internal services |
| **Authentication** | JWT with short expiry (15 min), refresh token rotation, device binding |
| **Authorization** | RBAC (Consumer, Merchant, Agent, Corporate, Admin roles) with fine-grained permissions |
| **Data at Rest** | AES-256 encryption for sensitive fields (PIN, NRC); database-level encryption |
| **Data Masking** | PAN masking in logs; NRC partial display; phone number masking in reports |
| **PIN Security** | Bcrypt hashing with salt; max 5 attempts → 30 min lock; 3 locks → account freeze |
| **OTP Security** | 6-digit OTP, 3-min expiry, max 3 attempts, SMS rate limiting |
| **Device Security** | Device fingerprinting, jailbreak/root detection, certificate pinning |
| **API Security** | Rate limiting (per-user, per-endpoint), input validation, SQL injection protection |
| **Network** | VPC isolation, WAF, DDoS protection, network segmentation |
| **Secrets** | HashiCorp Vault for secrets management; no secrets in code or config |

### 10.2 Myanmar Regulatory Compliance

| Requirement | Implementation |
|-------------|----------------|
| **CBM Licensing** | FDB Pay operates under FDB Bank's CBM digital payment license |
| **KYC/AML** | Tiered KYC per CBM guidelines; CDD for all users; EDD for high-risk |
| **Transaction Reporting** | STR filing to Financial Intelligence Unit (FIU) for suspicious transactions |
| **Currency** | All transactions in MMK; cross-border via approved channels only |
| **Data Residency** | All user data and transaction records stored within Myanmar (on-premise DC) |
| **Audit** | Complete audit trail retained for 7 years per CBM requirements |
| **Sanctions** | Real-time screening against UN, OFAC, and EU sanctions lists |
| **PEP Screening** | Politically Exposed Person checks at onboarding and periodically |
| **CTR** | Currency Transaction Report for transactions exceeding MMK 10,000,000 |
| **Record Keeping** | All transaction records maintained for minimum 5 years |

### 10.3 PCI DSS Compliance

FDB Pay will maintain PCI DSS Level 1 compliance for any card-related transactions:
- Tokenization of card numbers (no PAN storage)
- Secure key management via HSM
- Network segmentation for card data environment
- Regular penetration testing and vulnerability scans

---

## 11. Infrastructure & Deployment

### 11.1 Infrastructure Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Orchestration** | Kubernetes (on-premise) | Container orchestration |
| **Containers** | Docker | Application packaging |
| **Service Mesh** | Istio | mTLS, traffic management, observability |
| **CI/CD** | GitLab CI + ArgoCD | Automated build, test, deploy |
| **Database** | PostgreSQL 16 (Primary + 2 replicas) | OLTP, HA with streaming replication |
| **Cache** | Redis Cluster (3 nodes) | Session, cache, rate limiting |
| **Document Store** | MongoDB Replica Set (3 nodes) | KYC documents, unstructured data |
| **Message Queue** | Apache Kafka (3 brokers) | Event streaming, async processing |
| **Search** | Elasticsearch (3 nodes) | Logs, audit trail, full-text search |
| **Object Storage** | MinIO (on-prem S3) | KYC documents, reports, exports |
| **Monitoring** | Prometheus + Grafana | Metrics, dashboards, alerting |
| **Logging** | Fluentd → Elasticsearch → Kibana | Centralized logging |
| **Tracing** | Jaeger (OpenTelemetry) | Distributed tracing |
| **APM** | Custom + Grafana Tempo | Application performance |
| **HSM** | Thales Luna HSM | Cryptographic key management |
| **WAF** | ModSecurity / Nginx | Web application firewall |
| **Load Balancer** | Nginx / HAProxy | External traffic routing |
| **DNS** | CoreDNS (internal) + external DNS | Service discovery |
| **Secrets** | HashiCorp Vault | Secret management |
| **Backup** | Velero (K8s) + pgBackRest (PG) | Disaster recovery |

### 11.2 Deployment Architecture

```
┌─────────────────────────────────────────────────┐
│                  PRIMARY DC                      │
│                                                  │
│  ┌─────────────┐  ┌─────────────┐               │
│  │ K8s Cluster │  │ PostgreSQL  │               │
│  │ (8+ nodes)  │  │ Primary     │               │
│  │             │  └──────┬──────┘               │
│  │ ┌─────────┐ │         │ streaming            │
│  │ │App Pods │ │         │ replication          │
│  │ │(N svc)  │ │         │                      │
│  │ └─────────┘ │  ┌──────┴──────┐               │
│  │             │  │ PostgreSQL  │               │
│  │ ┌─────────┐ │  │ Replica 1   │               │
│  │ │Kafka    │ │  └─────────────┘               │
│  │ │Cluster  │ │                                │
│  │ └─────────┘ │  ┌─────────────┐               │
│  │             │  │ HSM         │               │
│  └─────────────┘  └─────────────┘               │
│                                                  │
└───────────────────┬─────────────────────────────┘
                    │ Async Replication
┌───────────────────┴─────────────────────────────┐
│                  DR DC                           │
│                                                  │
│  ┌─────────────┐  ┌─────────────┐               │
│  │ K8s Cluster │  │ PostgreSQL  │               │
│  │ (4+ nodes)  │  │ Standby     │               │
│  │ (reduced)   │  └─────────────┘               │
│  └─────────────┘                                │
│                                                  │
└─────────────────────────────────────────────────┘
```

### 11.3 Environment Strategy

| Environment | Purpose | Infrastructure |
|-------------|---------|---------------|
| `dev` | Active development, unit tests | 2-node K8s, shared PG |
| `staging` | Integration testing, UAT | Mirror of prod (smaller) |
| `pre-prod` | Performance testing, final validation | Prod replica |
| `prod` | Live traffic | Full production stack |
| `dr` | Disaster recovery | Warm standby in secondary DC |

---

## 12. Integrations

### 12.1 Integration Map

```
                          ┌──────────────┐
                          │   FDB Pay    │
                          │   Platform   │
                          └──────┬───────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
┌───────┴───────┐   ┌───────────┴──────────┐   ┌────────┴────────┐
│   BANKING     │   │     PAYMENT          │   │   TELECOM &     │
│   SYSTEMS     │   │     NETWORKS         │   │   BILLERS       │
│               │   │                      │   │                 │
│ • FDB Core    │   │ • MPU (Myanmar       │   │ • MPT           │
│   Banking     │   │   Payment Union)     │   │ • Ooredoo       │
│   (CBS API)   │   │ • MPGS (Mastercard)  │   │ • Mytel         │
│ • CBM RTGS    │   │ • Visa               │   │ • Atom/Telenor  │
│               │   │ • UPI (future)       │   │ • MEP/SEPE (elec)│
└───────────────┘   └──────────────────────┘   │ • Water boards  │
                                               │ • ISPs          │
┌───────────────┐   ┌──────────────────────┐   └─────────────────┘
│   IDENTITY    │   │     MESSAGING &      │
│   & KYC       │   │     COMPLIANCE       │
│               │   │                      │
│ • NRC Verify  │   │ • SMS providers      │
│   (Immig.)    │   │ • Sanctions DB       │
│ • Passport    │   │ • FIU STR filing     │
│   Verify      │   │ • Push notification  │
│ • Business    │   │   (FCM / APNs)       │
│   License     │   │ • Email (SMTP)       │
└───────────────┘   └──────────────────────┘
```

### 12.2 Integration Specifications

| Partner | Protocol | SLA | Fallback |
|---------|----------|-----|----------|
| FDB CBS | REST API (internal) | 99.99% | Queue + retry |
| MPU | ISO 8583 / REST | 99.9% | Retry + manual reconciliation |
| MPGS | REST API | 99.9% | Alternate rail |
| CBM RTGS | SWIFT / File | CBM hours | Next cycle |
| SMS (MPT) | SMPP / REST | 99.5% | Fallback provider |
| Biller APIs | REST / File upload | 99.0% | Manual posting |
| NRC Verification | REST API | 99.0% | Manual review queue |
| Sanctions DB | REST API (daily sync) | 99.9% | Last cached list |

---

## 13. Non-Functional Requirements

### 13.1 Performance

| Metric | Target |
|--------|--------|
| API Response Time (p50) | < 200ms |
| API Response Time (p99) | < 1s |
| Wallet Balance Query | < 50ms (from cache) |
| P2P Transfer Completion | < 3s end-to-end |
| QR Payment Completion | < 5s end-to-end |
| Throughput | 1,000 TPS (initial), scalable to 5,000 TPS |
| Bulk Disbursement | 10,000 rows processed in < 60s |

### 13.2 Availability

| Metric | Target |
|--------|--------|
| Platform Uptime | 99.95% (excl. planned maintenance) |
| Planned Maintenance Window | Sundays 02:00–06:00 MM time |
| RTO (Recovery Time Objective) | < 30 minutes |
| RPO (Recovery Point Objective) | < 1 minute (streaming replication) |
| Failover Time | < 60 seconds (automated) |

### 13.3 Scalability

| Dimension | Design |
|-----------|--------|
| Horizontal | All services stateless, scale via K8s HPA |
| Database | Read replicas for read-heavy paths; connection pooling (PgBouncer) |
| Caching | Multi-layer: L1 (in-process) → L2 (Redis) → L3 (DB) |
| Kafka | Partitioned topics by wallet_id for parallel processing |

### 13.4 Monitoring & Alerting

| Alert | Condition | Severity |
|-------|-----------|----------|
| High error rate | > 1% 5xx errors in 5 min | P1 — Page |
| Latency spike | p99 > 3s for 5 min | P1 — Page |
| Kafka lag | Consumer lag > 10,000 messages | P2 — Alert |
| DB replication lag | > 5s | P1 — Page |
| Disk usage | > 80% | P2 — Alert |
| Failed logins | > 100 in 5 min (single user) | P2 — Alert |
| Settlement failure | Any CBS credit failure | P1 — Page |
| AML alert | High-risk transaction flagged | P2 — Alert |

---

## 14. Error Handling & Resilience

### 14.1 Saga Pattern (Distributed Transactions)

FDB Pay uses the **Choreography-based Saga** pattern for multi-service transactions:

```
Transfer Request
    │
    ▼
[1] Debit Sender ──── FAIL ──▶ Return INSUFFICIENT_BALANCE
    │ OK
    ▼
[2] Credit Receiver ── FAIL ──▶ [1] Reverse Debit (Compensate)
    │ OK                      │ OK ──▶ Return TRANSFER_FAILED
    ▼                         │ FAIL ──▶ Dead Letter Queue + Manual Reconciliation
[3] Emit Event
    │ OK
    ▼
COMPLETED
```

### 14.2 Retry & Backoff

| Scenario | Strategy |
|----------|----------|
| Transient DB error | 3 retries, exponential backoff (100ms, 200ms, 400ms) |
| External API timeout | 3 retries, exponential backoff (500ms, 1s, 2s) |
| Kafka publish failure | Local WAL, async retry up to 5 times |
| CBS unavailability | Queue request, process when CBS recovers, alert ops |

### 14.3 Circuit Breaker

External integrations (CBS, MPU, Billers) wrapped with circuit breaker (Hystrix/Resilience4j):
- **Open**: 5 consecutive failures → stop calling for 30s
- **Half-Open**: Allow 1 probe request → if success, close circuit
- **Closed**: Normal operation

### 14.4 Dead Letter Queue (DLQ)

Failed events routed to DLQ topic in Kafka. DLQ monitor:
- Auto-retry DLQ messages up to 3 times
- Alert ops team after 3 failures
- Provide admin dashboard for manual DLQ processing

---

## 15. Analytics & Reporting

### 15.1 Real-Time Dashboard Metrics

- Transaction volume (by type, region, time)
- Active users (DAU, MAU)
- Transaction success/failure rate
- Average transaction value
- Merchant settlement status
- Agent float utilization
- AML alert queue depth

### 15.2 Scheduled Reports

| Report | Frequency | Audience |
|--------|-----------|----------|
| Daily Transaction Summary | Daily 08:00 | Management, Ops |
| Merchant Settlement Report | Daily 15:00 | Merchants, Finance |
| Weekly Transaction Analytics | Monday 09:00 | Product, Marketing |
| Monthly Financial Report | 1st of month | Finance, Compliance |
| AML/CFT Report | Monthly | Compliance, FIU |
| Agent Performance Report | Weekly | Agent Ops |
| Customer Acquisition Report | Weekly | Marketing, Management |

### 15.3 Data Pipeline

```
Kafka Events → Flink (real-time processing)
    ├── Real-time dashboards (Grafana)
    └── Data Warehouse (ClickHouse / PostgreSQL)
         ├── Report generation
         ├── ML model training
         └── Ad-hoc analytics
```

---

## 16. Phased Rollout Plan

### Phase 1 — MVP (Months 1–4)

| Feature | Status |
|---------|--------|
| User registration & KYC (basic tier) | Must have |
| Wallet (top-up via bank transfer) | Must have |
| P2P transfer (internal) | Must have |
| QR merchant payment | Must have |
| Bill payment (electricity, water) | Must have |
| Airtime top-up | Must have |
| Agent cash-in / cash-out | Must have |
| Merchant settlement (T+1) | Must have |
| Transaction history | Must have |
| Push + SMS notifications | Must have |
| Admin dashboard (basic) | Must have |
| USSD fallback (basic) | Must have |

### Phase 2 — Growth (Months 5–8)

| Feature | Status |
|---------|--------|
| Enhanced & full KYC tiers | Must have |
| Card top-up (MPU) | Must have |
| Interbank transfers (RTGS) | Must have |
| Promotions & cashback engine | Should have |
| Merchant web dashboard | Must have |
| Corporate bulk disbursement | Must have |
| Merchant sound-box POS | Should have |
| Agent commission system | Must have |
| Dispute management | Must have |
| USSD (full feature set) | Must have |

### Phase 3 — Expansion (Months 9–12)

| Feature | Status |
|---------|--------|
| Inbound remittance | Should have |
| Invoicing for merchants | Nice to have |
| Savings pockets | Nice to have |
| Scheduled / recurring payments | Should have |
| Corporate payroll API | Should have |
| Advanced AML (network analysis) | Must have |
| Multi-language (Shan, Kachin, Karen) | Nice to have |
| Deep analytics & ML fraud model | Should have |
| Open API for third parties | Nice to have |
| Cross-border outbound remittance | Future |

---

## 17. Glossary

| Term | Definition |
|------|-----------|
| **CBS** | Core Banking System — FDB Bank's central ledger and account management |
| **CICO** | Cash-In / Cash-Out — Agent-based cash deposit and withdrawal |
| **CBM** | Central Bank of Myanmar — Regulatory authority |
| **FIU** | Financial Intelligence Unit — Myanmar's AML/CFT reporting body |
| **KYC** | Know Your Customer — Identity verification process |
| **AML/CFT** | Anti-Money Laundering / Combating the Financing of Terrorism |
| **MPU** | Myanmar Payment Union — Domestic card payment network |
| **MPGS** | Mastercard Payment Gateway Services |
| **NRC** | National Registration Card — Myanmar national ID |
| **UBS/MRB** | User Business Registration / Ministry of Registration & Business |
| **RTGS** | Real-Time Gross Settlement — Interbank transfer system |
| **STR** | Suspicious Transaction Report — Filed with FIU |
| **CTR** | Currency Transaction Report — Large transaction filing |
| **HSM** | Hardware Security Module — Tamper-resistant key storage |
| **DLQ** | Dead Letter Queue — Storage for unprocessable messages |
| **CQRS** | Command Query Responsibility Segregation — Read/write separation pattern |
| **Saga** | Distributed transaction pattern using compensating actions |
| **HPA** | Horizontal Pod Autoscaler — Kubernetes auto-scaling |
| **EMVCo** | Europay, Mastercard, Visa — QR payment standard body |

---

*This document serves as the single source of truth for the FDB Pay platform. It should be reviewed and updated as the project evolves.*
