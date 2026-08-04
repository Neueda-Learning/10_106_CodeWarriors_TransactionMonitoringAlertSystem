package com.monitoring.transactions.Accounts;

import java.time.LocalDateTime;

public class Accounts {

	private Long id;
	private String customerName;
	private String accountType;
	private String country;
	private LocalDateTime createdAt;

	public Accounts() {
	}

	public Accounts(String customerName, String accountType, String country) {
		this.customerName = customerName;
		this.accountType = accountType;
		this.country = country;
	}

	public Accounts(Long id, String customerName, String accountType, String country, LocalDateTime createdAt) {
		this.id = id;
		this.customerName = customerName;
		this.accountType = accountType;
		this.country = country;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getAccountType() {
		return accountType;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
