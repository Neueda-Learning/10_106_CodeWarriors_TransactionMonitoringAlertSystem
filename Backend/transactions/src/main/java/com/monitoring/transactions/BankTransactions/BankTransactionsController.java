package com.monitoring.transactions.BankTransactions;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*") // Allows the frontend to call this API locally
public class BankTransactionsController {

    private final BankTransactionsService service;

    public BankTransactionsController(BankTransactionsService service) {
        this.service = service;
    }

    @GetMapping
    public List<BankTransactions> getAllTransactions() {
        return service.getAllTransactions();
    }
}
