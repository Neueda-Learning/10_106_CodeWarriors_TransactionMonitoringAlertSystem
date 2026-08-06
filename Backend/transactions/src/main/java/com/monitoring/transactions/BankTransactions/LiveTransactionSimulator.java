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

    
    @Scheduled(fixedRate = 60000)
    public void generateLiveTransactions() {
        System.out.println("LiveTransactionSimulator: Generating 5 new transactions...");
        
        String[] statuses = {"COMPLETED", "PENDING", "FAILED"};
        
        for (int i = 0; i < 5; i++) {
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
        System.out.println("LiveTransactionSimulator: 5 transactions inserted successfully.");
    }
}
