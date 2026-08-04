package com.monitoring.transactions.Alerts;

import java.util.List;

import com.monitoring.transactions.Exception.GeneralizedException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
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
				throw new GeneralizedException("Failed to create alert", HttpStatus.INTERNAL_SERVER_ERROR);
			}
			return affectedRows;
		} catch (DataAccessException exception) {
			throw new GeneralizedException("Failed to create alert", exception, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public List<Alerts> getAllAlerts() {
		try {
			return alertsRepository.getAllAlerts();
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
			int affectedRows = alertsRepository.updateAlertStatus(id, oldStatus.trim(), newStatus.trim());
			if (affectedRows <= 0) {
				throw new GeneralizedException("Alert not found", HttpStatus.NOT_FOUND);
			}
			return affectedRows;
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

	// Validation for create operation to ensure required foreign keys are provided.
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
}
