package com.monitoring.transactions.Accounts;

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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountsControllerTests {

    @Mock
    private AccountsServices accountsServices;

    private MockMvc mockMvc;

    private final LocalDateTime fixedTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

    @BeforeEach
    void setUp() {
        AccountsController controller = new AccountsController(accountsServices);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllAccounts_returns200WithListOfAccounts() throws Exception {
        when(accountsServices.getAllAccounts()).thenReturn(List.of(
                new Accounts(1L, "Customer 1", "CHECKING", "US", fixedTime),
                new Accounts(2L, "Customer 2", "SAVINGS", "AU", fixedTime)));

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].customerName").value("Customer 1"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getAllAccounts_returns200WithEmptyList() throws Exception {
        when(accountsServices.getAllAccounts()).thenReturn(List.of());

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllAccounts_returns500WhenServiceFails() throws Exception {
        when(accountsServices.getAllAccounts())
                .thenThrow(new GeneralizedException("Unable to fetch accounts.", HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void getAccountById_returns200WithAccount() throws Exception {
        Accounts account = new Accounts(5L, "Customer 5", "CORPORATE", "CA", fixedTime);
        when(accountsServices.getAccountById(5L)).thenReturn(account);

        mockMvc.perform(get("/accounts/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.customerName").value("Customer 5"))
                .andExpect(jsonPath("$.accountType").value("CORPORATE"))
                .andExpect(jsonPath("$.country").value("CA"));
    }

    @Test
    void getAccountById_returns404WhenAccountNotFound() throws Exception {
        when(accountsServices.getAccountById(999L))
                .thenThrow(new GeneralizedException("Account not found for id: 999", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/accounts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getAccountById_returns400WhenIdIsInvalid() throws Exception {
        when(accountsServices.getAccountById(0L))
                .thenThrow(new GeneralizedException("Account id must be a positive number.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(get("/accounts/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAccount_returns201WithCreatedAccount() throws Exception {
        Accounts saved = new Accounts(10L, "New Customer", "SAVINGS", "UK", fixedTime);
        when(accountsServices.createAccount(any(Accounts.class))).thenReturn(saved);

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"New Customer","accountType":"SAVINGS","country":"UK"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.customerName").value("New Customer"))
                .andExpect(jsonPath("$.accountType").value("SAVINGS"));
    }

    @Test
    void createAccount_returns400WhenValidationFails() throws Exception {
        when(accountsServices.createAccount(any(Accounts.class)))
                .thenThrow(new GeneralizedException("Invalid account input.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":null,"accountType":"INVALID"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createAccount_returns400WhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-valid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAccount_returns200WithUpdatedAccount() throws Exception {
        Accounts updated = new Accounts(3L, "Updated Name", "CORPORATE", "AU", fixedTime);
        when(accountsServices.updateAccount(eq(3L), any(Accounts.class))).thenReturn(updated);

        mockMvc.perform(put("/accounts/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Updated Name","accountType":"CORPORATE","country":"AU"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.customerName").value("Updated Name"));
    }

    @Test
    void updateAccount_returns404WhenAccountNotFound() throws Exception {
        when(accountsServices.updateAccount(eq(999L), any(Accounts.class)))
                .thenThrow(new GeneralizedException("Account not found for id: 999", HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/accounts/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Customer","accountType":"SAVINGS","country":"US"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAccount_returns400WhenIdIsInvalid() throws Exception {
        when(accountsServices.updateAccount(eq(0L), any(Accounts.class)))
                .thenThrow(new GeneralizedException("Account id must be a positive number.", HttpStatus.BAD_REQUEST));

        mockMvc.perform(put("/accounts/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Updated Name","accountType":"CHECKING","country":"US"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateAccount_returns400WhenBodyIsMalformed() throws Exception {
        mockMvc.perform(put("/accounts/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAccount_returns204WhenDeleted() throws Exception {
        doNothing().when(accountsServices).deleteAccount(7L);

        mockMvc.perform(delete("/accounts/7"))
                .andExpect(status().isNoContent());

        verify(accountsServices).deleteAccount(7L);
    }

    @Test
    void deleteAccount_returns404WhenAccountNotFound() throws Exception {
        doThrow(new GeneralizedException("Account not found for id: 999", HttpStatus.NOT_FOUND))
                .when(accountsServices).deleteAccount(999L);

        mockMvc.perform(delete("/accounts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAccount_returns400WhenIdIsInvalid() throws Exception {
        doThrow(new GeneralizedException("Account id must be a positive number.", HttpStatus.BAD_REQUEST))
                .when(accountsServices).deleteAccount(0L);

        mockMvc.perform(delete("/accounts/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createAccount_returns500WhenServiceThrowsUnexpectedError() throws Exception {
        when(accountsServices.createAccount(any(Accounts.class)))
                .thenThrow(new GeneralizedException("Unable to create account.", HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Customer","accountType":"CHECKING","country":"US"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }
}
