package com.monitoring.transactions.Accounts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountsRepositoryTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AccountsRepository accountsRepository;

    private final LocalDateTime fixedTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

    @BeforeEach
    void setUp() {
        accountsRepository = new AccountsRepository(jdbcTemplate);
    }

    @Test
    void findAll_returnsAllAccounts() {
        Accounts a1 = new Accounts(1L, "Customer 1", "CHECKING", "US", fixedTime);
        Accounts a2 = new Accounts(2L, "Customer 2", "SAVINGS", "AU", fixedTime);
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Accounts>>any()))
                .thenReturn(List.of(a1, a2));

        List<Accounts> result = accountsRepository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void findAll_returnsEmptyListWhenNoAccounts() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Accounts>>any()))
                .thenReturn(List.of());

        List<Accounts> result = accountsRepository.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findById_returnsAccountWhenFound() {
        Accounts account = new Accounts(5L, "Customer 5", "CORPORATE", "CA", fixedTime);
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Accounts>>any(), eq(5L)))
                .thenReturn(List.of(account));

        Optional<Accounts> result = accountsRepository.findById(5L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(5L);
        assertThat(result.get().getCustomerName()).isEqualTo("Customer 5");
        assertThat(result.get().getAccountType()).isEqualTo("CORPORATE");
        assertThat(result.get().getCountry()).isEqualTo("CA");
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Accounts>>any(), eq(999L)))
                .thenReturn(List.of());

        Optional<Accounts> result = accountsRepository.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_returnsPersistedAccountWithGeneratedId() {
        Accounts input = new Accounts("New Customer", "SAVINGS", "UK");
        Accounts saved = new Accounts(42L, "New Customer", "SAVINGS", "UK", fixedTime);

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            ((GeneratedKeyHolder) keyHolder).getKeyList().add(java.util.Map.of("GENERATED_KEY", 42L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Accounts>>any(), eq(42L)))
                .thenReturn(List.of(saved));

        Accounts result = accountsRepository.save(input);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getCustomerName()).isEqualTo("New Customer");
        assertThat(result.getAccountType()).isEqualTo("SAVINGS");
    }

    @Test
    void save_returnsInputAccountWhenKeyHolderHasNoKey() {
        Accounts input = new Accounts("New Customer", "CHECKING", "US");

        doAnswer(invocation -> 1)
                .when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        Accounts result = accountsRepository.save(input);

        assertThat(result).isSameAs(input);
    }

    @Test
    void update_returnsTrueWhenRowIsUpdated() {
        Accounts account = new Accounts("Updated Name", "CORPORATE", "AU");
        when(jdbcTemplate.update(anyString(),
                eq("Updated Name"), eq("CORPORATE"), eq("AU"), eq(3L)))
                .thenReturn(1);

        boolean result = accountsRepository.update(3L, account);

        assertThat(result).isTrue();
    }

    @Test
    void update_returnsFalseWhenAccountDoesNotExist() {
        Accounts account = new Accounts("Ghost", "SAVINGS", "NZ");
        when(jdbcTemplate.update(anyString(),
                eq("Ghost"), eq("SAVINGS"), eq("NZ"), eq(999L)))
                .thenReturn(0);

        boolean result = accountsRepository.update(999L, account);

        assertThat(result).isFalse();
    }

    @Test
    void deleteById_returnsTrueWhenAccountDeleted() {
        when(jdbcTemplate.update(anyString(), eq(7L))).thenReturn(1);

        boolean result = accountsRepository.deleteById(7L);

        assertThat(result).isTrue();
    }

    @Test
    void deleteById_returnsFalseWhenAccountDoesNotExist() {
        when(jdbcTemplate.update(anyString(), eq(999L))).thenReturn(0);

        boolean result = accountsRepository.deleteById(999L);

        assertThat(result).isFalse();
    }
}
