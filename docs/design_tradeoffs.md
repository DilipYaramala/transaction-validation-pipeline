
# Design Decisions and Trade-offs


## Stack

Java and Spring Boot provide a maintainable backend service with separate controller, service, repository, and entity layers.

MySQL is used for persistent storage of pipeline runs, curated transactions, and rejected transactions.

Maven is used for dependency management and project builds. JUnit is used for automated testing.

Synthetic CSV datasets were used to test transaction validation and processing with larger input volumes.

Apache Commons CSV is a Java library used to efficiently parse CSV files and extract transaction data for validation and processing.

## Correctness

- `BigDecimal` avoids binary floating-point errors. Amounts must be positive and have a maximum scale of four decimal places.
- Timestamps are parsed into `Instant`. Timezone-naive ISO timestamps are interpreted as UTC.
- Validation occurs at both row and transfer levels. A transfer-level failure quarantines every parseable leg, preventing partial transfer publication.
- Row-level missing values, invalid leg types, invalid amounts, unsupported currencies, and unknown accounts are quarantined.
- Missing required CSV headers and CSV parsing or file-reading errors fail the batch.
- Account IDs are checked against reference data.
- Currency validation uses an allow-list: INR, USD, EUR, and GBP. Cross-currency transfers are rejected rather than converted.
- Event ordering is checked within each transfer. Global per-account event-time watermarks are outside scope.
- Duplicate event IDs are checked within the input batch and against previously curated transactions.

## Idempotency and Recovery

The starter identifies a run using its source filename.

A completed filename returns its previous result. An existing non-completed run can be restarted.

Existing curated event IDs are checked in batches of 500. Duplicate event IDs within the current CSV are also rejected. A unique constraint on the curated event ID provides an additional database-level safeguard.

This approach is adequate for a controlled demo, but filenames do not distinguish changed file contents. Concurrent workers also require stronger locking and coordination.

Production should use immutable producer batch IDs or SHA-256 checksums, a dedicated batch ledger, database uniqueness and locking, and immutable versioned inputs for backfills.

## Scale and Performance

Rows, rejected records, and transfer groups are retained in memory, giving approximately O(n) time and O(n) memory complexity.

Existing curated event IDs are retrieved from MySQL in batches of 500 to avoid querying all identifiers in one request.

This is a reasonable vertical slice for testing with 100,000 records.

For multi-million-row inputs, chunked staging-table ingestion, appropriate database indexes, and set-based SQL validation would reduce JVM memory usage and improve scalability.

## Operational Trade-offs

`pipeline_run` records execution status, timestamps, counts, and batch failure reasons.

`rejected_transaction` preserves rejection details and source lineage, including the source filename, row number, available event ID, record representation, and rejection reason.

Local path-based ingestion is easy to demonstrate but needs authentication and path restrictions before deployment.

Hibernate schema update is convenient locally; production should use Flyway or Liquibase.

Seeded demo accounts are not a substitute for authoritative reference data.

## Known Limitations

- No streaming ingestion.
- No foreign-exchange conversion.
- No reversals, fees, or multi-leg journal support.
- No global event-time watermark.
- No object-storage integration.
- No authentication or authorization.
- No concurrency-safe run locking.
- No production database migration strategy.

These features are intentionally excluded to deliver a tested transaction-validation vertical slice within the 72-hour assignment.
