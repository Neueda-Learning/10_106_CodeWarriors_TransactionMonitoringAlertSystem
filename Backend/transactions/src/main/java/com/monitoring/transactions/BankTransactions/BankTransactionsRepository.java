package com.monitoring.transactions.BankTransactions;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class BankTransactionsRepository {

    private final JdbcTemplate jdbcTemplate;

    public BankTransactionsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<BankTransactions> rowMapper = new RowMapper<BankTransactions>() {
        @Override
        public BankTransactions mapRow(ResultSet rs, int rowNum) throws SQLException {
            BankTransactions tx = new BankTransactions();
            tx.setId(rs.getLong("id"));
            tx.setFromAccountId(rs.getLong("from_account_id"));
            tx.setToAccountId(rs.getLong("to_account_id"));
            tx.setAmount(rs.getBigDecimal("amount"));
            tx.setCurrency(rs.getString("currency"));
            tx.setTransactionTime(rs.getTimestamp("transaction_time").toLocalDateTime());
            tx.setStatus(rs.getString("status"));
            if (rs.getTimestamp("created_at") != null) {
                tx.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            return tx;
        }
    };

    public List<BankTransactions> findAll() {
        return jdbcTemplate.query("SELECT * FROM bank_transactions ORDER BY transaction_time DESC", rowMapper);
    }

    public void save(BankTransactions tx) {
        String sql = "INSERT INTO bank_transactions (from_account_id, to_account_id, amount, currency, transaction_time, status) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, tx.getFromAccountId(), tx.getToAccountId(), tx.getAmount(), tx.getCurrency(), tx.getTransactionTime(), tx.getStatus());
    }
}
