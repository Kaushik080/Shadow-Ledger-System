# Event Ordering Rules

## Overview
The Shadow Ledger System implements deterministic ordering of financial events to ensure consistency and reproducibility of balance calculations across distributed systems.

## Ordering Strategy

### Primary Ordering: Timestamp (Ascending)
- Events are primarily ordered by their `timestamp` field in ascending order
- Timestamp represents the event occurrence time in milliseconds since Unix epoch
- Earlier events are processed before later events

### Secondary Ordering: Event ID (Ascending)
- When multiple events share the same timestamp, they are ordered by `eventId` lexicographically in ascending order
- This provides a deterministic tie-breaker for concurrent events
- Event IDs are unique and immutable

### SQL Implementation
```sql
ORDER BY timestamp ASC, event_id ASC
```

## Event Deduplication

### Idempotency Requirement
- Each event has a unique `eventId`
- Before processing, the system checks if the `eventId` already exists in the ledger
- Duplicate events are rejected silently to ensure idempotency
- This prevents double-processing of events from Kafka retries or replays

### Implementation
1. Event Service: Checks database before persisting and producing to Kafka
2. Shadow Ledger Service: Checks ledger table before inserting event

## Immutable Ledger Semantics

### Append-Only Design
- The ledger table is **append-only** - no updates or deletes are allowed
- Once an event is written to the ledger, it becomes permanent
- Corrections are handled by adding new correction events, not modifying existing ones

### Benefits
1. **Auditability**: Complete history of all transactions is preserved
2. **Consistency**: No race conditions from concurrent updates
3. **Reproducibility**: Balance can be recalculated at any point in time
4. **Compliance**: Meets regulatory requirements for financial record-keeping

## Balance Computation with Window Functions

### SQL Window Function
The shadow balance is computed using SQL window functions to calculate running balance:

```sql
SELECT 
    account_id,
    event_id,
    type,
    amount,
    timestamp,
    SUM(CASE WHEN type='credit' THEN amount ELSE -amount END) 
        OVER (PARTITION BY account_id ORDER BY timestamp ASC, event_id ASC) 
        as running_balance
FROM ledger 
WHERE account_id = ?
ORDER BY timestamp ASC, event_id ASC
```

### How It Works
1. **PARTITION BY account_id**: Calculates balance separately for each account
2. **ORDER BY timestamp ASC, event_id ASC**: Applies deterministic ordering
3. **CASE WHEN**: Credits add to balance, debits subtract from balance
4. **SUM() OVER**: Computes cumulative sum (running balance) for each row

### Final Balance Query
```sql
SELECT COALESCE(SUM(CASE WHEN type='credit' THEN amount ELSE -amount END), 0) 
FROM ledger 
WHERE account_id = ?
```

## Negative Balance Prevention

### Validation Logic
- Before persisting an event, the system checks if the transaction would result in negative balance
- Current balance is computed: `balance = SUM(credits) - SUM(debits)`
- New balance is calculated: `new_balance = current_balance ± amount`
- If `new_balance < 0`, the transaction is rejected with an error

### Error Handling
- Rejected transactions are logged with error details
- Kafka message is not acknowledged (will be retried or sent to DLQ)
- Client receives appropriate error response

## Event Flow Diagram

```
Event Source → Event Service → Kafka (transactions.raw)
                                  ↓
                         Shadow Ledger Service
                                  ↓
                         Deduplication Check
                                  ↓
                         Balance Validation
                                  ↓
                         Append to Ledger (Immutable)
                                  ↓
                         Compute Running Balance
```

## Concurrent Event Handling

### Kafka Partitioning
- Events for the same account are sent to the same Kafka partition using `accountId` as the key
- This ensures events for a single account are processed in order
- Different accounts can be processed concurrently without interference

### Transaction Isolation
- Each event is processed in a database transaction
- ACID properties ensure consistency even with concurrent consumers
- Row-level locking prevents race conditions during balance checks

## Testing Determinism

To verify ordering is deterministic:
1. Process a set of events in order A
2. Clear the ledger
3. Process the same events in a different order B (by shuffling timestamps)
4. Final balance should be identical if ordering rules are followed
5. Running balance at each step should match when events are in the same order

---

**Last Updated**: December 2025  
**Version**: 1.0

