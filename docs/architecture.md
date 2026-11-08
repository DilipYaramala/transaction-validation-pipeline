
# Architecture and Processing Flow

## 1. Architecture Overview

The Transaction Validation Pipeline processes CSV transaction records through schema validation, row-level validation, duplicate event checks, and transfer-level validation.

The application separates valid transactions from rejected transactions and records execution statistics in the pipeline_run table.

## 2. Processing Flow

```text
                  CSV Source
                      |
                      v
             Header / Schema Validation
                      |
             +--------+--------+
             |                 |
           Valid             Invalid
             |                 |
             v                 v
       Row Parsing       FAILED pipeline_run
       and Validation
             |
       +-----+------+
       |            |
   Valid Rows   Invalid Rows
       |            |
       |            v
       |     rejected_transaction
       |
       v
 Duplicate Event Checks
       |
       v
 Group by transfer_id
       |
       v
 Transfer-Level Validation
       |
       +-----------------------+
       |                       |
   Valid Transfer        Invalid Transfer
       |                       |
       v                       v
curated_transaction    rejected_transaction
       |                       |
       +-----------+-----------+
                   |
                   v
              pipeline_run
           Counts and Status
```

## 3. Transfer-Level Validation

Transactions are grouped using transfer_id.

Each transfer is checked for:

- Exactly two transaction legs.
- One DEBIT leg and one CREDIT leg.
- Matching debit and credit amounts.
- Matching currencies.
- Distinct source and destination accounts.
- Debit event_time occurring before or at the same time as credit event_time.

Transfers that pass the validation rules are included in curated_transaction.

Transfers that fail validation are stored in rejected_transaction with the appropriate rejection reason.

## 4. Publication Boundary

A transfer is the validation unit.

Both legs of a transfer are accepted or rejected together. This prevents partial transfer publication.

A malformed row that cannot be parsed is rejected at row level. If its companion leg is parseable, that leg is subsequently rejected as part of an incomplete transfer.

Therefore, an incomplete transfer cannot enter curated output.

## 5. Operational Metrics

The pipeline_run table stores:

- Run ID
- Source filename
- Execution status
- Start and end timestamps
- Input row count
- Curated transaction count
- Rejected transaction count
- Batch-level failure reason

For completed runs, the following reconciliation should hold:

Input Count = Curated Count + Rejected Count

Or:

input_count - curated_count - rejected_count = 0

## 6. Conclusion

The architecture separates CSV ingestion, transaction validation, record classification, and database persistence.

Transfer-level validation ensures that both legs of a transfer are handled consistently, while pipeline execution metrics provide visibility into processing results.
```
