package com.monitoring.transactions.Alerts;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.monitoring.transactions.BankTransactions.BankTransactionsRepository;
import com.monitoring.transactions.Exception.GeneralizedException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AlertsService {

	private final AlertsRepository alertsRepository;
	private final BankTransactionsRepository bankTransactionsRepository;

	private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
		"OPEN", Set.of("ACKNOWLEDGED"),
		"ACKNOWLEDGED", Set.of("INVESTIGATING"),
		"INVESTIGATING", Set.of("CLOSED", "DISMISSED"),
		"CLOSED", Set.of(),
		"DISMISSED", Set.of());

	public AlertsService(AlertsRepository alertsRepository, BankTransactionsRepository bankTransactionsRepository) {
		this.alertsRepository = alertsRepository;
		this.bankTransactionsRepository = bankTransactionsRepository;
	}

	public int createAlert(Alerts alert) {
		validateAlertForCreate(alert);

		try {
			int affectedRows = alertsRepository.saveAlert(alert);
			if (affectedRows <= 0) {
				throw new GeneralizedException("Failed to create alert", HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return affectedRows;
		} catch (DataAccessException exception) {
			throw new GeneralizedException("Failed to create alert", exception, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public List<Alerts> getAllAlerts() {
		return getAllAlerts(null, null);
	}

	public List<Alerts> getAllAlerts(String status, String severity) {
		String normalizedStatus = normalizeFilter(status);
		String normalizedSeverity = normalizeFilter(severity);

		try {
			if (normalizedStatus == null && normalizedSeverity == null) {
				return alertsRepository.getAllAlerts();
			}
			return alertsRepository.getAlertsByFilters(normalizedStatus, normalizedSeverity);
		} catch (DataAccessException exception) {
			throw new GeneralizedException("Failed to fetch alerts", exception, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public Alerts getAlertById(Long id) {
		validateAlertId(id);

		try {
			Alerts alert = alertsRepository.getAlertById(id);
			if (alert == null) {
				throw new GeneralizedException("Alert not found", HttpStatus.NOT_FOUND);
			}
			return alert;
		} catch (EmptyResultDataAccessException exception) {
			throw new GeneralizedException("Alert not found", exception, HttpStatus.NOT_FOUND);
		} catch (DataAccessException exception) {
			throw new GeneralizedException("Failed to fetch alert", exception, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public int updateAlertStatus(Long id, String oldStatus, String newStatus) {
		validateAlertId(id);
		validateStatus(oldStatus, newStatus);

		try {
			Alerts existingAlert = alertsRepository.getAlertById(id);
			String currentStatus = normalizeStatus(existingAlert.getNewStatus());
			String expectedCurrentStatus = normalizeStatus(oldStatus);
			String targetStatus = normalizeStatus(newStatus);

			if (!currentStatus.equals(expectedCurrentStatus)) {
				throw new GeneralizedException("Alert status mismatch. Refresh data and retry.", HttpStatus.CONFLICT);
			}

			validateTransition(currentStatus, targetStatus);

			int affectedRows = alertsRepository.updateAlertStatus(id, currentStatus, targetStatus);
			if (affectedRows <= 0) {
				throw new GeneralizedException("Alert not found", HttpStatus.NOT_FOUND);
			}

			updateLinkedTransactionStatus(existingAlert.getTransactionId(), targetStatus);
			return affectedRows;
		} catch (EmptyResultDataAccessException exception) {
			throw new GeneralizedException("Alert not found", exception, HttpStatus.NOT_FOUND);
		} catch (DataAccessException exception) {
			throw new GeneralizedException("Failed to update alert", exception, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public int deleteAlert(Long id) {
		validateAlertId(id);

		try {
			int affectedRows = alertsRepository.deleteAlert(id);
			if (affectedRows <= 0) {
				throw new GeneralizedException("Alert not found", HttpStatus.NOT_FOUND);
			}
			return affectedRows;
		} catch (DataAccessException exception) {
			throw new GeneralizedException("Failed to delete alert", exception, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	
	private void validateAlertForCreate(Alerts alert) {
		if (alert == null) {
			throw new GeneralizedException("Alert object cannot be null", HttpStatus.BAD_REQUEST);
		}

		if (alert.getTransactionId() == null) {
			throw new GeneralizedException("Transaction ID cannot be null", HttpStatus.BAD_REQUEST);
		}

		if (alert.getRuleId() == null) {
			throw new GeneralizedException("Rule ID cannot be null", HttpStatus.BAD_REQUEST);
		}
	}

	private void validateAlertId(Long id) {
		if (id == null) {
			throw new GeneralizedException("Invalid Alert ID", HttpStatus.BAD_REQUEST);
		}
	}

	private void validateStatus(String oldStatus, String newStatus) {
		if (oldStatus == null || oldStatus.isBlank()) {
			throw new GeneralizedException("Status cannot be empty", HttpStatus.BAD_REQUEST);
		}

		if (newStatus == null || newStatus.isBlank()) {
			throw new GeneralizedException("Status cannot be empty", HttpStatus.BAD_REQUEST);
		}
	}

	private void validateTransition(String currentStatus, String targetStatus) {
		Set<String> allowedTargets = ALLOWED_TRANSITIONS.get(currentStatus);
		if (allowedTargets == null || !allowedTargets.contains(targetStatus)) {
			throw new GeneralizedException(
				"Invalid alert status transition from " + currentStatus + " to " + targetStatus + ".",
				HttpStatus.BAD_REQUEST);
		}
	}

	private String normalizeStatus(String status) {
		if (status == null || status.isBlank()) {
			return "OPEN";
		}
		return status.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeFilter(String value) {
		if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim())) {
			return null;
		}
		return value.trim().toUpperCase(Locale.ROOT);
	}

	private void updateLinkedTransactionStatus(Long transactionId, String targetStatus) {
		if (transactionId == null) {
			return;
		}

		String transactionStatus = null;
		if ("CLOSED".equals(targetStatus)) {
			transactionStatus = "COMPLETED";
		}
		if ("DISMISSED".equals(targetStatus)) {
			transactionStatus = "FAILED";
		}
		if ("ACKNOWLEDGED".equals(targetStatus) || "INVESTIGATING".equals(targetStatus) || "OPEN".equals(targetStatus)) {
			transactionStatus = "PENDING";
		}

		if (transactionStatus != null) {
			boolean updated = bankTransactionsRepository.updateStatus(transactionId, transactionStatus);
			if (!updated) {
				throw new GeneralizedException("Linked transaction not found", HttpStatus.NOT_FOUND);
			}
		}
	}
}
