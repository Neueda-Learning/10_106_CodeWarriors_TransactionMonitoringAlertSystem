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
import com.monitoring.transactions.Exception.GeneralizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;

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

	@Test
	void updateAlertStatus_shouldReturnConflictWhenCurrentStatusDiffersFromPayloadOldStatus() {
		Alerts existing = new Alerts();
		existing.setTransactionId(30L);
		existing.setNewStatus("ACKNOWLEDGED");
		when(alertsRepository.getAlertById(9L)).thenReturn(existing);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> alertsService.updateAlertStatus(9L, "OPEN", "INVESTIGATING"));
		assertEquals("Alert status mismatch. Refresh data and retry.", exception.getMessage());
		assertTrue(exception instanceof GeneralizedException);
		assertEquals(HttpStatus.CONFLICT, ((GeneralizedException) exception).getStatus());
	}

	@Test
	void updateAlertStatus_shouldMoveToInvestigatingAndKeepTransactionPending() {
		Alerts existing = new Alerts();
		existing.setTransactionId(40L);
		existing.setNewStatus("ACKNOWLEDGED");
		when(alertsRepository.getAlertById(6L)).thenReturn(existing);
		when(alertsRepository.updateAlertStatus(6L, "ACKNOWLEDGED", "INVESTIGATING")).thenReturn(1);
		when(bankTransactionsRepository.updateStatus(40L, "PENDING")).thenReturn(true);

		int result = alertsService.updateAlertStatus(6L, "ACKNOWLEDGED", "INVESTIGATING");

		assertEquals(1, result);
		verify(alertsRepository).updateAlertStatus(6L, "ACKNOWLEDGED", "INVESTIGATING");
		verify(bankTransactionsRepository).updateStatus(40L, "PENDING");
	}

	@Test
	void updateAlertStatus_shouldCloseAlertAndMarkLinkedTransactionCompleted() {
		Alerts existing = new Alerts();
		existing.setTransactionId(41L);
		existing.setNewStatus("INVESTIGATING");
		when(alertsRepository.getAlertById(11L)).thenReturn(existing);
		when(alertsRepository.updateAlertStatus(11L, "INVESTIGATING", "CLOSED")).thenReturn(1);
		when(bankTransactionsRepository.updateStatus(41L, "COMPLETED")).thenReturn(true);

		int result = alertsService.updateAlertStatus(11L, "INVESTIGATING", "CLOSED");

		assertEquals(1, result);
		verify(bankTransactionsRepository).updateStatus(41L, "COMPLETED");
	}

	@Test
	void updateAlertStatus_shouldDismissAlertAndMarkLinkedTransactionFailed() {
		Alerts existing = new Alerts();
		existing.setTransactionId(42L);
		existing.setNewStatus("INVESTIGATING");
		when(alertsRepository.getAlertById(12L)).thenReturn(existing);
		when(alertsRepository.updateAlertStatus(12L, "INVESTIGATING", "DISMISSED")).thenReturn(1);
		when(bankTransactionsRepository.updateStatus(42L, "FAILED")).thenReturn(true);

		int result = alertsService.updateAlertStatus(12L, "INVESTIGATING", "DISMISSED");

		assertEquals(1, result);
		verify(bankTransactionsRepository).updateStatus(42L, "FAILED");
	}

	@Test
	void updateAlertStatus_shouldThrowWhenLinkedTransactionCannotBeUpdated() {
		Alerts existing = new Alerts();
		existing.setTransactionId(77L);
		existing.setNewStatus("INVESTIGATING");
		when(alertsRepository.getAlertById(13L)).thenReturn(existing);
		when(alertsRepository.updateAlertStatus(13L, "INVESTIGATING", "CLOSED")).thenReturn(1);
		when(bankTransactionsRepository.updateStatus(77L, "COMPLETED")).thenReturn(false);

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> alertsService.updateAlertStatus(13L, "INVESTIGATING", "CLOSED"));
		assertEquals("Linked transaction not found", exception.getMessage());
	}

	@Test
	void updateAlertStatus_shouldSkipTransactionUpdateWhenAlertHasNoLinkedTransactionId() {
		Alerts existing = new Alerts();
		existing.setTransactionId(null);
		existing.setNewStatus("OPEN");
		when(alertsRepository.getAlertById(14L)).thenReturn(existing);
		when(alertsRepository.updateAlertStatus(14L, "OPEN", "ACKNOWLEDGED")).thenReturn(1);

		int result = alertsService.updateAlertStatus(14L, "OPEN", "ACKNOWLEDGED");

		assertEquals(1, result);
		verifyNoInteractions(bankTransactionsRepository);
	}

	@Test
	void updateAlertStatus_shouldTreatBlankCurrentStatusAsOpen() {
		Alerts existing = new Alerts();
		existing.setTransactionId(15L);
		existing.setNewStatus("   ");
		when(alertsRepository.getAlertById(15L)).thenReturn(existing);
		when(alertsRepository.updateAlertStatus(15L, "OPEN", "ACKNOWLEDGED")).thenReturn(1);
		when(bankTransactionsRepository.updateStatus(15L, "PENDING")).thenReturn(true);

		int result = alertsService.updateAlertStatus(15L, "OPEN", "ACKNOWLEDGED");

		assertEquals(1, result);
		verify(alertsRepository).updateAlertStatus(15L, "OPEN", "ACKNOWLEDGED");
	}
}
