package com.monitoring.transactions.Accounts;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = { "http://localhost:8090", "http://10.9.73.115:8090" })
@RequestMapping("/accounts")
public class AccountsController {

  private final AccountsServices accountsServices;

  public AccountsController(AccountsServices accountsServices) {
    this.accountsServices = accountsServices;
  }

  @GetMapping
  public List<Accounts> getAllAccounts() {
    return accountsServices.getAllAccounts();
  }

  @GetMapping("/{id}")
  public Accounts getAccountById(@PathVariable Long id) {
    return accountsServices.getAccountById(id);
  }

  @PostMapping
  public ResponseEntity<Accounts> createAccount(@RequestBody Accounts account) {
    Accounts created = accountsServices.createAccount(account);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{id}")
  public Accounts updateAccount(@PathVariable Long id, @RequestBody Accounts account) {
    return accountsServices.updateAccount(id, account);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
    accountsServices.deleteAccount(id);
    return ResponseEntity.noContent().build();
  }
}
