package com.monitoring.transactions.Alerts;

import java.time.LocalDateTime;

public class Alerts {

	private Long id;
	private Long transactionId;
	private Long ruleId;
	private String alertReason;
	private String severity;
	private String oldStatus;
	private String newStatus;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Alerts() {
	}

	public Alerts(Long transactionId, Long ruleId, String alertReason, String severity, String oldStatus, String newStatus) {
		this.transactionId = transactionId;
		this.ruleId = ruleId;
		this.alertReason = alertReason;
		this.severity = severity;
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
	}

	public Alerts(Long id, Long transactionId, Long ruleId, String alertReason, String severity, String oldStatus,
				  String newStatus, LocalDateTime createdAt, LocalDateTime updatedAt) {
		this.id = id;
		this.transactionId = transactionId;
		this.ruleId = ruleId;
		this.alertReason = alertReason;
		this.severity = severity;
		this.oldStatus = oldStatus;
		this.newStatus = newStatus;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(Long transactionId) {
		this.transactionId = transactionId;
	}

	public Long getRuleId() {
		return ruleId;
	}

	public void setRuleId(Long ruleId) {
		this.ruleId = ruleId;
	}

	public String getAlertReason() {
		return alertReason;
	}

	public void setAlertReason(String alertReason) {
		this.alertReason = alertReason;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
