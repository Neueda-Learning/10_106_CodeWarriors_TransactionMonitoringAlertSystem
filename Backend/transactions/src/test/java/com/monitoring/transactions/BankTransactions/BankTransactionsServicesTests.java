package com.monitoring.transactions.BankTransactions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankTransactionsServicesTests {

    @Mock
    private BankTransactionsRepository bankTransactionsRepository;

    private BankTransactionServices bankTransactionServices;

    private final LocalDateTime fixedTime = LocalDateTime.of(2026, 8, 1, 10, 0, 0);

    @BeforeEach
    void setUp() {
        bankTransactionServices = new BankTransactionServices(bankTransactionsRepository);
    }

    @Test
    void getAllTransactions_returnsTransactionsWhenAvailable() {
        List<BankTransactions> expected = List.of(
                new BankTransactions(1L, 1L, 2L, new BigDecimal("100.00"), "USD", fixedTime, "COMPLETED", fixedTime),
                new BankTransactions(2L, 3L, 4L, new BigDecimal("40.00"), "EUR", fixedTime, "PENDING", fixedTime));
        when(bankTransactionsRepository.findAll()).thenReturn(expected);

        List<BankTransactions> result = bankTransactionServices.getAllTransactions();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllTransactions_throwsInternalServerErrorWhenRepositoryFails() {
        when(bankTransactionsRepository.findAll()).thenThrow(new DataAccessResourceFailureException("DB error"));

        assertThatThrownBy(() -> bankTransactionServices.getAllTransactions())
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void getTransactionById_returnsTransactionWhenFound() {
        BankTransactions tx = new BankTransactions(10L, 2L, 3L, new BigDecimal("75.00"), "USD", fixedTime, "FAILED", fixedTime);
        when(bankTransactionsRepository.findById(10L)).thenReturn(Optional.of(tx));

        BankTransactions result = bankTransactionServices.getTransactionById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void getTransactionById_throwsBadRequestWhenIdIsInvalid() {
        assertThatThrownBy(() -> bankTransactionServices.getTransactionById(0L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getTransactionById_throwsNotFoundWhenTransactionMissing() {
        when(bankTransactionsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankTransactionServices.getTransactionById(999L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void createTransaction_savesValidTransactionAndNormalizesTextFields() {
        BankTransactions input = new BankTransactions(1L, 2L, new BigDecimal("55.50"), " usd ", fixedTime, " pending ");
        BankTransactions saved = new BankTransactions(25L, 1L, 2L, new BigDecimal("55.50"), "USD", fixedTime, "PENDING", fixedTime);
        when(bankTransactionsRepository.save(any(BankTransactions.class))).thenReturn(saved);

        BankTransactions result = bankTransactionServices.createTransaction(input);

        assertThat(result.getId()).isEqualTo(25L);
        verify(bankTransactionsRepository).save(any(BankTransactions.class));
    }

    @Test
    void createTransaction_throwsBadRequestWhenPayloadIsNull() {
        assertThatThrownBy(() -> bankTransactionServices.createTransaction(null))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createTransaction_throwsBadRequestWhenPayloadHasMultipleValidationErrors() {
        BankTransactions input = new BankTransactions();
        input.setFromAccountId(0L);
        input.setToAccountId(-1L);
        input.setAmount(BigDecimal.ZERO);
        input.setCurrency("   ");
        input.setStatus("UNKNOWN");
        input.setTransactionTime(null);

        assertThatThrownBy(() -> bankTransactionServices.createTransaction(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> {
                    GeneralizedException gex = (GeneralizedException) ex;
                    assertThat(gex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(gex.getDetails()).containsKeys(
                            "fromAccountId", "toAccountId", "amount", "currency", "transactionTime", "status");
                });
    }

    @Test
    void createTransaction_throwsInternalServerErrorWhenRepositoryFails() {
        BankTransactions input = new BankTransactions(1L, 2L, new BigDecimal("20.00"), "USD", fixedTime, "PENDING");
        when(bankTransactionsRepository.save(any(BankTransactions.class)))
                .thenThrow(new DataAccessResourceFailureException("DB error"));

        assertThatThrownBy(() -> bankTransactionServices.createTransaction(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void updateTransaction_returnsUpdatedTransactionWhenSuccessful() {
        BankTransactions input = new BankTransactions(1L, 2L, new BigDecimal("200.00"), "usd", fixedTime, "completed");
        BankTransactions updated = new BankTransactions(50L, 1L, 2L, new BigDecimal("200.00"), "USD", fixedTime, "COMPLETED", fixedTime);
        when(bankTransactionsRepository.update(eq(50L), any(BankTransactions.class))).thenReturn(true);
        when(bankTransactionsRepository.findById(50L)).thenReturn(Optional.of(updated));

        BankTransactions result = bankTransactionServices.updateTransaction(50L, input);

        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void updateTransaction_throwsNotFoundWhenUpdateAffectsNoRows() {
        BankTransactions input = new BankTransactions(1L, 2L, new BigDecimal("200.00"), "USD", fixedTime, "COMPLETED");
        when(bankTransactionsRepository.update(eq(404L), any(BankTransactions.class))).thenReturn(false);

        assertThatThrownBy(() -> bankTransactionServices.updateTransaction(404L, input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateTransaction_throwsBadRequestWhenIdIsInvalid() {
        BankTransactions input = new BankTransactions(1L, 2L, new BigDecimal("20.00"), "USD", fixedTime, "PENDING");

        assertThatThrownBy(() -> bankTransactionServices.updateTransaction(-2L, input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deleteTransaction_completesWhenTransactionExists() {
        when(bankTransactionsRepository.deleteById(8L)).thenReturn(true);

        bankTransactionServices.deleteTransaction(8L);

        verify(bankTransactionsRepository).deleteById(8L);
    }

    @Test
    void deleteTransaction_throwsNotFoundWhenTransactionMissing() {
        when(bankTransactionsRepository.deleteById(300L)).thenReturn(false);

        assertThatThrownBy(() -> bankTransactionServices.deleteTransaction(300L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteTransaction_throwsBadRequestWhenIdIsInvalid() {
        assertThatThrownBy(() -> bankTransactionServices.deleteTransaction(null))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deleteTransaction_throwsInternalServerErrorWhenRepositoryFails() {
        when(bankTransactionsRepository.deleteById(2L)).thenThrow(new DataAccessResourceFailureException("DB error"));

        assertThatThrownBy(() -> bankTransactionServices.deleteTransaction(2L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
