# 01 — Customer Portal

The customer portal is the primary end-user surface. It is served by the same React SPA
as every other portal and is gated to the `CONSUMER` role.

- **Frontend**: `frontend/src/pages/{wallet,transfer,bills,airtime,savings,scheduled,remittance,promotions,directory,disputes,support}/...`
- **API client**: `frontend/src/services/api.ts`
- **Routes**: `/wallet /transfer /request-money /bills /airtime /savings /scheduled /remittance /promotions /directory /disputes /support`

All requests go through the API gateway (`/v1/**`), authenticated with the JWT; the
frontend API layer appends `userId=` query params on most calls.

---

## 1. Wallet dashboard — `/wallet`

**What it is.** The landing page after login. Shows three balance cards
(Total, Available, Held/Frozen) and the recent ledger.

**How it works.**
- `GET /v1/wallet?userId=` (gateway `StripPrefix=1` → wallet-service `/wallet?userId=`)
  returns wallet totals.
- `GET /v1/wallet/ledger?userId=&page=&size=` returns a Spring `Page` of
  `ledger_entries` (the frontend maps `data.content`).

**Cross-portal integration.** The balance is the single source of truth for every other
portal's money movement. When an agent does cash-in, a merchant settles, or a corporate
payroll lands, the customer's `ledger_entries` and `transactions` are updated here via
wallet-service. Top-Up/Withdraw buttons are present but disabled in the UI (no wired
bank/card channel yet).

**Backend**: wallet-service `WalletController` (`/wallet`, `/wallet/ledger`).
**Data**: `wallets`, `ledger_entries`, `transactions` (fdbpay_wallet).

---

## 2. P2P transfer — `/transfer`

**What it is.** Send money to another FDB Pay user by phone or wallet id.

**How it works.**
- `POST /v1/transfer?userId=` with `{recipientIdentifier, amount, type?, description?}`
  plus a client `idempotencyKey`.
- Gateway → transfer-service `POST /transfer`:
  1. Resolves the recipient (phone → user/wallet) via auth-service.
  2. Calls wallet-service `POST /wallet/debit` (sender) and `POST /wallet/credit`
     (recipient).
  3. Writes the transaction and publishes `txn.completed` on Kafka.

**Cross-portal integration.** This is the transaction rail other portals build on:
bill payments, airtime, savings deposits, request-money acceptance, scheduled payments
and payroll all funnel through the same wallet debit/credit + `txn.completed` pipeline.
Consumers of `txn.completed` include notification, audit, fraud, settlement, reporting
and promotions.

**Backend**: transfer-service `TransferController` (`/transfer`), wallet-service
`WalletController` (`/wallet/debit`, `/wallet/credit`, `/wallet/owner`).
**Data**: `transactions` (fdbpay_transfer), `ledger_entries` (fdbpay_wallet).

---

## 3. Request money — `/request-money`

**What it is.** Ask another user to pay you, then accept/cancel incoming requests.

**How it works.**
- List: `GET /v1/request-money/my?userId=` → transfer-service `/transfer/request/my`
  (frontend reads `data.content`, maps `requesterUserId` → `requesterId`).
- Create: `POST /v1/request-money?userId=` `{targetPhone, amount, description?}` →
  `/transfer/request`; generates a payment link in the UI.
- Respond: `PUT /v1/request-money/{id}/respond?targetUserId=` `{action: ACCEPT|CANCEL}`.

**Cross-portal integration.** Accepting a request triggers the same transfer pipeline as
P2P (wallet debit/credit, `txn.completed`). The counterparty is another customer; both
sides see the request in their own portal. `notification.send` is emitted so the target
user is alerted.

**Backend**: transfer-service `MoneyRequestController` (`/transfer/request/**`).
**Data**: `money_requests` (fdbpay_transfer).

---

## 4. Bill payment — `/bills`

**What it is.** Pay utility bills (electricity, water, internet, TV).

**How it works.**
- `GET /v1/bills/categories`, `GET /v1/bills/billers?category=`,
  `GET /v1/bills/billers/{id}/lookup?account=` (fetch bill detail).
- `POST /v1/bills/pay?userId=` `{billerId, accountNumber, amount, idempotencyKey}` →
  bill-payment-service `/bills/pay`, which debits the customer wallet via
  wallet-service and publishes `txn.completed`.

**Cross-portal integration.** Consumer-facing only. Settles through the wallet pipeline;
bill-payment-service publishes `txn.completed` so audit/fraud/notification react. Biller
connectors are stubbed/mocked in the demo.

**Backend**: bill-payment-service `BillController` (`/bills/**`).
**Data**: `bill_payments` (fdbpay_bill_payment).

---

## 5. Airtime top-up — `/airtime`

**What it is.** Top up mobile airtime for any provider.

**How it works.**
- `GET /v1/airtime/providers` → list MPT/Ooredoo/Mytel/Atom.
- `POST /v1/airtime/topup?userId=` `{provider, phone, amount, idempotencyKey}` →
  bill-payment-service `/airtime/topup`, debits wallet, publishes `airtime.completed`.
- `GET /v1/airtime/history?userId=` shows the last top-ups.

**Cross-portal integration.** Wallet debit + `airtime.completed` → notification. No
other portal involved.

**Backend**: bill-payment-service `AirtimeController` (`/airtime/**`).
**Data**: `airtime_topups` (fdbpay_bill_payment).

---

## 6. Savings pockets — `/savings`

**What it is.** Goal-based sub-wallets (pocket name, goal amount, target date, interest
earned).

**How it works.**
- `GET /v1/savings/pockets?userId=`, `POST /v1/savings/pockets?userId=` (create).
- `POST /v1/savings/deposit?userId=` and `POST /v1/savings/withdraw?userId=` move money
  between the main wallet and a pocket.
- `GET /v1/savings/transactions?userId=&pocketId=` lists pocket activity.

**Cross-portal integration.** Gateway rewrites `/v1/savings/**` → wallet-service
`/wallet/savings/**`. Deposit/withdraw reuse the wallet ledger so balances stay
consistent with the customer dashboard. Notifications on movement flow via
`txn.completed`.

**Backend**: wallet-service `SavingsController` (`/wallet/savings/**`).
**Data**: `savings_pockets`, `savings_transactions` (fdbpay_wallet).

---

## 7. Scheduled payments — `/scheduled`

**What it is.** Recurring transfers (`daily / weekly / monthly`) to the same recipient.

**How it works.**
- Create: `POST /v1/scheduled?userId=` with `{recipientIdentifier, amount, type:'P2P',
  frequency (uppercased), description?, idempotencyKey}` → gateway rewrites to
  `/transfer/schedule/**`.
- List: `GET /v1/scheduled/my?userId=` → returns a Spring `Page`; the frontend reads
  `data.content` and maps `recipientIdentifier`→`recipient`,
  `frequency`→lowercase, `nextExecutionDate`→`nextExecution`.
- Control: `PUT /v1/scheduled/{id}/pause?userId=`, `.../resume?userId=`,
  `DELETE /v1/scheduled/{id}?userId=`.

**Cross-portal integration.** transfer-service executes each occurrence as a normal P2P
transfer (wallet debit/credit + `txn.completed`). The **recipient** (another customer)
sees the incoming money in their wallet without any action. Note: the UI offers a
`biweekly` frequency but the backend `PaymentFrequency` enum is only
`DAILY/WEEKLY/MONTHLY` — `biweekly` creation would fail validation.

**Backend**: transfer-service `ScheduledPaymentController` (`/transfer/schedule/**`).
**Data**: `scheduled_payments` (fdbpay_transfer).

---

## 8. Remittance (inbound) — `/remittance`

**What it is.** Receive international remittances via partner corridors.

**How it works.**
- `GET /v1/remittance/corridors` → active corridors (source country, currency, FX rate,
  fee, partner, limits).
- `GET /v1/remittance/quote?corridorId=&sourceAmount=` → destination amount/fee/rate.
- `POST /v1/remittance/initiate?userId=` `{corridorId, sourceAmount, recipientPhone,
  senderName, senderPhone, idempotencyKey}`.
- `GET /v1/remittance/my?userId=` → history.

**Cross-portal integration.** On completion remittance-service credits the recipient
wallet via wallet-service and publishes `remittance.received` (consumed by
remittance + notification). Outbound corridor partners are stubbed.

**Backend**: remittance-service `RemittanceController` (`/remittance/**`).
**Data**: `remittances`, `remittance_corridors` (fdbpay_remittance).

---

## 9. Promotions & cashback — `/promotions`

**What it is.** Redeem promo codes and view/withdraw a cashback wallet (shared with
merchant role).

**How it works.**
- `GET /v1/promotions/active` → promotions; `GET /v1/promotions/validate?code=`.
- `POST /v1/promotions/apply?userId=` applies a code (records usage).
- `GET /v1/promotions/cashback-wallet?userId=` → balance earned/redeemed.
- `POST /v1/promotions/cashback-redeem?userId=` moves cashback to the main wallet
  (promotions-service credits wallet-service, publishes `txn.completed`).

**Cross-portal integration.** Promotions are merchant/bank-funded; when a customer
redeems, the credit lands in the customer wallet and shows up on the wallet dashboard.
`promotions` subscribes to `txn.completed` to accrue cashback automatically.

**Backend**: promotions-service `PromotionController` (`/promotions/**`).
**Data**: `promotions`, `promotion_usages`, `cashback_wallets`, `cashback_transactions`
(fdbpay_promotions).

---

## 10. Merchant directory — `/directory`

**What it is.** Browse/search/nearby merchants; view a merchant's static QR to pay.

**How it works.**
- `GET /v1/directory/search?query=&category=` and
  `GET /v1/directory/nearby?latitude=&longitude=&radius=&category=` → gateway rewrites to
  merchant-service `/merchants/directory/**`; both return Spring `Page`s and the
  frontend reads `data.content`.
- The nearby call uses the browser's `navigator.geolocation`; when coordinates are
  missing the backend falls back to a full (unfiltered) search.

**Cross-portal integration.** This is the customer-facing window into merchant-service:
merchants registered in the merchant portal and approved by admin appear here. The QR
shown (`deepLink`/`qrUrl` from merchant-service) is the same static QR the merchant
displays in its own portal; a real payment would flow through the P2P transfer rail to
the merchant's wallet.

**Backend**: merchant-service `MerchantDirectoryController`
(`/merchants/directory/**`).
**Data**: `merchants` (fdbpay_merchant).

---

## 11. Disputes — `/disputes` (all roles)

**What it is.** Raise a dispute on a transaction, attach evidence, and (admin side)
resolve it.

**How it works.**
- `GET /v1/disputes/stats`, `GET /v1/disputes/my?userId=` (own),
  `GET /v1/disputes/all?page=&size=` (admin "All Disputes" tab).
- `POST /v1/disputes?userId=` `{transactionId, type, amount, description}`.
- `POST /v1/disputes/{id}/evidence` (add evidence),
  `POST /v1/disputes/{id}/resolve` `{action: refund|partial|dismiss, notes}`.

**Cross-portal integration.** This is a customer→admin workflow: the customer files the
dispute; the admin resolves it in the admin portal (`/admin`). `dispute.created` /
`dispute.resolved` events feed the audit service. Refund actions are intended to call
wallet-service (refund path) — the UI exposes the resolve modal to admins only.

**Backend**: dispute-service `DisputeController` (`/disputes/**`).
**Data**: `disputes`, `dispute_evidence` (fdbpay_dispute).

---

## 12. Support — `/support` (all roles)

**What it is.** Create support tickets, reply in a message thread, and (in the manager
view) resolve/escalate.

**How it works.**
- `GET /v1/support/stats`, `GET /v1/support/my-tickets?userId=` (list),
  `GET /v1/support/tickets/{id}`, `GET /v1/support/tickets/{id}/messages`.
- `POST /v1/support/tickets?userId=` `{subject, category, priority, message}`,
  `POST /v1/support/tickets/{id}/messages?userId=`.
- `PUT /v1/support/tickets/{id}/resolve?userId=`, `.../escalate`.

**Cross-portal integration.** Available to every role. The "Manager View" tab reuses the
same data source (resolve/escalate actions) and is meant for admin/ops staff. SLA
deadlines are stored on tickets in support-service.

**Backend**: support-service `SupportController` (`/support/**`).
**Data**: `support_tickets`, `ticket_messages`, `faqs` (fdbpay_support).

---

## 13. Cross-portal touchpoints (customer)

| Feature | Reads from | Writes to | Consumed by |
|---|---|---|---|
| Wallet / Ledger | wallet-service | wallet-service | every portal's money view |
| P2P transfer | transfer + auth + wallet | wallet | notification, audit, fraud, settlement, reporting, promotions |
| Request money | transfer | transfer, wallet (on accept) | notification |
| Bills / Airtime | bill-payment + wallet | wallet | notification |
| Savings | wallet-service savings | wallet | notification |
| Scheduled | transfer | wallet (per run) | notification |
| Remittance | remittance + wallet | wallet | notification |
| Promotions/cashback | promotions + wallet | wallet | notification |
| Directory | merchant-service | — | merchant portal, admin |
| Disputes | dispute-service | dispute | admin portal, audit |
| Support | support-service | support | admin/ops |

## 14. Known limitations

- Top-Up / Withdraw buttons are UI-only (disabled).
- `biweekly` scheduled frequency is not supported by the backend enum.
- KYC document upload UI is not wired into this portal page set (KYC review is
  admin-side; submission endpoints exist in kyc-service).
