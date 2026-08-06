package com.monitoring.transactions.BankTransactions;

import com.monitoring.transactions.Alerts.AlertsRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Random;

@Component
public class LiveTransactionSimulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveTransactionSimulator.class);
    private static final int MAX_TRANSACTIONS = 700;
    private static final int BATCH_SIZE = 5;

    private final BankTransactionServices bankTransactionServices;
    private final BankTransactionsRepository bankTransactionsRepository;
    private final AlertsRepository alertsRepository;
    private final Random random = new Random();

    public LiveTransactionSimulator(
            BankTransactionServices bankTransactionServices,
            BankTransactionsRepository bankTransactionsRepository,
            AlertsRepository alertsRepository) {
        this.bankTransactionServices = bankTransactionServices;
        this.bankTransactionsRepository = bankTransactionsRepository;
        this.alertsRepository = alertsRepository;
    }

    
    @Scheduled(fixedRate = 60000)
    public void generateLiveTransactions() {
        enforceTransactionCap();
        LOGGER.info("LiveTransactionSimulator: Generating {} new transactions...", BATCH_SIZE);

        for (int i = 0; i < BATCH_SIZE; i++) {
            BankTransactions tx = new BankTransactions();
            
            
            long fromAccount = random.nextInt(20) + 1;
            long toAccount = random.nextInt(20) + 1;
            while (fromAccount == toAccount) {
                toAccount = random.nextInt(20) + 1;
            }
            
            tx.setFromAccountId(fromAccount);
            tx.setToAccountId(toAccount);
            
            
            double amountDouble = 10.00 + (14990.00 * random.nextDouble());
            BigDecimal amount = new BigDecimal(amountDouble).setScale(2, RoundingMode.HALF_UP);
            tx.setAmount(amount);
            
            tx.setCurrency("USD");
            tx.setTransactionTime(LocalDateTime.now());
            
            
            int statusRoll = random.nextInt(10);
            if (statusRoll < 8) {
                tx.setStatus("COMPLETED");
            } else if (statusRoll == 8) {
                tx.setStatus("PENDING");
            } else {
                tx.setStatus("FAILED");
            }
            
            bankTransactionServices.createTransaction(tx);
        }
        LOGGER.info("LiveTransactionSimulator: {} transactions inserted successfully.", BATCH_SIZE);
    }

    private void enforceTransactionCap() {
        long currentCount = bankTransactionsRepository.countAllTransactions();
        if (currentCount < MAX_TRANSACTIONS) {
            return;
        }

        // Alerts reference transactions via FK, so remove alerts first before truncating transactions.
        alertsRepository.deleteAllAlerts();
        bankTransactionsRepository.truncateAllTransactions();
        LOGGER.warn("LiveTransactionSimulator: Transaction cap reached ({}). Alerts deleted and transactions truncated.", currentCount);
    }
}
