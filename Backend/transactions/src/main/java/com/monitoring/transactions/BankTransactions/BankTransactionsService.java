package com.monitoring.transactions.BankTransactions;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BankTransactionsService {

    private final BankTransactionsRepository repository;

    public BankTransactionsService(BankTransactionsRepository repository) {
        this.repository = repository;
    }

    public List<BankTransactions> getAllTransactions() {
        return repository.findAll();
    }
}
