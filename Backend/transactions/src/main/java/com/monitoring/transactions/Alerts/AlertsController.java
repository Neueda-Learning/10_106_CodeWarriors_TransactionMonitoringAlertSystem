package com.monitoring.transactions.Alerts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alerts")
public class AlertsController {

	private final AlertsService alertsService;

	public AlertsController(AlertsService alertsService) {
		this.alertsService = alertsService;
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> createAlert(@RequestBody Alerts alert) {
		try {
			int rowsAffected = alertsService.createAlert(alert);
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(successResponse("Alert created successfully", Map.of("rowsAffected", rowsAffected)));
		} catch (RuntimeException exception) {
			return handleRuntimeException(exception, "Failed to create alert");
		} catch (Exception exception) {
			return internalServerError("Failed to create alert", exception);
		}
	}

	@GetMapping
	public ResponseEntity<?> getAllAlerts() {
		try {
			List<Alerts> alerts = alertsService.getAllAlerts();
			return ResponseEntity.ok(alerts);
		} catch (RuntimeException exception) {
			return handleRuntimeException(exception, "Failed to fetch alerts");
		} catch (Exception exception) {
			return internalServerError("Failed to fetch alerts", exception);
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getAlertById(@PathVariable Long id) {
		try {
			Alerts alert = alertsService.getAlertById(id);
			return ResponseEntity.ok(alert);
		} catch (RuntimeException exception) {
			return handleRuntimeException(exception, "Failed to fetch alert");
		} catch (Exception exception) {
			return internalServerError("Failed to fetch alert", exception);
		}
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<Map<String, Object>> updateAlertStatus(@PathVariable Long id,
			@RequestBody UpdateAlertStatusRequest request) {
		try {
			int rowsAffected = alertsService.updateAlertStatus(id, request.getOldStatus(), request.getNewStatus());
			return ResponseEntity.ok(successResponse("Alert status updated successfully", Map.of("rowsAffected", rowsAffected)));
		} catch (RuntimeException exception) {
			return handleRuntimeException(exception, "Failed to update alert");
		} catch (Exception exception) {
			return internalServerError("Failed to update alert", exception);
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> deleteAlert(@PathVariable Long id) {
		try {
			int rowsAffected = alertsService.deleteAlert(id);
			return ResponseEntity.ok(successResponse("Alert deleted successfully", Map.of("rowsAffected", rowsAffected)));
		} catch (RuntimeException exception) {
			return handleRuntimeException(exception, "Failed to delete alert");
		} catch (Exception exception) {
			return internalServerError("Failed to delete alert", exception);
		}
	}

	private ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException exception, String fallbackMessage) {
		String message = exception.getMessage() == null || exception.getMessage().isBlank()
				? fallbackMessage
				: exception.getMessage();

		HttpStatus status = resolveStatus(message);
		return ResponseEntity.status(status).body(errorResponse(message));
	}

	private ResponseEntity<Map<String, Object>> internalServerError(String message, Exception exception) {
		String errorMessage = exception.getMessage() == null || exception.getMessage().isBlank()
				? message
				: exception.getMessage();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse(errorMessage));
	}

	private HttpStatus resolveStatus(String message) {
		if ("Alert not found".equalsIgnoreCase(message)) {
			return HttpStatus.NOT_FOUND;
		}

		if ("Invalid Alert ID".equalsIgnoreCase(message)
				|| "Status cannot be empty".equalsIgnoreCase(message)
				|| "Alert object cannot be null".equalsIgnoreCase(message)
				|| "Transaction ID cannot be null".equalsIgnoreCase(message)
				|| "Rule ID cannot be null".equalsIgnoreCase(message)) {
			return HttpStatus.BAD_REQUEST;
		}

		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

	private Map<String, Object> successResponse(String message, Map<String, Object> payload) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("success", true);
		response.put("message", message);
		response.putAll(payload);
		return response;
	}

	private Map<String, Object> errorResponse(String message) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("success", false);
		response.put("message", message);
		return response;
	}

	public static class UpdateAlertStatusRequest {

		private String oldStatus;
		private String newStatus;

		public String getOldStatus() {
			return oldStatus;
		}

		public void setOldStatus(String oldStatus) {
			this.oldStatus = oldStatus;
		}

		public String getNewStatus() {
			return newStatus;
		}

		public void setNewStatus(String newStatus) {
			this.newStatus = newStatus;
		}
	}
}
