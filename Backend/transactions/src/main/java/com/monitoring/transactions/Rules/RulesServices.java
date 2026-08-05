package com.monitoring.transactions.Rules;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.monitoring.transactions.Alerts.Alerts;
import com.monitoring.transactions.Alerts.AlertsRepository;
import com.monitoring.transactions.Alerts.AlertsService;
import com.monitoring.transactions.BankTransactions.BankTransactions;
import com.monitoring.transactions.BankTransactions.BankTransactionsRepository;
import com.monitoring.transactions.Exception.GeneralizedException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RulesServices {

	private final RulesRepository rulesRepository;
	private final AlertsService alertsService;
	private final AlertsRepository alertsRepository;
	private final BankTransactionsRepository bankTransactionsRepository;
	private final RuleEngineService ruleEngineService;

	public RulesServices(
		RulesRepository rulesRepository,
		AlertsService alertsService,
		AlertsRepository alertsRepository,
		BankTransactionsRepository bankTransactionsRepository,
		RuleEngineService ruleEngineService) {
		this.rulesRepository = rulesRepository;
		this.alertsService = alertsService;
		this.alertsRepository = alertsRepository;
		this.bankTransactionsRepository = bankTransactionsRepository;
		this.ruleEngineService = ruleEngineService;
	}

	public List<Rules> getAllRules() {
		try {
			return rulesRepository.findAll();
		} catch (DataAccessException exception) {
			throw new GeneralizedException(
				"Unable to fetch rules.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public Rules getRuleById(Long id) {
		validateId(id);
		try {
			return rulesRepository.findById(id)
				.orElseThrow(() -> new GeneralizedException("Rule not found for id: " + id, HttpStatus.NOT_FOUND));
		} catch (DataAccessException exception) {
			throw new GeneralizedException(
				"Unable to fetch rule.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public Rules createRule(Rules rule) {
		validateRule(rule);
		try {
			Rules newRule = new Rules(
				rule.getName(),
				rule.getType(),
				rule.getThreshold(),
				rule.getTimeWindow(),
				rule.getMaxTransactions(),
				rule.getSeverity(),
				rule.getActive());
			Rules saved = rulesRepository.save(newRule);
			synchronizeAlertsForRule(saved);
			return saved;
		} catch (DataAccessException exception) {
			throw new GeneralizedException(
				"Unable to create rule.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public Rules updateRule(Long id, Rules rule) {
		validateId(id);
		validateRule(rule);
		try {
			boolean updated = rulesRepository.update(id, rule);
			if (!updated) {
				throw new GeneralizedException("Rule not found for id: " + id, HttpStatus.NOT_FOUND);
			}
			Rules updatedRule = getRuleById(id);
			synchronizeAlertsForRule(updatedRule);
			return updatedRule;
		} catch (DataAccessException exception) {
			throw new GeneralizedException(
				"Unable to update rule.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public void deleteRule(Long id) {
		validateId(id);
		try {
			alertsRepository.deleteAlertsByRuleId(id);
			boolean deleted = rulesRepository.deleteById(id);
			if (!deleted) {
				throw new GeneralizedException("Rule not found for id: " + id, HttpStatus.NOT_FOUND);
			}
		} catch (DataAccessException exception) {
			throw new GeneralizedException(
				"Unable to delete rule.",
				exception,
				HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void validateId(Long id) {
		if (id == null || id <= 0) {
			throw new GeneralizedException("Rule id must be a positive number.", HttpStatus.BAD_REQUEST);
		}
	}


	private void validateRule(Rules rule) {
		if (rule == null) {
			throw new GeneralizedException("Rule payload is required.", HttpStatus.BAD_REQUEST);
		}

		Map<String, String> details = new LinkedHashMap<>();
		String name = normalize(rule.getName());
		String type = normalize(rule.getType());
		String severity = normalize(rule.getSeverity());

		if (name == null) {
			details.put("name", "name is required.");
		} else if (name.length() > 100) {
			details.put("name", "name must be at most 100 characters.");
		}

		if (type == null) {
			details.put("type", "type is required.");
		} else if (type.length() > 50) {
			details.put("type", "type must be at most 50 characters.");
		}

		BigDecimal threshold = rule.getThreshold();
		if (threshold != null && threshold.compareTo(BigDecimal.ZERO) < 0) {
			details.put("threshold", "threshold must be greater than or equal to zero.");
		}

		Integer timeWindow = rule.getTimeWindow();
		if (timeWindow != null && timeWindow <= 0) {
			details.put("timeWindow", "timeWindow must be greater than zero.");
		}

		Integer maxTransactions = rule.getMaxTransactions();
		if (maxTransactions != null && maxTransactions <= 0) {
			details.put("maxTransactions", "maxTransactions must be greater than zero.");
		}

		if (severity != null && severity.length() > 20) {
			details.put("severity", "severity must be at most 20 characters.");
		}

		if (!details.isEmpty()) {
			throw new GeneralizedException("Invalid rule input.", HttpStatus.BAD_REQUEST, details);
		}

		rule.setName(name);
		rule.setType(type.toUpperCase(Locale.ROOT));
		rule.setSeverity(severity == null ? null : severity.toUpperCase(Locale.ROOT));
		rule.setActive(rule.getActive() == null ? Boolean.TRUE : rule.getActive());
	}

	private String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private void synchronizeAlertsForRule(Rules rule) {
		if (rule == null || rule.getId() == null || !Boolean.TRUE.equals(rule.getActive())) {
			return;
		}

		List<BankTransactions> transactions = bankTransactionsRepository.findAll();
		for (BankTransactions transaction : transactions) {
			if (transaction.getId() == null) {
				continue;
			}
			boolean matches = ruleEngineService.matchesPersistedTransaction(transaction, rule, transactions);
			if (!matches) {
				continue;
			}
			boolean alreadyTracked = alertsRepository.hasActiveAlertForTransactionAndRule(transaction.getId(), rule.getId());
			if (alreadyTracked) {
				continue;
			}

			Alerts alert = new Alerts(
				transaction.getId(),
				rule.getId(),
				ruleEngineService.buildAlertReason(rule),
				rule.getSeverity() == null ? "MEDIUM" : rule.getSeverity(),
				"OPEN",
				"OPEN");
			alertsService.createAlert(alert);

			if (!"PENDING".equalsIgnoreCase(transaction.getStatus())) {
				bankTransactionsRepository.updateStatus(transaction.getId(), "PENDING");
			}
		}
	}
}
