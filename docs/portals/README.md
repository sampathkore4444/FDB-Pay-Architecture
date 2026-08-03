# FDB Pay — Portal Documentation

This documentation set explains every **user portal** of the FDB Pay platform: how each
feature works, which backend services and endpoints back it, and how the portals
integrate with one another.

## Portal index

| Portal | Role | Route prefix (UI) | Document |
|--------|------|-------------------|----------|
| Customer portal | `CONSUMER` | `/wallet`, `/transfer`, `/bills`, ... | [01-customer-portal.md](./01-customer-portal.md) |
| Merchant portal | `MERCHANT` | `/merchant`, `/invoices`, `/staff`, ... | [02-merchant-portal.md](./02-merchant-portal.md) |
| Agent portal | `AGENT` | `/agent` | [03-agent-portal.md](./03-agent-portal.md) |
| Corporate portal | `CORPORATE` | `/corporate`, `/payroll` | [04-corporate-portal.md](./04-corporate-portal.md) |
| Admin portal | `ADMIN` | `/admin`, `/admin/kyc`, `/admin/aml`, `/audit`, ... | [05-admin-portal.md](./05-admin-portal.md) |

Two pages are shared by **all** roles and are documented in the customer portal guide:
`Support` (`/support`) and `Disputes` (`/disputes`).

---

## 1. Platform architecture

```
                     ┌─────────────────────────────────────────────┐
                     │             React Web Portal (frontend)     │
                     │   Customer | Merchant | Agent | Corporate   │
                     │   Admin   | Shared pages (support, disputes)│
                     └──────────────────────┬──────────────────────┘
                                            │  HTTP /v1/**  (axios, Bearer JWT)
                     ┌──────────────────────┴──────────────────────┐
                     │        API Gateway  (api-gateway :8080)      │
                     │  JWT validation (AuthFilter) → injects       │
                     │  X-User-Id header; routes by Path predicate  │
                     └───┬───────┬───────┬───────┬────────┬─────────┘
                         │       │       │       │        │
          ┌──────────────┴───┐ ┌─┴─────┐ ┌─┴────┐ ┌─┴──────┐ ┌─┴──────────┐
          │  Core services   │ │ KYC / │ │ Fraud│ │ Audit  │ │ Cross-     │
          │  auth, wallet,   │ │ AML   │ │ & Risk│ │Service │ │ cutting    │
          │  transfer, mer-  │ │ (kyc) │ │ (fraud│ │(audit) │ │ support,   │
          │  chant, agent,   │ │       │ │ -risk)│ │        │ │ promotions,│
          │  corporate, bill │ │       │ │       │ │        │ │ remittance,│
          │  payment, settl.,│ │       │ │       │ │        │ │ dispute    │
          │  reporting       │ │       │ │       │ │        │ │            │
          └──────────────────┴───────┴─┴───────┴─┴────────┴─┴────────────┘
                         │ service-to-service HTTP (WebClient)
          ┌──────────────┴──────────────────────────────────────┐
          │ Eureka discovery │ Kafka (events) │ PostgreSQL/Redis│
          │                  │                │ MongoDB (docs)  │
          └─────────────────────────────────────────────────────┘
```

- **Frontend**: React 18 + Vite + TypeScript + Tailwind, served by nginx, all portals in
  one SPA. Source: `frontend/src`, API client: `frontend/src/services/api.ts`.
- **Backend**: Java 21 + Spring Boot 3 microservices in `backend/`, discovered through
  Eureka (`eureka-server`), fronted by Spring Cloud Gateway (`api-gateway`).
- **Messages**: Apache Kafka is the event bus. Services publish events (e.g.
  `txn.completed`, `notification.send`) and consumers (notification, audit, settlement,
  reporting, fraud, promotions) react asynchronously.
- **Data**: one PostgreSQL 16 server with a **separate database per service**
  (e.g. `fdbpay_auth`, `fdbpay_wallet`, `fdbpay_transfer`, `fdbpay_merchant`,
  `fdbpay_agent`, `fdbpay_corporate`, ...), Redis 7 for cache/session/OTP/rate limits,
  MongoDB 7 for KYC documents.
- **Run** (docker-compose): `docker compose --profile build build backend-build`,
  `docker compose build frontend`, `docker compose up -d --remove-orphans`.

### Services (backend modules)

| Module | Port | Owns | Fronts |
|--------|------|------|--------|
| `auth-service` | 8081 | users, roles, PIN, JWT, admin user management | `/v1/auth/**`, `/v1/admin/users/**` |
| `wallet-service` | 8082 | wallets, ledger, top-up, withdrawal, savings pockets | `/v1/wallet/**`, `/v1/savings/**` |
| `transfer-service` | 8083 | P2P transfer, scheduled payments, request-money | `/v1/transfer/**`, `/v1/scheduled/**`, `/v1/request-money/**` |
| `merchant-service` | 8084 | merchants, QR, directory, POS, staff, invoices, admin merchant mgmt | `/v1/merchant/**`, `/v1/merchants/**`, `/v1/directory/**`, `/v1/invoices/**`, `/v1/staff/**`, `/v1/admin/merchants/**` |
| `bill-payment-service` | 8085 | bill pay + airtime top-up | `/v1/bills/**`, `/v1/airtime/**` |
| `agent-service` | 8086 | agent accounts, cash-in/out, float, commissions | `/v1/agent/**` |
| `corporate-service` | 8087 | bulk disbursement, payroll, approvals, reconciliation, account managers | `/v1/corp/**`, `/v1/payroll/**` |
| `settlement-service` | 8088 | merchant settlement batches | `/v1/settlements/**` |
| `dispute-service` | 8089 | disputes, evidence, resolution | `/v1/disputes/**` |
| `audit-service` | 8090 | append-only audit log | `/v1/audit/**` |
| `fraud-risk-service` | 8091 | fraud alerts, AML alerts | `/v1/fraud/**`, `/v1/admin/aml/**` |
| `kyc-service` | 8092 | KYC submissions, admin KYC review | `/v1/kyc/**`, `/v1/admin/kyc/**` |
| `notification-service` | 8093 | push/SMS/email notifications | `/v1/notifications/**` |
| `reporting-service` | 8094 | admin dashboard KPIs, reporting cache | `/v1/admin/**` |
| `remittance-service` | 8095 | inbound remittance corridors/quote/initiate | `/v1/remittance/**` |
| `promotions-service` | 8096 | promotions, cashback wallets | `/v1/promotions/**` |
| `support-service` | 8097 | support tickets, messages, SLAs, FAQs | `/v1/support/**` |
| `eureka-server` | 8761 | service discovery | — |
| `api-gateway` | 8080 | routing, JWT filter | everything `/v1/**` |
| `shared` | — | `ApiResponse`, exceptions, JWT utils, constants | library |

---

## 2. Authentication & authorization model

- **Identity is phone + PIN** (not password). The `User` entity has `pin_hash`,
  `pin_attempts`, `pin_locked_until`, no password field.
- **Roles**: `CONSUMER`, `MERCHANT`, `AGENT`, `CORPORATE`, `ADMIN`.
- **Registration** (`POST /v1/auth/register`): creates the user (default role —
  registrations are effectively consumer) and **auto-creates a wallet** by calling
  `wallet-service` `POST /wallet` on the user's behalf.
- **Login** (`POST /v1/auth/login`): validates phone + PIN, applies a 5-attempt lockout,
  returns `{ accessToken, refreshToken, expiresIn, user }`. JWT `sub` claim = `userId`.
- **Gateway filtering** (`api-gateway` `AuthFilter`): validates the Bearer JWT on
  non-public paths and injects **`X-User-Id`** from the JWT subject. Public paths include
  `/v1/auth/login`, `/v1/auth/register`, `/v1/auth/otp/**`.
- **Two identification conventions** exist in the codebase (important for integration):
  - *Query param* `userId=...` — used by most frontend API clients
    (wallet, transfer, bills, airtime, savings, disputes, staff, invoices, remittance,
    promotions, support, request-money, scheduled).
  - *`X-User-Id` header* — used by `agentApi`, `payrollApi`, and `corporateApi`, which
    match controllers that read `@RequestHeader("X-User-Id")`.
  Both come from the same logged-in user; the gateway also injects `X-User-Id` from the
  JWT, so header-based calls are doubly covered.
- **Frontend session**: Zustand store persisted in `localStorage`; axios request
  interceptor adds `Authorization: Bearer <token>`; a `401` response clears the session
  and redirects to `/login`.
- **i18n**: the web app is bilingual (English / Myanmar), stored in
  `frontend/src/i18n/locales/{en,my}.ts`.

---

## 3. API Gateway route map (`/v1/**` → service)

`StripPrefix=1` removes the `/v1/<service>` prefix; `RewritePath` remaps paths so portal
facing URLs differ from service URLs.

| Client path (portal calls) | Service | Actual service path |
|---|---|---|
| `/v1/auth/**` | auth-service | `/auth/**` |
| `/v1/wallet/**` | wallet-service | `/wallet/**` (StripPrefix) |
| `/v1/savings/**` | wallet-service | `/wallet/savings/**` |
| `/v1/transfer/**` | transfer-service | `/transfer/**` |
| `/v1/scheduled/**` | transfer-service | `/transfer/schedule/**` |
| `/v1/request-money/**` | transfer-service | `/transfer/request/**` |
| `/v1/merchant/**`, `/v1/merchants/**` | merchant-service | `/merchant/**` |
| `/v1/directory/**` | merchant-service | `/merchants/directory/**` |
| `/v1/invoices/**` | merchant-service | `/merchant/invoices/**` |
| `/v1/staff/**` | merchant-service | `/staff/**` |
| `/v1/bills/**` | bill-payment-service | `/bills/**` |
| `/v1/airtime/**` | bill-payment-service | `/airtime/**` |
| `/v1/agent/**` | agent-service | `/agent/**` |
| `/v1/corp/**` | corporate-service | `/corp/**` |
| `/v1/payroll/**` | corporate-service | `/corp/payroll/**` |
| `/v1/settlements/**` | settlement-service | `/settlements/**` |
| `/v1/disputes/**` | dispute-service | `/disputes/**` |
| `/v1/audit/**` | audit-service | `/audit/**` |
| `/v1/remittance/**` | remittance-service | `/remittance/**` |
| `/v1/promotions/**` | promotions-service | `/promotions/**` |
| `/v1/support/**` | support-service | `/support/**` |
| `/v1/kyc/**` | kyc-service | `/kyc/**` |
| `/v1/fraud/**` | fraud-risk-service | `/fraud/**` |
| `/v1/notifications/**` | notification-service | `/notifications/**` |
| `/v1/admin/kyc/**` | kyc-service | `/admin/kyc/**` |
| `/v1/admin/aml/**` | fraud-risk-service | `/admin/aml/**` |
| `/v1/admin/users/**` | auth-service | `/admin/users/**` |
| `/v1/admin/merchants/**` | merchant-service | `/admin/merchants/**` |
| `/v1/admin/**` (dashboard) | reporting-service | `/admin/**` |

> Note: `/v1/admin/**` is caught by the more specific `admin-kyc`, `admin-aml`,
> `admin-users`, `admin-merchants` routes first; everything else under `/v1/admin/`
> (e.g. the dashboard) goes to reporting-service.

---

## 4. Data layer

One PostgreSQL server, one database per service (created by `docker/postgres-init.sql`):

| DB | Owned by | Main tables |
|---|---|---|
| `fdbpay_auth` | auth-service | `users`, admin user actions |
| `fdbpay_wallet` | wallet-service | `wallets`, `ledger_entries`, `transactions`, `savings_pockets`, `savings_transactions` |
| `fdbpay_transfer` | transfer-service | `transactions`, `scheduled_payments`, `money_requests` |
| `fdbpay_merchant` | merchant-service | `merchants`, `staff_accounts`, `pos_terminals`, `invoices`, `merchant_products` |
| `fdbpay_bill_payment` | bill-payment-service | `bill_payments`, `airtime_topups` |
| `fdbpay_agent` | agent-service | `agent_accounts`, `agent_transactions`, `commission_records` |
| `fdbpay_corporate` | corporate-service | `bulk_disbursements`, `payroll_runs`, `payroll_employees`, `payroll_schedules`, `approval_workflows` |
| `fdbpay_settlement` | settlement-service | `settlement_batches`, `settlements` |
| `fdbpay_dispute` | dispute-service | `disputes`, `dispute_evidence` |
| `fdbpay_audit` | audit-service | `audit_entries` |
| `fdbpay_fraud_risk` | fraud-risk-service | `fraud_alerts` |
| `fdbpay_kyc` | kyc-service | `kyc_submissions`, `kyc_documents` (documents in MongoDB) |
| `fdbpay_notification` | notification-service | `notifications` |
| `fdbpay_reporting` | reporting-service | `report_cache` |
| `fdbpay_remittance` | remittance-service | `remittances`, `remittance_corridors` |
| `fdbpay_promotions` | promotions-service | `promotions`, `promotion_usages`, `cashback_wallets`, `cashback_transactions` |
| `fdbpay_support` | support-service | `support_tickets`, `ticket_messages`, `faqs` |

Money amounts are stored as `BIGINT` in the smallest unit (kyat). Wallet balance
handling uses optimistic locking (`version` column) plus `balance_total` /
`balance_held` / `balance_frozen`. All money-moving writes carry an `idempotency_key`.

---

## 5. Event bus (Kafka)

| Topic | Producers | Consumers |
|---|---|---|
| `txn.completed` | wallet, transfer, bill-payment, agent | settlement, dispute, audit, fraud, notification, reporting, promotions |
| `notification.send` | auth, merchant, invoice, money-request, remittance | notification-service |
| `airtime.completed` | bill-payment | notification |
| `remittance.received` | remittance | remittance, notification |
| `settlement.completed` | settlement | audit, notification |
| `bulk.disbursement.initiated` | corporate | notification |
| `kyc.submitted` / `kyc.reviewed` | kyc | kyc, audit (`kyc.reviewed`) |
| `dispute.created` / `dispute.resolved` | dispute | audit |
| `settlement.daily` | — (declared, no producer) | notification |

Every money movement therefore triggers **multiple downstream effects** without the
originating portal needing to call them directly: notifications, audit trail, fraud
screening, settlement aggregation, and promotions/cashback all subscribe to
`txn.completed`.

---

## 6. Cross-portal integration flows (end-to-end)

### 6.1 Customer → Customer P2P transfer
`Customer A` (customer portal) → `POST /v1/transfer?userId=A` (gateway → transfer-service)
→ transfer-service looks up recipient by phone via auth-service, calls wallet-service
`/wallet/debit` (A) and `/wallet/credit` (B) → publishes `txn.completed` → notification
pushes to A & B; ledger entries written for both wallets. **Touchpoints**: customer
portal, transfer-service, auth-service, wallet-service, notification-service.

### 6.2 Customer pays Merchant (QR / directory)
Customer opens the Merchant Directory (`/v1/directory/**` → merchant-service
`/merchants/directory/**`), picks a merchant and its static QR → in a real payment flow
the customer's `/v1/transfer` debit is credited to the **merchant's wallet**; the
merchant later sees it in **settlements** (`settlement-service` consumes `txn.completed`
and aggregates batches). **Touchpoints**: customer portal, merchant-service,
transfer-service, wallet-service, settlement-service, merchant portal.

### 6.3 Customer cash-in / cash-out at an Agent
Agent portal → `POST /v1/agent/cash-in` or `/cash-out` (agent-service, `X-User-Id`
header) → agent-service should debit/credit the **customer wallet** via wallet-service.
**Known gap:** agent-service calls `/api/wallets/credit|debit` on wallet-service, but
wallet-service exposes `/wallet/credit|debit` — the agent CICO wallet call is currently
mis-routed (see §8). **Touchpoints**: agent portal, agent-service, wallet-service,
auth-service (phone→user lookup).

### 6.4 Corporate payroll / bulk disbursement
Corporate portal creates a payroll run (`POST /v1/payroll/create`) with employees
(phone + amount), submits it (`POST /v1/payroll/{id}/submit`), and a **different**
approver approves it (`PUT /v1/payroll/{id}/approve`, maker-checker enforced). Approval
credits each employee via wallet-service (`/wallet/credit`) and publishes
`bulk.disbursement.initiated`. Employees (CONSUMER users) then see the money in their
customer wallet. **Touchpoints**: corporate portal, corporate-service, wallet-service,
customer portal (receiver side), notification-service.

### 6.5 Customer dispute → Admin resolution
Customer portal `/disputes` → `POST /v1/disputes?userId=` creates a dispute referencing a
transaction → admin portal "All Disputes" reviews evidence (`dispute-service`) → admin
resolves (refund/partial/dismiss) → `dispute.resolved` published → audit trail written.
**Touchpoints**: customer portal, admin portal, dispute-service, audit-service,
wallet-service (refund path).

### 6.6 Customer KYC submission → Admin review
Customer submits KYC (kyc-service) → `kyc.submitted` published → admin portal
`/admin/kyc` reviews documents (stored in MongoDB) → `PUT /v1/admin/kyc/{id}/review`
approve/reject → `kyc.reviewed` event → user's KYC tier/status updated, notification
sent. **Touchpoints**: customer portal (KYC), admin portal, kyc-service, auth-service,
notification-service.

### 6.7 Merchant settlement
`txn.completed` events accumulate in settlement-service → merchant (or system) triggers
settlement (`POST /v1/settlements/trigger`) → batch computed (gross − fees = net) →
`settlement.completed` → merchant portal shows settlement history with reference.
**Touchpoints**: customer (payments), merchant portal, settlement-service,
merchant-service (merchant profile), reporting-service.

### 6.8 Request money / scheduled payments
Request-money (`/v1/request-money/**` → `/transfer/request/**`): a CONSUMER requests
money from another user; the target user responds accept/cancel; acceptance executes a
P2P transfer. Scheduled payments (`/v1/scheduled/**` → `/transfer/schedule/**`):
recurring `DAILY/WEEKLY/MONTHLY` P2P jobs executed by transfer-service against
wallet-service. **Touchpoints**: customer portal (both parties), transfer-service,
wallet-service, notification-service.

---

## 7. Demo / test accounts

> There is **no seed data** in the backend; accounts are created through registration.
> The values below are ones used during development and are not guaranteed to exist in a
> fresh database.

| Role | Phone | PIN | Notes |
|---|---|---|---|
| ADMIN | `+95999000001` | `1234` | admin portal |
| MERCHANT | `+95999000002` | `1234` | merchant portal |
| AGENT | `+95999000003` | `1234` | agent portal |
| CORPORATE | `+95999000004` | `1234` | corporate portal |
| CONSUMER | `+959999000111` | `1234` | customer portal |

---

## 8. Known integration gaps / caveats

- **Agent cash-in / cash-out wallet calls are mis-routed**: agent-service WebClient uses
  `/api/wallets/credit` and `/api/wallets/debit`, but wallet-service exposes
  `/wallet/credit` and `/wallet/debit`. Agent CICO therefore fails to move customer
  wallet funds until the agent-service URIs are corrected.
- **Agent / corporate `walletWebClient` base URL** defaults to `http://localhost:8082`
  and bypasses service discovery/load balancing (works in single-node docker, fragile
  under multi-instance deployments).
- **Payroll approval credits `PayrollEmployee.id`** (a payroll row id) to wallet-service
  rather than a real user's wallet, so automated payroll "PAID" steps currently fail and
  runs finish `FAILED`/`COMPLETED` with failures unless employee ids map to real wallets.
- **Payroll maker-checker** prevents a submitter from approving their own run; with a
  single corporate account the approve button errors.
- **Agent/corporate controllers** read `X-User-Id`; the gateway injects it from the JWT,
  but if a caller omits the JWT, those endpoints 401.
- **No backend `reject` endpoint** for payroll — the frontend no longer offers a reject
  action.
- **`txn.failed`** topic is declared in constants but has no producer.
- Some cross-service calls publish events with inconsistent metadata (e.g. agent events
  carry `currency="KES"` while the rest of the system uses `MMK`).
