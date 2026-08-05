package com.monitoring.transactions.BankTransactions;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.monitoring.transactions.Alerts.Alerts;
import com.monitoring.transactions.Alerts.AlertsService;
import com.monitoring.transactions.Exception.GeneralizedException;
import com.monitoring.transactions.Rules.RuleEngineService;
import com.monitoring.transactions.Rules.Rules;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BankTransactionServices {

	private static final Set<String> ALLOWED_STATUSES = Set.of("COMPLETED", "PENDING", "FAILED");

	private final BankTransactionsRepository bankTransactionsRepository;
	private final RuleEngineService ruleEngineService;
	private final AlertsService alertsService;

	public BankTransactionServices(
		BankTransactionsRepository bankTransactionsRepository,
		RuleEngineService ruleEngineService,
		AlertsService alertsService) {
		this.bankTransactionsRepository = bankTransactionsRepository;
		this.ruleEngineService = ruleEngineService;
		this.alertsService = alertsService;
	}

	public List<BankTransactions> getAllTransactions() {
		try {
			return bankTransactionsRepository.findAll();
		} catch (DataAccessException exception) {
			throw new GeneralizedException(
				"Unable to fetch bank transactions.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public BankTransactions getTransactionById(Long id) {
		validateId(id);
		try {
			return bankTransactionsRepository.findById(id)
				.orElseThrow(() -> new GeneralizedException("Transaction not found for id: " + id, HttpStatus.NOT_FOUND));
		} catch (DataAccessException exception) {
			throw new GeneralizedException(
				"Unable to fetch bank transaction.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public BankTransactions createTransaction(BankTransactions transaction) {
		validateTransaction(transaction);
		try {
			List<Rules> triggeredRules = ruleEngineService.evaluateIncomingTransaction(transaction);
			String finalStatus = triggeredRules.isEmpty() ? "COMPLETED" : "PENDING";

			BankTransactions newTransaction = new BankTransactions(
				transaction.getFromAccountId(),
				transaction.getToAccountId(),
				transaction.getAmount(),
				transaction.getCurrency(),
				transaction.getTransactionTime(),
				finalStatus);

			BankTransactions savedTransaction = bankTransactionsRepository.save(newTransaction);

			for (Rules triggeredRule : triggeredRules) {
				Alerts generatedAlert = new Alerts(
					savedTransaction.getId(),
					triggeredRule.getId(),
					ruleEngineService.buildAlertReason(triggeredRule),
					triggeredRule.getSeverity() == null ? "MEDIUM" : triggeredRule.getSeverity(),
					"OPEN",
					"OPEN");
				alertsService.createAlert(generatedAlert);
			}

			return savedTransaction;
		} catch (DataAccessException exception) {
			throw new GeneralizedException(
				"Unable to create bank transaction.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (GeneralizedException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new GeneralizedException(
				"Unable to evaluate transaction against monitoring rules.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public BankTransactions updateTransaction(Long id, BankTransactions transaction) {
		validateId(id);
		validateTransaction(transaction);
		try {
			boolean updated = bankTransactionsRepository.update(id, transaction);
			if (!updated) {
				throw new GeneralizedException("Transaction not found for id: " + id, HttpStatus.NOT_FOUND);
			}
			return getTransactionById(id);
		} catch (DataAccessException exception) {
			throw new GeneralizedException(
				"Unable to update bank transaction.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public void deleteTransaction(Long id) {
		validateId(id);
		try {
			boolean deleted = bankTransactionsRepository.deleteById(id);
			if (!deleted) {
				throw new GeneralizedException("Transaction not found for id: " + id, HttpStatus.NOT_FOUND);
			}
		} catch (DataAccessException exception) {
			throw new GeneralizedException(
				"Unable to delete bank transaction.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void validateId(Long id) {
		if (id == null || id <= 0) {
			throw new GeneralizedException("Transaction id must be a positive number.", HttpStatus.BAD_REQUEST);
		}
	}

	private void validateTransaction(BankTransactions transaction) {
		if (transaction == null) {
			throw new GeneralizedException("Transaction payload is required.", HttpStatus.BAD_REQUEST);
		}

		Map<String, String> details = new LinkedHashMap<>();
		Long fromAccountId = transaction.getFromAccountId();
		Long toAccountId = transaction.getToAccountId();
		BigDecimal amount = transaction.getAmount();
		String currency = normalize(transaction.getCurrency());
		String status = normalize(transaction.getStatus());

		if (fromAccountId == null || fromAccountId <= 0) {
			details.put("fromAccountId", "fromAccountId must be a positive number.");
		}
		if (toAccountId == null || toAccountId <= 0) {
			details.put("toAccountId", "toAccountId must be a positive number.");
		}
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			details.put("amount", "amount must be greater than zero.");
		}
		if (currency == null) {
			details.put("currency", "currency is required.");
		} else if (currency.length() > 10) {
			details.put("currency", "currency must be at most 10 characters.");
		}
		if (transaction.getTransactionTime() == null) {
			details.put("transactionTime", "transactionTime is required.");
		}
		if (status != null) {
			String upperStatus = status.toUpperCase(Locale.ROOT);
			if (!ALLOWED_STATUSES.contains(upperStatus)) {
				details.put("status", "status must be one of COMPLETED, PENDING, FAILED.");
			} else {
				status = upperStatus;
			}
		}

		if (!details.isEmpty()) {
			throw new GeneralizedException("Invalid bank transaction input.", HttpStatus.BAD_REQUEST, details);
		}

		transaction.setCurrency(currency.toUpperCase(Locale.ROOT));
		transaction.setStatus(status == null ? "PENDING" : status);
	}

	private String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
