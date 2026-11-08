
# AI Usage and Prompt History

## AI Tool Used
ChatGPT was used as an AI assistant during the development of the Transaction Validation Pipeline.

## Key Areas of AI Assistance

- Understanding the assignment requirements and designing the transaction validation workflow.
- Guidance on implementing CSV processing using Java and Apache Commons CSV.
- Implementing debit and credit validation, amount matching, currency checks, and event ordering.
- Understanding duplicate event detection and rejection handling.
- Troubleshooting Spring Boot, MySQL, and REST API issues.
- Preparing and reviewing project documentation and test results.

## Representative Prompts

1. Help me design a transaction validation pipeline using Java, Spring Boot, and MySQL.
2. How do I process CSV files using Apache Commons CSV?
3. How can I validate debit and credit transaction legs and reject unbalanced transfers?
4. How do I handle duplicate event IDs and prevent invalid records from entering curated transactions?
5. Help me troubleshoot application errors and verify REST API endpoints.

## Personal Contribution

I configured and ran the application, worked with the MySQL database, prepared and tested CSV datasets, executed API requests, investigated errors, and verified the processing results.

AI-generated suggestions were reviewed and applied where appropriate. The implementation was validated through practical execution, database queries, API testing, and automated tests.

## Testing Evidence

- Automated tests: 13 passed, 0 failures, 0 errors.
- Large dataset: 100,000 input records.
- Curated records: 95,000.
- Rejected records: 5,000.
- Pipeline status: COMPLETED.
