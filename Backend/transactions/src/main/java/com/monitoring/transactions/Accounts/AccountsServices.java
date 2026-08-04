package com.monitoring.transactions.Accounts;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.monitoring.transactions.Exception.GeneralizedException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AccountsServices {

  private static final Set<String> ALLOWED_ACCOUNT_TYPES = Set.of("CHECKING", "SAVINGS", "CORPORATE");

  private final AccountsRepository accountsRepository;

  public AccountsServices(AccountsRepository accountsRepository) {
    this.accountsRepository = accountsRepository;
  }

  public List<Accounts> getAllAccounts() {
    try {
      return accountsRepository.findAll();
    } catch (DataAccessException exception) {
      throw new GeneralizedException(
        "Unable to fetch accounts.",
        exception,
        HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public Accounts getAccountById(Long id) {
    validateId(id);
    try {
      return accountsRepository.findById(id)
        .orElseThrow(() -> new GeneralizedException("Account not found for id: " + id, HttpStatus.NOT_FOUND));
    } catch (DataAccessException exception) {
      throw new GeneralizedException(
        "Unable to fetch account.",
        exception,
        HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public Accounts createAccount(Accounts account) {
    validateAccount(account);
    try {
      Accounts newAccount = new Accounts(account.getCustomerName(), account.getAccountType(), account.getCountry());
      return accountsRepository.save(newAccount);
    } catch (DataAccessException exception) {
      throw new GeneralizedException(
        "Unable to create account.",
        exception,
        HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public Accounts updateAccount(Long id, Accounts account) {
    validateId(id);
    validateAccount(account);
    try {
      boolean updated = accountsRepository.update(id, account);
      if (!updated) {
        throw new GeneralizedException("Account not found for id: " + id, HttpStatus.NOT_FOUND);
      }
      return getAccountById(id);
    } catch (DataAccessException exception) {
      throw new GeneralizedException(
        "Unable to update account.",
        exception,
        HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public void deleteAccount(Long id) {
    validateId(id);
    try {
      boolean deleted = accountsRepository.deleteById(id);
      if (!deleted) {
        throw new GeneralizedException("Account not found for id: " + id, HttpStatus.NOT_FOUND);
      }
    } catch (DataAccessException exception) {
      throw new GeneralizedException(
        "Unable to delete account.",
        exception,
        HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private void validateId(Long id) {
    if (id == null || id <= 0) {
      throw new GeneralizedException("Account id must be a positive number.", HttpStatus.BAD_REQUEST);
    }
  }

  private void validateAccount(Accounts account) {
    Map<String, String> details = new LinkedHashMap<>();
    if (account == null) {
      throw new GeneralizedException("Account payload is required.", HttpStatus.BAD_REQUEST);
    }

    String customerName = normalize(account.getCustomerName());
    String accountType = normalize(account.getAccountType());
    String country = normalize(account.getCountry());

    if (customerName == null) {
      details.put("customerName", "customerName is required.");
    } else if (customerName.length() > 100) {
      details.put("customerName", "customerName must be at most 100 characters.");
    }

    if (accountType == null) {
      details.put("accountType", "accountType is required.");
    } else {
      String upperType = accountType.toUpperCase(Locale.ROOT);
      if (!ALLOWED_ACCOUNT_TYPES.contains(upperType)) {
        details.put("accountType", "accountType must be one of CHECKING, SAVINGS, CORPORATE.");
      } else {
        accountType = upperType;
      }
    }

    if (country != null && country.length() > 50) {
      details.put("country", "country must be at most 50 characters.");
    }

    if (!details.isEmpty()) {
      throw new GeneralizedException("Invalid account input.", HttpStatus.BAD_REQUEST, details);
    }

    account.setCustomerName(customerName);
    account.setAccountType(accountType);
    account.setCountry(country);
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
