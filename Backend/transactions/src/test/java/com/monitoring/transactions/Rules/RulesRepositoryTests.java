package com.monitoring.transactions.Rules;

import java.math.BigDecimal;
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
class RulesRepositoryTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private RulesRepository rulesRepository;

    @BeforeEach
    void setUp() {
        rulesRepository = new RulesRepository(jdbcTemplate);
    }

    @Test
    void findAll_returnsRulesWhenRecordsExist() {
        List<Rules> expected = List.of(
                new Rules(1L, "Large Transaction", "AMOUNT_THRESHOLD", new BigDecimal("10000.00"), null, null, "HIGH", true),
                new Rules(2L, "High Velocity Check", "VELOCITY", null, 10, 5, "MEDIUM", true));
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Rules>>any())).thenReturn(expected);

        List<Rules> result = rulesRepository.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void findAll_returnsEmptyListWhenNoRecordsExist() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Rules>>any())).thenReturn(List.of());

        List<Rules> result = rulesRepository.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findById_returnsRuleWhenFound() {
        Rules expected = new Rules(1L, "Large Transaction", "AMOUNT_THRESHOLD", new BigDecimal("10000.00"), null, null, "HIGH", true);
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Rules>>any(), eq(1L))).thenReturn(List.of(expected));

        Optional<Rules> result = rulesRepository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Large Transaction");
    }

    @Test
    void findById_returnsEmptyWhenRuleDoesNotExist() {
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Rules>>any(), eq(999L))).thenReturn(List.of());

        Optional<Rules> result = rulesRepository.findById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_returnsPersistedRuleWhenGeneratedIdExists() {
        Rules input = new Rules("New Rule", "VELOCITY", null, 5, 3, "MEDIUM", true);
        Rules persisted = new Rules(25L, "New Rule", "VELOCITY", null, 5, 3, "MEDIUM", true);

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            ((GeneratedKeyHolder) keyHolder).getKeyList().add(java.util.Map.of("GENERATED_KEY", 25L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Rules>>any(), eq(25L))).thenReturn(List.of(persisted));

        Rules result = rulesRepository.save(input);

        assertThat(result.getId()).isEqualTo(25L);
        assertThat(result.getName()).isEqualTo("New Rule");
    }

    @Test
    void save_returnsInputRuleWhenGeneratedIdMissing() {
        Rules input = new Rules("Rule A", "AMOUNT_THRESHOLD", new BigDecimal("2000.00"), null, null, "HIGH", true);

        doAnswer(invocation -> 1).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        Rules result = rulesRepository.save(input);

        assertThat(result).isSameAs(input);
    }

    @Test
    void save_returnsInputRuleWhenGeneratedIdCannotBeReloaded() {
        Rules input = new Rules("Rule A", "AMOUNT_THRESHOLD", new BigDecimal("2000.00"), null, null, "HIGH", true);

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            ((GeneratedKeyHolder) keyHolder).getKeyList().add(java.util.Map.of("GENERATED_KEY", 44L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<Rules>>any(), eq(44L))).thenReturn(List.of());

        Rules result = rulesRepository.save(input);

        assertThat(result).isSameAs(input);
    }

    @Test
    void update_returnsTrueWhenRecordUpdated() {
        Rules input = new Rules("Rule B", "VELOCITY", null, 20, 10, "MEDIUM", true);
        when(jdbcTemplate.update(anyString(),
                eq("Rule B"), eq("VELOCITY"), eq(null), eq(20), eq(10), eq("MEDIUM"), eq(true), eq(2L)))
                .thenReturn(1);

        boolean result = rulesRepository.update(2L, input);

        assertThat(result).isTrue();
    }

    @Test
    void update_returnsFalseWhenRecordNotFound() {
        Rules input = new Rules("Rule B", "VELOCITY", null, 20, 10, "MEDIUM", true);
        when(jdbcTemplate.update(anyString(),
                eq("Rule B"), eq("VELOCITY"), eq(null), eq(20), eq(10), eq("MEDIUM"), eq(true), eq(999L)))
                .thenReturn(0);

        boolean result = rulesRepository.update(999L, input);

        assertThat(result).isFalse();
    }

    @Test
    void deleteById_returnsTrueWhenRecordDeleted() {
        when(jdbcTemplate.update(anyString(), eq(3L))).thenReturn(1);

        boolean result = rulesRepository.deleteById(3L);

        assertThat(result).isTrue();
    }

    @Test
    void deleteById_returnsFalseWhenRecordNotFound() {
        when(jdbcTemplate.update(anyString(), eq(500L))).thenReturn(0);

        boolean result = rulesRepository.deleteById(500L);

        assertThat(result).isFalse();
    }
}
