package com.monitoring.transactions.Rules;

import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RulesRepository {

	private static final RowMapper<Rules> RULE_ROW_MAPPER = (rs, rowNum) -> new Rules(
		rs.getLong("id"),
		rs.getString("name"),
		rs.getString("type"),
		rs.getBigDecimal("threshold"),
		(Integer) rs.getObject("time_window"),
		(Integer) rs.getObject("max_transactions"),
		rs.getString("severity"),
		(Boolean) rs.getObject("active"));

	private final JdbcTemplate jdbcTemplate;

	public RulesRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Rules> findAll() {
		String sql = """
			SELECT id, name, type, threshold, time_window, max_transactions, severity, active
			FROM rules
			ORDER BY id
			""";
		return jdbcTemplate.query(sql, RULE_ROW_MAPPER);
	}

	public Optional<Rules> findById(Long id) {
		String sql = """
			SELECT id, name, type, threshold, time_window, max_transactions, severity, active
			FROM rules
			WHERE id = ?
			""";
		List<Rules> rules = jdbcTemplate.query(sql, RULE_ROW_MAPPER, id);
		return rules.stream().findFirst();
	}

	public Rules save(Rules rule) {
		String sql = """
			INSERT INTO rules (name, type, threshold, time_window, max_transactions, severity, active)
			VALUES (?, ?, ?, ?, ?, ?, ?)
			""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, rule.getName());
			statement.setString(2, rule.getType());
			if (rule.getThreshold() != null) {
				statement.setBigDecimal(3, rule.getThreshold());
			} else {
				statement.setNull(3, Types.DECIMAL);
			}
			if (rule.getTimeWindow() != null) {
				statement.setInt(4, rule.getTimeWindow());
			} else {
				statement.setNull(4, Types.INTEGER);
			}
			if (rule.getMaxTransactions() != null) {
				statement.setInt(5, rule.getMaxTransactions());
			} else {
				statement.setNull(5, Types.INTEGER);
			}
			if (rule.getSeverity() != null) {
				statement.setString(6, rule.getSeverity());
			} else {
				statement.setNull(6, Types.VARCHAR);
			}
			if (rule.getActive() != null) {
				statement.setBoolean(7, rule.getActive());
			} else {
				statement.setNull(7, Types.BOOLEAN);
			}
			return statement;
		}, keyHolder);

		Number key = keyHolder.getKey();
		if (key == null) {
			return rule;
		}
		return findById(key.longValue()).orElse(rule);
	}

	public boolean update(Long id, Rules rule) {
		String sql = """
			UPDATE rules
			SET name = ?, type = ?, threshold = ?, time_window = ?, max_transactions = ?, severity = ?, active = ?
			WHERE id = ?
			""";
		int rows = jdbcTemplate.update(sql,
			rule.getName(),
			rule.getType(),
			rule.getThreshold(),
			rule.getTimeWindow(),
			rule.getMaxTransactions(),
			rule.getSeverity(),
			rule.getActive(),
			id);
		return rows > 0;
	}

	public boolean deleteById(Long id) {
		String sql = "DELETE FROM rules WHERE id = ?";
		return jdbcTemplate.update(sql, id) > 0;
	}
}
