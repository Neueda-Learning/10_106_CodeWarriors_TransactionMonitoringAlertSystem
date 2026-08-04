package com.monitoring.transactions.Alerts;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Service
public class AlertsService {

	private final AlertsRepository alertsRepository;

	public AlertsService(AlertsRepository alertsRepository) {
		this.alertsRepository = alertsRepository;
	}

	public int createAlert(Alerts alert) {
		validateAlertForCreate(alert);

		try {
			int affectedRows = alertsRepository.saveAlert(alert);
			if (affectedRows <= 0) {
				throw new RuntimeException("Failed to create alert");
			}
			return affectedRows;
		} catch (DataAccessException exception) {
			throw new RuntimeException("Failed to create alert", exception);
		}
	}

	public List<Alerts> getAllAlerts() {
		try {
			return alertsRepository.getAllAlerts();
		} catch (DataAccessException exception) {
			throw new RuntimeException("Failed to fetch alerts", exception);
		}
	}

	public Alerts getAlertById(Long id) {
		validateAlertId(id);

		try {
			Alerts alert = alertsRepository.getAlertById(id);
			if (alert == null) {
				throw new RuntimeException("Alert not found");
			}
			return alert;
		} catch (EmptyResultDataAccessException exception) {
			throw new RuntimeException("Alert not found", exception);
		} catch (DataAccessException exception) {
			throw new RuntimeException("Failed to fetch alert", exception);
		}
	}

	public int updateAlertStatus(Long id, String oldStatus, String newStatus) {
		validateAlertId(id);
		validateStatus(oldStatus, newStatus);

		try {
			int affectedRows = alertsRepository.updateAlertStatus(id, oldStatus.trim(), newStatus.trim());
			if (affectedRows <= 0) {
				throw new RuntimeException("Alert not found");
			}
			return affectedRows;
		} catch (DataAccessException exception) {
			throw new RuntimeException("Failed to update alert", exception);
		}
	}

	public int deleteAlert(Long id) {
		validateAlertId(id);

		try {
			int affectedRows = alertsRepository.deleteAlert(id);
			if (affectedRows <= 0) {
				throw new RuntimeException("Alert not found");
			}
			return affectedRows;
		} catch (DataAccessException exception) {
			throw new RuntimeException("Failed to delete alert", exception);
		}
	}

	// Validation for create operation to ensure required foreign keys are provided.
	private void validateAlertForCreate(Alerts alert) {
		if (alert == null) {
			throw new RuntimeException("Alert object cannot be null");
		}

		if (alert.getTransactionId() == null) {
			throw new RuntimeException("Transaction ID cannot be null");
		}

		if (alert.getRuleId() == null) {
			throw new RuntimeException("Rule ID cannot be null");
		}
	}

	private void validateAlertId(Long id) {
		if (id == null) {
			throw new RuntimeException("Invalid Alert ID");
		}
	}

	private void validateStatus(String oldStatus, String newStatus) {
		if (oldStatus == null || oldStatus.isBlank()) {
			throw new RuntimeException("Status cannot be empty");
		}

		if (newStatus == null || newStatus.isBlank()) {
			throw new RuntimeException("Status cannot be empty");
		}
	}
}
