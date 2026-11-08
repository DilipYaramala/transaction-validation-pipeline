
# Transaction Validation Pipeline

A Java Spring Boot application that ingests, validates, and processes financial transaction records using CSV files and MySQL.

## 1. Project Overview

The Transaction Validation Pipeline is a data engineering application developed to validate financial transaction records and ensure that invalid transactions do not enter the curated dataset.

The application reads transaction data from CSV files, applies validation rules, classifies records as valid or rejected, and stores the results in a MySQL database.

### Key Objectives

- Validate debit and credit transaction legs.
- Verify transfer amount balancing.
- Validate transaction currencies.
- Check transaction event ordering.
- Detect duplicate transaction events.
- Identify and reject invalid records.
- Maintain pipeline execution status and record counts.
- Provide APIs to retrieve curated and rejected transactions.

## 2. Technology Stack

| Technology | Purpose |
|---|---|
| Java | Backend development |
| Spring Boot | Application and REST API development |
| MySQL | Database management |
| Spring Data JPA | Database operations |
| Hibernate | Object-relational mapping |
| Maven | Build and dependency management |
| CSV | Input data format |
| JUnit | Application testing |
| Eclipse IDE | Development environment |

## 3. Application Architecture

The application follows a layered architecture to maintain separation of responsibilities.

| Layer | Responsibility |
|---|---|
| Controller | Handles incoming REST API requests |
| Service | Implements transaction processing and validation logic |
| Repository | Performs database operations |
| Entity | Represents database tables |
| DTO | Transfers data between application components |

### Processing Workflow

CSV Input → Data Ingestion → Transaction Validation → Record Classification → Database Storage → REST API Response

### Transaction Classification

**Curated Transactions**
- Records that successfully pass validation.
- Stored in the curated transaction dataset.
- Available through the curated transactions API.

**Rejected Transactions**
- Records that fail validation.
- Stored separately with their rejection reasons.
- Excluded from the curated dataset.

## 4. Project Structure

```text
transaction-validation-pipeline/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/dilip/transaction/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── entity/
│   │   │       └── dto/
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       └── data/
│   │
│   └── test/
│
├── docs/
│   ├── architecture.md
│   ├── data_dictionary.md
│   ├── design_tradeoffs.md
│   └── expected_results.md
│
├── scripts/
├── pom.xml
├── README.md
└── .gitignore
```

## 5. Prerequisites

Install the following software:

- Java JDK compatible with the project configuration.
- MySQL Server.
- Eclipse IDE with Maven integration.
- Git.

## 6. Database Configuration

### Step 1: Create the Database

Execute the following query in MySQL Workbench:

```sql
CREATE DATABASE transaction_pipeline;
```

### Step 2: Configure Database Connection

Open:

`src/main/resources/application.properties`

Configure the following properties with your local MySQL credentials:

- Database URL
- Database username
- Database password

Make sure database credentials are not committed to the public GitHub repository.

## 7. Build and Run

### Step 1: Import the Project

1. Open Eclipse IDE.
2. Select File → Import.
3. Choose Existing Maven Projects.
4. Select the project directory.
5. Click Finish.

### Step 2: Configure Java

Configure the project to use a JDK compatible with the project configuration.

### Step 3: Build and Execute Tests

1. Right-click the project.
2. Select Run As → Maven build.
3. Enter the following Maven goal:

```text
clean test
```

### Step 4: Start the Application

1. Locate `TransactionPipelineApplication.java`.
2. Right-click the class.
3. Select Run As → Spring Boot App.

The application starts at:

```text
http://localhost:8080
```

## 8. REST API Documentation

Base URL:

```text
http://localhost:8080/api/pipeline
```

| HTTP Method | Endpoint | Description |
|---|---|---|
| POST | `/runs?filePath={path}` | Starts transaction processing |
| GET | `/runs/{id}` | Retrieves pipeline run details |
| GET | `/runs/{id}/curated` | Retrieves curated transactions |
| GET | `/runs/{id}/rejects` | Retrieves rejected transactions |

### 8.1 Start Pipeline Processing

**Method:** POST

```text
/api/pipeline/runs?filePath={path}
```

PowerShell example:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/pipeline/runs?filePath=C:/path/to/transactions.csv"
```

Replace the example path with the actual CSV file path accessible to the application.

### 8.2 Retrieve Pipeline Run Details

**Method:** GET

```text
/api/pipeline/runs/{id}
```

Retrieves the execution status, source file, processing counts, and run details.

### 8.3 Retrieve Curated Transactions

**Method:** GET

```text
/api/pipeline/runs/{id}/curated
```

Retrieves successfully validated transaction records.

### 8.4 Retrieve Rejected Transactions

**Method:** GET

```text
/api/pipeline/runs/{id}/rejects
```

Retrieves rejected transaction records along with their rejection information.

## 9. Transaction Validation Process

The pipeline performs the following operations:

1. Reads transaction records from the input CSV file.
2. Parses and processes transaction events.
3. Validates transaction identifiers.
4. Checks debit and credit transaction legs.
5. Validates transfer amounts and currencies.
6. Checks transaction event ordering.
7. Identifies duplicate events and invalid transactions.
8. Separates valid and rejected records.
9. Stores processing results in MySQL.
10. Updates pipeline execution status and record counts.

## 10. Validation Results

The application was tested using a generated dataset containing 100,000 transaction records.

### Execution Summary

| Metric | Result |
|---|---:|
| Total Input Records | 100,000 |
| Curated Records | 95,000 |
| Rejected Records | 5,000 |
| Pipeline Status | COMPLETED |

### Rejection Analysis

| Rejection Reason | Records |
|---|---:|
| UNBALANCED_TRANSFER_AMOUNT | 5,000 |

The pipeline successfully identified 5,000 unbalanced transactions and prevented them from entering the curated dataset.

All 100,000 input records were accounted for.

### Processing Summary

- Total records processed: 100,000
- Valid records stored: 95,000
- Invalid records rejected: 5,000
- Processing status: COMPLETED
- Processing failure: None

## 11. Test Results

The Maven test execution completed successfully.

```text
Tests run: 13
Failures: 0
Errors: 0
Skipped: 0

BUILD SUCCESS
```

### API Verification

| Verification | Result |
|---|---|
| Application startup | Successful |
| MySQL connectivity | Successful |
| Pipeline execution | Successful |
| Run Status API | Passed |
| Curated Transactions API | Passed |
| Rejected Transactions API | Passed |
| Database record count verification | Passed |

The application was successfully started, connected to MySQL, and verified using the REST APIs.

## 12. Key Features

- CSV-based transaction ingestion.
- Automated transaction validation.
- Debit and credit leg validation.
- Transfer amount balancing.
- Currency validation.
- Event ordering validation.
- Duplicate event detection.
- Separation of curated and rejected records.
- Pipeline execution status tracking.
- Input, curated, and rejected record counts.
- REST API integration.
- MySQL persistence.

## 13. Future Enhancements

- Docker containerization.
- Jenkins CI/CD integration.
- AWS cloud deployment.
- Centralized logging and monitoring.
- Authentication and authorization.
- Enhanced batch processing and scalability.

## 14. Author

**Dilip Yaramala**

Java | Spring Boot | MySQL | Data Engineering
