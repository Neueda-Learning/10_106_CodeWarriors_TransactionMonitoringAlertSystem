package com.monitoring.transactions.Alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class AlertsRepositoryTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
	private Alerts alert;

	private AlertsRepository alertsRepository;

	@BeforeEach
	void setUp() {
		alertsRepository = new AlertsRepository(jdbcTemplate);
	}

	@Test
	void saveAlert_shouldInsertAlertAndReturnAffectedRows() {
		when(alert.getTransactionId()).thenReturn(1001L);
		when(alert.getRuleId()).thenReturn(2002L);
		when(alert.getAlertReason()).thenReturn("High amount detected");
		when(alert.getSeverity()).thenReturn("HIGH");
		when(alert.getOldStatus()).thenReturn("PENDING");
		when(alert.getNewStatus()).thenReturn("FLAGGED");

		when(jdbcTemplate.update(anyString(), eq(1001L), eq(2002L), eq("High amount detected"),
				eq("HIGH"), eq("PENDING"), eq("FLAGGED"))).thenReturn(1);

		int result = alertsRepository.saveAlert(alert);

		assertEquals(1, result);

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sqlCaptor.capture(), eq(1001L), eq(2002L), eq("High amount detected"),
				eq("HIGH"), eq("PENDING"), eq("FLAGGED"));
		assertTrue(sqlCaptor.getValue().contains("INSERT INTO alerts"));
	}

	@Test
	void getAllAlerts_shouldReturnAllAlertsOrderedByCreatedAtDesc() {
		Alerts a1 = new Alerts();
		Alerts a2 = new Alerts();
		List<Alerts> expected = List.of(a1, a2);

		when(jdbcTemplate.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Alerts>>any())).thenReturn(expected);

		List<Alerts> result = alertsRepository.getAllAlerts();

		assertSame(expected, result);

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).query(sqlCaptor.capture(), org.mockito.ArgumentMatchers.<RowMapper<Alerts>>any());
		assertTrue(sqlCaptor.getValue().contains("ORDER BY created_at DESC"));
	}

	@Test
	void getAlertById_shouldReturnAlertWhenFound() {
		Long id = 10L;
		Alerts expected = new Alerts();

		when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Alerts>>any(), eq(id))).thenReturn(expected);

		Alerts result = alertsRepository.getAlertById(id);

		assertSame(expected, result);

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), org.mockito.ArgumentMatchers.<RowMapper<Alerts>>any(), eq(id));
		assertTrue(sqlCaptor.getValue().contains("WHERE id = ?"));
	}

	@Test
	void getAlertById_shouldPropagateExceptionWhenNotFound() {
		Long id = 999L;
		when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Alerts>>any(), eq(id)))
				.thenThrow(new EmptyResultDataAccessException(1));

		assertThrows(EmptyResultDataAccessException.class, () -> alertsRepository.getAlertById(id));
	}

	@Test
	void updateAlertStatus_shouldUpdateStatusesAndReturnAffectedRows() {
		Long id = 5L;
		String oldStatus = "PENDING";
		String newStatus = "RESOLVED";

		when(jdbcTemplate.update(anyString(), eq(oldStatus), eq(newStatus), eq(id))).thenReturn(1);

		int result = alertsRepository.updateAlertStatus(id, oldStatus, newStatus);

		assertEquals(1, result);

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sqlCaptor.capture(), eq(oldStatus), eq(newStatus), eq(id));
		assertTrue(sqlCaptor.getValue().contains("SET old_status = ?, new_status = ?, updated_at = CURRENT_TIMESTAMP"));
	}

	@Test
	void deleteAlert_shouldDeleteByIdAndReturnAffectedRows() {
		Long id = 7L;
		when(jdbcTemplate.update(anyString(), eq(id))).thenReturn(1);

		int result = alertsRepository.deleteAlert(id);

		assertEquals(1, result);

		ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
		verify(jdbcTemplate).update(sqlCaptor.capture(), eq(id));
		assertTrue(sqlCaptor.getValue().contains("DELETE FROM alerts"));
	}
}
