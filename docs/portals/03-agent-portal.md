# 03 — Agent Portal

The agent portal serves `AGENT`-role users (FDB branch staff or authorized agent shops)
who operate cash-in / cash-out (CICO) for customers and manage their own float and
commission.

- **Frontend**: `frontend/src/pages/agent/AgentPage.tsx`
- **API client**: `agentApi` in `frontend/src/services/api.ts`
- **Routes**: `/agent`

> All agent endpoints read the `X-User-Id` header (injected by the gateway from the JWT
> subject) rather than a `userId=` query param.

---

## 1. Agent account — `GET /v1/agent/account`

**What it is.** The agent's own account: float balance, commission balance and status.

**How it works.**
- `GET /v1/agent/account` (header `X-User-Id`) → agent-service `/agent/account`.
- Returns `{ id, userId, floatBalance, commissionBalance, status }` for the logged-in
  agent.

**Cross-portal integration.** An agent is a real FDB Pay user (`AGENT` role) with a
linked **wallet** (the float). The float moves as cash-in/cash-out happen. **Important**:
the `agent_accounts` row must exist — the backend has **no auto-provisioning** for new
AGENT users, so a freshly registered agent (or one whose account row is missing) gets a
`USER_NOT_FOUND` 404 until the row is seeded/created.

**Backend**: agent-service `AgentController` (`/agent/account`).
**Data**: `agent_accounts` (fdbpay_agent) — `float_balance`, `commission_balance`,
`status`, `daily_limit`, `wallet_id` → fdbpay_wallet.

---

## 2. Cash-in — `POST /v1/agent/cash-in`

**What it is.** A customer hands cash to the agent; the agent credits the customer's
wallet.

**How it works.**
- `POST /v1/agent/cash-in` (header `X-User-Id`) with
  `{ customerPhone, amount, idempotencyKey }` → agent-service `/agent/cash-in`.
- Intended flow: agent-service resolves the customer by phone (auth-service), debits the
  agent's float, credits the customer's wallet, records an `agent_transaction` and emits
  `txn.completed`.

**Cross-portal integration.** The customer's wallet balance (customer portal) increases
the moment cash-in completes; the agent's float decreases. Notification fires on
`txn.completed`.

> **Known gap**: agent-service's WebClient calls wallet-service at
> `/api/wallets/credit` / `/api/wallets/debit`, but wallet-service exposes
> `/wallet/credit` / `/wallet/debit`. Until those URIs are corrected, the wallet leg of
> cash-in/cash-out will fail (see [README.md §8](./README.md)).

**Backend**: agent-service `AgentController` (`/agent/cash-in`).
**Data**: `agent_transactions` (fdbpay_agent), `ledger_entries` (fdbpay_wallet).

---

## 3. Cash-out — `POST /v1/agent/cash-out`

**What it is.** A customer withdraws cash from the agent; the agent debits the
customer's wallet.

**How it works.**
- `POST /v1/agent/cash-out` (header `X-User-Id`) with
  `{ customerPhone, amount, idempotencyKey }` → agent-service `/agent/cash-out`.
- Mirror image of cash-in: debit customer wallet, credit agent float, record
  transaction, emit `txn.completed`.

**Cross-portal integration.** Customer wallet decreases; agent float increases. Same
`/api/wallets/*` mismatch applies.

**Backend**: agent-service `AgentController` (`/agent/cash-out`).
**Data**: `agent_transactions` (fdbpay_agent).

---

## 4. Float history — `GET /v1/agent/float-history`

**What it is.** The agent's recent float movement log.

**How it works.**
- `GET /v1/agent/float-history?page=&size=` (header `X-User-Id`) → agent-service
  `/agent/float-history`.
- Returns a plain array of `{ id, type, amount, description, createdAt }` (the frontend
  reads `r.data.data` directly — not a `Page`).

**Cross-portal integration.** Read-only view of float movements that happen through
cash-in/cash-out and float rebalancing. Rebalancing (bank float top-up) endpoints exist
in agent-service but are not wired into the portal UI.

**Backend**: agent-service `AgentController` (`/agent/float-history`).
**Data**: `agent_transactions` (fdbpay_agent).

---

## 5. Commissions

**What it is.** Agents earn commission per transaction; tracked separately from float.

**How it works.**
- `commission_balance` is stored on `agent_accounts`; `commission_records` capture the
  earnings.
- Commission is awarded by agent-service (e.g. `CommissionService`) as CICO and other
  agent-driven transactions complete.

**Cross-portal integration.** Commission accrual is internal to agent-service. The portal
currently shows the commission balance on the account card; withdrawal of commissions is
not wired in the UI.

**Backend**: agent-service `CommissionController` (`/agent/commission/**`).
**Data**: `commission_records` (fdbpay_agent).

---

## 6. Cross-portal touchpoints (agent)

| Feature | Reads from | Writes to | Consumed by |
|---|---|---|---|
| Agent account | agent + wallet (float) | — | — |
| Cash-in | auth (customer lookup) | wallet (customer + float) | customer wallet, notification, audit |
| Cash-out | auth (customer lookup) | wallet (customer + float) | customer wallet, notification, audit |
| Float history | agent | agent | — |
| Commission | agent | agent | — |

## 7. Known limitations

- **No auto-provisioning** of `agent_accounts`; missing rows cause 404s (a seed SQL is
  required or an endpoint should be added).
- **Wallet leg mis-routed**: agent-service calls `/api/wallets/credit|debit` but
  wallet-service exposes `/wallet/credit|debit` — CICO money movement currently fails.
- Commission withdrawal / float rebalancing are not exposed in the portal UI.
