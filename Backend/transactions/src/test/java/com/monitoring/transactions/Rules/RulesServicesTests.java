package com.monitoring.transactions.Rules;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.monitoring.transactions.Alerts.AlertsRepository;
import com.monitoring.transactions.Alerts.AlertsService;
import com.monitoring.transactions.BankTransactions.BankTransactions;
import com.monitoring.transactions.BankTransactions.BankTransactionsRepository;
import com.monitoring.transactions.Exception.GeneralizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RulesServicesTests {

    @Mock
    private RulesRepository rulesRepository;

    @Mock
    private AlertsService alertsService;

    @Mock
    private AlertsRepository alertsRepository;

    @Mock
    private BankTransactionsRepository bankTransactionsRepository;

    @Mock
    private RuleEngineService ruleEngineService;

    private RulesServices rulesServices;

    @BeforeEach
    void setUp() {
        rulesServices = new RulesServices(
                rulesRepository,
                alertsService,
                alertsRepository,
                bankTransactionsRepository,
                ruleEngineService);
        lenient().when(bankTransactionsRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void getAllRules_returnsRulesWhenAvailable() {
        List<Rules> expected = List.of(
                new Rules(1L, "Large Transaction", "AMOUNT_THRESHOLD", new BigDecimal("10000.00"), null, null, "HIGH", true),
                new Rules(2L, "High Velocity Check", "VELOCITY", null, 10, 5, "MEDIUM", true));
        when(rulesRepository.findAll()).thenReturn(expected);

        List<Rules> result = rulesServices.getAllRules();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllRules_returnsEmptyListWhenNoRules() {
        when(rulesRepository.findAll()).thenReturn(List.of());

        List<Rules> result = rulesServices.getAllRules();

        assertThat(result).isEmpty();
    }

    @Test
    void getAllRules_throwsInternalServerErrorWhenRepositoryFails() {
        when(rulesRepository.findAll()).thenThrow(new DataAccessResourceFailureException("DB error"));

        assertThatThrownBy(() -> rulesServices.getAllRules())
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void getRuleById_returnsRuleWhenFound() {
        Rules expected = new Rules(1L, "Large Transaction", "AMOUNT_THRESHOLD", new BigDecimal("10000.00"), null, null, "HIGH", true);
        when(rulesRepository.findById(1L)).thenReturn(Optional.of(expected));

        Rules result = rulesServices.getRuleById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Large Transaction");
    }

    @Test
    void getRuleById_throwsBadRequestWhenIdIsInvalid() {
        assertThatThrownBy(() -> rulesServices.getRuleById(0L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getRuleById_throwsNotFoundWhenRuleMissing() {
        when(rulesRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rulesServices.getRuleById(999L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getRuleById_throwsInternalServerErrorWhenRepositoryFails() {
        when(rulesRepository.findById(1L)).thenThrow(new DataAccessResourceFailureException("DB error"));

        assertThatThrownBy(() -> rulesServices.getRuleById(1L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void createRule_returnsSavedRuleWhenInputValid() {
        Rules input = new Rules("  New Rule  ", " velocity ", null, 15, 4, " medium ", true);
        Rules saved = new Rules(10L, "New Rule", "VELOCITY", null, 15, 4, "MEDIUM", true);
        when(rulesRepository.save(any(Rules.class))).thenReturn(saved);

        Rules result = rulesServices.createRule(input);

        assertThat(result.getId()).isEqualTo(10L);
        verify(rulesRepository).save(any(Rules.class));
    }

    @Test
    void createRule_setsActiveTrueWhenMissing() {
        Rules input = new Rules("Rule X", "VELOCITY", null, 10, 5, "MEDIUM", null);
        Rules saved = new Rules(11L, "Rule X", "VELOCITY", null, 10, 5, "MEDIUM", true);
        when(rulesRepository.save(any(Rules.class))).thenReturn(saved);

        Rules result = rulesServices.createRule(input);

        assertThat(result.getActive()).isTrue();
    }

    @Test
    void createRule_throwsBadRequestWhenPayloadIsNull() {
        assertThatThrownBy(() -> rulesServices.createRule(null))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createRule_throwsBadRequestWhenRequiredFieldsMissing() {
        Rules input = new Rules();

        assertThatThrownBy(() -> rulesServices.createRule(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> {
                    GeneralizedException gex = (GeneralizedException) ex;
                    assertThat(gex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(gex.getDetails()).containsKeys("name", "type");
                });
    }

    @Test
    void createRule_throwsBadRequestWhenNumericFieldsInvalid() {
        Rules input = new Rules("Rule", "TYPE", new BigDecimal("-1.00"), 0, -5, "HIGH", true);

        assertThatThrownBy(() -> rulesServices.createRule(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> {
                    GeneralizedException gex = (GeneralizedException) ex;
                    assertThat(gex.getDetails()).containsKeys("threshold", "timeWindow", "maxTransactions");
                });
    }

    @Test
    void createRule_throwsBadRequestWhenStringFieldsExceedLength() {
        Rules input = new Rules(
                "A".repeat(101),
                "B".repeat(51),
                null,
                null,
                null,
                "C".repeat(21),
                true);

        assertThatThrownBy(() -> rulesServices.createRule(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> {
                    GeneralizedException gex = (GeneralizedException) ex;
                    assertThat(gex.getDetails()).containsKeys("name", "type", "severity");
                });
    }

    @Test
    void createRule_throwsInternalServerErrorWhenRepositoryFails() {
        Rules input = new Rules("Rule", "VELOCITY", null, 10, 5, "MEDIUM", true);
        when(rulesRepository.save(any(Rules.class))).thenThrow(new DataAccessResourceFailureException("DB error"));

        assertThatThrownBy(() -> rulesServices.createRule(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void createRule_generatesAlertAndSetsTransactionPendingWhenRuleMatches() {
        Rules input = new Rules("Large Tx", "AMOUNT_THRESHOLD", new BigDecimal("10000.00"), null, null, "HIGH", true);
        Rules saved = new Rules(50L, "Large Tx", "AMOUNT_THRESHOLD", new BigDecimal("10000.00"), null, null, "HIGH", true);
        BankTransactions tx = new BankTransactions();
        tx.setId(9L);
        tx.setStatus("COMPLETED");

        when(rulesRepository.save(any(Rules.class))).thenReturn(saved);
        when(bankTransactionsRepository.findAll()).thenReturn(List.of(tx));
        when(ruleEngineService.matchesPersistedTransaction(eq(tx), eq(saved), any(List.class))).thenReturn(true);
        when(alertsRepository.hasActiveAlertForTransactionAndRule(9L, 50L)).thenReturn(false);
        when(ruleEngineService.buildAlertReason(saved)).thenReturn("Rule matched");
        when(bankTransactionsRepository.updateStatus(9L, "PENDING")).thenReturn(true);

        Rules result = rulesServices.createRule(input);

        assertThat(result.getId()).isEqualTo(50L);
        verify(alertsService).createAlert(any());
        verify(bankTransactionsRepository).updateStatus(9L, "PENDING");
    }

    @Test
    void createRule_doesNotCreateDuplicateAlertWhenActiveAlertAlreadyExists() {
        Rules input = new Rules("Velocity", "VELOCITY", null, 10, 5, "MEDIUM", true);
        Rules saved = new Rules(51L, "Velocity", "VELOCITY", null, 10, 5, "MEDIUM", true);
        BankTransactions tx = new BankTransactions();
        tx.setId(10L);

        when(rulesRepository.save(any(Rules.class))).thenReturn(saved);
        when(bankTransactionsRepository.findAll()).thenReturn(List.of(tx));
        when(ruleEngineService.matchesPersistedTransaction(eq(tx), eq(saved), any(List.class))).thenReturn(true);
        when(alertsRepository.hasActiveAlertForTransactionAndRule(10L, 51L)).thenReturn(true);

        rulesServices.createRule(input);

        verifyNoInteractions(alertsService);
    }

    @Test
    void createRule_doesNotUpdateTransactionStatusWhenAlreadyPending() {
        Rules input = new Rules("Daily", "DAILY_LIMIT", new BigDecimal("5000.00"), null, null, "HIGH", true);
        Rules saved = new Rules(52L, "Daily", "DAILY_LIMIT", new BigDecimal("5000.00"), null, null, "HIGH", true);
        BankTransactions tx = new BankTransactions();
        tx.setId(11L);
        tx.setStatus("PENDING");

        when(rulesRepository.save(any(Rules.class))).thenReturn(saved);
        when(bankTransactionsRepository.findAll()).thenReturn(List.of(tx));
        when(ruleEngineService.matchesPersistedTransaction(eq(tx), eq(saved), any(List.class))).thenReturn(true);
        when(alertsRepository.hasActiveAlertForTransactionAndRule(11L, 52L)).thenReturn(false);
        when(ruleEngineService.buildAlertReason(saved)).thenReturn("Daily limit exceeded");

        rulesServices.createRule(input);

        verify(alertsService).createAlert(any());
    }

    @Test
    void updateRule_returnsUpdatedRuleWhenSuccessful() {
        Rules input = new Rules("Updated", "velocity", null, 20, 10, "medium", true);
        Rules updated = new Rules(2L, "Updated", "VELOCITY", null, 20, 10, "MEDIUM", true);
        when(rulesRepository.update(eq(2L), any(Rules.class))).thenReturn(true);
        when(rulesRepository.findById(2L)).thenReturn(Optional.of(updated));

        Rules result = rulesServices.updateRule(2L, input);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getType()).isEqualTo("VELOCITY");
    }

    @Test
    void updateRule_throwsNotFoundWhenRuleMissing() {
        Rules input = new Rules("Updated", "VELOCITY", null, 20, 10, "MEDIUM", true);
        when(rulesRepository.update(eq(500L), any(Rules.class))).thenReturn(false);

        assertThatThrownBy(() -> rulesServices.updateRule(500L, input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateRule_throwsBadRequestWhenIdInvalid() {
        Rules input = new Rules("Updated", "VELOCITY", null, 20, 10, "MEDIUM", true);

        assertThatThrownBy(() -> rulesServices.updateRule(-1L, input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateRule_throwsInternalServerErrorWhenRepositoryFails() {
        Rules input = new Rules("Updated", "VELOCITY", null, 20, 10, "MEDIUM", true);
        when(rulesRepository.update(eq(2L), any(Rules.class))).thenThrow(new DataAccessResourceFailureException("DB error"));

        assertThatThrownBy(() -> rulesServices.updateRule(2L, input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void deleteRule_completesWhenRuleExists() {
        when(rulesRepository.deleteById(7L)).thenReturn(true);

        rulesServices.deleteRule(7L);

        verify(alertsRepository).deleteAlertsByRuleId(7L);
        verify(rulesRepository).deleteById(7L);
    }

    @Test
    void deleteRule_throwsNotFoundWhenRuleMissing() {
        when(rulesRepository.deleteById(700L)).thenReturn(false);

        assertThatThrownBy(() -> rulesServices.deleteRule(700L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteRule_throwsBadRequestWhenIdInvalid() {
        assertThatThrownBy(() -> rulesServices.deleteRule(null))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deleteRule_throwsInternalServerErrorWhenRepositoryFails() {
        when(rulesRepository.deleteById(8L)).thenThrow(new DataAccessResourceFailureException("DB error"));

        assertThatThrownBy(() -> rulesServices.deleteRule(8L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void deleteRule_throwsInternalServerErrorWhenAlertCleanupFails() {
        when(alertsRepository.deleteAlertsByRuleId(8L)).thenThrow(new DataAccessResourceFailureException("DB error"));

        assertThatThrownBy(() -> rulesServices.deleteRule(8L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
