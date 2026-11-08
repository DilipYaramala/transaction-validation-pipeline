package com.dilip.transaction;

import com.dilip.transaction.dto.RunResponse;

import com.dilip.transaction.entity.Account;
import com.dilip.transaction.repository.AccountRepository;
import com.dilip.transaction.repository.CuratedRepository;
import com.dilip.transaction.repository.RejectRepository;
import com.dilip.transaction.service.PipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import com.dilip.transaction.entity.RejectedTransaction;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class PipelineServiceTest {
    @Autowired PipelineService service;
    @Autowired AccountRepository accounts;
    @Autowired CuratedRepository curated;
    @Autowired RejectRepository rejects;
    @Autowired
    private RejectRepository rejectRepository;

    @BeforeEach
    void seedAccounts() {
        for (int i = 1; i <= 5; i++) {
            String id = "ACC100" + i;
            if (!accounts.existsById(id)) accounts.save(new Account(id));
        }
    }

    @Test
    void publishesBalancedTransferAndRejectsInvalidTransfers() throws Exception {
        Path file = csv("quality-case", String.join("\n",
            "E101,T101,DEBIT,ACC1001,100.00,INR,2026-11-01T10:00:00Z",
            "E102,T101,CREDIT,ACC1002,100.00,INR,2026-11-01T10:01:00Z",
            "E103,T102,DEBIT,ACC1001,50.00,INR,2026-11-01T10:02:00Z",
            "E104,T102,CREDIT,ACC1003,49.00,INR,2026-11-01T10:03:00Z",
            "E105,T103,DEBIT,ACC1001,10.00,INR,2026-11-01T10:04:00Z",
            "E106,T103,CREDIT,ACC1003,10.00,USD,2026-11-01T10:05:00Z",
            "E107,T104,DEBIT,ACC1001,20.00,INR,2026-11-01T10:07:00Z",
            "E108,T104,CREDIT,ACC1003,20.00,INR,2026-11-01T10:06:00Z",
            "E109,T105,DEBIT,UNKNOWN,5.00,INR,2026-11-01T10:08:00Z"
        ));
        RunResponse result = service.process(file);
        assertThat(result.inputCount()).isEqualTo(9);
        assertThat(result.curatedCount()).isEqualTo(2);
        assertThat(result.rejectedCount()).isEqualTo(7);
        assertThat(curated.findByRunId(result.runId())).hasSize(2);
        assertThat(rejects.findByRunId(result.runId())).hasSize(7);
    }

    @Test
    void handlesEmptyBatch() throws Exception {
        Path file = csv("empty-case", "");
        RunResponse result = service.process(file);
        assertThat(result.inputCount()).isZero();
        assertThat(result.curatedCount()).isZero();
        assertThat(result.rejectedCount()).isZero();
    }

    @Test
    void quarantinesMissingRequiredValueAndDuplicateEventId() throws Exception {

        Path file = csv("duplicate-case", String.join("\n",
            "E201,T201,DEBIT,ACC1001,12.00,INR,2026-11-01T10:00:00Z",
            "E201,T201,CREDIT,ACC1002,12.00,INR,2026-11-01T10:01:00Z",
            "E203,T202,DEBIT,ACC1001,,INR,2026-11-01T10:02:00Z",
            "E204,T202,CREDIT,ACC1002,12.00,INR,2026-11-01T10:03:00Z"
        ));

        RunResponse result = service.process(file);

        assertThat(result.curatedCount()).isZero();
        assertThat(result.rejectedCount()).isEqualTo(4);

        List<RejectedTransaction> rejected =
            rejectRepository.findByRunId(result.runId());

        assertThat(rejected).hasSize(4);

        assertThat(rejected)
        .extracting(RejectedTransaction::getReason)
        .contains(
            "DUPLICATE_EVENT_ID_IN_BATCH",
            "MISSING_AMOUNT",
            "TRANSFER_MUST_HAVE_EXACTLY_TWO_LEGS"
        );
    }
    @Test
    void rejectsMissingEventId() throws Exception {

        Path file = csv("missing-event-id", String.join("\n",
            ",T601,DEBIT,ACC1001,25.00,INR,2026-11-01T10:00:00Z",
            "E602,T601,CREDIT,ACC1002,25.00,INR,2026-11-01T10:01:00Z"
        ));

        RunResponse result = service.process(file);

        assertThat(result.inputCount()).isEqualTo(2);
        assertThat(result.curatedCount()).isZero();
        assertThat(result.rejectedCount()).isEqualTo(2);
    }

    @Test
    void rerunningSameFilenameReturnsOriginalRun() throws Exception {
        Path file = csv("replay-case", String.join("\n",
            "E301,T301,DEBIT,ACC1001,7.00,INR,2026-11-01T10:00:00Z",
            "E302,T301,CREDIT,ACC1002,7.00,INR,2026-11-01T10:01:00Z"
        ));
        RunResponse first = service.process(file);
        RunResponse second = service.process(file);
        assertThat(second.runId()).isEqualTo(first.runId());
        assertThat(curated.findByRunId(first.runId())).hasSize(2);
    }

    @Test
    void rejectsUnexpectedSchema() throws Exception {
        Path file = Files.createTempFile("schema-case-", ".csv");
        Files.writeString(file, "id,amount\nX1,10\n");
        RunResponse result = service.process(file);
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.failureReason()).contains("UNEXPECTED_SCHEMA");
    }

@Test
void rejectsInvalidAmountsAndCurrencies() throws Exception {
    Path file = csv("invalid-amount-currency", String.join("\n",
        // Transfer 1: Zero amount
        "E401,T401,DEBIT,ACC1001,0.00,INR,2026-11-01T10:00:00Z",
        "E402,T401,CREDIT,ACC1002,0.00,INR,2026-11-01T10:01:00Z",

        // Transfer 2: Negative amount
        "E403,T402,DEBIT,ACC1001,-50.00,INR,2026-11-01T10:02:00Z",
        "E404,T402,CREDIT,ACC1002,-50.00,INR,2026-11-01T10:03:00Z",

        // Transfer 3: Unsupported currency
        "E405,T403,DEBIT,ACC1001,100.00,XYZ,2026-11-01T10:04:00Z",
        "E406,T403,CREDIT,ACC1002,100.00,XYZ,2026-11-01T10:05:00Z"
    ));

    RunResponse result = service.process(file);

    assertThat(result.inputCount()).isEqualTo(6);
    assertThat(result.curatedCount()).isZero();
    assertThat(result.rejectedCount()).isEqualTo(6);

    assertThat(rejects.findByRunId(result.runId())).hasSize(6);
}
@Test
void rejectsInvalidLegType() throws Exception {

    Path file = csv("invalid-leg-type", String.join("\n",
        "E701,T701,TRANSFER,ACC1001,30.00,INR,2026-11-01T10:00:00Z",
        "E702,T701,CREDIT,ACC1002,30.00,INR,2026-11-01T10:01:00Z"
    ));

    RunResponse result = service.process(file);

    assertThat(result.inputCount()).isEqualTo(2);
    assertThat(result.curatedCount()).isZero();
    assertThat(result.rejectedCount()).isEqualTo(2);
}
@Test
void rejectsIncompleteTransferWithOnlyOneLeg() throws Exception {

    Path file = csv("incomplete-transfer-case", String.join("\n",
        "E401,T401,DEBIT,ACC1001,100.00,INR,2026-11-01T10:00:00Z"
    ));

    RunResponse result = service.process(file);

    assertThat(result.inputCount()).isEqualTo(1);
    assertThat(result.curatedCount()).isZero();
    assertThat(result.rejectedCount()).isEqualTo(1);

    assertThat(rejects.findByRunId(result.runId())).hasSize(1);
}

@Test
void rejectsInvalidTimestamp() throws Exception {

    Path file = csv("invalid-timestamp-case", String.join("\n",
        "E501,T501,DEBIT,ACC1001,100.00,INR,NOT_A_VALID_DATE",
        "E502,T501,CREDIT,ACC1002,100.00,INR,2026-11-01T10:01:00Z"
    ));

    RunResponse result = service.process(file);

    assertThat(result.inputCount()).isEqualTo(2);
    assertThat(result.curatedCount()).isZero();
    assertThat(result.rejectedCount()).isEqualTo(2);

    assertThat(rejects.findByRunId(result.runId())).hasSize(2);
}

@Test
void rejectsDuplicateEventIdsAcrossSeparateRuns() throws Exception {

    // First CSV: publish a valid transfer
    Path firstFile = csv("first-duplicate-run", String.join("\n",
        "E601,T601,DEBIT,ACC1001,100.00,INR,2026-11-01T10:00:00Z",
        "E602,T601,CREDIT,ACC1002,100.00,INR,2026-11-01T10:01:00Z"
    ));

    RunResponse firstResult = service.process(firstFile);

    assertThat(firstResult.curatedCount()).isEqualTo(2);
    assertThat(firstResult.rejectedCount()).isZero();

    // Second CSV: reuse the same event IDs in a different file
    Path secondFile = csv("second-duplicate-run", String.join("\n",
        "E601,T602,DEBIT,ACC1001,100.00,INR,2026-11-01T10:02:00Z",
        "E602,T602,CREDIT,ACC1002,100.00,INR,2026-11-01T10:03:00Z"
    ));

    RunResponse secondResult = service.process(secondFile);

    assertThat(secondResult.runId()).isNotEqualTo(firstResult.runId());
    assertThat(secondResult.curatedCount()).isZero();
    assertThat(secondResult.rejectedCount()).isEqualTo(2);

    assertThat(curated.findByRunId(firstResult.runId())).hasSize(2);
    assertThat(curated.findByRunId(secondResult.runId())).isEmpty();
    assertThat(rejects.findByRunId(secondResult.runId())).hasSize(2);
}

@Test
void rejectsMalformedCsv() throws Exception {

    Path file = Files.createTempFile("malformed-csv-", ".csv");

    Files.writeString(file,
        "event_id,transfer_id,leg_type,account_id,amount,currency,event_time\n" +
        "E701,T701,DEBIT,ACC1001,100.00,INR,2026-11-01T10:00:00Z\n" +
        "\"E702,T701,CREDIT,ACC1002,100.00,INR,2026-11-01T10:01:00Z\n"
    );

    RunResponse result = service.process(file);

    assertThat(result.status()).isEqualTo("FAILED");
    assertThat(result.failureReason()).isNotBlank();
}


@Test
void retriesFailedRunSuccessfully() throws Exception {

    Path file = Files.createTempFile("retry-case-", ".csv");

    // First attempt: invalid CSV schema
    Files.writeString(file, "id,amount\nX1,10\n");

    RunResponse first = service.process(file);

    assertThat(first.status()).isEqualTo("FAILED");

    // Second attempt: correct the CSV using the same filename
    Files.writeString(file,
        "event_id,transfer_id,leg_type,account_id,amount,currency,event_time\n" +
        "E701,T701,DEBIT,ACC1001,25.00,INR,2026-11-01T10:00:00Z\n" +
        "E702,T701,CREDIT,ACC1002,25.00,INR,2026-11-01T10:01:00Z\n"
    );

    RunResponse second = service.process(file);

    assertThat(second.runId()).isEqualTo(first.runId());
    assertThat(second.status()).isEqualTo("COMPLETED");
    assertThat(second.inputCount()).isEqualTo(2);
    assertThat(second.curatedCount()).isEqualTo(2);
    assertThat(second.rejectedCount()).isZero();
}



    private Path csv(String prefix, String rows) throws Exception {
        Path file = Files.createTempFile(prefix + "-", ".csv");
        String header = "event_id,transfer_id,leg_type,account_id,amount,currency,event_time\n";
        Files.writeString(file, header + (rows.isBlank() ? "" : rows + "\n"));
        return file;
    }
}
