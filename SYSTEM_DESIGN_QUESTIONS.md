# FDB Pay — System Design Interview Questions & Detailed Answers

**Based on:** FDB Pay SPEC.md v1.0
**Context:** FDB Bank's digital payment platform for Myanmar

---

## Table of Contents

1. [High-Level Architecture & Design Choices](#1-high-level-architecture--design-choices)
2. [Database & Data Modeling](#2-database--data-modeling)
3. [Payment Flows & Money Movement](#3-payment-flows--money-movement)
4. [Scalability & Performance](#4-scalability--performance)
5. [Reliability & Consistency](#5-reliability--consistency)
6. [Authentication & Authorization](#6-authentication--authorization)
7. [Security & Fraud](#7-security--fraud)
8. [API Design](#8-api-design)
9. [Real-Time & Async Processing](#9-real-time--async-processing)
10. [Settlement & Reconciliation](#10-settlement--reconciliation)
11. [Mobile & Offline Considerations](#11-mobile--offline-considerations)
12. [Compliance & Regulatory](#12-compliance--regulatory)
13. [Operational & Observability](#13-operational--observability)
14. [Cost & Trade-offs](#14-cost--trade-offs)

---

## 1. High-Level Architecture & Design Choices

---

### Q1.1: Why did you choose a microservices architecture over a monolith for FDB Pay? What are the trade-offs?

**Answer:**

For FDB Pay, microservices are the right choice because of the **multiple distinct bounded contexts** with very different scaling, deployment, and team ownership needs.

**Why Microservices for FDB Pay:**

| Concern | Monolith Problem | Microservices Solution |
|---------|-----------------|----------------------|
| **Team autonomy** | FDB Bank has separate teams for wallet, merchant, compliance — all deploying together causes conflicts | Each team owns and deploys their service independently (e.g., Merchant team can deploy QR changes without touching Wallet) |
| **Scaling asymmetry** | Wallet service handles 10x the traffic of Corporate Bulk Disbursement, but monolith scales everything together | Wallet service scales to 20 pods; Corporate service runs on 2 pods |
| **Different failure domains** | A bug in Bill Payment service crashes the entire platform, including live P2P transfers | Bill Payment failure is isolated — P2P transfers continue unaffected |
| **Technology fit** | One language/framework for everything | Fraud service can use Python/ML libraries; high-perf Wallet service uses Go/Java |
| **Compliance isolation** | KYC/compliance logic tangled with payment logic | KYC service has its own database access patterns and audit requirements |

**Real-World Scenario:**
On Thingyan (Myanmar Water Festival), P2P transfer volume spikes 5x. With microservices, we auto-scale Wallet and Transfer services from 8 to 40 pods independently. The Corporate service (not needed during holidays) scales down to 2 pods, saving resources. A monolith would require scaling the entire application 5x.

**Trade-offs We Accept:**

| Trade-off | Mitigation |
|-----------|-----------|
| Network latency between services (5-20ms per hop) | Co-locate services on same K8s nodes; Redis cache for hot paths |
| Distributed transactions complexity | Saga pattern with compensating actions; idempotency keys |
| Operational overhead (15+ services) | Kubernetes + Helm charts; standardized service template; service mesh (Istio) |
| Data consistency challenges | Eventual consistency where acceptable; strong consistency only for money movement via distributed lock |
| Debugging difficulty | Distributed tracing via OpenTelemetry; correlation IDs propagated across all services |

**When we WOULD use a monolith:**
If FDB Pay were a simple wallet app with only P2P transfers and no merchant ecosystem, a modular monolith would be simpler and faster to build. The decision is driven by the **multi-persona, multi-integration, regulatory** complexity.

---

### Q1.2: Why Apache Kafka over RabbitMQ or AWS SQS for FDB Pay's event layer?

**Answer:**

Kafka is the right choice for FDB Pay for several specific reasons:

**1. Event Sourcing & Audit Trail (Critical for Payments):**
Kafka retains events for configurable periods (7 days to forever). For FDB Pay, every transaction event (`txn.created`, `txn.completed`, `txn.failed`) is a permanent, replayable record. If an auditor asks "what happened to transaction X at 14:32?", we can replay the exact event sequence.

- RabbitMQ: Messages are deleted after consumption — no audit trail
- SQS: Messages deleted after processing — no replay capability

**2. Multiple Consumer Groups:**
A single `txn.completed` event is consumed independently by:

```
Topic: txn.completed
  Consumer Group: notification-service  -> sends push/SMS
  Consumer Group: analytics-service     -> updates dashboards
  Consumer Group: settlement-service    -> adds to daily settlement batch
  Consumer Group: fraud-service         -> updates ML model features
  Consumer Group: ledger-service        -> writes double-entry ledger
```

Each consumer processes at its own pace without affecting others. RabbitMQ requires exchange/fanout configuration and doesn't retain messages per consumer.

**3. Exactly-Once Semantics:**
Kafka provides exactly-once delivery within a consumer group via offset commits + idempotent producers. For FDB Pay, this means:
- Notification Service never sends duplicate SMS (critical — SMS costs money, duplicates erode trust)
- Ledger Service never double-credits an account

**4. Ordered Event Processing per Wallet:**
Kafka guarantees ordering within a partition. We partition by `wallet_id`:

```
Partition = hash(wallet_id) % num_partitions
```

All events for wallet W1 go to the same partition, ensuring we process debit-before-credit in the correct order.

**5. Real-Time Stream Processing:**
FDB Pay's Fraud & Risk Service needs real-time aggregation:

```
Kafka -> Flink -> sliding window of "5 transactions in 60 seconds from same device"
```

Kafka Streams and Flink integrate natively. RabbitMQ and SQS require external tooling.

**Real-World Scenario:**
A merchant in Yangon receives 50 QR payments between 12:00-13:00 (lunch rush). Each payment generates events across 5 consumer groups. Kafka handles this as 5 independent reads of the same data — the settlement consumer batching for T+1, the notification consumer sending confirmations, the fraud consumer checking velocity — all without impacting each other's throughput.

---

### Q1.3: Why PostgreSQL as the primary database? Why not MySQL or MongoDB?

**Answer:**

PostgreSQL is the optimal choice for FDB Pay's OLTP workload:

**1. ACID Compliance (Non-Negotiable for Payments):**
FDB Pay moves real money. A wallet debit and credit MUST be atomic:

```sql
BEGIN;
  UPDATE wallets SET balance_total = balance_total - 50000
    WHERE id = 'sender-wallet' AND balance_total >= 50000;
  UPDATE wallets SET balance_total = balance_total + 50000
    WHERE id = 'receiver-wallet';
  INSERT INTO ledger_entries ...;
  INSERT INTO ledger_entries ...;
COMMIT;
```

If either UPDATE fails, the entire transaction rolls back. MongoDB's multi-document transactions exist but are less mature and not the primary use case.

**2. Double-Entry Ledger:**
FDB Pay requires a double-entry accounting ledger. PostgreSQL's constraint system ensures integrity with CHECK constraints, foreign keys, and triggers that validate every ledger entry.

**3. Rich Query Capabilities:**
Merchant settlement requires complex aggregations:

```sql
SELECT merchant_id,
       SUM(CASE WHEN type='CREDIT' THEN amount ELSE 0 END) as gross,
       SUM(fee) as total_fees,
       SUM(amount) - SUM(fee) as net
FROM transactions
WHERE status = 'COMPLETED'
  AND created_at BETWEEN '2026-07-26 00:00' AND '2026-07-26 23:59'
GROUP BY merchant_id;
```

**4. JSONB for Flexible Metadata:**
Transaction metadata varies by type. PostgreSQL's JSONB column gives schema flexibility within a structured database.

**5. Table Partitioning:**
The `audit_log` and `ledger_entries` tables grow to billions of rows. PostgreSQL's native declarative partitioning by month keeps queries fast.

**Why Not MySQL:** No native partitioning as clean, no JSONB, less strict default transaction isolation.

**Why Not MongoDB as Primary:** We DO use MongoDB for KYC documents (unstructured, file-heavy). But for financial ledgers, relational integrity is paramount. MongoDB's eventual consistency model is risky for money movement.

---

### Q1.4: Explain the API Gateway's role in FDB Pay. What would happen without it?

**Answer:**

The API Gateway is the single entry point for ALL external traffic into FDB Pay.

**Responsibilities:**

| Function | Implementation | FDB Pay Example |
|----------|---------------|----------------|
| **Authentication** | JWT validation, token introspection | Every request checked for valid JWT; expired tokens rejected before reaching services |
| **Rate Limiting** | Sliding window per user/IP | A user can make max 10 transfer requests/minute; an IP can make 100 requests/minute |
| **Request Routing** | Path-based routing | `/v1/wallet/*` -> Wallet Service, `/v1/merchant/*` -> Merchant Service |
| **SSL Termination** | TLS offloading | All HTTPS terminated at gateway; internal services communicate via mTLS |
| **Response Caching** | Cache GET endpoints | Biller list cached for 1 hour; merchant directory cached for 5 minutes |
| **Circuit Breaking** | Fail-fast for unhealthy services | If Wallet Service is down, gateway returns 503 immediately instead of waiting |

**Without the API Gateway:**

1. **Security Chaos:** Every microservice must independently implement JWT validation, rate limiting, CORS — inconsistent and error-prone.
2. **Client Complexity:** Mobile app needs to know the internal service topology. If Wallet Service moves from port 8080 to 8081, every client breaks.
3. **No Centralized Rate Limiting:** An attacker could bypass rate limits by hitting different services directly.
4. **DDoS Vulnerability:** Without Gateway's WAF + rate limiting, DDoS attacks hit individual services directly.

**Real-World Scenario:**
During a promotional campaign ("Top up MMK 50,000, get MMK 5,000 cashback"), FDB Pay expects 10x normal traffic. The API Gateway detects the surge, auto-scales, returns 429 for excess traffic, and logs all rejected requests for analysis. Without Gateway, each service independently handles the surge — Wallet Service gets overwhelmed, Transfer Service is fine, but the platform appears broken.

---

## 2. Database & Data Modeling

---

### Q2.1: Why store balances as BIGINT (kyat) instead of DECIMAL?

**Answer:**

**The Problem with DECIMAL/FLOAT:**
DECIMAL(15,2) has edge cases with precision. For a payment platform processing millions of transactions, even tiny rounding errors compound.

**FDB Pay Implementation:**

```sql
balance_total BIGINT DEFAULT 0  -- MMK 50,000 stored as 50000
```

All amounts are in the **smallest unit** (kyat, since Myanmar has no subdivision). Conversions happen at the application layer:

```
Application layer:  MMK 50,000.00  ->  50000 (BIGINT)
Display layer:      50000  ->  "MMK 50,000"
API layer:          { "amount": 50000, "formatted": "MMK 50,000" }
```

**Why This Matters for FDB Pay:**

1. **Settlement accuracy:** When settling MMK 1,234,567,890 across 500 merchant transactions, rounding errors in DECIMAL could create discrepancies of thousands of kyat. BIGINT eliminates this.
2. **Comparison operations:** `WHERE balance >= 50000` is a simple integer comparison — faster than DECIMAL.
3. **Storage efficiency:** BIGINT is 8 bytes fixed. DECIMAL(15,2) requires up to 9 bytes + metadata.

---

### Q2.2: Explain the double-entry ledger design. Why is it critical for FDB Pay?

**Answer:**

Double-entry bookkeeping means **every financial transaction creates two entries**: a debit and a credit, which must always balance to zero.

**FDB Pay Ledger:**
When User A sends MMK 50,000 to User B:

```sql
-- Entry 1: Debit from A's wallet
INSERT INTO ledger_entries (wallet_id, type, amount, balance_after, txn_id)
VALUES ('wallet-A', 'DEBIT', 50000, 450000, 'txn-001');

-- Entry 2: Credit to B's wallet
INSERT INTO ledger_entries (wallet_id, type, amount, balance_after, txn_id)
VALUES ('wallet-B', 'CREDIT', 50000, 250000, 'txn-001');

-- Entry 3: Platform fee (if applicable)
INSERT INTO ledger_entries (wallet_id, type, amount, balance_after, txn_id)
VALUES ('wallet-platform', 'CREDIT', 500, 1234500, 'txn-001');
```

**Invariant:** `SUM(CREDIT) - SUM(DEBIT) = 0` for every transaction.

**Why Critical for FDB Pay:**

1. **Balance Reconstruction:** If `wallets.balance_total` gets corrupted, we reconstruct the correct balance from ledger entries. This always matches the wallet's balance — if not, we have a bug.
2. **Audit Trail:** Myanmar's Central Bank requires complete financial records. The ledger provides an immutable, chronological record of every money movement.
3. **Reconciliation:** When FDB Pay's balance doesn't match FDB Bank's CBS records, we trace individual ledger entries to find the discrepancy.
4. **Dispute Resolution:** If a customer claims "I was charged MMK 100,000 but only sent MMK 50,000", the ledger shows the exact entries for that transaction.

**Real-World Scenario:**
FDB Pay processes 100,000 transactions daily. At month-end, the CFO asks: "What's our total float held in wallets?" We run two queries — one summing `wallets.balance_total` and another summing all ledger entries. If they don't match, there's a data integrity issue that must be investigated immediately.

---

### Q2.3: How do you handle the N+1 query problem when loading a user's transaction history?

**Answer:**

**FDB Pay Solutions:**

**1. CQRS (Command Query Responsibility Segregation):**
Separate read and write models. The read model uses denormalized materialized views:

```sql
CREATE MATERIALIZED VIEW mv_user_transactions AS
SELECT
    t.id, t.type, t.status, t.amount, t.fee,
    t.created_at, t.completed_at,
    sender.phone as sender_phone,
    sender.name as sender_name,
    receiver.phone as receiver_phone,
    receiver.name as receiver_name,
    m.business_name as merchant_name
FROM transactions t
LEFT JOIN wallets sw ON t.sender_wallet = sw.id
LEFT JOIN users sender ON sw.user_id = sender.id
LEFT JOIN wallets rw ON t.receiver_wallet = rw.id
LEFT JOIN users receiver ON rw.user_id = receiver.id
LEFT JOIN merchants m ON rw.id = m.wallet_id;
```

**2. API-Level Data Loader:**

```python
# Instead of N queries:
for txn in transactions:
    txn.sender = db.query(User, txn.sender_wallet.user_id)  # N queries!

# Use batch loading:
sender_ids = {txn.sender_wallet_id for txn in transactions}
senders = db.query(User).filter(User.id.in_(sender_ids)).all()
sender_map = {s.id: s for s in senders}
```

**3. Redis Pre-Aggregation:**
For the transaction history page, cache pre-serialized JSON with resolved names (TTL: 60 seconds).

**Performance Comparison:**

| Approach | Queries per Request | Latency |
|----------|-------------------|---------|
| Naive N+1 | 20+ | 500ms+ |
| CQRS + Materialized View | 1 | 15ms |
| DataLoader + Redis | 2 (DB) + 1 (Redis) | 30ms |

---

### Q2.4: What indexing strategy would you use for FDB Pay's transaction table?

**Answer:**

**Growth Projections:**
- 500K users, 450K transactions/day
- ~5 TPS average, 50 TPS peak

**Indexing Strategy:**

```sql
-- Unique constraints (implicit index)
CREATE UNIQUE INDEX idx_users_phone ON users(phone);
CREATE UNIQUE INDEX idx_transactions_idempotency ON transactions(idempotency_key);

-- Composite indexes for hot paths
CREATE INDEX idx_txn_sender_date ON transactions(sender_wallet, created_at DESC);
CREATE INDEX idx_txn_receiver_date ON transactions(receiver_wallet, created_at DESC);

-- Covering index for transaction list page
CREATE INDEX idx_txn_covering ON transactions(
    sender_wallet, receiver_wallet, created_at DESC
) INCLUDE (type, status, amount, fee, description);

-- Partial index for active transactions only
CREATE INDEX idx_txn_status_date ON transactions(status, created_at)
WHERE status IN ('PENDING', 'FAILED');

-- JSONB index for merchant transactions
CREATE INDEX idx_txn_merchant_date ON transactions(
    (metadata->>'merchant_id'), created_at
) WHERE type = 'QR_MERCHANT';

-- Ledger queries
CREATE INDEX idx_ledger_wallet_date ON ledger_entries(wallet_id, created_at DESC);
CREATE INDEX idx_ledger_txn ON ledger_entries(txn_id);
```

**Partitioning Strategy:**

```sql
CREATE TABLE transactions (
    id UUID DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(64) NOT NULL,
    ...
    created_at TIMESTAMPTZ DEFAULT NOW()
) PARTITION BY RANGE (created_at);

CREATE TABLE transactions_2026_07 PARTITION OF transactions
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
```

---

## 3. Payment Flows & Money Movement

---

### Q3.1: Walk me through a P2P transfer end-to-end. How do you ensure money doesn't appear or disappear?

**Answer:**

**End-to-End P2P Transfer:**

```
Step 1: User A -> App: "Send MMK 50,000 to +95987654321, PIN: 1234"

Step 2: API Gateway:
         - Validate JWT token
         - Rate limit check (User A: 3 transfers today, limit: 50/day)
         - Route to Transfer Service

Step 3: Transfer Service:
         a. Idempotency check: Have I seen this idempotency_key before?
            - If yes, return cached response (no double processing)
         b. Validate recipient: +95987654321 -> resolve to wallet-xyz
         c. Fraud check: velocity, device fingerprint, ML score

Step 4: Wallet Service (Saga Step 1: Debit):
         a. Acquire distributed lock on wallet-abc (Redis SETNX with 10s TTL)
         b. BEGIN TRANSACTION:
            UPDATE wallets SET balance_total = balance_total - 50000,
                               version = version + 1
            WHERE id = 'wallet-abc' AND balance_total >= 50000 AND version = 5;
         c. If rows_affected == 0 -> rollback, return error
         d. INSERT INTO ledger_entries (wallet-abc, DEBIT, 50000, 450000, txn-id)
         e. COMMIT, release lock

Step 5: Wallet Service (Saga Step 2: Credit):
         a. Acquire distributed lock on wallet-xyz
         b. BEGIN TRANSACTION:
            UPDATE wallets SET balance_total = balance_total + 50000,
                               version = version + 1
            WHERE id = 'wallet-xyz' AND version = 3;
         c. INSERT INTO ledger_entries (wallet-xyz, CREDIT, 50000, 250000, txn-id)
         d. COMMIT, release lock

Step 6: Transfer Service:
         a. UPDATE transactions SET status = 'COMPLETED', completed_at = NOW()
         b. Publish to Kafka: topic='txn.completed'

Step 7: Notification Service (async):
         - Push to User A: "You sent MMK 50,000"
         - Push + SMS to User B: "You received MMK 50,000"

Step 8: Response to User A: "Transfer successful, reference: TXN-20260727-ABC"
```

**How Money Doesn't Appear or Disappear:**

1. **Atomic Debit + Credit:** Each wallet operation is a separate DB transaction. If Credit fails, Debit is compensated via a reverse transaction.
2. **Optimistic Locking:** The `version` field prevents double-spending from concurrent transactions.
3. **Double-Entry Ledger:** Every transaction creates matching DEBIT and CREDIT entries.
4. **Idempotency:** The `idempotency_key` ensures retries don't create duplicate charges.

---

### Q3.2: What happens if the sender's debit succeeds but the receiver's credit fails?

**Answer:**

This is the classic **distributed transaction failure**. We handle it via the **Saga Pattern with Compensating Transactions:**

```
Debit A: SUCCESS
Credit B: FAILED
Compensation: Reverse Debit A -> Credit A back MMK 50,000
             Mark transaction as FAILED
             Notify both parties
```

**Implementation:**

```python
def execute_transfer(sender_wallet, receiver_wallet, amount, txn_id):
    try:
        wallet_service.debit(sender_wallet, amount, txn_id)

        try:
            wallet_service.credit(receiver_wallet, amount, txn_id)
        except Exception as credit_error:
            # Compensate: credit sender back
            wallet_service.credit(sender_wallet, amount, txn_id,
                                  note="Compensation: failed transfer")
            transaction_service.mark_failed(txn_id, str(credit_error))
            return FAILED

        transaction_service.mark_completed(txn_id)
        kafka_publish('txn.completed', txn_id)
        return SUCCESS

    except Exception as debit_error:
        transaction_service.mark_failed(txn_id, str(debit_error))
        return FAILED
```

**Edge Case: What if compensation itself fails?**

A **reconciliation service** runs every 5 minutes:

```sql
SELECT t.id, t.sender_wallet, t.amount
FROM transactions t
WHERE t.status = 'PENDING'
  AND t.created_at < NOW() - INTERVAL '5 minutes'
  AND EXISTS (
      SELECT 1 FROM ledger_entries le
      WHERE le.txn_id = t.id AND le.type = 'DEBIT'
  )
  AND NOT EXISTS (
      SELECT 1 FROM ledger_entries le
      WHERE le.txn_id = t.id AND le.type = 'CREDIT'
        AND le.wallet_id = t.sender_wallet
  );
```

These are flagged for **manual review** by the operations team.

---

### Q3.3: How does the QR merchant payment flow handle the sound-box notification?

**Answer:**

The sound-box is an IoT device connected via persistent WebSocket:

```
FDB Pay Server --WebSocket--> Sound-Box Device
```

When a `txn.completed` event arrives for a merchant:

```python
def handle_merchant_payment(event):
    merchant_id = event['merchant_id']
    amount = event['amount']

    # 1. Try WebSocket (real-time, < 1 second)
    if websocket_manager.is_connected(merchant_id):
        websocket_manager.send(merchant_id, {
            "type": "PAYMENT_RECEIVED",
            "amount": amount,
            "message": f"Payment received: {format_mmk(amount)}"
        })
        return

    # 2. Fallback: Push notification to merchant's phone
    push_service.send(merchant_id, {
        "title": "Payment Received",
        "body": f"MMK {amount:,} received from customer"
    })

    # 3. Fallback: SMS
    merchant = merchant_service.get(merchant_id)
    sms_service.send(merchant.phone,
        f"FDB Pay: MMK {amount:,} received. Ref: {event['txn_id']}")
```

**Handling Offline Sound-Box:**

| Scenario | Detection | Fallback |
|----------|-----------|----------|
| Sound-box powered off | WebSocket disconnect | Push + SMS |
| Internet down at shop | Ping timeout (10s) | Push + SMS |
| Hardware failure | No health ping response | Alert merchant via SMS |

**Real-World Scenario:**
A tea shop in Mandalay loses power. Between 14:00-16:00, 15 customers make QR payments totaling MMK 375,000. Each customer's app shows "Payment Successful". The merchant receives push notifications on their phone. When power returns, the sound-box reconnects and plays all 15 missed payment announcements.

---

## 4. Scalability & Performance

---

### Q4.1: How do you design FDB Pay for 500K users and 450K daily transactions?

**Answer:**

**Load Analysis:**

| Metric | Value |
|--------|-------|
| Total users | 500,000 |
| Monthly Active Users | 150,000 (30%) |
| Daily Active Users | 50,000 (10%) |
| Transactions per day | 450,000 |
| TPS (average) | ~5 |
| TPS (peak, lunch rush) | ~50 |
| Concurrent users (peak) | ~5,000 |
| API requests per second | ~200 RPS |

**Architecture Scaling:**

```
Traffic Layer:    Nginx Load Balancer (2 instances, active-active)
                  -> 10K concurrent connections, SSL termination

API Gateway:      3 replicas
                  -> 200 RPS sustained, 1000 RPS burst
                  -> JWT validation ~2ms per request

Core Services:    Wallet:     4-8 pods (HPA: CPU > 60%)
                  Transfer:   4-8 pods
                  Auth:       2-4 pods
                  Merchant:   2-4 pods
                  Bill Pay:   2-4 pods
                  Others:     2 pods each

Data Layer:       PostgreSQL: Primary + 2 read replicas, PgBouncer (200 conns)
                  Redis:      3-node cluster, 16GB total
                  Kafka:      3 brokers, 12 partitions per topic
```

**Key Scaling Decisions:**

1. **Wallet balance in Redis:** Sub-millisecond reads. DB only serves ~1% cache miss rate.
2. **Kafka for async operations:** Notifications, analytics, settlement processing happen async.
3. **Read replicas:** Merchant dashboard queries hit replicas, not primary.

---

### Q4.2: How do you handle traffic spikes (e.g., a "Double Cashback Day" promotion)?

**Answer:**

**Pre-Event (2 hours before):**
```bash
kubectl scale deployment wallet-service --replicas=20
kubectl scale deployment transfer-service --replicas=20
# Scale Redis, Kafka partitions, warm caches
```

**Auto-Scaling (Kubernetes HPA):**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  scaleTargetRef:
    name: wallet-service
  minReplicas: 8
  maxReplicas: 30
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 60
```

**Degradation Strategy (Load Shedding):**

```python
PRIORITY_CRITICAL = ['transfer.confirm', 'auth.login', 'wallet.balance']
PRIORITY_HIGH = ['transfer.initiate', 'qr.scan']
PRIORITY_LOW = ['history.list', 'profile.update', 'export.ledger']

if system_load > 0.8:
    if endpoint.priority == PRIORITY_LOW:
        return Response(status=503, body="Temporarily unavailable")
    if endpoint.priority == PRIORITY_HIGH:
        return queue_request(endpoint, timeout=10)
```

---

## 5. Reliability & Consistency

---

### Q5.1: How do you ensure idempotency across FDB Pay's payment APIs?

**Answer:**

**Why Idempotency Matters:**
Network timeouts, app crashes, and retries are common in mobile environments. Without idempotency: User taps "Send" -> timeout -> taps again -> **double charge**.

**Implementation:**

```
Mobile App -> API Gateway:
  - Includes X-Idempotency-Key header (client-generated UUID)

API Gateway:
  1. Check Redis for existing key
  2. If found -> return cached response (same result, no reprocessing)
  3. If not found -> process request, store result in Redis (TTL: 24h)
```

**Idempotency Key Generation:**

```javascript
// Mobile app generates UUID v4 per transaction
const idempotencyKey = `fdb-${Date.now()}-${uuidv4()}`;
// Example: "fdb-1690444200000-a1b2c3d4-e5f6-7890-abcd-ef1234567890"

// For retries (same logical operation), app reuses the same key
// For new transactions, app generates a new key
```

---

### Q5.2: What happens if Kafka goes down? What's the impact on FDB Pay?

**Answer:**

**Impact Analysis:**

| Component | Impact if Kafka Down |
|-----------|---------------------|
| Payment processing (debit/credit) | **No impact** — payments still process, events queued locally |
| Notifications (SMS, push) | **Delayed** — queued in service memory, sent when Kafka recovers |
| Settlement | **Delayed** — batch waits, processes next window |
| Fraud detection | **Degraded** — real-time ML unavailable, falls back to rule-based |
| Audit logging | **Degraded** — entries queued, written when Kafka recovers |
| Analytics | **Delayed** — dashboard data stale |

**Resilience Strategy:**

**1. Local Event Buffer (Write-Ahead Log):**

```python
class LocalEventBuffer:
    def __init__(self, buffer_file="/var/fdbpay/events/pending.log"):
        self.buffer = open(buffer_file, "a+")

    def publish(self, topic, event):
        # 1. Write to local disk first (durable)
        self.buffer.write(json.dumps({"topic": topic, "event": event}))
        self.buffer.flush()

        # 2. Try Kafka
        try:
            kafka_producer.send(topic, event)
            # 3. Mark as sent in local buffer
            self.mark_sent(event['id'])
        except KafkaError:
            # Kafka is down — event is safe on disk
            # Background thread will retry
            pass
```

**2. Background Retry Thread:**
```python
def retry_failed_events():
    while True:
        unsent = read_unsent_from_buffer()
        for event in unsent:
            try:
                kafka_producer.send(event['topic'], event['event'])
                mark_sent(event['id'])
            except KafkaError:
                time.sleep(5)  # Backoff
        time.sleep(10)
```

**3. Graceful Degradation:**
```python
def process_payment(payment):
    # Always process payment (critical path)
    debit_sender(payment)
    credit_receiver(payment)

    # Try to publish event (non-critical path)
    try:
        kafka_publish('txn.completed', payment)
    except KafkaError:
        local_buffer.store(payment)  # Will retry later
        log.warning(f"Kafka down, buffered event for {payment.id}")

    # Notifications are best-effort — if Kafka is down,
    # we rely on SMS fallback (direct API call, not via Kafka)
    sms_service.send(payment.receiver, "Payment received!")
```

---

### Q5.3: How do you handle distributed locks for wallet operations? What if the lock holder crashes?

**Answer:**

**Lock Implementation (Redis-based):**

```python
import redis
import uuid
import time

class DistributedLock:
    def __init__(self, redis_client):
        self.redis = redis_client

    def acquire(self, lock_name, ttl=10):
        lock_id = str(uuid.uuid4())
        acquired = self.redis.set(
            f"lock:{lock_name}",
            lock_id,
            nx=True,      # Only set if not exists
            ex=ttl         # Auto-expire after TTL (safety net)
        )
        return lock_id if acquired else None

    def release(self, lock_name, lock_id):
        # Only release if we own the lock (prevent releasing someone else's)
        script = """
        if redis.call("get", KEYS[1]) == ARGV[1] then
            return redis.call("del", KEYS[1])
        else
            return 0
        end
        """
        self.redis.eval(script, 1, f"lock:{lock_name}", lock_id)

    def __enter__(self):
        self.lock_id = self.acquire(self.lock_name)
        if not self.lock_id:
            raise LockAcquisitionError("Could not acquire lock")
        return self.lock_id

    def __exit__(self, *args):
        self.release(self.lock_name, self.lock_id)
```

**What If Lock Holder Crashes?**

The `ex=ttl` (10 seconds) is the safety net. If a service crashes while holding a lock:

1. **Within 10 seconds:** Other services wait and retry (the lock is still held)
2. **After 10 seconds:** Redis auto-deletes the lock (TTL expiry)
3. **Next service acquires the lock** and processes the wallet operation

**But what about the in-flight operation?**

If the crash happened AFTER the DB commit but BEFORE releasing the lock:
- The debit/credit is already committed (durable in PostgreSQL)
- The lock auto-expires after 10s
- The next request acquires the lock and sees the updated balance

If the crash happened BEFORE the DB commit:
- The transaction never committed (rolled back by PostgreSQL)
- The lock auto-expires
- The next request sees the original balance and can retry

**Real-World Scenario:**
User A transfers MMK 50,000. The Wallet Service acquires the lock, debits the wallet, but crashes before crediting the receiver. The Saga orchestrator detects the incomplete transaction (via Kafka timeout or local state), acquires the lock (now expired), and compensates by crediting User A back. The reconciliation service runs 5 minutes later and verifies the ledger is balanced.

---

### Q5.4: How do you prevent double-spending in FDB Pay?

**Answer:**

**Multiple Layers of Protection:**

```
Layer 1: Idempotency Key
  - Same request (same idempotency_key) -> return cached response
  - Prevents app-level retries from creating duplicate transactions

Layer 2: Optimistic Locking (version field)
  UPDATE wallets SET balance_total = balance_total - 50000, version = version + 1
  WHERE id = 'wallet-abc' AND version = 5 AND balance_total >= 50000;
  - If another transaction modified the wallet, version won't match -> retry

Layer 3: Distributed Lock (Redis)
  - Ensures only one operation processes a wallet at a time
  - TTL of 10s prevents deadlock if holder crashes

Layer 4: Database Constraints
  CHECK (balance_total >= 0) -- Cannot go negative
  UNIQUE (idempotency_key)  -- Cannot insert duplicate transactions

Layer 5: Fraud Detection
  - Velocity checks: "5 transfers in 60 seconds" -> block
  - Device fingerprint: "Same device, different accounts" -> flag
  - ML model: "Unusual amount for this user" -> step-up auth
```

**Real-World Scenario:**
A user in Yangon tries to pay at two shops simultaneously by scanning two QR codes. Both transactions target the same wallet:

```
Transaction A: Debit wallet-X 5000, Credit merchant-1
Transaction B: Debit wallet-X 8000, Credit merchant-2
```

Both try to acquire the lock on wallet-X. Transaction A gets the lock first, debits the wallet (balance: 20000 -> 15000), releases the lock. Transaction B acquires the lock, sees balance 15000 >= 8000, debits (15000 -> 7000), succeeds. Both transactions complete.

If the user only had MMK 5000:
- Transaction A succeeds (5000 >= 5000, balance -> 0)
- Transaction B fails (0 < 8000, returns INSUFFICIENT_BALANCE)

---

## 6. Authentication & Authorization

---

### Q6.1: Walk me through the full authentication flow for a new FDB Pay user registering on a mobile device in Myanmar. What security considerations are specific to Myanmar?

**Answer:**

**End-to-End Registration & First Login Flow:**

```
Step 1: User downloads FDB Pay app from Google Play / Huawei AppGallery
        (iOS App Store has low penetration in Myanmar — most users on Android)

Step 2: App launches -> User taps "Register"

Step 3: User enters Myanmar phone number: +959XXXXXXXXX
        App validates format: must be 9-10 digits after +959 prefix
        App checks network connectivity (may be intermittent)

Step 4: App requests OTP
        POST /auth/otp/send
        Body: { "phone": "+959123456789" }

Step 5: OTP Service generates 6-digit code
        - Store in Redis: otp:+959123456789 -> { code: "482917", attempts: 0, expires_at: +3min }
        - Send via MPT Bulk SMS (primary) or Twilio (fallback)
        - User receives SMS: "Your FDB Pay verification code is 482917. Valid for 3 minutes."

Step 6: User enters OTP in app
        POST /auth/otp/verify
        Body: { "phone": "+959123456789", "code": "482917" }

Step 7: OTP Service validates:
        - Code matches? Yes
        - Not expired? Yes (1 min 42 sec elapsed)
        - Attempts < 3? Yes (first attempt)
        -> Mark phone as verified

Step 8: App prompts user to set MPIN (4-6 digit numeric PIN)
        User enters: 1234 (for demo — app encourages stronger PIN)
        POST /auth/pin/set
        Body: { "phone": "+959123456789", "pin": "1234" }

Step 9: Auth Service:
        - Validate PIN complexity (no repeating digits, not sequential)
        - Hash with bcrypt (cost factor 12): pin_hash = bcrypt("1234")
        - Store in users table: pin_hash, pin_attempts=0

Step 10: Auth Service creates user record:
         INSERT INTO users (phone, status, kyc_tier, pin_hash, referral_code)
         VALUES ('+959123456789', 'ACTIVE', 'BASIC', '$2b$12$...', 'ABCD1234')

Step 11: Auth Service creates wallet:
         INSERT INTO wallets (user_id, currency, status, balance_total, daily_limit, kyc_tier)
         VALUES (user_id, 'MMK', 'ACTIVE', 0, 500000, 'BASIC')

Step 12: Auth Service issues JWT:
         {
           "sub": "user_id_abc123",
           "phone": "+959123456789",
           "kyc_tier": "BASIC",
           "iat": 1690444200,
           "exp": 1690445100  // 15 minutes
         }
         Sign with RS256 (RSA private key)

Step 13: Store refresh token in Redis:
         session:user_id_abc123 -> {
           "access_token": "eyJ...",
           "refresh_token": "eyJ...",
           "device_id": "device_xyz",
           "expires_at": "2026-07-27T10:45:00Z"
         }

Step 14: App stores tokens securely:
         - Access token: in-memory only (lost on app kill)
         - Refresh token: Android Keystore / iOS Keychain
         - Device fingerprint: hashed device model + IMEI

Step 15: App registers device (trusted device):
         POST /auth/device/register
         Body: { "device_fingerprint": "abc123...", "device_name": "Samsung Galaxy A12" }
         Max 3 trusted devices per user

Step 16: User sees home screen with MMK 0 balance
```

**Myanmar-Specific Security Considerations:**

| Consideration | Implementation |
|---------------|---------------|
| **Phone number format** | All Myanmar numbers are +959XXXXXXXXX (10 digits after +959). Validate strictly — no landline numbers for registration. |
| **SIM swap risk** | Myanmar has high SIM swap fraud. For transfers > MMK 500,000, require step-up auth (re-enter PIN + OTP). |
| **Shared devices** | Many families share a single Android phone. Device trust is per-user, not per-device. Allow multiple users on same device but flag when > 2 accounts share a device. |
| **Low-literacy users** | MPIN is 4 digits (not complex passwords). Biometric (fingerprint) supported on capable devices. USSD fallback for feature phones. |
| **USSD sessions** | Feature phone users authenticate via USSD: dial *123#, enter phone, enter PIN. Session timeout: 3 minutes. Max 3 PIN attempts per session. |
| **Offline OTP delivery** | SMS delivery can be delayed in rural areas. OTP validity extended to 5 minutes (vs 3 min standard). Allow voice call OTP as fallback. |
| **Network interruptions** | App caches last known good JWT. If network drops mid-transaction, app queues the request and sends when connectivity resumes (with idempotency key). |

---

### Q6.2: How does the JWT-based session management work in FDB Pay? Why 15-minute access tokens?

**Answer:**

**JWT Token Architecture:**

```
┌──────────────────────────────────────────────────────────────────┐
│                    TOKEN LIFECYCLE                                │
│                                                                  │
│  Login                                                           │
│    │                                                             │
│    v                                                             │
│  [Access Token]  (15 min, JWT, RS256 signed)                    │
│    │                                                             │
│    ├── Used for: API requests (Authorization: Bearer <token>)    │
│    ├── Contains: user_id, phone, kyc_tier, iat, exp             │
│    ├── Stateless: any service can validate without DB lookup     │
│    └── Cannot be revoked before expiry (by design)               │
│                                                                  │
│  [Refresh Token]  (7 days, opaque, stored in Redis)              │
│    │                                                             │
│    ├── Used for: Getting new access tokens                       │
│    ├── Stateless: not needed — Redis lookup required             │
│    ├── CAN be revoked: delete from Redis = instant invalidation  │
│    └── Rotated on each use: old refresh token invalidated        │
│                                                                  │
│  Access Token expires                                            │
│    │                                                             │
│    v                                                             │
│  POST /auth/token/refresh                                        │
│    │  Body: { "refresh_token": "eyJ..." }                       │
│    │                                                             │
│    v                                                             │
│  Auth Service:                                                   │
│    1. Look up refresh token in Redis                             │
│    2. If not found -> expired/revoked -> force re-login          │
│    3. If found -> issue new access + refresh token pair          │
│    4. Delete old refresh token from Redis (rotation)             │
│    5. Store new refresh token in Redis                           │
│    6. Return new tokens to client                                │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Why 15-Minute Access Tokens?**

| Factor | Reasoning |
|--------|-----------|
| **Stolen token window** | If a JWT is intercepted (man-in-the-middle, log leak), it's only valid for 15 minutes. An attacker has a very narrow window. |
| **No revocation needed** | Since tokens expire quickly, we don't need a token blacklist. If a user logs out, we just delete the refresh token from Redis — the access token expires naturally within 15 min. |
| **Myanmar network reality** | 15 minutes is long enough that a user on a slow 3G connection in Sagaing won't get logged out mid-transaction, but short enough to limit exposure. |
| **Refresh overhead** | Refreshing every 15 min is lightweight: one Redis GET + JWT signing (CPU-only, no DB call). |
| **Mobile battery** | Longer token life means fewer refresh calls, saving battery on low-end Android devices common in Myanmar. |

**Alternative considered:** 5-minute tokens (more secure) rejected because Myanmar's 3G latency (200-500ms) means the refresh round-trip adds noticeable delay on every app open.

---

### Q6.3: How would you implement Role-Based Access Control (RBAC) for FDB Pay's different user types?

**Answer:**

**RBAC Architecture:**

```
┌──────────────────────────────────────────────────────────────────┐
│                    RBAC ROLE HIERARCHY                            │
│                                                                  │
│  ┌──────────┐                                                    │
│  │ CONSUMER │  Permissions: wallet.read, transfer.initiate,      │
│  └──────────┘           bill.pay, history.read                  │
│       │                                                          │
│  ┌──────────┐                                                    │
│  │ MERCHANT │  Permissions: consumer.all +                       │
│  └──────────┘           merchant.qr.generate,                   │
│                          merchant.transactions.read,             │
│                          merchant.settlements.read               │
│       │                                                          │
│  ┌──────────┐                                                    │
│  │  AGENT   │  Permissions: consumer.all +                       │
│  └──────────┘           agent.cashin, agent.cashout,             │
│                          agent.float.manage                      │
│       │                                                          │
│  ┌──────────┐                                                    │
│  │ CORPORATE│  Permissions: consumer.all +                       │
│  └──────────┘           corp.bulk_disburse,                      │
│                          corp.reconciliation.read,               │
│                          corp.payroll.manage                     │
│       │                                                          │
│  ┌──────────┐                                                    │
│  │  ADMIN   │  Permissions: ALL (with approval workflows)        │
│  └──────────┘                                                    │
│       │                                                          │
│  ┌──────────┐                                                    │
│  │ SUPER    │  Permissions: system.config, admin.manage          │
│  │  ADMIN   │  (FDB Bank internal only, max 3 people)           │
│  └──────────┘                                                    │
└──────────────────────────────────────────────────────────────────┘
```

**Permission Definition:**

```yaml
# permissions.yml
roles:
  CONSUMER:
    permissions:
      - wallet.read
      - wallet.topup
      - wallet.withdraw
      - transfer.initiate
      - transfer.history
      - bill.pay
      - bill.history
      - profile.read
      - profile.update
      - kyc.submit

  MERCHANT:
    inherits: CONSUMER
    permissions:
      - merchant.qr.generate
      - merchant.qr.static
      - merchant.transactions.read
      - merchant.settlements.read
      - merchant.refund.initiate
      - merchant.staff.manage
      - merchant.dashboard.read

  AGENT:
    inherits: CONSUMER
    permissions:
      - agent.cashin
      - agent.cashout
      - agent.float.read
      - agent.commission.read
      - agent.qr.generate

  CORPORATE:
    inherits: CONSUMER
    permissions:
      - corp.bulk_disburse
      - corp.reconciliation.read
      - corp.payroll.manage
      - corp.api.access

  ADMIN:
    permissions:
      - admin.dashboard.read
      - admin.users.read
      - admin.users.suspend
      - admin.merchants.read
      - admin.merchants.approve
      - admin.kyc.review
      - admin.disputes.resolve
      - admin.aml.alerts
      - admin.config.read
      - admin.audit.read
      - admin.reports.generate

  SUPER_ADMIN:
    inherits: ADMIN
    permissions:
      - admin.config.update
      - admin.admins.manage
      - system.fee.update
      - system.limit.update
```

**JWT Claims with Role & Permissions:**

```json
{
  "sub": "user_abc123",
  "phone": "+959123456789",
  "role": "MERCHANT",
  "permissions": [
    "wallet.read",
    "transfer.initiate",
    "merchant.qr.generate",
    "merchant.transactions.read"
  ],
  "kyc_tier": "ENHANCED",
  "iat": 1690444200,
  "exp": 1690445100
}
```

**API Gateway Permission Check:**

```python
# API Gateway middleware
def check_permission(required_permission):
    def middleware(request):
        token = extract_jwt(request)
        if not token:
            return Error(401, "Authentication required")

        permissions = token.get('permissions', [])

        if required_permission not in permissions:
            return Error(403, f"Permission denied: {required_permission} required")

        return next(request)

    return middleware

# Route protection examples:
router.post("/v1/merchant/qr/generate",
    check_permission("merchant.qr.generate"),
    merchant_controller.generate_qr)

router.post("/v1/admin/merchants/{id}/status",
    check_permission("admin.merchants.approve"),
    admin_controller.update_merchant_status)

router.post("/v1/corp/bulk-disburse",
    check_permission("corp.bulk_disburse"),
    corporate_controller.bulk_disburse)
```

**Real-World Scenario:**
A merchant's staff member logs into the merchant app. Their JWT has `role: "MERCHANT_STAFF"` with limited permissions: they can process QR payments but cannot view settlement reports or initiate refunds. When they try to access `/v1/merchant/settlements`, the API Gateway returns `403 Forbidden: settlement.read permission required`. The merchant owner, with full `MERCHANT` role, can access settlements.

---

### Q6.4: How do you handle authentication across FDB Pay's multiple channels (app, USSD, web, POS)? Is it the same everywhere?

**Answer:**

**Multi-Channel Authentication Architecture:**

```
┌──────────────────────────────────────────────────────────────────┐
│              CHANNEL-SPECIFIC AUTHENTICATION                      │
│                                                                  │
│  ┌──────────┐  Auth: Phone + MPIN + Device Fingerprint          │
│  │ Mobile   │  Token: JWT (access + refresh)                    │
│  │ App      │  Storage: Android Keystore / iOS Keychain         │
│  │          │  Biometric: Fingerprint/Face (optional)           │
│  └──────────┘                                                    │
│                                                                  │
│  ┌──────────┐  Auth: Phone + MPIN                               │
│  │   USSD   │  Token: Session ID (Redis, 3-min TTL)            │
│  │ Gateway  │  No persistent tokens — session-based             │
│  │          │  Each USSD menu = new request with session ID     │
│  └──────────┘                                                    │
│                                                                  │
│  ┌──────────┐  Auth: Email/Phone + Password (or SSO)            │
│  │   Web    │  Token: JWT (access + refresh)                    │
│  │ Portal   │  + CSRF token for state-changing operations       │
│  │          │  PKCE: For OAuth2 auth code flow (if applicable)  │
│  └──────────┘                                                    │
│                                                                  │
│  ┌──────────┐  Auth: Device certificate + Merchant PIN           │
│  │   POS    │  Token: Device JWT (long-lived, 30 days)          │
│  │Terminal  │  Auto-authenticated (no user interaction)         │
│  │          │  Audio confirmation (sound-box)                   │
│  └──────────┘                                                    │
│                                                                  │
│  ┌──────────┐  Auth: API Key + Secret (client_credentials)      │
│  │Corporate │  Token: OAuth2 access token (1 hour)              │
│  │  API     │  No refresh token (re-authenticate with creds)    │
│  │          │  IP whitelist for additional security              │
│  └──────────┘                                                    │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Channel-Specific Details:**

| Channel | Auth Method | Token Type | Session Duration | PIN/Password |
|---------|------------|------------|-----------------|--------------|
| Mobile App | Phone + MPIN | JWT | 15 min access, 7-day refresh | 4-6 digit MPIN |
| Mobile App (biometric) | Phone + Fingerprint | JWT | 15 min access, 7-day refresh | Biometric (replaces MPIN for login) |
| USSD | Phone + MPIN | Session ID | 3 minutes | 4-digit PIN |
| Web Portal | Email + Password | JWT | 15 min access, 7-day refresh | Complex password |
| POS Terminal | Device cert | Device JWT | 30 days | Device auto-auth |
| Corporate API | API Key + Secret | OAuth2 token | 1 hour | API secret |

**USSD Authentication Flow (Unique):**

```
User dials *123#
    |
    v
USSD Gateway: Check if session exists for this phone number
    |
    +-- New session:
    |   "Welcome to FDB Pay. Enter your PIN:"
    |   User enters: ****
    |   Auth Service validates PIN
    |   Create session in Redis: session:{phone} -> { user_id, created_at }
    |   TTL: 180 seconds (3 minutes)
    |
    +-- Existing session:
        Check Redis TTL
            +-- If valid: Process menu navigation
            +-- If expired: "Session expired. Please dial *123# again."
```

**POS Terminal Authentication:**

```python
# POS device authentication during setup
def register_pos_device(device_id, merchant_id, device_certificate):
    # 1. Validate device certificate (issued by FDB Pay)
    if not validate_certificate(device_certificate):
        return Error("Invalid device certificate")

    # 2. Link device to merchant
    db.insert("pos_devices", {
        "device_id": device_id,
        "merchant_id": merchant_id,
        "status": "ACTIVE",
        "last_heartbeat": datetime.now()
    })

    # 3. Issue long-lived device JWT
    token = jwt.encode({
        "device_id": device_id,
        "merchant_id": merchant_id,
        "type": "DEVICE",
        "iat": datetime.now(),
        "exp": datetime.now() + timedelta(days=30)
    }, private_key, algorithm="RS256")

    return Success(device_token=token)
```

**Real-World Scenario:**
A tea shop owner in Mandalay uses all channels:
- **Morning:** Opens FDB Pay app on phone (biometric login) to check yesterday's settlements
- **During the day:** Staff uses POS terminal (auto-authenticated device) to accept QR payments
- **Evening:** Owner dials *123# on feature phone (USSD, PIN auth) to check balance
- **Monthly:** Owner logs into web portal (email + password) to download tax reports

Each channel authenticates independently but shares the same wallet and user identity.

---

### Q6.5: How do you handle password/PIN reset securely? What if someone's phone is stolen?

**Answer:**

**PIN Reset Flow (Mobile App):**

```
Step 1: User taps "Forgot PIN?" on login screen

Step 2: App sends OTP to registered phone number
        POST /auth/otp/send
        Body: { "phone": "+959123456789", "purpose": "pin_reset" }

Step 3: User receives SMS: "Your FDB Pay PIN reset code is 739201"

Step 4: User enters OTP in app
        POST /auth/otp/verify
        Body: { "phone": "+959123456789", "code": "739201", "purpose": "pin_reset" }

Step 5: OTP validated -> App shows "Set new PIN" screen

Step 6: User enters new PIN (twice for confirmation)
        POST /auth/pin/reset
        Body: { "phone": "+959123456789", "otp_token": "verified_token_xyz", "new_pin": "5678" }

Step 7: Auth Service:
        - Validate OTP token is verified and not expired
        - Hash new PIN: bcrypt("5678")
        - Update users table: pin_hash = new_hash, pin_attempts = 0
        - Invalidate all existing sessions (force re-login on all devices)
        - Log audit event: PIN_RESET

Step 8: Response: "PIN reset successful. Please login with your new PIN."

Step 9: All existing JWTs become invalid:
        - Delete all refresh tokens for this user from Redis
        - Access tokens expire naturally within 15 minutes
        - User must re-authenticate on all devices
```

**Stolen Phone Scenario:**

```
┌──────────────────────────────────────────────────────────────────┐
│              STOLEN PHONE RESPONSE PLAN                          │
│                                                                  │
│  Scenario: User's phone is stolen in a Yangon market             │
│                                                                  │
│  Immediate Actions (User):                                       │
│  1. Call FDB Pay hotline: 01-XXXXXXX                             │
│  2. Provide: phone number + NRC number for identity verification │
│  3. Request: account freeze                                       │
│                                                                  │
│  System Response:                                                │
│  1. Admin freezes user account:                                  │
│     UPDATE users SET status = 'FROZEN' WHERE phone = '+959...';  │
│                                                                  │
│  2. All sessions invalidated:                                    │
│     DELETE FROM redis WHERE key LIKE 'session:user_abc%';        │
│                                                                  │
│  3. Wallet frozen (no transactions possible):                    │
│     UPDATE wallets SET status = 'FROZEN' WHERE user_id = 'abc';  │
│                                                                  │
│  4. If device reported stolen, POS devices flagged:              │
│     UPDATE pos_devices SET status = 'BLOCKED'                   │
│     WHERE merchant_id IN (SELECT merchant_id FROM ...);          │
│                                                                  │
│  Security Measures (Already in place):                           │
│  - App requires MPIN for every transaction (not just login)     │
│  - Biometric login doesn't bypass transaction PIN               │
│  - After 5 failed PIN attempts: 30-minute lockout               │
│  - After 3 lockouts: account auto-freeze                        │
│  - Device fingerprinting: stolen device flagged in system       │
│  - Transaction alerts: every transaction sends SMS notification │
│                                                                  │
│  Recovery:                                                       │
│  1. User visits FDB branch with NRC                             │
│  2. Identity verified in-person                                  │
│  3. Account unfrozen                                             │
│  4. New PIN set at branch (or via OTP on new SIM)               │
│  5. All previous devices de-registered                           │
│  6. User re-registers on new device                             │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**PIN Security Controls:**

```python
class PinSecurity:
    MAX_ATTEMPTS = 5
    LOCKOUT_DURATION = timedelta(minutes=30)
    MAX_LOCKOUTS = 3
    ACCOUNT_FREEZE_AFTER = 3

    def validate_pin(self, user, submitted_pin):
        # Check if account is locked
        if user.pin_locked_until and user.pin_locked_until > datetime.now():
            remaining = user.pin_locked_until - datetime.now()
            return Error(f"Account locked. Try again in {remaining.seconds // 60} minutes.")

        # Hash and compare
        if not bcrypt.checkpw(submitted_pin.encode(), user.pin_hash.encode()):
            # Increment attempts
            attempts = user.pin_attempts + 1

            if attempts >= self.MAX_ATTEMPTS:
                # Lock the account
                user.pin_locked_until = datetime.now() + self.LOCKOUT_DURATION
                user.pin_attempts = 0

                # Track lockout count
                lockout_count = redis.incr(f"pin_lockouts:{user.id}")
                redis.expire(f"pin_lockouts:{user.id}", 86400)  # 24h window

                if lockout_count >= self.MAX_LOCKOUTS:
                    # Freeze account entirely
                    self.freeze_account(user, reason="Multiple PIN lockouts - possible theft")
                    return Error("Account frozen for security. Please contact support.")

                return Error(f"Too many attempts. Account locked for {self.LOCKOUT_DURATION.seconds // 60} minutes.")

            user.pin_attempts = attempts
            user.save()
            return Error(f"Incorrect PIN. {self.MAX_ATTEMPTS - attempts} attempts remaining.")

        # PIN correct
        user.pin_attempts = 0
        user.pin_locked_until = None
        user.save()
        return Success()

    def freeze_account(self, user, reason):
        user.status = 'FROZEN'
        user.save()

        # Freeze wallet
        wallet = db.query("SELECT id FROM wallets WHERE user_id = %s", user.id)
        db.execute("UPDATE wallets SET status = 'FROZEN' WHERE id = %s", wallet.id)

        # Invalidate all sessions
        redis.delete_pattern(f"session:{user.id}:*")

        # Log audit
        db.insert("audit_log", {
            "actor_id": "SYSTEM",
            "actor_type": "SYSTEM",
            "action": "ACCOUNT_FROZEN",
            "resource_type": "USER",
            "resource_id": user.id,
            "details": {"reason": reason}
        })

        # Notify user via SMS (if SIM still active)
        sms_service.send(user.phone, "FDB Pay: Your account has been frozen for security. Please visit your nearest FDB branch.")

        # Alert compliance team
        kafka_publish("security.account_frozen", {
            "user_id": user.id,
            "reason": reason,
            "timestamp": datetime.now().isoformat()
        })
```

---

### Q6.6: How would you design the OAuth2 flow for FDB Pay's corporate API clients?

**Answer:**

**Corporate API Authentication (OAuth2 Client Credentials):**

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Corporate   │     │  FDB Pay     │     │  FDB Pay     │
│  ERP System  │     │  Token       │     │  API Gateway │
│              │     │  Service     │     │              │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       │ 1. POST /oauth/token                   │
       │ Body: {                                │
       │   "grant_type": "client_credentials",  │
       │   "client_id": "corp_abc123",          │
       │   "client_secret": "secret_xyz...",    │
       │   "scope": "bulk_disburse reconcile"   │
       │ }                                      │
       │───────────────────▶│                    │
       │                    │                    │
       │                    │ 2. Validate:       │
       │                    │ - client_id exists │
       │                    │ - secret matches   │
       │                    │ - IP whitelisted   │
       │                    │ - scope allowed    │
       │                    │                    │
       │ 3. Token Response  │                    │
       │ {                  │                    │
       │   "access_token":  │                    │
       │     "eyJhbG...",   │                    │
       │   "token_type":    │                    │
       │     "Bearer",      │                    │
       │   "expires_in":    │                    │
       │     3600,          │                    │
       │   "scope":         │                    │
       │     "bulk_disburse │                    │
       │      reconcile"    │                    │
       │ }                  │                    │
       │◀───────────────────│                    │
       │                    │                    │
       │ 4. API Request     │                    │
       │ Authorization:     │                    │
       │   Bearer eyJhbG... │                    │
       │───────────────────────────────────────▶│
       │                    │                    │
       │                    │ 5. Validate JWT:   │
       │                    │ - Signature valid  │
       │                    │ - Not expired      │
       │                    │ - Scope includes   │
       │                    │   "bulk_disburse"  │
       │                    │ - IP matches       │
       │                    │                    │
       │ 6. API Response    │                    │
       │◀───────────────────────────────────────│
```

**Token Service Implementation:**

```python
class TokenService:
    def create_client_credentials_token(self, client_id, client_secret, scope, request_ip):
        # 1. Validate client
        client = db.query("SELECT * FROM api_clients WHERE client_id = %s", client_id)
        if not client:
            raise AuthError("Invalid client_id")

        if not bcrypt.checkpw(client_secret.encode(), client.secret_hash.encode()):
            raise AuthError("Invalid client_secret")

        # 2. IP whitelist check
        if client.allowed_ips and request_ip not in client.allowed_ips:
            raise AuthError(f"IP {request_ip} not authorized for this client")

        # 3. Scope validation
        requested_scopes = set(scope.split(" "))
        allowed_scopes = set(client.allowed_scopes)
        if not requested_scopes.issubset(allowed_scopes):
            denied = requested_scopes - allowed_scopes
            raise AuthError(f"Scope not allowed: {denied}")

        # 4. Generate token
        token = jwt.encode({
            "sub": client_id,
            "type": "CLIENT_CREDENTIALS",
            "scope": scope,
            "iat": datetime.utcnow(),
            "exp": datetime.utcnow() + timedelta(hours=1)
        }, private_key, algorithm="RS256")

        # 5. Log token creation
        db.insert("api_tokens", {
            "client_id": client_id,
            "scope": scope,
            "ip": request_ip,
            "created_at": datetime.utcnow()
        })

        return {
            "access_token": token,
            "token_type": "Bearer",
            "expires_in": 3600,
            "scope": scope
        }
```

**Corporate API Usage Example:**

```bash
# Step 1: Get access token
curl -X POST https://api.fdbpay.com.mm/v1/oauth/token \
  -H "Content-Type: application/json" \
  -d '{
    "grant_type": "client_credentials",
    "client_id": "corp_myabank_001",
    "client_secret": "sk_live_abc123xyz789...",
    "scope": "bulk_disburse reconcile"
  }'

# Response:
# { "access_token": "eyJhbG...", "expires_in": 3600 }

# Step 2: Use token for bulk disbursement
curl -X POST https://api.fdbpay.com.mm/v1/corp/bulk-disburse \
  -H "Authorization: Bearer eyJhbG..." \
  -H "Content-Type: application/json" \
  -d '{
    "disbursements": [
      {"phone": "+959123456789", "amount": 500000, "reference": "SALARY-JUL"},
      {"phone": "+959987654321", "amount": 750000, "reference": "SALARY-JUL"}
    ]
  }'
```

---

### Q6.7: How do you handle session management across multiple devices? What happens when a user logs in on a new device?

**Answer:**

**Multi-Device Session Architecture:**

```
┌──────────────────────────────────────────────────────────────────┐
│              MULTI-DEVICE SESSION MANAGEMENT                     │
│                                                                  │
│  Redis Structure:                                                │
│  session:{user_id}:{device_id} -> {                             │
│    "access_token": "eyJ...",                                     │
│    "refresh_token": "eyJ...",                                    │
│    "device_info": {                                              │
│      "fingerprint": "abc123...",                                 │
│      "name": "Samsung Galaxy A12",                               │
│      "os": "Android 12",                                         │
│      "ip": "103.25.xx.xx"                                        │
│    },                                                            │
│    "created_at": "2026-07-27T10:30:00Z",                        │
│    "last_active": "2026-07-27T10:45:00Z",                       │
│    "expires_at": "2026-08-03T10:30:00Z"                         │
│  }                                                               │
│                                                                  │
│  Limits: Max 3 devices per user                                  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Device Registration Flow:**

```python
class DeviceManager:
    MAX_DEVICES = 3

    def register_device(self, user_id, device_fingerprint, device_name, device_os):
        # 1. Count existing devices
        existing_devices = redis.keys(f"session:{user_id}:*")

        if len(existing_devices) >= self.MAX_DEVICES:
            # 2a. Find least recently active device
            oldest_device = self.find_oldest_device(user_id)

            # 2b. Notify user about removal
            sms_service.send(user.device_phone,
                f"FDB Pay: New login detected on {device_name}. "
                f"Old device {oldest_device.name} has been removed. "
                f"If this wasn't you, call 01-XXXXXXX immediately.")

            # 2c. Remove oldest device session
            redis.delete(f"session:{user_id}:{oldest_device.id}")

        # 3. Create new device session
        device_id = str(uuid.uuid4())
        session_key = f"session:{user_id}:{device_id}"

        redis.set(session_key, json.dumps({
            "device_fingerprint": device_fingerprint,
            "device_name": device_name,
            "device_os": device_os,
            "created_at": datetime.utcnow().isoformat(),
            "last_active": datetime.utcnow().isoformat(),
            "expires_at": (datetime.utcnow() + timedelta(days=7)).isoformat()
        }), ex=604800)  # 7 days

        # 4. Log audit
        db.insert("audit_log", {
            "actor_id": user_id,
            "action": "DEVICE_REGISTERED",
            "resource_type": "DEVICE",
            "resource_id": device_id,
            "details": {"device_name": device_name, "device_os": device_os}
        })

        return Success(device_id=device_id)

    def find_oldest_device(self, user_id):
        devices = redis.keys(f"session:{user_id}:*")
        oldest = None
        for device_key in devices:
            device = json.loads(redis.get(device_key))
            if not oldest or device['last_active'] < oldest['last_active']:
                oldest = device
        return oldest
```

**What Happens on New Device Login:**

```
Scenario: User has 3 devices registered. Logs in on a 4th device.

1. User enters phone + PIN on new device

2. Auth Service validates credentials:
   - Phone exists? Yes
   - PIN correct? Yes
   - Account not frozen? Yes

3. Device Manager checks existing devices:
   - Count: 3 devices (max = 3)
   - Need to remove oldest

4. System actions:
   a. Remove oldest device session from Redis
   b. Create new device session in Redis
   c. Send SMS to user's phone:
      "FDB Pay: New login on iPhone 14. Old device 'Samsung Galaxy A12' removed.
       If this wasn't you, call 01-XXXXXXX."
   d. Send push notification to all remaining devices:
      "New login detected on iPhone 14. Tap to review devices."

5. User receives new JWT tokens
6. New device is now active
```

**Session Revocation (User-Initiated):**

```python
# User taps "Log out all devices" in app
def logout_all_devices(user_id):
    # Delete all device sessions
    devices = redis.keys(f"session:{user_id}:*")
    for device_key in devices:
        redis.delete(device_key)

    # Log audit
    db.insert("audit_log", {
        "actor_id": user_id,
        "action": "ALL_SESSIONS_REVOKED",
        "resource_type": "USER",
        "resource_id": user_id
    })

    # Notify user
    sms_service.send(user.phone, "FDB Pay: All devices logged out successfully.")

    return Success("All sessions revoked")
```

**Real-World Scenario:**
A merchant in Yangon has:
- **Device 1:** Personal phone (Samsung) — logged in for 5 days
- **Device 2:** Work phone (iPhone) — logged in for 3 days
- **Device 3:** Tablet at shop — logged in for 1 day

Merchant buys a new phone (4th device). When they log in:
1. The tablet (oldest active device) is automatically logged out
2. Merchant receives SMS: "New login on new device. Old device 'Tablet' removed."
3. If the merchant didn't do this (someone stole their credentials), they call the hotline immediately
4. Support team freezes the account, investigates the IP/device of the unauthorized login

---

### Q6.8: How would you handle SSO (Single Sign-On) for FDB Pay's admin portal and internal tools?

**Answer:**

**SSO Architecture for FDB Bank Employees:**

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  FDB Bank    │     │  FDB Pay     │     │  FDB Pay     │
│  Employee    │     │  Identity    │     │  Admin       │
│  (Browser)   │     │  Provider    │     │  Portal      │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       │ 1. Access admin.fdbpay.com.mm          │
       │───────────────────────────────────────▶│
       │                    │                    │
       │ 2. Redirect to IdP                     │
       │    (SAML/OIDC)                         │
       │◀───────────────────────────────────────│
       │                    │                    │
       │ 3. Employee authenticates              │
       │    via FDB Bank SSO (Active Directory) │
       │───────────────────▶│                    │
       │                    │                    │
       │ 4. SAML assertion / OIDC token         │
       │    with employee role & permissions    │
       │◀───────────────────│                    │
       │                    │                    │
       │ 5. Redirect back to admin portal       │
       │    with SAML/OIDC token               │
       │───────────────────────────────────────▶│
       │                    │                    │
       │                    │ 6. Validate token │
       │                    │    Map to FDB Pay │
       │                    │    admin role     │
       │                    │                    │
       │ 7. Admin session created               │
       │    (JWT issued)                        │
       │◀───────────────────────────────────────│
```

**Employee-to-Role Mapping:**

```python
# SAML/OIDC attribute mapping
SSO_ROLE_MAPPING = {
    "FDB_IT_ADMIN": "SUPER_ADMIN",
    "FDB_COMPLIANCE_OFFICER": "ADMIN",  # Limited to AML/KYC functions
    "FDB_OPERATIONS": "ADMIN",          # Limited to operations functions
    "FDB_MERCHANT_OPS": "ADMIN",        # Limited to merchant management
    "FDB_CUSTOMER_SUPPORT": "ADMIN",    # Limited to dispute resolution
    "FDB_FINANCE": "ADMIN",             # Limited to reporting/settlement
}

def process_sso_callback(saml_assertion):
    employee_id = saml_assertion['employee_id']
    employee_name = saml_assertion['name']
    sso_role = saml_assertion['role']
    department = saml_assertion['department']

    # Map SSO role to FDB Pay admin role
    fdb_role = SSO_ROLE_MAPPING.get(sso_role)
    if not fdb_role:
        raise AuthError(f"Unauthorized role: {sso_role}")

    # Map department to permissions
    permissions = DEPARTMENT_PERMISSIONS.get(department, [])

    # Create admin user if not exists
    admin = db.upsert("admin_users", {
        "employee_id": employee_id,
        "name": employee_name,
        "sso_role": sso_role,
        "fdb_role": fdb_role,
        "permissions": permissions,
        "last_sso_login": datetime.utcnow()
    })

    # Issue FDB Pay JWT
    token = jwt.encode({
        "sub": admin.id,
        "type": "ADMIN",
        "role": fdb_role,
        "permissions": permissions,
        "employee_id": employee_id,
        "iat": datetime.utcnow(),
        "exp": datetime.utcnow() + timedelta(hours=8)  # Work day
    }, private_key, algorithm="RS256")

    return Success(token=token)
```

**Multi-Factor Authentication for Admin:**

```
┌──────────────────────────────────────────────────────────────────┐
│              ADMIN MFA FLOW                                       │
│                                                                  │
│  Step 1: Employee SSO login (username + password + AD token)    │
│                                                                  │
│  Step 2: FDB Pay requires additional MFA for admin actions:     │
│          - Transaction monitoring dashboard                      │
│          - Account suspension                                    │
│          - KYC approval                                          │
│          - System configuration changes                          │
│                                                                  │
│  Step 3: MFA options:                                            │
│          a. TOTP (Google Authenticator / Authy)                  │
│          b. Hardware token (YubiKey)                             │
│          c. SMS OTP (fallback only)                              │
│                                                                  │
│  Step 4: Admin enters MFA code                                   │
│                                                                  │
│  Step 5: Access granted for this session                         │
│          (re-authentication required for sensitive operations)   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

```python
# MFA middleware for sensitive admin operations
def require_mfa(operation):
    def middleware(request):
        admin = request.admin

        # Check if MFA is required for this operation
        if operation in SENSITIVE_OPERATIONS:
            mfa_verified = redis.get(f"mfa_verified:{admin.id}")
            if not mfa_verified:
                return Error(403, "MFA verification required for this operation")

        return next(request)

    return middleware

SENSITIVE_OPERATIONS = [
    "admin.users.suspend",
    "admin.merchants.approve",
    "admin.kyc.review",
    "admin.config.update",
    "admin.aml.action",
    "admin.disputes.resolve",
]

# MFA verification endpoint
def verify_mfa(admin_id, mfa_code, mfa_type):
    if mfa_type == "TOTP":
        totp_secret = db.query("SELECT totp_secret FROM admin_users WHERE id = %s", admin_id)
        if pyotp.TOTP(totp_secret).verify(mfa_code):
            redis.set(f"mfa_verified:{admin_id}", "true", ex=3600)  # 1 hour
            return Success("MFA verified")
    elif mfa_type == "SMS":
        # Send OTP and verify (same as user OTP flow)
        pass
    elif mfa_type == "HARDWARE":
        # YubiKey validation
        pass

    return Error("Invalid MFA code")
```

---

### Q6.9: How do you secure inter-service authentication within FDB Pay's microservices architecture?

**Answer:**

**Internal Service Authentication:**

```
┌──────────────────────────────────────────────────────────────────┐
│              INTER-SERVICE AUTHENTICATION                         │
│                                                                  │
│  Method: mTLS (mutual TLS) + Service JWT                        │
│                                                                  │
│  ┌──────────┐    mTLS    ┌──────────┐    mTLS    ┌──────────┐  │
│  │ Transfer │◄──────────▶│  Wallet  │◄──────────▶│ Merchant │  │
│  │ Service  │            │ Service  │            │ Service  │  │
│  └──────────┘            └──────────┘            └──────────┘  │
│       │                       │                       │         │
│       │    Service JWT        │    Service JWT        │         │
│       │    (signed by         │    (signed by         │         │
│       │     internal CA)      │     internal CA)      │         │
│       │                       │                       │         │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Kubernetes Service Mesh (Istio)              │   │
│  │  - Automatic mTLS between all services                   │   │
│  │  - Certificate rotation every 24 hours                   │   │
│  │  - Service identity verified via SPIFFE                  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Service-to-Service Call:**

```python
class InternalAuthMiddleware:
    def __init__(self):
        self.ca_cert = load_certificate("/certs/internal-ca.pem")

    def validate_service_request(self, request):
        # 1. Validate mTLS certificate
        client_cert = request.headers.get("x-forwarded-client-cert")
        if not verify_certificate(client_cert, self.ca_cert):
            return Error(403, "Invalid service certificate")

        # 2. Extract service identity
        service_id = extract_service_id(client_cert)

        # 3. Validate service is authorized for this operation
        if not is_service_authorized(service_id, request.path):
            return Error(403, f"Service {service_id} not authorized for {request.path}")

        # 4. Validate request freshness (prevent replay)
        timestamp = request.headers.get("x-request-timestamp")
        if abs(datetime.utcnow() - parse_timestamp(timestamp)) > timedelta(seconds=30):
            return Error(401, "Request expired")

        return next(request)

# Service authorization matrix
SERVICE_PERMISSIONS = {
    "transfer-service": [
        "wallet.debit",
        "wallet.credit",
        "wallet.balance.read",
        "merchant.lookup",
        "fraud.check",
        "notification.send"
    ],
    "merchant-service": [
        "wallet.balance.read",
        "merchant.lookup",
        "merchant.update",
        "settlement.create"
    ],
    "notification-service": [
        "notification.send",
        "user.lookup",
        "merchant.lookup"
    ],
    "settlement-service": [
        "merchant.lookup",
        "transaction.aggregate",
        "settlement.create",
        "cbs.credit"
    ]
}
```

**Why Not Just Use JWT from External Clients?**

| Concern | External JWT | Internal Service Auth |
|---------|-------------|---------------------|
| **Who issues** | FDB Pay Auth Service | Kubernetes / Istio (infrastructure) |
| **Scope** | User-level permissions | Service-level permissions |
| **Lifetime** | 15 minutes | 24 hours (auto-rotated) |
| **Validation** | API Gateway validates | Each service validates via mTLS |
| **Network** | External (internet) | Internal (VPC, K8s network) |

**Real-World Scenario:**
The Transfer Service calls the Wallet Service to debit a user. The request includes:
1. **mTLS certificate** proving it's the Transfer Service (not a rogue pod)
2. **x-request-timestamp** to prevent replay attacks
3. **x-correlation-id** for distributed tracing

The Wallet Service validates the certificate, checks that Transfer Service is authorized for `wallet.debit`, processes the debit, and returns the result. No user JWT is needed for this internal call — the service identity is sufficient.

---

### Q6.10: How would you handle API key management for third-party developers who want to integrate with FDB Pay?

**Answer:**

**API Key Lifecycle:**

```
┌──────────────────────────────────────────────────────────────────┐
│              API KEY MANAGEMENT                                   │
│                                                                  │
│  1. Application                                                  │
│     Developer applies via developer.fdbpay.com.mm               │
│     Provides: business info, use case, expected volume           │
│                                                                  │
│  2. Approval                                                     │
│     FDB Pay team reviews application                             │
│     Approves/denies with specific scope limitations              │
│                                                                  │
│  3. Key Generation                                               │
│     System generates:                                             │
│     - api_key: "fak_live_abc123..." (public, can be in code)    │
│     - api_secret: "sk_live_xyz789..." (private, never in code)  │
│     - HMAC signing key for request signing                       │
│                                                                  │
│  4. Sandbox Access                                               │
│     Developer gets sandbox keys first:                           │
│     - api_key: "fak_test_def456..."                             │
│     - api_secret: "sk_test_uvw012..."                           │
│     - Test against sandbox environment                           │
│                                                                  │
│  5. Production Access                                            │
│     After sandbox testing + compliance review:                   │
│     - Production keys issued                                     │
│     - IP whitelist configured                                    │
│     - Rate limits set per agreement                              │
│                                                                  │
│  6. Key Rotation                                                 │
│     - Keys expire after 90 days (configurable)                  │
│     - Developer notified 14 days before expiry                  │
│     - Old key works for 7 days after new key created            │
│     - Emergency rotation: immediate, old key invalidated        │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

**Request Signing (HMAC):**

```python
# Developer signs requests
import hmac
import hashlib
import time

def sign_request(api_secret, method, path, body, timestamp):
    # Create signature payload
    payload = f"{method}\n{path}\n{body}\n{timestamp}"
    signature = hmac.new(
        api_secret.encode(),
        payload.encode(),
        hashlib.sha256
    ).hexdigest()
    return signature

# Developer's HTTP request:
# POST https://api.fdbpay.com.mm/v1/transfer
# Headers:
#   X-API-Key: fak_live_abc123...
#   X-Timestamp: 1690444200
#   X-Signature: a1b2c3d4e5f6...
#   Content-Type: application/json
# Body: { "recipient": "+95987654321", "amount": 50000 }
```

**API Gateway Validation:**

```python
def validate_api_key(request):
    api_key = request.headers.get("X-API-Key")
    timestamp = request.headers.get("X-Timestamp")
    signature = request.headers.get("X-Signature")

    # 1. Look up API key
    key_record = db.query("SELECT * FROM api_keys WHERE key = %s AND status = 'ACTIVE'", api_key)
    if not key_record:
        return Error(401, "Invalid API key")

    # 2. Check expiry
    if key_record.expires_at < datetime.utcnow():
        return Error(401, "API key expired. Please rotate your key.")

    # 3. Validate timestamp (prevent replay)
    if abs(datetime.utcnow() - datetime.utcfromtimestamp(int(timestamp))) > timedelta(minutes=5):
        return Error(401, "Request timestamp expired")

    # 4. Verify HMAC signature
    expected_signature = hmac.new(
        key_record.secret_hash.encode(),
        f"{request.method}\n{request.path}\n{request.body}\n{timestamp}".encode(),
        hashlib.sha256
    ).hexdigest()

    if not hmac.compare_digest(signature, expected_signature):
        return Error(401, "Invalid signature")

    # 5. Check rate limit
    rate_key = f"api_rl:{api_key}:{int(time.time()) // 60}"
    count = redis.incr(rate_key)
    redis.expire(rate_key, 60)
    if count > key_record.rate_limit:
        return Error(429, "Rate limit exceeded")

    # 6. Check scope
    if request.path not in key_record.allowed_endpoints:
        return Error(403, "Endpoint not allowed for this API key")

    return next(request)
```

---

*This section covers the core authentication and authorization patterns for FDB Pay, from consumer mobile app login to enterprise API access, with specific attention to Myanmar's unique requirements (shared devices, SIM swaps, low connectivity, multi-channel access).*


### Q7.1: How would you design the fraud detection system for FDB Pay?

**Answer:**

**Multi-Layer Fraud Architecture:**

```
Layer 1: Pre-Authorization (Real-Time, < 50ms)
  - Velocity checks (Redis counters)
  - Device fingerprint matching
  - Geo-location anomaly
  - Amount limits per KYC tier

Layer 2: Real-Time ML (Real-Time, < 100ms)
  - Anomaly detection model
  - Transaction pattern analysis
  - Network graph analysis

Layer 3: Post-Authorization (Batch, hourly)
  - Behavioral analysis
  - Cross-account pattern detection
  - Velocity aggregation across time windows

Layer 4: Compliance (Continuous)
  - Sanctions screening
  - PEP checks
  - STR generation
```

**Velocity Check Implementation:**

```python
def check_velocity(user_id, amount):
    # Check multiple time windows
    windows = [
        ('1min', 60, 5),       # Max 5 transactions per minute
        ('1hour', 3600, 20),    # Max 20 per hour
        ('1day', 86400, 50),    # Max 50 per day
    ]

    for window_name, seconds, max_count in windows:
        key = f"velocity:{user_id}:{window_name}"
        current = redis.incr(key)
        redis.expire(key, seconds)

        if current > max_count:
            # Flag for review, potentially block
            fraud_service.flag(user_id, f"Velocity exceeded: {current} in {window_name}")
            return BLOCKED

    # Check cumulative amount
    daily_amount_key = f"daily_amount:{user_id}"
    daily_total = redis.incrby(daily_amount_key, amount)
    redis.expire(daily_amount_key, 86400)

    if daily_total > get_daily_limit(user_id):
        return STEP_UP_AUTH  # Require additional verification

    return ALLOWED
```

**ML Model Features:**

| Feature | Description | Example |
|---------|-------------|---------|
| Transaction amount deviation | How different from user's average | User avg MMK 10K, current MMK 500K (50x) |
| Time-of-day pattern | Unusual hour for this user | User typically transacts 8am-8pm, current: 3am |
| Device change | New device for this user | User has used 2 devices, now using 3rd |
| Recipient pattern | Sending to new recipients | User has 10 contacts, now sending to 11th new one |
| Geographic anomaly | Transaction from unusual location | User in Yangon, transaction from Mandalay |
| Velocity anomaly | Burst of transactions | 10 transactions in 5 minutes (normal: 2/day) |

---

### Q7.2: How would you implement the tiered KYC system for FDB Pay?

**Answer:**

**KYC Tier Architecture:**

```
┌────────────────────────────────────────────────────────────────┐
│                     KYC TIER SYSTEM                            │
│                                                                │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                 │
│  │   BASIC  │───▶│ ENHANCED │───▶│   FULL   │                 │
│  │          │    │          │    │          │                 │
│  │ Phone+OTP│    │ NRC+Photo│    │ In-branch│                 │
│  │ MMK 500K │    │ MMK 5M   │    │ MMK 50M  │                 │
│  │ daily    │    │ daily    │    │ daily    │                 │
│  └──────────┘    └──────────┘    └──────────┘                 │
└────────────────────────────────────────────────────────────────┘
```

**BASIC Tier (Phone + OTP):**
```python
def register_basic(phone, otp):
    # 1. Verify OTP
    if not verify_otp(phone, otp):
        return Error("Invalid OTP")

    # 2. Check if phone already registered
    existing_user = db.query("SELECT id FROM users WHERE phone = %s", phone)
    if existing_user:
        return Error("Phone already registered")

    # 3. Create user
    user = db.insert("users", {
        "phone": phone,
        "status": "ACTIVE",
        "kyc_tier": "NONE"
    })

    # 4. Create wallet with basic limits
    wallet = db.insert("wallets", {
        "user_id": user.id,
        "daily_limit": 500000,    # MMK 500,000
        "monthly_limit": 5000000, # MMK 5,000,000
        "kyc_tier": "BASIC"
    })

    return Success(user, wallet)
```

**ENHANCED Tier (NRC + Photo):**
```python
def upgrade_to_enhanced(user_id, nrc_front, nrc_back, selfie):
    # 1. Upload documents to S3
    nrc_front_url = s3.upload(nrc_front, f"kyc/{user_id}/nrc_front.jpg")
    nrc_back_url = s3.upload(nrc_back, f"kyc/{user_id}/nrc_back.jpg")
    selfie_url = s3.upload(selfie, f"kyc/{user_id}/selfie.jpg")

    # 2. Create KYC document record
    db.insert("kyc_documents", {
        "user_id": user_id,
        "tier": "ENHANCED",
        "documents": [
            {"type": "NRC_FRONT", "file_url": nrc_front_url},
            {"type": "NRC_BACK", "file_url": nrc_back_url},
            {"type": "SELFIE", "file_url": selfie_url}
        ],
        "status": "PENDING"
    })

    # 3. Notify compliance team for review
    kafka_publish("kyc.submitted", {"user_id": user_id, "tier": "ENHANCED"})

    # 4. Update user status
    db.update("users", user_id, {"status": "KYC_PENDING"})

    return Success("KYC documents submitted for review")
```

**FULL Tier (In-branch):**
```python
def upgrade_to_full(user_id, branch_id, verifier_id):
    # 1. Verify in-person KYC completed
    if not verify_branch_visit(user_id, branch_id):
        return Error("Branch verification not found")

    # 2. Update KYC tier
    db.update("users", user_id, {"kyc_tier": "FULL", "status": "VERIFIED"})
    db.update("wallets", {"user_id": user_id}, {
        "kyc_tier": "FULL",
        "daily_limit": 50000000,    # MMK 50,000,000
        "monthly_limit": 500000000  # MMK 500,000,000
    })

    # 3. Log audit trail
    db.insert("audit_log", {
        "actor_id": verifier_id,
        "action": "KYC_UPGRADE_FULL",
        "resource_type": "USER",
        "resource_id": user_id
    })

    return Success("KYC upgraded to FULL tier")
```

---

## 7. Security & Fraud

---

### Q7.1: How would you design the fraud detection system for FDB Pay?

**Answer:**

**Multi-Layer Fraud Architecture:**

```
Layer 1: Pre-Authorization (Real-Time, < 50ms)
  - Velocity checks (Redis counters)
  - Device fingerprint matching
  - Geo-location anomaly
  - Amount limits per KYC tier

Layer 2: Real-Time ML (Real-Time, < 100ms)
  - Anomaly detection model
  - Transaction pattern analysis
  - Network graph analysis

Layer 3: Post-Authorization (Batch, hourly)
  - Behavioral analysis
  - Cross-account pattern detection
  - Velocity aggregation across time windows

Layer 4: Compliance (Continuous)
  - Sanctions screening
  - PEP checks
  - STR generation
```

**Velocity Check Implementation:**

```python
def check_velocity(user_id, amount):
    # Check multiple time windows
    windows = [
        ('1min', 60, 5),       # Max 5 transactions per minute
        ('1hour', 3600, 20),    # Max 20 per hour
        ('1day', 86400, 50),    # Max 50 per day
    ]

    for window_name, seconds, max_count in windows:
        key = f"velocity:{user_id}:{window_name}"
        current = redis.incr(key)
        redis.expire(key, seconds)

        if current > max_count:
            fraud_service.flag(user_id, f"Velocity exceeded: {current} in {window_name}")
            return BLOCKED

    # Check cumulative amount
    daily_amount_key = f"daily_amount:{user_id}"
    daily_total = redis.incrby(daily_amount_key, amount)
    redis.expire(daily_amount_key, 86400)

    if daily_total > get_daily_limit(user_id):
        return STEP_UP_AUTH

    return ALLOWED
```

**ML Model Features:**

| Feature | Description | Example |
|---------|-------------|---------|
| Transaction amount deviation | How different from user's average | User avg MMK 10K, current MMK 500K (50x) |
| Time-of-day pattern | Unusual hour for this user | User typically transacts 8am-8pm, current: 3am |
| Device change | New device for this user | User has used 2 devices, now using 3rd |
| Recipient pattern | Sending to new recipients | User has 10 contacts, now sending to 11th new one |
| Geographic anomaly | Transaction from unusual location | User in Yangon, transaction from Mandalay |
| Velocity anomaly | Burst of transactions | 10 transactions in 5 minutes (normal: 2/day) |

---

### Q7.2: How would you implement the tiered KYC system for FDB Pay?

**Answer:**

**KYC Tier Architecture:**

```
BASIC (Phone + OTP):   MMK 500K daily limit, MMK 5M monthly
ENHANCED (NRC + Photo): MMK 5M daily limit, MMK 50M monthly
FULL (In-branch):       MMK 50M daily limit, MMK 500M monthly
```

**BASIC Tier (Phone + OTP):**

```python
def register_basic(phone, otp):
    if not verify_otp(phone, otp):
        return Error("Invalid OTP")

    existing_user = db.query("SELECT id FROM users WHERE phone = %s", phone)
    if existing_user:
        return Error("Phone already registered")

    user = db.insert("users", {
        "phone": phone,
        "status": "ACTIVE",
        "kyc_tier": "NONE"
    })

    wallet = db.insert("wallets", {
        "user_id": user.id,
        "daily_limit": 500000,
        "monthly_limit": 5000000,
        "kyc_tier": "BASIC"
    })

    return Success(user, wallet)
```

**ENHANCED Tier (NRC + Photo):**

```python
def upgrade_to_enhanced(user_id, nrc_front, nrc_back, selfie):
    nrc_front_url = s3.upload(nrc_front, f"kyc/{user_id}/nrc_front.jpg")
    nrc_back_url = s3.upload(nrc_back, f"kyc/{user_id}/nrc_back.jpg")
    selfie_url = s3.upload(selfie, f"kyc/{user_id}/selfie.jpg")

    db.insert("kyc_documents", {
        "user_id": user_id,
        "tier": "ENHANCED",
        "documents": [
            {"type": "NRC_FRONT", "file_url": nrc_front_url},
            {"type": "NRC_BACK", "file_url": nrc_back_url},
            {"type": "SELFIE", "file_url": selfie_url}
        ],
        "status": "PENDING"
    })

    kafka_publish("kyc.submitted", {"user_id": user_id, "tier": "ENHANCED"})
    db.update("users", user_id, {"status": "KYC_PENDING"})

    return Success("KYC documents submitted for review")
```

**FULL Tier (In-branch):**

```python
def upgrade_to_full(user_id, branch_id, verifier_id):
    if not verify_branch_visit(user_id, branch_id):
        return Error("Branch verification not found")

    db.update("users", user_id, {"kyc_tier": "FULL", "status": "VERIFIED"})
    db.update("wallets", {"user_id": user_id}, {
        "kyc_tier": "FULL",
        "daily_limit": 50000000,
        "monthly_limit": 500000000
    })

    db.insert("audit_log", {
        "actor_id": verifier_id,
        "action": "KYC_UPGRADE_FULL",
        "resource_type": "USER",
        "resource_id": user_id
    })

    return Success("KYC upgraded to FULL tier")
```

---

## 8. API Design

---

### Q8.1: How would you design the API for FDB Pay's P2P transfer?

**Answer:**

**Endpoint:** `POST /v1/transfer`

**Request:**
```json
{
    "idempotency_key": "fdb-1690444200000-a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "recipient": "+95987654321",
    "amount": 50000,
    "currency": "MMK",
    "description": "Dinner split",
    "pin": "1234"
}
```

**Response (Success):**
```json
{
    "success": true,
    "data": {
        "transaction_id": "txn-20260727-abc123",
        "status": "COMPLETED",
        "sender": {
            "phone": "+95912345678",
            "name": "Aung Aung",
            "balance_after": 450000
        },
        "recipient": {
            "phone": "+95987654321",
            "name": "Su Su"
        },
        "amount": 50000,
        "fee": 0,
        "total_debited": 50000,
        "completed_at": "2026-07-27T10:30:15Z"
    },
    "meta": {
        "request_id": "req_abc123",
        "timestamp": "2026-07-27T10:30:15Z"
    }
}
```

**Response (Error - Insufficient Balance):**
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

**Validation Rules:**
```python
def validate_transfer_request(request):
    errors = []

    # Amount validation
    if request.amount <= 0:
        errors.append("Amount must be positive")
    if request.amount > 2000000:  # Per-txn limit for basic KYC
        errors.append("Amount exceeds per-transaction limit")

    # Recipient validation
    if not is_valid_myanmar_phone(request.recipient):
        errors.append("Invalid phone number")

    # Idempotency key
    if not request.idempotency_key:
        errors.append("Idempotency key required")

    # PIN
    if not request.pin or len(request.pin) != 4:
        errors.append("Invalid PIN")

    return errors
```

---

## 9. Real-Time & Async Processing

---

### Q9.1: How would you implement real-time transaction monitoring for FDB Pay?

**Answer:**

**Architecture:**

```
Kafka (txn.events) --> Flink (stream processing) --> Alerts Dashboard
                       |
                       +--> Anomaly Detection
                       +--> Velocity Aggregation
                       +--> Pattern Matching
```

**Flink Job for Real-Time Monitoring:**

```sql
-- Flink SQL: Detect suspicious velocity
SELECT
    user_id,
    COUNT(*) as txn_count,
    SUM(amount) as total_amount,
    TUMBLE_START(event_time, INTERVAL '5' MINUTE) as window_start
FROM transactions_stream
WHERE status = 'COMPLETED'
GROUP BY user_id, TUMBLE(event_time, INTERVAL '5' MINUTE)
HAVING COUNT(*) > 10 OR SUM(amount) > 1000000;
```

**Alert Triggers:**

| Condition | Alert Level | Action |
|-----------|-------------|--------|
| > 10 transactions in 5 minutes | WARNING | Flag for review |
| > MMK 1M in 5 minutes | CRITICAL | Block + alert compliance |
| Transaction from new device + high amount | WARNING | Step-up auth required |
| Transaction to sanctioned entity | CRITICAL | Block + report to FIU |
| Multiple accounts same device | CRITICAL | Freeze all accounts |

---

## 10. Settlement & Reconciliation

---

### Q10.1: How does the merchant settlement process work end-to-end?

**Answer:**

**Settlement Flow:**

```
14:00 MM Time (Daily)
    |
    v
[1] Scheduler triggers settlement job
    |
    v
[2] Settlement Service queries transactions:
    SELECT merchant_id, SUM(amount), SUM(fee)
    FROM transactions
    WHERE type = 'QR_MERCHANT' AND status = 'COMPLETED'
      AND created_at >= '2026-07-26 00:00' AND created_at < '2026-07-27 00:00'
    GROUP BY merchant_id
    |
    v
[3] For each merchant:
    - gross = SUM(amount)
    - fees = SUM(fee)
    - net = gross - fees
    |
    v
[4] Generate settlement file:
    merchant_id | account_number | net_amount | settlement_date
    M001       | 1234567890     | 375000     | 2026-07-26
    M002       | 0987654321     | 125000     | 2026-07-26
    |
    v
[5] Submit to CBS for bulk credit:
    CBS API: POST /api/v1/bulk-credit
    Body: { settlements: [...] }
    |
    v
[6] Record settlement status:
    INSERT INTO settlements (merchant_id, gross_amount, fees, net_amount, status)
    VALUES ...
    |
    v
[7] Notify merchants via Kafka event:
    topic: settlement.completed
    |
    v
[8] Merchant receives SMS/push:
    "Your settlement of MMK 375,000 has been credited to account 1234567890"
```

**Reconciliation:**

```sql
-- Daily reconciliation: Check if all completed transactions are settled
SELECT t.merchant_id, t.total_txn_amount, COALESCE(s.settled_amount, 0) as settled
FROM (
    SELECT metadata->>'merchant_id' as merchant_id, SUM(amount) as total_txn_amount
    FROM transactions
    WHERE type = 'QR_MERCHANT' AND status = 'COMPLETED'
      AND created_at >= '2026-07-26' AND created_at < '2026-07-27'
    GROUP BY metadata->>'merchant_id'
) t
LEFT JOIN settlements s ON t.merchant_id = s.merchant_id
WHERE t.total_txn_amount != COALESCE(s.settled_amount, 0);
-- If any rows returned -> discrepancy, alert operations team
```

---

## 11. Mobile & Offline Considerations

---

### Q11.1: How would you handle USSD-based transactions for feature phone users in Myanmar?

**Answer:**

**USSD Architecture:**

```
Feature Phone --USSD--> USSD Gateway --SMPP--> USSD Service --API--> FDB Pay Services
```

**USSD Session Flow:**

```
User dials *123# (FDB Pay USSD code)

Session 1: Main Menu
  "Welcome to FDB Pay"
  "1. Check Balance"
  "2. Send Money"
  "3. Pay Bill"
  "4. Top Up"
  "5. Mini Statement"
  User selects: 2

Session 2: Send Money
  "Enter recipient phone number:"
  User enters: 0987654321

Session 3: Amount
  "Enter amount (MMK):"
  User enters: 50000

Session 4: Confirm
  "Send MMK 50,000 to 0987654321?"
  "1. Confirm"
  "2. Cancel"
  User selects: 1

Session 5: PIN
  "Enter your PIN:"
  User enters: ****

Session 6: Result
  "Transfer successful!"
  "Ref: TXN-20260727-ABC"
  "New balance: MMK 450,000"
```

**USSD Service Implementation:**

```python
class USSDSession:
    def __init__(self, session_id, phone):
        self.session_id = session_id
        self.phone = phone
        self.state = "MAIN_MENU"
        self.data = {}

    def handle_input(self, user_input):
        if self.state == "MAIN_MENU":
            if user_input == "2":
                self.state = "SEND_MONEY_RECIPIENT"
                return "Enter recipient phone number:"
            # ... other menu options

        elif self.state == "SEND_MONEY_RECIPIENT":
            self.data['recipient'] = user_input
            self.state = "SEND_MONEY_AMOUNT"
            return "Enter amount (MMK):"

        elif self.state == "SEND_MONEY_AMOUNT":
            self.data['amount'] = int(user_input)
            self.state = "SEND_MONEY_CONFIRM"
            return f"Send MMK {user_input} to {self.data['recipient']}?\n1. Confirm\n2. Cancel"

        elif self.state == "SEND_MONEY_CONFIRM":
            if user_input == "1":
                self.state = "SEND_MONEY_PIN"
                return "Enter your PIN:"
            else:
                return "Transaction cancelled."

        elif self.state == "SEND_MONEY_PIN":
            # Process the transfer
            result = transfer_service.process(
                sender_phone=self.phone,
                recipient=self.data['recipient'],
                amount=self.data['amount'],
                pin=user_input
            )
            if result.success:
                return f"Transfer successful! Ref: {result.txn_id}\nNew balance: MMK {result.balance}"
            else:
                return f"Transfer failed: {result.error}"
```

**Offline Considerations:**

| Scenario | USSD Handling |
|----------|---------------|
| Network timeout | Session state preserved for 5 minutes |
| User drops mid-session | Session expires after 3 minutes |
| USSD gateway down | SMS fallback: "Dial *123# to access FDB Pay" |
| Transaction pending | "Your transaction is being processed. You will receive SMS confirmation." |

---

## 12. Compliance & Regulatory

---

### Q12.1: How would you implement AML/CFT compliance for FDB Pay?

**Answer:**

**AML/CFT Architecture:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    AML/CFT COMPLIANCE ENGINE                    │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Screening   │  │   Monitoring  │  │   Reporting   │         │
│  │  Engine      │  │   Engine      │  │   Engine      │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│         │                 │                 │                   │
│         v                 v                 v                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ Sanctions    │  │ Transaction  │  │ STR Filing   │         │
│  │ Lists        │  │ Patterns     │  │ (FIU)        │         │
│  │ (UN,OFAC,EU) │  │ (Velocity,   │  │              │         │
│  │              │  │  Amount,     │  │              │         │
│  │              │  │  Geography)  │  │              │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

**Sanctions Screening:**

```python
def screen_transaction(transaction):
    # Check sender against sanctions lists
    sender = user_service.get(transaction.sender_id)
    sanctions_result = sanctions_api.check({
        "name": sender.name,
        "nrc": sender.nrc_number,
        "phone": sender.phone
    })

    if sanctions_result.matched:
        # Block transaction immediately
        return BLOCKED, "Sender matches sanctions list"

    # Check recipient
    recipient = user_service.get(transaction.recipient_id)
    sanctions_result = sanctions_api.check({
        "name": recipient.name,
        "nrc": recipient.nrc_number,
        "phone": recipient.phone
    })

    if sanctions_result.matched:
        return BLOCKED, "Recipient matches sanctions list"

    # Check PEP status
    if sender.is_pep or recipient.is_pep:
        return FLAGGED, "PEP involvement detected"

    return CLEARED
```

**Suspicious Transaction Report (STR) Generation:**

```python
def generate_str(transaction, reason):
    str_report = {
        "report_id": f"STR-{datetime.now().strftime('%Y%m%d')}-{uuid4().hex[:8]}",
        "transaction_id": transaction.id,
        "reporting_entity": "FDB Bank",
        "transaction_date": transaction.created_at,
        "amount": transaction.amount,
        "currency": transaction.currency,
        "sender": {
            "name": transaction.sender.name,
            "phone": transaction.sender.phone,
            "nrc": transaction.sender.nrc_number,
            "account": transaction.sender_wallet_id
        },
        "recipient": {
            "name": transaction.recipient.name,
            "phone": transaction.recipient.phone,
            "nrc": transaction.recipient.nrc_number,
            "account": transaction.recipient_wallet_id
        },
        "reason": reason,
        "supporting_documents": []
    }

    # Submit to FIU
    fiu_api.submit_str(str_report)

    # Log in compliance system
    db.insert("aml_reports", str_report)

    # Notify compliance officer
    kafka_publish("aml.str.filed", str_report)
```

---

## 13. Operational & Observability

---

### Q13.1: How would you monitor FDB Pay in production? What dashboards would you create?

**Answer:**

**Monitoring Stack:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY STACK                           │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Prometheus  │  │    Grafana   │  │    Jaeger     │         │
│  │  (Metrics)   │  │  (Dashboards)│  │  (Tracing)    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│         │                 │                 │                   │
│         v                 v                 v                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Alert       │  │  Kibana      │  │  PagerDuty   │         │
│  │  Manager     │  │  (Logs)      │  │  (On-call)    │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

**Key Dashboards:**

**1. Business Dashboard:**

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| Transaction Volume | Real-time transaction count | < 100/min (unusual low) |
| Transaction Value | Total MMK processed | > MMK 100M/hour (unusual high) |
| Success Rate | % of completed transactions | < 99% |
| Active Users | Current concurrent users | > 10,000 (capacity) |
| Failed Transactions | Failed txns with reasons | > 1% of total |

**2. Technical Dashboard:**

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| API Latency (p50, p99) | Response times | p99 > 2s |
| Error Rate (5xx) | Server errors | > 0.1% |
| Kafka Consumer Lag | Message processing delay | > 10,000 messages |
| DB Connection Pool | Active/idle connections | > 80% pool utilized |
| Redis Hit Rate | Cache effectiveness | < 95% |

**3. Business Health Dashboard:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    FDB PAY - BUSINESS HEALTH                    │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Transactions│  │  Revenue     │  │  Settlement  │         │
│  │  Today: 450K │  │  Today: 12M  │  │  Pending: 50 │         │
│  │  vs Yesterday│  │  vs Yesterday│  │  Completed:  │         │
│  │  +12%        │  │ +15%         │  │  9,950       │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Fraud Alerts│  │  KYC Queue   │  │  Support     │         │
│  │  Today: 23   │  │  Pending: 150│  │  Open: 45    │         │
│  │  Blocked: 5  │  │  Avg Time:   │  │  Avg Response│         │
│  │  Review: 18  │  │  2.3 hours   │  │  4.5 hours   │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

**Alerting Rules:**

```yaml
# Prometheus alerting rules
groups:
  - name: fdbpay-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }}% (threshold: 1%)"

      - alert: HighLatency
        expr: histogram_quantile(0.99, rate(http_request_duration_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High p99 latency detected"
          description: "p99 latency is {{ $value }}s (threshold: 2s)"

      - alert: KafkaConsumerLag
        expr: kafka_consumer_group_lag > 10000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Kafka consumer lag high"
          description: "Consumer group {{ $labels.consumergroup }} lag: {{ $value }}"
```

---

## 14. Cost & Trade-offs

---

### Q14.1: What are the key architectural trade-offs you made for FDB Pay?

**Answer:**

**Trade-off 1: Consistency vs Availability**

| Choice | Decision | Reasoning |
|--------|----------|-----------|
| Wallet balance updates | Strong consistency (ACID) | Money must never be wrong |
| Transaction history | Eventual consistency (CQRS) | 1-second delay acceptable for read |
| Notification delivery | At-least-once (Kafka) | Duplicate notifications are better than lost ones |

**Real-World Impact:**
During a network partition between Wallet Service and DB, we **reject transactions** (sacrifice availability) rather than risk inconsistent balances (sacrifice consistency). For a payment platform, this is the correct choice.

**Trade-off 2: Latency vs Cost**

| Choice | Decision | Reasoning |
|--------|----------|-----------|
| Balance query | Redis cache (< 1ms) | 99% of balance queries served from cache |
| Transaction history | PostgreSQL (10-50ms) | Complex queries need relational DB |
| Settlement aggregation | Materialized view (100ms) | Pre-computed for speed |

**Real-World Impact:**
Adding Redis costs ~MMK 1M/month but reduces DB load by 95% and improves balance query latency from 20ms to 0.5ms. Worth it for 500K users.

**Trade-off 3: Simplicity vs Flexibility**

| Choice | Decision | Reasoning |
|--------|----------|-----------|
| Payment routing | Hardcoded rail selection | Simple, predictable, auditable |
| Fee calculation | Rule-based engine | Easy to modify without code changes |
| KYC tiers | Fixed 3-tier system | Clear, compliant, easy to explain |

**Real-World Impact:**
We could build a fully configurable payment routing system, but Myanmar's payment landscape has only 3-4 rails (internal, MPU, RTGS, mobile money). Hardcoding is simpler, faster, and less error-prone than a generic routing engine.

---

### Q14.2: If you could redesign FDB Pay from scratch, what would you change?

**Answer:**

**1. Event Sourcing from Day 1:**
Instead of the current dual-write (DB + Kafka), implement event sourcing where the event log IS the source of truth:

```
Current:  DB write -> Kafka publish (dual-write, potential inconsistency)
Better:   Kafka event -> DB projection (event-first, guaranteed consistency)
```

**2. GraphQL for Mobile App:**
Instead of REST, use GraphQL to reduce over-fetching:

```graphql
query {
  wallet {
    balance
    recentTransactions(limit: 20) {
      id
      amount
      counterparty { name }
      createdAt
    }
  }
}
```

One request vs. 3-4 REST calls.

**3. CQRS from Day 1:**
Don't wait for performance problems to introduce CQRS. Build read and write models separately from the start.

**4. Better Multi-Region Strategy:**
Myanmar has connectivity issues between regions. Consider regional read replicas in Mandalay and Yangon for lower latency.

---

### Q14.3: How would you estimate the infrastructure cost for FDB Pay at 500K users?

**Answer:**

**Infrastructure Cost Breakdown (Myanmar Data Center):**

| Component | Specification | Monthly Cost (MMK) |
|-----------|--------------|-------------------|
| **Kubernetes Cluster** | 16 nodes (8 vCPU, 32GB RAM each) | 8,000,000 |
| **PostgreSQL** | Primary (16 vCPU, 64GB) + 2 replicas | 3,000,000 |
| **Redis Cluster** | 3 nodes (16GB total) | 1,000,000 |
| **Kafka Cluster** | 3 brokers (8 vCPU, 16GB each) | 1,500,000 |
| **Elasticsearch** | 3 nodes (8 vCPU, 16GB each) | 1,200,000 |
| **MinIO (Object Storage)** | 5TB NVMe + 20TB HDD | 800,000 |
| **Load Balancer** | 2x Nginx (4 vCPU, 8GB each) | 400,000 |
| **Monitoring** | Prometheus + Grafana (4 vCPU, 8GB) | 300,000 |
| **HSM** | Thales Luna (hardware) | 500,000 |
| **Network** | 1Gbps dedicated + VPN | 1,000,000 |
| **Backup** | Daily full, hourly incremental | 500,000 |
| **DR Site** | Reduced capacity (warm standby) | 3,000,000 |
| **TOTAL** | | **~21,200,000/month** |

**Revenue Justification:**
- 450K daily transactions x MMK 500 avg fee = MMK 225K/day
- Monthly: ~MMK 6.75M
- Plus merchant fees (1.5% of GMV)
- Plus interest on float (MMK 500M avg float x 8% annual / 12 = ~MMK 3.3M/month)
- **Break-even at ~MMK 21M/month requires ~10K merchants at 1.5% fee on MMK 150M GMV**

---

*This document covers the most common system design interview questions for a payment platform like FDB Pay. Each answer is grounded in real-world scenarios specific to Myanmar's payment ecosystem and FDB Bank's requirements.*
