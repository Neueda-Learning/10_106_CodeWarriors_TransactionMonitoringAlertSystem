package com.monitoring.transactions.Alerts;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AlertsRepository {

	private static final String INSERT_ALERT_SQL = """
			INSERT INTO alerts
			(transaction_id, rule_id, alert_reason, severity, old_status, new_status)
			VALUES (?, ?, ?, ?, ?, ?)
			""";

	private static final String SELECT_ALL_ALERTS_SQL = """
			SELECT id, transaction_id, rule_id, alert_reason, severity, old_status, new_status, created_at, updated_at
			FROM alerts
			ORDER BY created_at DESC
			""";

	private static final String SELECT_ALERT_BY_ID_SQL = """
			SELECT id, transaction_id, rule_id, alert_reason, severity, old_status, new_status, created_at, updated_at
			FROM alerts
			WHERE id = ?
			""";

	private static final String UPDATE_ALERT_STATUS_SQL = """
			UPDATE alerts
			SET old_status = ?, new_status = ?, updated_at = CURRENT_TIMESTAMP
			WHERE id = ?
			""";

	private static final String DELETE_ALERT_SQL = """
			DELETE FROM alerts
			WHERE id = ?
			""";

	private static final String DELETE_ALERTS_BY_RULE_ID_SQL = """
			DELETE FROM alerts
			WHERE rule_id = ?
			""";

	private static final String COUNT_ACTIVE_ALERTS_BY_TX_AND_RULE_SQL = """
			SELECT COUNT(*)
			FROM alerts
			WHERE transaction_id = ?
			  AND rule_id = ?
			  AND new_status IN ('OPEN', 'ACKNOWLEDGED', 'INVESTIGATING')
			""";

	private final JdbcTemplate jdbcTemplate;
	private final RowMapper<Alerts> alertsRowMapper = new AlertsRowMapper();

	public AlertsRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int saveAlert(Alerts alert) {
		return jdbcTemplate.update(
				INSERT_ALERT_SQL,
				alert.getTransactionId(),
				alert.getRuleId(),
				alert.getAlertReason(),
				alert.getSeverity(),
				alert.getOldStatus(),
				alert.getNewStatus());
	}

	public List<Alerts> getAllAlerts() {
		return jdbcTemplate.query(SELECT_ALL_ALERTS_SQL, alertsRowMapper);
	}

	public List<Alerts> getAlertsByFilters(String status, String severity) {
		StringBuilder sql = new StringBuilder(
				"SELECT id, transaction_id, rule_id, alert_reason, severity, old_status, new_status, created_at, updated_at " +
				"FROM alerts WHERE 1=1");
		List<Object> params = new ArrayList<>();

		if (status != null) {
			sql.append(" AND new_status = ?");
			params.add(status);
		}

		if (severity != null) {
			sql.append(" AND severity = ?");
			params.add(severity);
		}

		sql.append(" ORDER BY created_at DESC");
		return jdbcTemplate.query(sql.toString(), alertsRowMapper, params.toArray());
	}

	public Alerts getAlertById(Long id) {
		return jdbcTemplate.queryForObject(SELECT_ALERT_BY_ID_SQL, alertsRowMapper, id);
	}

	public int updateAlertStatus(Long id, String oldStatus, String newStatus) {
		return jdbcTemplate.update(UPDATE_ALERT_STATUS_SQL, oldStatus, newStatus, id);
	}

	public int deleteAlert(Long id) {
		return jdbcTemplate.update(DELETE_ALERT_SQL, id);
	}

	public int deleteAlertsByRuleId(Long ruleId) {
		return jdbcTemplate.update(DELETE_ALERTS_BY_RULE_ID_SQL, ruleId);
	}

	public boolean hasActiveAlertForTransactionAndRule(Long transactionId, Long ruleId) {
		Integer count = jdbcTemplate.queryForObject(
			COUNT_ACTIVE_ALERTS_BY_TX_AND_RULE_SQL,
			Integer.class,
			transactionId,
			ruleId);
		return count != null && count > 0;
	}

	private static final class AlertsRowMapper implements RowMapper<Alerts> {

		@Override
		public Alerts mapRow(ResultSet rs, int rowNum) throws SQLException {
			Alerts alert = new Alerts();
			alert.setId(rs.getLong("id"));
			alert.setTransactionId(rs.getLong("transaction_id"));
			alert.setRuleId(rs.getLong("rule_id"));
			alert.setAlertReason(rs.getString("alert_reason"));
			alert.setSeverity(rs.getString("severity"));
			alert.setOldStatus(rs.getString("old_status"));
			alert.setNewStatus(rs.getString("new_status"));
			if (rs.getTimestamp("created_at") != null) {
				alert.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
			}
			if (rs.getTimestamp("updated_at") != null) {
				alert.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
			}
			return alert;
		}
	}
}
