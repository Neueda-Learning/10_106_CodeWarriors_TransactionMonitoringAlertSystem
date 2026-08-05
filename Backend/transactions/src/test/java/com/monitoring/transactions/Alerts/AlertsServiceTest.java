package com.monitoring.transactions.Alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import com.monitoring.transactions.BankTransactions.BankTransactionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;

@ExtendWith(MockitoExtension.class)
public class AlertsServiceTest {

	@Mock
	private AlertsRepository alertsRepository;

	@Mock
	private Alerts alert;

	@Mock
	private BankTransactionsRepository bankTransactionsRepository;

	private AlertsService alertsService;

	@BeforeEach
	void setUp() {
		alertsService = new AlertsService(alertsRepository, bankTransactionsRepository);
	}

	@Test
	void createAlert_shouldThrowWhenAlertIsNull() {
		RuntimeException exception = assertThrows(RuntimeException.class, () -> alertsService.createAlert(null));
		assertEquals("Alert object cannot be null", exception.getMessage());
		verifyNoInteractions(alertsRepository);
	}

	@Test
	void createAlert_shouldThrowWhenTransactionIdIsNull() {
		when(alert.getTransactionId()).thenReturn(null);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> alertsService.createAlert(alert));
		assertEquals("Transaction ID cannot be null", exception.getMessage());
		verifyNoInteractions(alertsRepository);
	}

	@Test
	void createAlert_shouldThrowWhenRuleIdIsNull() {
		when(alert.getTransactionId()).thenReturn(1L);
		when(alert.getRuleId()).thenReturn(null);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> alertsService.createAlert(alert));
		assertEquals("Rule ID cannot be null", exception.getMessage());
		verifyNoInteractions(alertsRepository);
	}

	@Test
	void createAlert_shouldReturnAffectedRowsWhenRepositorySucceeds() {
		when(alert.getTransactionId()).thenReturn(1L);
		when(alert.getRuleId()).thenReturn(2L);
		when(alertsRepository.saveAlert(alert)).thenReturn(1);

		int result = alertsService.createAlert(alert);

		assertEquals(1, result);
		verify(alertsRepository).saveAlert(alert);
	}

	@Test
	void createAlert_shouldThrowWhenRepositoryReturnsZero() {
		when(alert.getTransactionId()).thenReturn(1L);
		when(alert.getRuleId()).thenReturn(2L);
		when(alertsRepository.saveAlert(alert)).thenReturn(0);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> alertsService.createAlert(alert));
		assertEquals("Failed to create alert", exception.getMessage());
	}

	@Test
	void getAllAlerts_shouldReturnListWhenRepositorySucceeds() {
		List<Alerts> expected = List.of(new Alerts(), new Alerts());
		when(alertsRepository.getAllAlerts()).thenReturn(expected);

		List<Alerts> result = alertsService.getAllAlerts();

		assertSame(expected, result);
		verify(alertsRepository).getAllAlerts();
	}

	@Test
	void getAllAlerts_shouldThrowWhenRepositoryFails() {
		when(alertsRepository.getAllAlerts()).thenThrow(new DataRetrievalFailureException("DB error"));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> alertsService.getAllAlerts());
		assertEquals("Failed to fetch alerts", exception.getMessage());
		assertTrue(exception.getCause() instanceof DataRetrievalFailureException);
	}

	@Test
	void getAlertById_shouldThrowWhenIdIsNull() {
		RuntimeException exception = assertThrows(RuntimeException.class, () -> alertsService.getAlertById(null));
		assertEquals("Invalid Alert ID", exception.getMessage());
		verifyNoInteractions(alertsRepository);
	}

	@Test
	void getAlertById_shouldReturnAlertWhenFound() {
		Alerts expected = new Alerts();
		when(alertsRepository.getAlertById(10L)).thenReturn(expected);

		Alerts result = alertsService.getAlertById(10L);

		assertSame(expected, result);
		verify(alertsRepository).getAlertById(10L);
	}

	@Test
	void getAlertById_shouldThrowNotFoundWhenRepositoryThrowsEmptyResult() {
		when(alertsRepository.getAlertById(99L)).thenThrow(new EmptyResultDataAccessException(1));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> alertsService.getAlertById(99L));
		assertEquals("Alert not found", exception.getMessage());
	}

	@Test
	void updateAlertStatus_shouldThrowWhenStatusIsBlank() {
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> alertsService.updateAlertStatus(1L, " ", "RESOLVED"));
		assertEquals("Status cannot be empty", exception.getMessage());
		verifyNoInteractions(alertsRepository);
	}

	@Test
	void updateAlertStatus_shouldTrimStatusAndReturnAffectedRows() {
		Alerts existing = new Alerts();
		existing.setTransactionId(12L);
		existing.setNewStatus("OPEN");
		when(alertsRepository.getAlertById(5L)).thenReturn(existing);
		when(alertsRepository.updateAlertStatus(5L, "OPEN", "ACKNOWLEDGED")).thenReturn(1);
		when(bankTransactionsRepository.updateStatus(12L, "PENDING")).thenReturn(true);

		int result = alertsService.updateAlertStatus(5L, "  OPEN  ", "  ACKNOWLEDGED  ");

		assertEquals(1, result);
		verify(alertsRepository).updateAlertStatus(5L, "OPEN", "ACKNOWLEDGED");
		verify(bankTransactionsRepository).updateStatus(12L, "PENDING");
	}

	@Test
	void updateAlertStatus_shouldThrowWhenRepositoryReturnsZero() {
		Alerts existing = new Alerts();
		existing.setTransactionId(12L);
		existing.setNewStatus("OPEN");
		when(alertsRepository.getAlertById(5L)).thenReturn(existing);
		when(alertsRepository.updateAlertStatus(5L, "OPEN", "ACKNOWLEDGED")).thenReturn(0);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> alertsService.updateAlertStatus(5L, "OPEN", "ACKNOWLEDGED"));
		assertEquals("Alert not found", exception.getMessage());
	}

	@Test
	void updateAlertStatus_shouldThrowWhenRepositoryFails() {
		Alerts existing = new Alerts();
		existing.setTransactionId(12L);
		existing.setNewStatus("OPEN");
		when(alertsRepository.getAlertById(5L)).thenReturn(existing);
		when(alertsRepository.updateAlertStatus(5L, "OPEN", "ACKNOWLEDGED"))
				.thenThrow(new DataRetrievalFailureException("DB error"));

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> alertsService.updateAlertStatus(5L, "OPEN", "ACKNOWLEDGED"));
		assertEquals("Failed to update alert", exception.getMessage());
	}

	@Test
	void updateAlertStatus_shouldRejectInvalidLifecycleTransition() {
		Alerts existing = new Alerts();
		existing.setTransactionId(22L);
		existing.setNewStatus("OPEN");
		when(alertsRepository.getAlertById(5L)).thenReturn(existing);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> alertsService.updateAlertStatus(5L, "OPEN", "CLOSED"));
		assertTrue(exception.getMessage().contains("Invalid alert status transition"));
	}

	@Test
	void deleteAlert_shouldThrowWhenIdIsNull() {
		RuntimeException exception = assertThrows(RuntimeException.class, () -> alertsService.deleteAlert(null));
		assertEquals("Invalid Alert ID", exception.getMessage());
		verifyNoInteractions(alertsRepository);
	}

	@Test
	void deleteAlert_shouldReturnAffectedRowsWhenRepositorySucceeds() {
		when(alertsRepository.deleteAlert(7L)).thenReturn(1);

		int result = alertsService.deleteAlert(7L);

		assertEquals(1, result);
		verify(alertsRepository).deleteAlert(7L);
	}

	@Test
	void deleteAlert_shouldThrowWhenRepositoryReturnsZero() {
		when(alertsRepository.deleteAlert(7L)).thenReturn(0);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> alertsService.deleteAlert(7L));
		assertEquals("Alert not found", exception.getMessage());
	}

	@Test
	void deleteAlert_shouldThrowWhenRepositoryFails() {
		when(alertsRepository.deleteAlert(7L)).thenThrow(new DataRetrievalFailureException("DB error"));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> alertsService.deleteAlert(7L));
		assertEquals("Failed to delete alert", exception.getMessage());
	}
}
