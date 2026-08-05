package com.monitoring.transactions.BankTransactions;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Random;

@Component
public class LiveTransactionSimulator {

    private final BankTransactionServices bankTransactionServices;
    private final Random random = new Random();

    public LiveTransactionSimulator(BankTransactionServices bankTransactionServices) {
        this.bankTransactionServices = bankTransactionServices;
    }

    // Runs every 60 seconds (60000 ms)
    @Scheduled(fixedRate = 60000)
    public void generateLiveTransactions() {
        System.out.println("LiveTransactionSimulator: Generating 5 new transactions...");
        
        String[] statuses = {"COMPLETED", "PENDING", "FAILED"};
        
        for (int i = 0; i < 5; i++) {
            BankTransactions tx = new BankTransactions();
            
            // Random accounts from seeded dataset range.
            long fromAccount = random.nextInt(20) + 1;
            long toAccount = random.nextInt(20) + 1;
            while (fromAccount == toAccount) {
                toAccount = random.nextInt(20) + 1;
            }
            
            tx.setFromAccountId(fromAccount);
            tx.setToAccountId(toAccount);
            
            // Random amount between 10.00 and 15000.00
            double amountDouble = 10.00 + (14990.00 * random.nextDouble());
            BigDecimal amount = new BigDecimal(amountDouble).setScale(2, RoundingMode.HALF_UP);
            tx.setAmount(amount);
            
            tx.setCurrency("USD");
            tx.setTransactionTime(LocalDateTime.now());
            
            // 80% Completed, 10% Pending, 10% Failed
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
        System.out.println("LiveTransactionSimulator: 5 transactions inserted successfully.");
    }
}
