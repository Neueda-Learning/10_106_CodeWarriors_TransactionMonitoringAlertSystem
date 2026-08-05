package com.monitoring.transactions.BankTransactions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankTransactionsRepositoryTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private BankTransactionsRepository bankTransactionsRepository;

    private final LocalDateTime fixedTime = LocalDateTime.of(2026, 8, 1, 10, 0, 0);

    @BeforeEach
    void setUp() {
        bankTransactionsRepository = new BankTransactionsRepository(jdbcTemplate);
    }

    @Test
    void findAll_returnsAllTransactions() {
        List<BankTransactions> expected = List.of(
                new BankTransactions(1L, 1L, 2L, new BigDecimal("125.50"), "USD", fixedTime, "COMPLETED", fixedTime),
                new BankTransactions(2L, 3L, 4L, new BigDecimal("90.00"), "EUR", fixedTime, "PENDING", fixedTime));

        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<BankTransactions>>any())).thenReturn(expected);

        List<BankTransactions> result = bankTransactionsRepository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void findAll_returnsEmptyListWhenNoTransactions() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<BankTransactions>>any())).thenReturn(List.of());

        List<BankTransactions> result = bankTransactionsRepository.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findById_returnsTransactionWhenFound() {
        BankTransactions transaction = new BankTransactions(
                7L, 10L, 11L, new BigDecimal("500.00"), "USD", fixedTime, "FAILED", fixedTime);
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<BankTransactions>>any(), eq(7L)))
                .thenReturn(List.of(transaction));

        Optional<BankTransactions> result = bankTransactionsRepository.findById(7L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(7L);
        assertThat(result.get().getStatus()).isEqualTo("FAILED");
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<BankTransactions>>any(), eq(999L)))
                .thenReturn(List.of());

        Optional<BankTransactions> result = bankTransactionsRepository.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_returnsPersistedTransactionWhenGeneratedIdAvailable() {
        BankTransactions input = new BankTransactions(4L, 5L, new BigDecimal("250.00"), "USD", fixedTime, "PENDING");
        BankTransactions persisted = new BankTransactions(45L, 4L, 5L, new BigDecimal("250.00"), "USD", fixedTime, "PENDING", fixedTime);

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            ((GeneratedKeyHolder) keyHolder).getKeyList().add(java.util.Map.of("GENERATED_KEY", 45L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<BankTransactions>>any(), eq(45L)))
                .thenReturn(List.of(persisted));

        BankTransactions result = bankTransactionsRepository.save(input);

        assertThat(result.getId()).isEqualTo(45L);
        assertThat(result.getFromAccountId()).isEqualTo(4L);
        assertThat(result.getToAccountId()).isEqualTo(5L);
    }

    @Test
    void save_returnsInputTransactionWhenGeneratedIdMissing() {
        BankTransactions input = new BankTransactions(6L, 8L, new BigDecimal("80.00"), "EUR", fixedTime, "COMPLETED");

        doAnswer(invocation -> 1)
                .when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        BankTransactions result = bankTransactionsRepository.save(input);

        assertThat(result).isSameAs(input);
    }

    @Test
    void save_returnsInputTransactionWhenGeneratedIdCannotBeReloaded() {
        BankTransactions input = new BankTransactions(6L, 8L, new BigDecimal("80.00"), "EUR", fixedTime, "COMPLETED");

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            ((GeneratedKeyHolder) keyHolder).getKeyList().add(java.util.Map.of("GENERATED_KEY", 66L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<BankTransactions>>any(), eq(66L)))
                .thenReturn(List.of());

        BankTransactions result = bankTransactionsRepository.save(input);

        assertThat(result).isSameAs(input);
    }

    @Test
    void update_returnsTrueWhenRowUpdated() {
        BankTransactions input = new BankTransactions(1L, 2L, new BigDecimal("999.99"), "USD", fixedTime, "COMPLETED");
        when(jdbcTemplate.update(anyString(), eq(1L), eq(2L), eq(new BigDecimal("999.99")), eq("USD"), any(), eq("COMPLETED"), eq(10L)))
                .thenReturn(1);

        boolean result = bankTransactionsRepository.update(10L, input);

        assertThat(result).isTrue();
    }

    @Test
    void update_returnsFalseWhenTransactionNotFound() {
        BankTransactions input = new BankTransactions(1L, 2L, new BigDecimal("999.99"), "USD", fixedTime, "COMPLETED");
        when(jdbcTemplate.update(anyString(), eq(1L), eq(2L), eq(new BigDecimal("999.99")), eq("USD"), any(), eq("COMPLETED"), eq(404L)))
                .thenReturn(0);

        boolean result = bankTransactionsRepository.update(404L, input);

        assertThat(result).isFalse();
    }

    @Test
    void deleteById_returnsTrueWhenRowDeleted() {
        when(jdbcTemplate.update(anyString(), eq(12L))).thenReturn(1);

        boolean result = bankTransactionsRepository.deleteById(12L);

        assertThat(result).isTrue();
    }

    @Test
    void deleteById_returnsFalseWhenTransactionNotFound() {
        when(jdbcTemplate.update(anyString(), eq(777L))).thenReturn(0);

        boolean result = bankTransactionsRepository.deleteById(777L);

        assertThat(result).isFalse();
    }

    @Test
    void updateStatus_returnsTrueWhenStatusUpdated() {
        when(jdbcTemplate.update(anyString(), eq("FAILED"), eq(22L))).thenReturn(1);

        boolean result = bankTransactionsRepository.updateStatus(22L, "FAILED");

        assertThat(result).isTrue();
    }

    @Test
    void updateStatus_returnsFalseWhenTransactionNotFound() {
        when(jdbcTemplate.update(anyString(), eq("COMPLETED"), eq(999L))).thenReturn(0);

        boolean result = bankTransactionsRepository.updateStatus(999L, "COMPLETED");

        assertThat(result).isFalse();
    }
}
