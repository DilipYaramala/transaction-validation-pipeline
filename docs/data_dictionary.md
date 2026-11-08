
# Data Dictionary and Data Contracts

## 1. Source: Transaction-Leg CSV

**Grain:** One row represents one debit or credit leg. The `transfer_id` groups the two legs belonging to a transfer.

| Field | Type | Nullable | Key / Rule | Meaning |
|---|---|---|---|---|
| event_id | string | No | Unique in curated history | Stable transaction event identifier |
| transfer_id | string | No | Grouping key | Identifies the transfer |
| leg_type | string | No | DEBIT or CREDIT | Ledger direction |
| account_id | string | No | Must exist in account reference | Account affected |
| amount | decimal(19,4) | No | Greater than zero | Absolute transaction amount |
| currency | varchar(3) | No | INR, USD, EUR, GBP | Transaction currency |
| event_time | ISO-8601 timestamp | No | Must be parseable | Transaction event time |

## 2. Reference Entity: account

**Grain:** One row per account.

| Field | MySQL Type | Nullable | Key / Rule |
|---|---|---|---|
| account_id | varchar(40) | No | Primary key |

The demo environment contains seeded account identifiers. Production should use an authoritative account reference source.

## 3. Curated Target: curated_transaction

**Grain:** One validated transaction leg. Both legs of a transfer are published only when the transfer passes validation.

| Field | MySQL Type | Nullable | Key / Rule |
|---|---|---|---|
| id | bigint | No | Primary key, auto-increment |
| account_id | varchar(255) | No | Account identifier |
| amount | decimal(19,4) | No | Transaction amount |
| currency | varchar(3) | No | Currency code |
| event_id | varchar(255) | No | Unique key |
| event_time | datetime(6) | No | Event timestamp |
| leg_type | varchar(255) | No | DEBIT or CREDIT |
| run_id | varchar(255) | No | Pipeline run identifier |
| source_file | varchar(255) | No | Input filename |
| transfer_id | varchar(255) | No | Transfer grouping identifier |

## 4. Quarantine Target: rejected_transaction

**Grain:** One rejected source row.

| Field | MySQL Type | Nullable | Key / Rule |
|---|---|---|---|
| id | bigint | No | Primary key, auto-increment |
| event_id | varchar(255) | Yes | Source event identifier, if available |
| raw_record | tinytext | Yes | Original source row, if available |
| reason | varchar(500) | No | Rejection reason |
| run_id | varchar(255) | No | Pipeline run identifier |
| source_file | varchar(255) | No | Input filename |
| source_row | int | No | Source row number |

## 5. Operational Target: pipeline_run

**Grain:** One pipeline execution per source filename in the current implementation.

| Field | MySQL Type | Nullable | Key / Rule |
|---|---|---|---|
| run_id | varchar(36) | No | Primary key |
| source_file | varchar(255) | No | Unique filename |
| status | varchar(255) | No | RUNNING, COMPLETED, or FAILED |
| started_at | datetime(6) | No | Execution start time |
| ended_at | datetime(6) | Yes | Execution end time |
| input_count | int | Yes | Number of input rows |
| curated_count | int | Yes | Number of accepted rows |
| rejected_count | int | Yes | Number of rejected rows |
| failure_reason | varchar(1000) | Yes | Batch failure details, if any |

## 6. Relationships and Invariants

- `account.account_id` is the account reference used to validate source account identifiers.
- `transfer_id` groups the debit and credit legs of a transfer.
- A valid transfer must contain exactly one DEBIT leg and one CREDIT leg.
- The two legs must have matching amounts and currencies and distinct accounts.
- Debit event time must not be later than credit event time.
- `event_id` is unique in the curated transaction table.
- Account existence is validated by the application service.
- A completed run must satisfy:

  `input_count = curated_count + rejected_count`

- `curated_count` and `rejected_count` count transaction legs (rows), not transfers.
- Rejected records retain their run identifier, source filename, source row number, available event ID, raw record, and rejection reason.
