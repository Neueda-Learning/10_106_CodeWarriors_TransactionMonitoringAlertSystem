package com.monitoring.transactions.BankTransactions;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class BankTransactionsRepository {

	private static final RowMapper<BankTransactions> TRANSACTION_ROW_MAPPER = (rs, rowNum) -> {
		Timestamp transactionTimestamp = rs.getTimestamp("transaction_time");
		Timestamp createdTimestamp = rs.getTimestamp("created_at");
		LocalDateTime transactionTime = transactionTimestamp != null ? transactionTimestamp.toLocalDateTime() : null;
		LocalDateTime createdAt = createdTimestamp != null ? createdTimestamp.toLocalDateTime() : null;
		return new BankTransactions(
			rs.getLong("id"),
			rs.getLong("from_account_id"),
			rs.getLong("to_account_id"),
			rs.getBigDecimal("amount"),
			rs.getString("currency"),
			transactionTime,
			rs.getString("status"),
			createdAt);
	};

	private final JdbcTemplate jdbcTemplate;

	public BankTransactionsRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<BankTransactions> findAll() {
		String sql = """
			SELECT id, from_account_id, to_account_id, amount, currency, transaction_time, status, created_at
			FROM bank_transactions
			ORDER BY id
			""";
		return jdbcTemplate.query(sql, TRANSACTION_ROW_MAPPER);
	}

	public Optional<BankTransactions> findById(Long id) {
		String sql = """
			SELECT id, from_account_id, to_account_id, amount, currency, transaction_time, status, created_at
			FROM bank_transactions
			WHERE id = ?
			""";
		List<BankTransactions> transactions = jdbcTemplate.query(sql, TRANSACTION_ROW_MAPPER, id);
		return transactions.stream().findFirst();
	}

	public BankTransactions save(BankTransactions transaction) {
		String sql = """
			INSERT INTO bank_transactions
			(from_account_id, to_account_id, amount, currency, transaction_time, status)
			VALUES (?, ?, ?, ?, ?, ?)
			""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			statement.setLong(1, transaction.getFromAccountId());
			statement.setLong(2, transaction.getToAccountId());
			statement.setBigDecimal(3, transaction.getAmount());
			statement.setString(4, transaction.getCurrency());
			statement.setTimestamp(5, Timestamp.valueOf(transaction.getTransactionTime()));
			statement.setString(6, transaction.getStatus());
			return statement;
		}, keyHolder);

		Number key = keyHolder.getKey();
		if (key == null) {
			return transaction;
		}
		return findById(key.longValue()).orElse(transaction);
	}

	public boolean update(Long id, BankTransactions transaction) {
		String sql = """
			UPDATE bank_transactions
			SET from_account_id = ?, to_account_id = ?, amount = ?, currency = ?, transaction_time = ?, status = ?
			WHERE id = ?
			""";
		int rows = jdbcTemplate.update(
			sql,
			transaction.getFromAccountId(),
			transaction.getToAccountId(),
			transaction.getAmount(),
			transaction.getCurrency(),
			Timestamp.valueOf(transaction.getTransactionTime()),
			transaction.getStatus(),
			id);
		return rows > 0;
	}

	public boolean deleteById(Long id) {
		String sql = "DELETE FROM bank_transactions WHERE id = ?";
		return jdbcTemplate.update(sql, id) > 0;
	}

	public boolean updateStatus(Long id, String status) {
		String sql = "UPDATE bank_transactions SET status = ? WHERE id = ?";
		return jdbcTemplate.update(sql, status, id) > 0;
	}

	public long countAllTransactions() {
		String sql = "SELECT COUNT(*) FROM bank_transactions";
		Long count = jdbcTemplate.queryForObject(sql, Long.class);
		return count == null ? 0L : count;
	}

	public void truncateAllTransactions() {
		jdbcTemplate.execute("TRUNCATE TABLE bank_transactions");
	}
}
