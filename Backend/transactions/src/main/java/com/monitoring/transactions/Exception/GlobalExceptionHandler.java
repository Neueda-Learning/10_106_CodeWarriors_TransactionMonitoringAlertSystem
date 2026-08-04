package com.monitoring.transactions.Exception;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(GeneralizedException.class)
	public ResponseEntity<ErrorResponse> handleGeneralizedException(GeneralizedException exception,
			HttpServletRequest request) {
		return buildResponse(
			exception.getStatus(),
			exception.getMessage(),
			request.getRequestURI(),
			exception.getDetails());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		Map<String, String> validationErrors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
			.forEach(error -> validationErrors.put(error.getField(), error.getDefaultMessage()));
		return buildResponse(
			HttpStatus.BAD_REQUEST,
			"Validation failed for one or more fields.",
			request.getRequestURI(),
			validationErrors);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception,
			HttpServletRequest request) {
		Map<String, String> details = Map.of(
			exception.getParameterName(),
			"Required request parameter is missing.");
		return buildResponse(
			HttpStatus.BAD_REQUEST,
			exception.getMessage(),
			request.getRequestURI(),
			details);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
			HttpServletRequest request) {
		String expectedType = exception.getRequiredType() != null
			? exception.getRequiredType().getSimpleName()
			: "the expected type";
		Map<String, String> details = Map.of(
			exception.getName(),
			"Value '" + exception.getValue() + "' is not valid for type " + expectedType + ".");
		return buildResponse(
			HttpStatus.BAD_REQUEST,
			"Request parameter type mismatch.",
			request.getRequestURI(),
			details);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return buildResponse(
			HttpStatus.BAD_REQUEST,
			"Malformed request body. Please check the JSON payload and try again.",
			request.getRequestURI(),
			Collections.emptyMap());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception,
			HttpServletRequest request) {
		return buildResponse(
			HttpStatus.BAD_REQUEST,
			exception.getMessage(),
			request.getRequestURI(),
			Collections.emptyMap());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception,
			HttpServletRequest request) {
		HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
		String message = exception.getReason() != null ? exception.getReason() : "Request could not be processed.";
		return buildResponse(status, message, request.getRequestURI(), Collections.emptyMap());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnhandledException(Exception exception, HttpServletRequest request) {
		return buildResponse(
			HttpStatus.INTERNAL_SERVER_ERROR,
			"An unexpected error occurred. Please try again later.",
			request.getRequestURI(),
			Collections.emptyMap());
	}

	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path,
			Map<String, String> details) {
		ErrorResponse body = new ErrorResponse(
			LocalDateTime.now(),
			status.value(),
			status.getReasonPhrase(),
			message,
			path,
			details == null ? Collections.emptyMap() : new LinkedHashMap<>(details));
		return ResponseEntity.status(status).body(body);
	}

	public static class ErrorResponse {

		private final LocalDateTime timestamp;
		private final int status;
		private final String error;
		private final String message;
		private final String path;
		private final Map<String, String> details;

		public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path,
				Map<String, String> details) {
			this.timestamp = timestamp;
			this.status = status;
			this.error = error;
			this.message = message;
			this.path = path;
			this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}

		public int getStatus() {
			return status;
		}

		public String getError() {
			return error;
		}

		public String getMessage() {
			return message;
		}

		public String getPath() {
			return path;
		}

		public Map<String, String> getDetails() {
			return details;
		}
	}
}

