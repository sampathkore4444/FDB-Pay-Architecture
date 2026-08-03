# 02 — Merchant Portal

The merchant portal serves `MERCHANT`-role users who run a registered business on FDB
Pay: accept QR payments, issue invoices, manage staff tills and view settlements.

- **Frontend**: `frontend/src/pages/{merchant,invoices,inventory,staff,settlements,promotions}/...`
- **API client**: `frontend/src/services/api.ts`
- **Routes**: `/merchant /invoices /inventory /staff /settlements /promotions`

---

## 1. Merchant dashboard — `/merchant`

**What it is.** The landing page: wallet balance, the merchant's static QR (rendered
client-side with the `qrcode` library into a data URL), recent settlements, and quick
links to invoices/staff/settlements/inventory.

**How it works.**
- `GET /v1/merchant/by-user/{userId}` → merchant-service `/merchant/by-user/{userId}`
  (returns the merchant profile + id).
- `GET /v1/wallet?userId=` → wallet balance of the merchant's wallet.
- `GET /v1/merchant/{merchantId}/qr` → `{qrUrl, deepLink}`; the frontend renders the QR
  locally.
- `GET /v1/settlements/merchant/{merchantId}` → recent settlement batches.

**Cross-portal integration.** The QR displayed here is what a **customer** scans from the
customer portal's Merchant Directory (the same static QR endpoint). Payments to this
merchant arrive in this wallet through transfer-service and later become settlements via
settlement-service. The merchant profile and status are managed by the **admin** portal
(approve/suspend).

**Backend**: merchant-service `MerchantController` (`/merchant/**`),
`SettlementService` via `/v1/settlements/**`.
**Data**: `merchants` (fdbpay_merchant), `wallets` (fdbpay_wallet),
`settlements` (fdbpay_settlement).

---

## 2. Merchant registration / profile — `/merchant` (register)

**What it is.** Register a business and obtain a merchant profile so payments can be
received.

**How it works.**
- `POST /v1/merchant?userId=` `{businessName, businessType?, category?, address?}` →
  merchant-service `/merchant`. A merchant row is created in `PENDING` status with a
  linked wallet.

**Cross-portal integration.** `PENDING` merchants are listed in the **admin** portal's
Merchant Management (`/v1/admin/merchants/**` → merchant-service); admin approval flips
status to `ACTIVE`, at which point the merchant can display QR, and the customer
directory starts showing the business. Merchant status changes are audited.

**Backend**: merchant-service `MerchantController`.
**Data**: `merchants`.

---

## 3. Invoices — `/invoices`

**What it is.** Generate and send digital invoices to customers with line items, tax and
due dates; track DRAFT → SENT → PAID / CANCELLED.

**How it works.**
- List: `GET /v1/invoices?userId=&page=0&size=100` → gateway rewrites to merchant-service
  `/merchant/invoices/**`; returns a Page (`data.content`); the frontend parses the
  JSONB `items` column.
- Create: `POST /v1/invoices?userId=` with `{customerPhone, customerName,
  items: JSON.stringify([...]), subtotal, tax, total, dueDate, idempotencyKey}`.
- State: `PUT /v1/invoices/{id}/send?userId=`, `.../paid?userId=`,
  `.../cancel?userId=`.

**Cross-portal integration.** Invoices target customers by phone; when marked PAID the
money is expected to move through the wallet pipeline (payment rail), and
`notification.send` is emitted on invoice actions. The invoice items are stored as JSONB
in `invoices` (fdbpay_merchant). Currently invoice creation does not itself transfer
money — payment is tracked by status.

**Backend**: merchant-service `InvoiceController` (`/merchant/invoices/**`).
**Data**: `invoices` (fdbpay_merchant).

---

## 4. Inventory / POS — `/inventory`

**What it is.** A placeholder integration screen for POS terminals / inventory sync.

**How it works.** **Client-side mock only** — no `api.ts` calls. Connect/disconnect/sync
buttons simulate timeouts and random data; webhook URL config and API keys are
no-ops. Sync logs are local state.

**Cross-portal integration.** None yet. Backend endpoints for `pos_terminals` and
merchant products exist in merchant-service (`/merchant/pos/**`) but the UI is not wired
to them.

**Data**: `pos_terminals`, `merchant_products` (fdbpay_merchant) — for future wiring.

---

## 5. Staff management — `/staff`

**What it is.** Create staff sub-accounts (tills) with roles and daily limits.

**How it works.**
- `GET /v1/merchant/by-user/{userId}` resolves the merchant id.
- `GET /v1/staff?merchantId=` → staff list (role lowercased client-side).
- `POST /v1/staff?merchantId=&userId=` `{userId, role: CASHIER|MANAGER|VIEWER,
  dailyLimit, idempotencyKey}`.
- `PUT /v1/staff/{staffId}/role?merchantId=&role=`, `DELETE /v1/staff/{staffId}?merchantId=`.

**Cross-portal integration.** Staff are added by **user id** — i.e. another FDB Pay user
(typically a customer) becomes a till operator under this merchant. Role permissions and
daily limits are enforced by merchant-service when that staff member transacts. Staff
data belongs to the merchant's organization and is separate from the customer wallet
role.

**Backend**: merchant-service `StaffController` (`/staff/**`).
**Data**: `staff_accounts` (fdbpay_merchant).

---

## 6. Settlements — `/settlements`

**What it is.** View settlement batches and trigger settlement of accumulated payments.

**How it works.**
- `GET /v1/settlements/summary` → platform totals (used by admin).
- `GET /v1/settlements/merchant/{merchantId}` → Page of batches; the frontend maps
  `netAmount`→`totalAmount`, `fees`, `transactionCount`, `settlementRef`.
- `POST /v1/settlements/trigger` `{merchantId}` → creates a batch and returns `batchId`.

**Cross-portal integration.** settlement-service subscribes to `txn.completed` to
aggregate the merchant's daily payments. When a batch is triggered/completed it writes
`settlement_batches`/`settlements`, emits `settlement.completed` (audit + notification),
and the merchant dashboard's "recent settlements" reflects the net of gross − fees.
Merchant fees/settlement type come from the merchant profile governed by **admin**.

**Backend**: settlement-service `SettlementController` (`/settlements/**`).
**Data**: `settlement_batches`, `settlements` (fdbpay_settlement).

---

## 7. Promotions — `/promotions` (shared with customer)

Merchants share the promotions page with customers: they can view active promotions and
their own cashback wallet (merchant-funded promos reward their customers). See
[01-customer-portal.md §9](./01-customer-portal.md).

---

## 8. Cross-portal touchpoints (merchant)

| Feature | Reads from | Writes to | Consumed by |
|---|---|---|---|
| Dashboard / QR | merchant + wallet + settlement | — | customer directory (QR), admin (profile/status) |
| Registration | auth (user) | merchant | admin merchant management |
| Invoices | merchant invoices | merchant invoices | notification, customer (payment) |
| Staff | merchant staff | merchant staff | staff tills transacting on merchant's behalf |
| Settlements | settlement + merchant | settlement | audit, notification, admin reporting |
| Promotions | promotions + wallet | promotions | customer cashback |

## 9. Known limitations

- `/inventory` is fully mock; POS/API-key wiring is not connected to the backend.
- Invoice "mark paid" does not move money between wallets automatically.
- Merchant approval is admin-driven; a merchant cannot self-activate.
