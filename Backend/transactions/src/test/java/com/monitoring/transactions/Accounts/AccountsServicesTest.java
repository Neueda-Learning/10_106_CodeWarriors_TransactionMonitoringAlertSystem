package com.monitoring.transactions.Accounts;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountsServicesTest {

    @Mock
    private AccountsRepository accountsRepository;

    private AccountsServices accountsServices;

    private final LocalDateTime fixedTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

    @BeforeEach
    void setUp() {
        accountsServices = new AccountsServices(accountsRepository);
    }

    @Test
    void getAllAccounts_returnsAllAccounts() {
        List<Accounts> accounts = List.of(
                new Accounts(1L, "Customer 1", "CHECKING", "US", fixedTime),
                new Accounts(2L, "Customer 2", "SAVINGS", "AU", fixedTime));
        when(accountsRepository.findAll()).thenReturn(accounts);

        List<Accounts> result = accountsServices.getAllAccounts();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void getAllAccounts_returnsEmptyListWhenNoAccounts() {
        when(accountsRepository.findAll()).thenReturn(List.of());

        List<Accounts> result = accountsServices.getAllAccounts();

        assertThat(result).isEmpty();
    }

    @Test
    void getAllAccounts_throwsInternalErrorWhenDatabaseFails() {
        when(accountsRepository.findAll()).thenThrow(new DataAccessResourceFailureException("DB down"));

        assertThatThrownBy(() -> accountsServices.getAllAccounts())
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void getAccountById_returnsAccountWhenFound() {
        Accounts account = new Accounts(5L, "Customer 5", "CORPORATE", "CA", fixedTime);
        when(accountsRepository.findById(5L)).thenReturn(Optional.of(account));

        Accounts result = accountsServices.getAccountById(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getCustomerName()).isEqualTo("Customer 5");
    }

    @Test
    void getAccountById_throwsNotFoundWhenAccountDoesNotExist() {
        when(accountsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountsServices.getAccountById(999L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getAccountById_throwsBadRequestForZeroId() {
        assertThatThrownBy(() -> accountsServices.getAccountById(0L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getAccountById_throwsBadRequestForNegativeId() {
        assertThatThrownBy(() -> accountsServices.getAccountById(-1L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getAccountById_throwsInternalErrorWhenDatabaseFails() {
        when(accountsRepository.findById(1L)).thenThrow(new DataAccessResourceFailureException("DB down"));

        assertThatThrownBy(() -> accountsServices.getAccountById(1L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void createAccount_savesAndReturnsNewAccount() {
        Accounts input = new Accounts("New Customer", "SAVINGS", "UK");
        Accounts saved = new Accounts(10L, "New Customer", "SAVINGS", "UK", fixedTime);
        when(accountsRepository.save(any(Accounts.class))).thenReturn(saved);

        Accounts result = accountsServices.createAccount(input);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getCustomerName()).isEqualTo("New Customer");
        assertThat(result.getAccountType()).isEqualTo("SAVINGS");
    }

    @Test
    void createAccount_normalizesAccountTypeToUppercase() {
        Accounts input = new Accounts("Customer", "savings", "US");
        Accounts saved = new Accounts(11L, "Customer", "SAVINGS", "US", fixedTime);
        when(accountsRepository.save(any(Accounts.class))).thenReturn(saved);

        Accounts result = accountsServices.createAccount(input);

        assertThat(result.getAccountType()).isEqualTo("SAVINGS");
    }

    @Test
    void createAccount_trimsWhitespaceFromFields() {
        Accounts input = new Accounts("  Customer  ", "  CHECKING  ", "  US  ");
        Accounts saved = new Accounts(12L, "Customer", "CHECKING", "US", fixedTime);
        when(accountsRepository.save(any(Accounts.class))).thenReturn(saved);

        accountsServices.createAccount(input);

        verify(accountsRepository).save(argThat(a ->
                "Customer".equals(a.getCustomerName()) &&
                "CHECKING".equals(a.getAccountType()) &&
                "US".equals(a.getCountry())));
    }

    @Test
    void createAccount_allowsNullCountry() {
        Accounts input = new Accounts("Customer", "CHECKING", null);
        Accounts saved = new Accounts(13L, "Customer", "CHECKING", null, fixedTime);
        when(accountsRepository.save(any(Accounts.class))).thenReturn(saved);

        Accounts result = accountsServices.createAccount(input);

        assertThat(result.getCountry()).isNull();
    }

    @Test
    void createAccount_throwsBadRequestWhenCustomerNameIsMissing() {
        Accounts input = new Accounts(null, "CHECKING", "US");

        assertThatThrownBy(() -> accountsServices.createAccount(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> {
                    GeneralizedException gex = (GeneralizedException) ex;
                    assertThat(gex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(gex.getDetails()).containsKey("customerName");
                });
    }

    @Test
    void createAccount_throwsBadRequestWhenCustomerNameIsBlank() {
        Accounts input = new Accounts("   ", "CHECKING", "US");

        assertThatThrownBy(() -> accountsServices.createAccount(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getDetails())
                        .containsKey("customerName"));
    }

    @Test
    void createAccount_throwsBadRequestWhenCustomerNameExceedsMaxLength() {
        Accounts input = new Accounts("A".repeat(101), "SAVINGS", "US");

        assertThatThrownBy(() -> accountsServices.createAccount(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getDetails())
                        .containsKey("customerName"));
    }

    @Test
    void createAccount_throwsBadRequestWhenAccountTypeIsMissing() {
        Accounts input = new Accounts("Customer", null, "US");

        assertThatThrownBy(() -> accountsServices.createAccount(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getDetails())
                        .containsKey("accountType"));
    }

    @Test
    void createAccount_throwsBadRequestWhenAccountTypeIsInvalid() {
        Accounts input = new Accounts("Customer", "INVALID_TYPE", "US");

        assertThatThrownBy(() -> accountsServices.createAccount(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getDetails())
                        .containsKey("accountType"));
    }

    @Test
    void createAccount_throwsBadRequestWhenCountryExceedsMaxLength() {
        Accounts input = new Accounts("Customer", "CHECKING", "C".repeat(51));

        assertThatThrownBy(() -> accountsServices.createAccount(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getDetails())
                        .containsKey("country"));
    }

    @Test
    void createAccount_collectsMultipleValidationErrors() {
        Accounts input = new Accounts(null, "BAD_TYPE", "US");

        assertThatThrownBy(() -> accountsServices.createAccount(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> {
                    GeneralizedException gex = (GeneralizedException) ex;
                    assertThat(gex.getDetails()).containsKey("customerName");
                    assertThat(gex.getDetails()).containsKey("accountType");
                });
    }

    @Test
    void createAccount_throwsInternalErrorWhenDatabaseFails() {
        Accounts input = new Accounts("Customer", "CHECKING", "US");
        when(accountsRepository.save(any(Accounts.class)))
                .thenThrow(new DataAccessResourceFailureException("DB down"));

        assertThatThrownBy(() -> accountsServices.createAccount(input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void updateAccount_returnsUpdatedAccount() {
        Accounts input = new Accounts("Updated", "CORPORATE", "AU");
        Accounts updated = new Accounts(3L, "Updated", "CORPORATE", "AU", fixedTime);
        when(accountsRepository.update(eq(3L), any(Accounts.class))).thenReturn(true);
        when(accountsRepository.findById(3L)).thenReturn(Optional.of(updated));

        Accounts result = accountsServices.updateAccount(3L, input);

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getCustomerName()).isEqualTo("Updated");
    }

    @Test
    void updateAccount_throwsNotFoundWhenAccountDoesNotExist() {
        Accounts input = new Accounts("Updated", "SAVINGS", "UK");
        when(accountsRepository.update(eq(999L), any(Accounts.class))).thenReturn(false);

        assertThatThrownBy(() -> accountsServices.updateAccount(999L, input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateAccount_throwsBadRequestForInvalidId() {
        Accounts input = new Accounts("Updated", "CHECKING", "US");

        assertThatThrownBy(() -> accountsServices.updateAccount(0L, input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateAccount_throwsBadRequestForInvalidPayload() {
        Accounts input = new Accounts(null, "BAD", "US");

        assertThatThrownBy(() -> accountsServices.updateAccount(1L, input))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deleteAccount_deletesSuccessfully() {
        when(accountsRepository.deleteById(5L)).thenReturn(true);

        accountsServices.deleteAccount(5L);

        verify(accountsRepository).deleteById(5L);
    }

    @Test
    void deleteAccount_throwsNotFoundWhenAccountDoesNotExist() {
        when(accountsRepository.deleteById(999L)).thenReturn(false);

        assertThatThrownBy(() -> accountsServices.deleteAccount(999L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteAccount_throwsBadRequestForInvalidId() {
        assertThatThrownBy(() -> accountsServices.deleteAccount(-5L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deleteAccount_throwsInternalErrorWhenDatabaseFails() {
        when(accountsRepository.deleteById(1L)).thenThrow(new DataAccessResourceFailureException("DB down"));

        assertThatThrownBy(() -> accountsServices.deleteAccount(1L))
                .isInstanceOf(GeneralizedException.class)
                .satisfies(ex -> assertThat(((GeneralizedException) ex).getStatus())
                        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
