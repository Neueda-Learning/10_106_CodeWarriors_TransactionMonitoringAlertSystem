package com.monitoring.transactions.BankTransactions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.monitoring.transactions.Exception.GeneralizedException;
import com.monitoring.transactions.Exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BankTransactionsControllerTests {

    @Mock
    private BankTransactionServices bankTransactionServices;

    private MockMvc mockMvc;

    private final LocalDateTime fixedTime = LocalDateTime.of(2026, 8, 1, 10, 0, 0);

    @BeforeEach
    void setUp() {
        BankTransactionsController controller = new BankTransactionsController(bankTransactionServices);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllTransactions_returns200WithTransactions() throws Exception {
        when(bankTransactionServices.getAllTransactions()).thenReturn(List.of(
                new BankTransactions(1L, 1L, 2L, new BigDecimal("100.00"), "USD", fixedTime, "COMPLETED", fixedTime),
                new BankTransactions(2L, 3L, 4L, new BigDecimal("50.00"), "EUR", fixedTime, "PENDING", fixedTime)));

        mockMvc.perform(get("/bank-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].status").value("PENDING"));
    }

    @Test
    void getAllTransactions_returns200WithEmptyList() throws Exception {
        when(bankTransactionServices.getAllTransactions()).thenReturn(List.of());

        mockMvc.perform(get("/bank-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getTransactionById_returns200WhenFound() throws Exception {
        BankTransactions transaction = new BankTransactions(10L, 1L, 2L, new BigDecimal("77.77"), "USD", fixedTime, "FAILED", fixedTime);
        when(bankTransactionServices.getTransactionById(10L)).thenReturn(transaction);

        mockMvc.perform(get("/bank-transactions/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void getTransactionById_returns404WhenNotFound() throws Exception {
        when(bankTransactionServices.getTransactionById(404L))
                .thenThrow(new GeneralizedException("Transaction not found for id: 404", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/bank-transactions/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getTransactionById_returns400WhenIdInvalid() throws Exception {
        when(bankTransactionServices.getTransactionById(0L))
                .thenThrow(new GeneralizedException("Transaction id must be a positive number.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(get("/bank-transactions/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createTransaction_returns201WithCreatedTransaction() throws Exception {
        BankTransactions created = new BankTransactions(20L, 2L, 3L, new BigDecimal("45.00"), "USD", fixedTime, "PENDING", fixedTime);
        when(bankTransactionServices.createTransaction(any(BankTransactions.class))).thenReturn(created);

        mockMvc.perform(post("/bank-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId": 2,
                                  "toAccountId": 3,
                                  "amount": 45.00,
                                  "currency": "USD",
                                  "transactionTime": "2026-08-01T10:00:00",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void createTransaction_returns400WhenPayloadInvalid() throws Exception {
        when(bankTransactionServices.createTransaction(any(BankTransactions.class)))
                .thenThrow(new GeneralizedException("Invalid bank transaction input.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/bank-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId": 0,
                                  "toAccountId": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createTransaction_returns400WhenBodyMalformed() throws Exception {
        mockMvc.perform(post("/bank-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTransaction_returns200WithUpdatedTransaction() throws Exception {
        BankTransactions updated = new BankTransactions(8L, 3L, 4L, new BigDecimal("150.00"), "EUR", fixedTime, "COMPLETED", fixedTime);
        when(bankTransactionServices.updateTransaction(eq(8L), any(BankTransactions.class))).thenReturn(updated);

        mockMvc.perform(put("/bank-transactions/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId": 3,
                                  "toAccountId": 4,
                                  "amount": 150.00,
                                  "currency": "EUR",
                                  "transactionTime": "2026-08-01T10:00:00",
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void updateTransaction_returns404WhenTransactionMissing() throws Exception {
        when(bankTransactionServices.updateTransaction(eq(1000L), any(BankTransactions.class)))
                .thenThrow(new GeneralizedException("Transaction not found for id: 1000", HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/bank-transactions/1000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromAccountId": 1,
                                  "toAccountId": 2,
                                  "amount": 10.00,
                                  "currency": "USD",
                                  "transactionTime": "2026-08-01T10:00:00",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteTransaction_returns204WhenDeleted() throws Exception {
        doNothing().when(bankTransactionServices).deleteTransaction(9L);

        mockMvc.perform(delete("/bank-transactions/9"))
                .andExpect(status().isNoContent());

        verify(bankTransactionServices).deleteTransaction(9L);
    }

    @Test
    void deleteTransaction_returns404WhenTransactionMissing() throws Exception {
        doThrow(new GeneralizedException("Transaction not found for id: 123", HttpStatus.NOT_FOUND))
                .when(bankTransactionServices).deleteTransaction(123L);

        mockMvc.perform(delete("/bank-transactions/123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
