package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Internal API — consumed only by other microservices (e.g. transaction-service).
 * Not exposed through api-gateway. No JWT required (service-to-service, internal network only).
 */
@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountService accountService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<AccountResponse> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(accountService.getAccountByUserId(userId));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @PostMapping("/{accountNumber}/debit")
    public ResponseEntity<Void> debit(
            @PathVariable String accountNumber,
            @RequestBody Map<String, BigDecimal> body
    ) {
        accountService.debit(accountNumber, body.get("amount"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<Void> credit(
            @PathVariable String accountNumber,
            @RequestBody Map<String, BigDecimal> body
    ) {
        accountService.credit(accountNumber, body.get("amount"));
        return ResponseEntity.ok().build();
    }
}
