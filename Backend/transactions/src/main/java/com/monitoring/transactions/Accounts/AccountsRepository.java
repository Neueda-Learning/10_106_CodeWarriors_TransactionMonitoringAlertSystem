package com.monitoring.transactions.Accounts;

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
public class AccountsRepository {

  private static final RowMapper<Accounts> ACCOUNT_ROW_MAPPER = (rs, rowNum) -> {
    Timestamp createdTimestamp = rs.getTimestamp("created_at");
    LocalDateTime createdAt = createdTimestamp != null ? createdTimestamp.toLocalDateTime() : null;
    return new Accounts(
      rs.getLong("id"),
      rs.getString("customer_name"),
      rs.getString("account_type"),
      rs.getString("country"),
      createdAt);
  };

  private final JdbcTemplate jdbcTemplate;

  public AccountsRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Accounts> findAll() {
    String sql = "SELECT id, customer_name, account_type, country, created_at FROM accounts ORDER BY id";
    return jdbcTemplate.query(sql, ACCOUNT_ROW_MAPPER);
  }

  public Optional<Accounts> findById(Long id) {
    String sql = "SELECT id, customer_name, account_type, country, created_at FROM accounts WHERE id = ?";
    List<Accounts> accounts = jdbcTemplate.query(sql, ACCOUNT_ROW_MAPPER, id);
    return accounts.stream().findFirst();
  }

  public Accounts save(Accounts account) {
    String sql = "INSERT INTO accounts (customer_name, account_type, country) VALUES (?, ?, ?)";
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      statement.setString(1, account.getCustomerName());
      statement.setString(2, account.getAccountType());
      statement.setString(3, account.getCountry());
      return statement;
    }, keyHolder);

    Number key = keyHolder.getKey();
    if (key == null) {
      return account;
    }
    return findById(key.longValue()).orElse(account);
  }

  public boolean update(Long id, Accounts account) {
    String sql = "UPDATE accounts SET customer_name = ?, account_type = ?, country = ? WHERE id = ?";
    int rows = jdbcTemplate.update(sql, account.getCustomerName(), account.getAccountType(), account.getCountry(), id);
    return rows > 0;
  }

  public boolean deleteById(Long id) {
    String sql = "DELETE FROM accounts WHERE id = ?";
    return jdbcTemplate.update(sql, id) > 0;
  }
}
