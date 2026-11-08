
package com.dilip.transaction.service;

import com.dilip.transaction.dto.RunResponse;
import com.dilip.transaction.entity.*;
import com.dilip.transaction.repository.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;

import org.apache.commons.csv.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineService {

    private static final Set<String> CURRENCIES =
            Set.of("INR", "USD", "EUR", "GBP");

    private final AccountRepository accounts;
    private final CuratedRepository curated;
    private final RejectRepository rejects;
    private final PipelineRunRepository runs;

    public PipelineService(
            AccountRepository a,
            CuratedRepository c,
            RejectRepository q,
            PipelineRunRepository r) {
        accounts = a;
        curated = c;
        rejects = q;
        runs = r;
    }

    @Transactional
    public RunResponse process(Path path) throws IOException {

        String file = path.getFileName().toString();

        Optional<PipelineRun> old = runs.findBySourceFile(file);

        // Return the previous result if this file was already completed.
        if (old.isPresent()
                && old.get().getStatus().equals("COMPLETED")) {
            return RunResponse.from(old.get());
        }

        PipelineRun run;

        if (old.isPresent()) {
            run = old.get();
            run.restart();
            runs.saveAndFlush(run);
        } else {
            run = new PipelineRun(UUID.randomUUID().toString(), file);
            runs.saveAndFlush(run);
        }

        List<Row> validRows = new ArrayList<>();
        List<RejectedTransaction> bad = new ArrayList<>();

        int rowNo = 1;
        int input = 0;

        try (
            Reader reader = Files.newBufferedReader(
                    path, StandardCharsets.UTF_8);

            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build()
                    .parse(reader)
        ) {

            Set<String> headers = parser.getHeaderMap().keySet();

            if (!headers.containsAll(Set.of(
                    "event_id",
                    "transfer_id",
                    "leg_type",
                    "account_id",
                    "amount",
                    "currency",
                    "event_time"))) {

                throw new IllegalArgumentException("UNEXPECTED_SCHEMA");
            }
            Set<String> validAccountIds = new HashSet<>();

            for (Account accountRecord : accounts.findAll()) {
                validAccountIds.add(accountRecord.getAccountId());
            }

            for (CSVRecord rec : parser) {

                rowNo++;
                input++;

                String id = get(rec, "event_id");
                String raw = rec.toString();

                try {

                    String transfer = req(rec, "transfer_id");

                    String leg = req(rec, "leg_type")
                            .toUpperCase(Locale.ROOT);

                    String account = req(rec, "account_id");

                    BigDecimal amount =
                            new BigDecimal(req(rec, "amount"));

                    String currency = req(rec, "currency")
                            .toUpperCase(Locale.ROOT);

                    // Corrected timestamp parsing.
                    Instant time = parseEventTime(
                            req(rec, "event_time"));

                    if (id == null || id.isBlank()) {
                        throw new IllegalArgumentException(
                                "MISSING_EVENT_ID");
                    }

                    if (!Set.of("DEBIT", "CREDIT").contains(leg)) {
                        throw new IllegalArgumentException(
                                "INVALID_LEG_TYPE");
                    }

                    if (amount.signum() <= 0 || amount.scale() > 4) {
                        throw new IllegalArgumentException(
                                "INVALID_AMOUNT");
                    }

                    if (!CURRENCIES.contains(currency)) {
                        throw new IllegalArgumentException(
                                "UNSUPPORTED_CURRENCY");
                    }

                    if (!validAccountIds.contains(account)) {
                        throw new IllegalArgumentException("UNKNOWN_ACCOUNT");
                    }

//                    if (curated.existsByEventId(id)) {
//                        throw new IllegalArgumentException(
//                                "DUPLICATE_EVENT_ID");
//                    }

                    validRows.add(new Row(
                            rowNo,
                            id,
                            transfer,
                            leg,
                            account,
                            amount,
                            currency,
                            time,
                            raw
                    ));

                } catch (Exception ex) {

                    bad.add(reject(
                            run,
                            file,
                            rowNo,
                            id,
                            raw,
                            reason(ex)
                    ));
                }
            }

        } catch (Exception ex) {

            run.fail("INGESTION_ERROR: " + ex.getMessage());
            runs.save(run);

            return RunResponse.from(run);
        }

     // Collect event IDs from the input file.
        Set<String> eventIds = new HashSet<>();

        for (Row r : validRows) {
            eventIds.add(r.id);
        }

        // Check existing event IDs in MySQL using batches.
        Set<String> existingIds = new HashSet<>();

        List<String> eventIdList = new ArrayList<>(eventIds);

        int batchSize = 500;

        for (int i = 0; i < eventIdList.size(); i += batchSize) {

            Set<String> batch = new HashSet<>(
                    eventIdList.subList(
                            i,
                            Math.min(i + batchSize, eventIdList.size())
                    )
            );

            existingIds.addAll(
                    curated.findExistingEventIds(batch)
            );
        }

        // Detect duplicates in the database and current CSV.
        Set<String> seen = new HashSet<>();
        List<Row> unique = new ArrayList<>();

        for (Row r : validRows) {

            if (existingIds.contains(r.id)) {

                bad.add(reject(
                        run, file, r.row, r.id, r.raw,
                        "DUPLICATE_EVENT_ID"
                ));

            } else if (!seen.add(r.id)) {

                bad.add(reject(
                        run, file, r.row, r.id, r.raw,
                        "DUPLICATE_EVENT_ID_IN_BATCH"
                ));

            } else {
                unique.add(r);
            }
        }

        // Group debit and credit records by transfer ID.
        Map<String, List<Row>> groups = new LinkedHashMap<>();

        for (Row r : unique) {
            groups.computeIfAbsent(
                    r.transfer,
                    k -> new ArrayList<>()
            ).add(r);
        }

        List<CuratedTransaction> good = new ArrayList<>();

        // Validate each transfer.
        for (List<Row> legs : groups.values()) {

            String issue = validate(legs);

            if (issue != null) {

                for (Row r : legs) {
                    bad.add(reject(
                            run,
                            file,
                            r.row,
                            r.id,
                            r.raw,
                            issue
                    ));
                }

            } else {

                for (Row r : legs) {

                    good.add(new CuratedTransaction(
                            r.id,
                            r.transfer,
                            r.leg,
                            r.account,
                            r.amount,
                            r.currency,
                            r.time,
                            file,
                            run.getRunId()
                    ));
                }
            }
        }

        // Save curated and rejected records.
        curated.saveAll(good);
        rejects.saveAll(bad);

        run.complete(input, good.size(), bad.size());
        runs.save(run);

        return RunResponse.from(run);
    }

    // Supports timestamps with or without timezone suffix.
    private Instant parseEventTime(String value) {

        try {
            // Example: 2026-01-02T03:35:25Z
            return Instant.parse(value);

        } catch (DateTimeParseException e) {

            // Example: 2026-01-02T03:35:25
            return LocalDateTime.parse(value)
                    .toInstant(ZoneOffset.UTC);
        }
    }

    // Validate transfer debit and credit legs.
    private String validate(List<Row> legs) {

        if (legs.size() != 2) {
            return "TRANSFER_MUST_HAVE_EXACTLY_TWO_LEGS";
        }

        long d = legs.stream()
                .filter(x -> x.leg.equals("DEBIT"))
                .count();

        long c = legs.stream()
                .filter(x -> x.leg.equals("CREDIT"))
                .count();

        if (d != 1 || c != 1) {
            return "TRANSFER_REQUIRES_ONE_DEBIT_AND_ONE_CREDIT";
        }

        Row debit = legs.stream()
                .filter(x -> x.leg.equals("DEBIT"))
                .findFirst()
                .orElseThrow();

        Row credit = legs.stream()
                .filter(x -> x.leg.equals("CREDIT"))
                .findFirst()
                .orElseThrow();

        if (debit.amount.compareTo(credit.amount) != 0) {
            return "UNBALANCED_TRANSFER_AMOUNT";
        }

        if (!debit.currency.equals(credit.currency)) {
            return "CURRENCY_MISMATCH";
        }

        if (debit.account.equals(credit.account)) {
            return "SAME_SOURCE_AND_DESTINATION_ACCOUNT";
        }

        if (debit.time.isAfter(credit.time)) {
            return "OUT_OF_ORDER_TRANSFER_LEGS";
        }

        return null;
    }

    // Create rejected transaction record.
    private RejectedTransaction reject(
            PipelineRun r,
            String f,
            int n,
            String id,
            String raw,
            String why) {

        return new RejectedTransaction(
                r.getRunId(),
                f,
                n,
                id,
                raw,
                why
        );
    }

    // Read mandatory CSV fields.
    private String req(CSVRecord r, String k) {

        String v = get(r, k);

        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(
                    "MISSING_" + k.toUpperCase(Locale.ROOT));
        }

        return v;
    }

    // Safely read CSV fields.
    private String get(CSVRecord r, String k) {

        try {
            return r.isMapped(k) ? r.get(k) : null;

        } catch (Exception e) {
            return null;
        }
    }

    // Extract rejection reason.
    private String reason(Exception e) {

        String m = e.getMessage();

        if (m == null) {
            return "MALFORMED_RECORD";
        }

        return m.length() > 490 ? m.substring(0, 490) : m;
    }

    public List<CuratedTransaction> curatedForRun(String id) {
        return curated.findByRunId(id);
    }

    public List<RejectedTransaction> rejectsForRun(String id) {
        return rejects.findByRunId(id);
    }

    public Optional<PipelineRun> run(String id) {
        return runs.findById(id);
    }

    private record Row(
            int row,
            String id,
            String transfer,
            String leg,
            String account,
            BigDecimal amount,
            String currency,
            Instant time,
            String raw) {
    }
}
