package com.monitoring.transactions.Rules;

import java.math.BigDecimal;

public class Rules {

	private Long id;
	private String name;
	private String type;
	private BigDecimal threshold;
	private Integer timeWindow;
	private Integer maxTransactions;
	private String severity;
	private Boolean active;

	public Rules() {
	}

	public Rules(String name, String type, BigDecimal threshold, Integer timeWindow, Integer maxTransactions,
				 String severity, Boolean active) {
		this.name = name;
		this.type = type;
		this.threshold = threshold;
		this.timeWindow = timeWindow;
		this.maxTransactions = maxTransactions;
		this.severity = severity;
		this.active = active;
	}

	public Rules(Long id, String name, String type, BigDecimal threshold, Integer timeWindow, Integer maxTransactions,
				 String severity, Boolean active) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.threshold = threshold;
		this.timeWindow = timeWindow;
		this.maxTransactions = maxTransactions;
		this.severity = severity;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public BigDecimal getThreshold() {
		return threshold;
	}

	public void setThreshold(BigDecimal threshold) {
		this.threshold = threshold;
	}

	public Integer getTimeWindow() {
		return timeWindow;
	}

	public void setTimeWindow(Integer timeWindow) {
		this.timeWindow = timeWindow;
	}

	public Integer getMaxTransactions() {
		return maxTransactions;
	}

	public void setMaxTransactions(Integer maxTransactions) {
		this.maxTransactions = maxTransactions;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
