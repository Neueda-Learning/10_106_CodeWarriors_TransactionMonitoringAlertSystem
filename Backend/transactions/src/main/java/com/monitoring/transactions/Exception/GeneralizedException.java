package com.monitoring.transactions.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

public class GeneralizedException extends RuntimeException {

	private final HttpStatus status;
	private final Map<String, String> details;

	public GeneralizedException(String message) {
		this(message, HttpStatus.BAD_REQUEST);
	}

	public GeneralizedException(String message, HttpStatus status) {
		this(message, status, Collections.emptyMap());
	}

	public GeneralizedException(String message, HttpStatus status, Map<String, String> details) {
		super(message);
		this.status = status;
		this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
	}

	public GeneralizedException(String message, Throwable cause, HttpStatus status) {
		this(message, cause, status, Collections.emptyMap());
	}

	public GeneralizedException(String message, Throwable cause, HttpStatus status, Map<String, String> details) {
		super(message, cause);
		this.status = status;
		this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
	}

	public HttpStatus getStatus() {
		return status;
	}

	public Map<String, String> getDetails() {
		return details;
	}
}

