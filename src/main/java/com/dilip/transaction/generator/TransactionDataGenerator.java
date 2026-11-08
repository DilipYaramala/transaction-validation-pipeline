
package com.dilip.transaction.generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

public class TransactionDataGenerator {

    private static final String[] ACCOUNTS = {
        "ACC1001", "ACC1002", "ACC1003",
        "ACC1004", "ACC1005"
    };

    private static final String[] CURRENCIES = {
        "INR", "USD", "EUR", "GBP"
    };

    private static final Random RANDOM = new Random(42);

    private static final Path OUTPUT =
    	    Path.of("src", "main", "resources", "data",
    	            "generated_transactions.csv");
    public static void main(String[] args) {

        int transferCount = args.length > 0
                ? Integer.parseInt(args[0]) : 50_000;

        int invalidEvery = args.length > 1
                ? Integer.parseInt(args[1]) : 0;

        if (transferCount <= 0 || invalidEvery < 0) {
            throw new IllegalArgumentException(
                "Transfer count must be positive and invalid interval cannot be negative."
            );
        }

        try {
            generateTransactions(transferCount, invalidEvery);
        } catch (IOException e) {
            System.err.println("Generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateTransactions(
            int transferCount, int invalidEvery) throws IOException {

        Files.createDirectories(OUTPUT.getParent());

        LocalDateTime baseTime =
                LocalDateTime.of(2026, 1, 1, 0, 0, 0);

        try (BufferedWriter writer = Files.newBufferedWriter(
                OUTPUT, StandardCharsets.UTF_8)) {

            // CSV header
            writer.write(
                "event_id,transfer_id,leg_type,account_id,amount,currency,event_time"
            );
            writer.newLine();
            String uniquePrefix = UUID.randomUUID()
                    .toString()
                    .substring(0, 8);
            System.out.println("Generated batch prefix: " + uniquePrefix);

            for (int i = 1; i <= transferCount; i++) {

            	String transferId = uniquePrefix + "-TRF-" + i;
                String debitAccount = ACCOUNTS[
                    RANDOM.nextInt(ACCOUNTS.length)
                ];

                String creditAccount;

                do {
                    creditAccount = ACCOUNTS[
                        RANDOM.nextInt(ACCOUNTS.length)
                    ];
                } while (debitAccount.equals(creditAccount));

                String currency = CURRENCIES[
                    RANDOM.nextInt(CURRENCIES.length)
                ];

                BigDecimal debitAmount = BigDecimal.valueOf(
                    RANDOM.nextInt(100000) + 100, 2
                );

                BigDecimal creditAmount = debitAmount;

                // Introduce an invalid amount mismatch when requested
                if (invalidEvery > 0 && i % invalidEvery == 0) {
                    creditAmount = debitAmount.add(
                        new BigDecimal("1.00")
                    );
                }

                String debitEventId = uniquePrefix + "-EVT-" + i + "-D";

                String creditEventId = uniquePrefix + "-EVT-" + i + "-C";
                LocalDateTime debitTime =
                        baseTime.plusSeconds(i * 2L);

                LocalDateTime creditTime =
                        debitTime.plusSeconds(1);

                // Write DEBIT leg
                writer.write(String.join(",",
                    debitEventId,
                    transferId,
                    "DEBIT",
                    debitAccount,
                    debitAmount.toPlainString(),
                    currency,
                    debitTime.toString()
                ));
                writer.newLine();

                // Write CREDIT leg
                writer.write(String.join(",",
                    creditEventId,
                    transferId,
                    "CREDIT",
                    creditAccount,
                    creditAmount.toPlainString(),
                    currency,
                    creditTime.toString()
                ));
                writer.newLine();
            }
        }

        System.out.println("Transaction generation completed!");
        System.out.println("Output file: " + OUTPUT.toAbsolutePath());
        System.out.println("Transfer pairs: " + transferCount);
        System.out.println("Total records: " + (transferCount * 2));
        System.out.println("Invalid transfer pairs: "
                + (invalidEvery == 0
                   ? 0 : transferCount / invalidEvery));
    }
}
