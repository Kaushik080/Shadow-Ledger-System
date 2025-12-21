# Correction Strategy

## Overview
The Drift and Correction Service detects mismatches between the Core Banking System (CBS) reported balances and the Shadow Ledger balances, then generates correction events to reconcile the differences.

## Drift Detection Algorithm

### Step 1: Balance Comparison
```
difference = CBS_reported_balance - shadow_balance
```

### Step 2: Tolerance Check
- Tolerance threshold: **0.01** (1 cent)
- If `|difference| ≤ tolerance`, balances are considered to **MATCH**
- If `|difference| > tolerance`, a **MISMATCH** is detected

### Step 3: Mismatch Classification

#### Case 1: Missing Credit (difference > 0)
- **Condition**: `difference > tolerance`
- **Meaning**: CBS balance is higher than shadow balance
- **Cause**: Shadow ledger is missing a credit transaction
- **Action**: Generate **credit correction event** for `|difference|` amount

#### Case 2: Incorrect Debit (difference < 0)
- **Condition**: `difference < -tolerance`
- **Meaning**: CBS balance is lower than shadow balance
- **Cause**: Shadow ledger has extra balance (missing debit or incorrect credit)
- **Action**: Generate **debit correction event** for `|difference|` amount

#### Case 3: Unknown Mismatch
- **Condition**: Complex scenarios requiring manual review
- **Examples**:
  - Multiple missing transactions
  - Timing issues with pending transactions
  - Data corruption
- **Action**: Flag for manual review, optionally generate correction based on heuristics

## Correction Event Generation

### Event Structure
```json
{
  "eventId": "CORR-{accountId}-{uuid}",
  "accountId": "A10",
  "type": "credit",
  "amount": 50.00,
  "timestamp": 1735561800000,
  "reason": "Auto-correction: missing credit detected"
}
```

### Event ID Format
- Prefix: `CORR-` (identifies as correction)
- Account ID: For traceability
- UUID: 8-character unique identifier
- Example: `CORR-A10-7f3a2b1c`

### Correction Types

#### Automatic Corrections
1. **Missing Credit**: Add credit to increase shadow balance
2. **Missing Debit**: Add debit to decrease shadow balance
3. Triggered automatically during drift check
4. Published to `transactions.corrections` Kafka topic

#### Manual Corrections
1. Triggered by admin via `POST /correct/{accountId}` endpoint
2. Requires explicit amount, type, and reason
3. Used for complex scenarios or overriding automatic corrections
4. Requires admin role (enforced by API Gateway)

## Correction Workflow

### Automatic Correction Flow
```
CBS Balance Report → Drift Check Controller
                            ↓
                    Drift Service (Compare)
                            ↓
                    Detect Mismatch Type
                            ↓
                    Generate Correction Event
                            ↓
                    Kafka (transactions.corrections)
                            ↓
                    Shadow Ledger Service (Consume)
                            ↓
                    Apply Correction to Ledger
                            ↓
                    Updated Shadow Balance
```

### Manual Correction Flow
```
Admin Request → API Gateway (RBAC Check)
                        ↓
                Correction Controller
                        ↓
                Generate Correction Event
                        ↓
                Kafka (transactions.corrections)
                        ↓
                Shadow Ledger Service (Consume)
                        ↓
                Apply Correction to Ledger
```

## Reconciliation Process

### Daily Reconciliation
1. CBS generates end-of-day balance report
2. Report is submitted to `/drift-check` endpoint
3. System compares each account's balance
4. Generates correction events for mismatches
5. Corrections are applied automatically via Kafka
6. Final report shows reconciliation status

### Response Format
```json
{
  "totalAccounts": 100,
  "mismatches": 3,
  "results": [
    {
      "accountId": "A10",
      "shadowBalance": 700.00,
      "reportedBalance": 750.00,
      "difference": 50.00,
      "status": "MISMATCH",
      "mismatchType": "missing_credit",
      "correctionEventId": "CORR-A10-7f3a2b1c",
      "message": "Shadow ledger is missing credit of 50.00"
    }
  ]
}
```

## Correction Event Processing

### Deduplication
- Correction events have unique `eventId`
- Ledger Service checks for duplicate `eventId` before processing
- Prevents double-application of corrections

### Ordering
- Correction events follow the same ordering rules as regular events
- Ordered by timestamp, then eventId
- Ensures deterministic balance calculation

### Negative Balance Protection
- Debit corrections are validated to prevent negative balances
- If correction would cause negative balance, it's rejected
- Admin must investigate and apply manual correction with proper safeguards

## Audit Trail

### Logging
All corrections are logged with:
- Timestamp
- Account ID
- Correction type and amount
- Reason for correction
- Event ID
- Original mismatch details

### Traceability
- Correction events remain in immutable ledger
- Can trace correction back to drift check that triggered it
- Reason field provides context for audit

## Error Handling

### Kafka Publish Failure
- Correction event generation is logged
- Kafka publish failures trigger retry (3 attempts)
- If all retries fail, error is logged for manual intervention

### Correction Application Failure
- Shadow Ledger Service logs rejection reason
- Failed corrections remain in Kafka for retry
- Manual review required for persistent failures

## Multi-Step Corrections

For complex scenarios requiring multiple corrections:

1. **Identify Root Cause**: Manual investigation
2. **Calculate Net Adjustment**: Determine single correction amount
3. **Apply Single Correction**: Use manual correction endpoint
4. **Verify Result**: Run drift check again to confirm balance match

## Reversal Strategy

If a correction was applied incorrectly:

1. **Generate Reversal Event**: Opposite type (credit ↔ debit) with same amount
2. **Apply Correct Event**: New correction with proper amount
3. **Audit Trail**: Both reversal and correction are logged

Example:
```
Original: CORR-A10-abc (credit 50.00) - INCORRECT
Reversal: CORR-A10-def (debit 50.00) - Reverses incorrect correction
Correct:  CORR-A10-ghi (credit 30.00) - Applies correct amount
```

## Best Practices

### 1. Regular Reconciliation
- Run drift checks daily
- Investigate persistent mismatches
- Monitor correction frequency

### 2. Threshold Tuning
- Adjust tolerance based on business requirements
- Consider currency precision
- Account for timing differences

### 3. Manual Review
- Large corrections should be reviewed by auditor
- Set thresholds for automatic vs manual corrections
- Investigate patterns of corrections for same account

### 4. Idempotency
- Ensure CBS reports are idempotent
- Don't submit same balance report multiple times
- Version balance reports if needed

## Performance Considerations

### Batch Processing
- Process multiple accounts in single drift check
- Parallelize balance comparisons
- Batch correction event generation

### Kafka Throughput
- Use proper partitioning for corrections
- Monitor lag in corrections topic
- Scale consumers if needed

---

**Last Updated**: December 2025  
**Version**: 1.0

