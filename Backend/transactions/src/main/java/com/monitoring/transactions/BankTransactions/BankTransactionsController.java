package com.monitoring.transactions.BankTransactions;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/transactions")
public class BankTransactionsController {

	private final BankTransactionServices bankTransactionServices;

	public BankTransactionsController(BankTransactionServices bankTransactionServices) {
		this.bankTransactionServices = bankTransactionServices;
	}

	@GetMapping
	public List<BankTransactions> getAllTransactions() {
		return bankTransactionServices.getAllTransactions();
	}

	@GetMapping("/{id}")
	public BankTransactions getTransactionById(@PathVariable Long id) {
		return bankTransactionServices.getTransactionById(id);
	}

	@PostMapping
	public ResponseEntity<BankTransactions> createTransaction(@RequestBody BankTransactions transaction) {
		BankTransactions created = bankTransactionServices.createTransaction(transaction);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PutMapping("/{id}")
	public BankTransactions updateTransaction(@PathVariable Long id, @RequestBody BankTransactions transaction) {
		return bankTransactionServices.updateTransaction(id, transaction);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
		bankTransactionServices.deleteTransaction(id);
		return ResponseEntity.noContent().build();
	}
}
