
## 1. Sample Run: Actual Outcomes

**Input file:** `src/main/resources/data/sample_transactions.csv`

The sample dataset contains 12 transaction records.

### Rejected Transaction Details

| Event ID | Actual Rejection Reason |
|---|---|
| EVT005 | UNBALANCED_TRANSFER_AMOUNT |
| EVT006 | UNBALANCED_TRANSFER_AMOUNT |
| EVT007 | CURRENCY_MISMATCH |
| EVT008 | CURRENCY_MISMATCH |
| EVT009 | UNKNOWN_ACCOUNT |
| EVT010 | OUT_OF_ORDER_TRANSFER_LEGS |
| EVT011 | OUT_OF_ORDER_TRANSFER_LEGS |
| EVT012 | INVALID_AMOUNT |

### Sample Processing Results

| Metric | Actual Count |
|---|---:|
| Input Records | 12 |
| Curated Records | 4 |
| Rejected Records | 8 |
| Pipeline Status | COMPLETED |

**Record reconciliation:**

12 = 4 + 8

The pipeline successfully classified the sample records into curated and rejected outputs.

The rejected records were stored with their corresponding validation reasons.
