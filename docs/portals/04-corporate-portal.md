# 04 — Corporate Portal

The corporate portal serves `CORPORATE`-role users (businesses) who run bulk
disbursements, payroll and reconciliation. All corporate endpoints read the
`X-User-Id` header.

- **Frontend**: `frontend/src/pages/{corporate,payroll}/...`
- **API client**: `corporateApi` + `payrollApi` in `frontend/src/services/api.ts`
- **Routes**: `/corporate` (bulk + reconciliation), `/payroll` (payroll runs)

> Gateway rewrites: `/v1/corp/**` → `/corp/**` (StripPrefix), and
> `/v1/payroll/**` → `/corp/payroll/**`.

---

## 1. Bulk disbursement — `/corporate`

**What it is.** Upload a CSV/file reference describing many wallet payments and track
the batch.

**How it works.**
- `POST /v1/corp/bulk-disburse` (header `X-User-Id`) with
  `{ fileRef, description?, idempotencyKey }` → corporate-service `/corp/bulk-disburse`.
  Returns `{ id, batchId }`.
- `GET /v1/corp/bulk-disburse/{batchId}` (header `X-User-Id`) → batch progress:
  `{ status, totalAmount, totalRecipients, processedCount, failedCount }`.
- The UI seeds a small recent-disbursements list locally (statuses static).

**Cross-portal integration.** A bulk disbursement credits many **customer** wallets via
wallet-service. corporate-service publishes `bulk.disbursement.initiated` so
notification reacts. Recipients (CONSUMER users) see funds land in their customer wallet
ledger.

> **Known gap**: bulk disbursement also relies on the same `walletWebClient`
> (`http://localhost:8082`, not discovery-based) as payroll; see README §8.

**Backend**: corporate-service `CorporateController` (`/corp/bulk-disburse/**`).
**Data**: `bulk_disbursements` (fdbpay_corporate).

---

## 2. Reconciliation — `/corporate`

**What it is.** Fetch a reconciliation report for a given month.

**How it works.**
- `GET /v1/corp/reconciliation?period=YYYY-MM` (header `X-User-Id`) →
  corporate-service `/corp/reconciliation`. Returns
  `{ period, totalTransactions, totalAmount, discrepancies, status }`.

**Cross-portal integration.** Reconciliation draws on the corporate account's
transactions across the platform (wallets credited by corporate-initiated flows). It is a
read-side report; the corporate finance team cross-checks it against their payroll/bulk
uploads.

**Backend**: corporate-service `CorporateController` (`/corp/reconciliation`).
**Data**: `bulk_disbursements`/`payroll_*` (fdbpay_corporate) + wallet transactions.

---

## 3. Payroll runs — `/payroll`

**What it is.** Create payroll batches (employees, salary amounts, pay period), submit
them, and approve them with a maker-checker rule.

**How it works.**
- List: `GET /v1/payroll/history` (header `X-User-Id`) → corporate-service
  `/corp/payroll/history`; returns an **array** of runs (the frontend maps
  `totalEmployees`→`employeeCount`, `period`/`completedAt`→`payDate`).
- Create: `POST /v1/payroll/create` (header `X-User-Id`) with
  `{ period, employees: [{ employeeId, employeeName, phone, amount }] }` → returns the
  run (`DRAFT`).
- Submit: `POST /v1/payroll/{id}/submit` (header `X-User-Id`) → `DRAFT` → `SUBMITTED`
  (records `submittedBy`).
- Approve: `PUT /v1/payroll/{id}/approve` (header `X-User-Id`) → `SUBMITTED` → approved;
  **enforces maker-checker** — an approver cannot approve their own submission.
- Status lifecycle in the UI: `DRAFT → SUBMITTED → APPROVED/PROCESSING/COMPLETED/FAILED`
  (status colors map real backend statuses).

**Cross-portal integration.** On approval, corporate-service iterates the run's
employees and calls wallet-service `/wallet/credit` for each (publishing
`bulk.disbursement.initiated`). Employees are CONSUMER users whose wallets get credited —
visible immediately in the customer portal. Audit and notification consume the events.

> **Known gaps**: (1) the approval loop credits `PayrollEmployee.id` (a payroll row id)
> rather than a real user wallet, so automated "PAID" steps currently fail and runs can
> end `FAILED`; (2) with a single corporate account, the same user cannot approve their
> own run (maker-checker) — a second approver account is required; (3) there is no
> backend `reject` endpoint (the UI no longer offers one).

**Backend**: corporate-service `PayrollController` (`/corp/payroll/**`).
**Data**: `payroll_runs`, `payroll_employees`, `payroll_schedules` (fdbpay_corporate).

---

## 4. Approval workflow

**What it is.** Maker-checker for payroll/bulk approvals (`approval_workflows`).

**How it works.** corporate-service records submitter vs approver on payroll runs
(`submitted_by`, `approved_by`). The maker-checker rule is enforced in
`approvePayroll`: submitting and approving with the same user id is rejected with
`VALIDATION_ERROR`.

**Cross-portal integration.** Enforces the two-person rule that compliance requires for
corporate money movement. A separate CORPORATE (or admin-created) approver account is
needed to complete the cycle.

**Data**: `approval_workflows` (fdbpay_corporate).

---

## 5. Account managers

**What it is.** Dedicated corporate account managers (bank staff) assigned to corporate
clients.

**How it works.** `account_managers` tables exist in corporate-service; not exposed in
the current portal UI.

**Data**: `account_managers` (fdbpay_corporate).

---

## 6. Cross-portal touchpoints (corporate)

| Feature | Reads from | Writes to | Consumed by |
|---|---|---|---|
| Bulk disbursement | corporate | wallet (customer credits) | customer wallet, notification, audit |
| Reconciliation | corporate + wallet | — | corporate finance (read) |
| Payroll create/submit | corporate | corporate payroll | — |
| Payroll approve | corporate | wallet (employee credits) | customer wallet, notification, audit, settlement |

## 7. Known limitations

- `walletWebClient` in corporate-service points at `http://localhost:8082` directly
  (no discovery/LB).
- Payroll approval credits payroll-row ids, not real wallet ids — employee payout fails
  in practice.
- Maker-checker blocks single-user approve; reconciliation depends on upstream data
  being populated.
- Bulk disbursement UI file upload is simulated via a `fileRef` string (no real file
  parsing endpoint wired).
