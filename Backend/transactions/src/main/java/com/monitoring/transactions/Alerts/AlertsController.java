package com.monitoring.transactions.Alerts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:8090}")
@RequestMapping("/alerts")
public class AlertsController {

	private final AlertsService alertsService;

	public AlertsController(AlertsService alertsService) {
		this.alertsService = alertsService;
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> createAlert(@RequestBody Alerts alert) {
		int rowsAffected = alertsService.createAlert(alert);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(successResponse("Alert created successfully", Map.of("rowsAffected", rowsAffected)));
	}

	@GetMapping
	public ResponseEntity<List<Alerts>> getAllAlerts(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String severity) {
		List<Alerts> alerts = alertsService.getAllAlerts(status, severity);
		return ResponseEntity.ok(alerts);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Alerts> getAlertById(@PathVariable Long id) {
		Alerts alert = alertsService.getAlertById(id);
		return ResponseEntity.ok(alert);
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<Map<String, Object>> updateAlertStatus(@PathVariable Long id,
			@RequestBody UpdateAlertStatusRequest request) {
		int rowsAffected = alertsService.updateAlertStatus(id, request.getOldStatus(), request.getNewStatus());
		return ResponseEntity.ok(successResponse("Alert status updated successfully", Map.of("rowsAffected", rowsAffected)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> deleteAlert(@PathVariable Long id) {
		int rowsAffected = alertsService.deleteAlert(id);
		return ResponseEntity.ok(successResponse("Alert deleted successfully", Map.of("rowsAffected", rowsAffected)));
	}

	private Map<String, Object> successResponse(String message, Map<String, Object> payload) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("success", true);
		response.put("message", message);
		response.putAll(payload);
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
