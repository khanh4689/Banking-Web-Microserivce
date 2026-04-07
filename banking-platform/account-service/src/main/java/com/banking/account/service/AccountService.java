package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.entity.Account;
import com.banking.account.entity.AccountStatus;
import com.banking.account.exception.AccountNotFoundException;
import com.banking.account.exception.InsufficientBalanceException;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final Random random = new Random();

    @Transactional
    public AccountResponse createAccount(String userId) {
        log.info("ACCOUNT_CREATE_START - userId: {}", userId);

        if (accountRepository.existsByUserId(userId)) {
            log.warn("Account already exists for userId={}, returning existing account", userId);
            return toResponse(accountRepository.findByUserId(userId).orElseThrow());
        }

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .accountNumber(generateAccountNumber())
                .balance(BigDecimal.ZERO.setScale(2))
                .currency("VND")
                .status(AccountStatus.ACTIVE)
                .build();

        accountRepository.save(account);
        log.info("ACCOUNT_CREATE - userId: {}, accountNumber: {}", userId, account.getAccountNumber());
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByUserId(String userId) {
        log.info("Get account by userId={}", userId);
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Account not found for userId={}", userId);
                    return new AccountNotFoundException("Account not found for userId: " + userId);
                });
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        log.info("Get account by accountNumber={}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn("Account not found: accountNumber={}", accountNumber);
                    return new AccountNotFoundException("Account not found: " + accountNumber);
                });
        return toResponse(account);
    }

    /**
     * Debit: subtract amount from account balance.
     * Uses pessimistic write lock to prevent concurrent balance corruption.
     */
    @Transactional
    public void debit(String accountNumber, BigDecimal amount) {
        log.info("WITHDRAW_START - accountNumber: {}, amount: {}", accountNumber, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("WITHDRAW_REJECTED - invalid amount={} for accountNumber={}", amount, accountNumber);
            throw new IllegalArgumentException("Debit amount must be greater than zero");
        }

        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> {
                    log.warn("Debit failed — account not found: accountNumber={}", accountNumber);
                    return new AccountNotFoundException("Account not found: " + accountNumber);
                });

        if (account.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Debit failed — account not ACTIVE: accountNumber={}, status={}", accountNumber, account.getStatus());
            throw new IllegalStateException("Account is not ACTIVE: " + accountNumber);
        }
        if (account.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance: accountNumber={}, balance={}, requested={}", accountNumber, account.getBalance(), amount);
            throw new InsufficientBalanceException("Insufficient balance for account: " + accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount).setScale(2, java.math.RoundingMode.HALF_UP));
        accountRepository.save(account);
        log.info("WITHDRAW_SUCCESS - accountNumber: {}, amount: {}, newBalance: {}", accountNumber, amount, account.getBalance());
    }

    /**
     * Credit: add amount to account balance.
     * Uses pessimistic write lock to prevent concurrent balance corruption.
     */
    @Transactional
    public void credit(String accountNumber, BigDecimal amount) {
        log.info("DEPOSIT_START - accountNumber: {}, amount: {}", accountNumber, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("DEPOSIT_REJECTED - invalid amount={} for accountNumber={}", amount, accountNumber);
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }

        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> {
                    log.warn("Credit failed — account not found: accountNumber={}", accountNumber);
                    return new AccountNotFoundException("Account not found: " + accountNumber);
                });

        if (account.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Credit failed — account not ACTIVE: accountNumber={}, status={}", accountNumber, account.getStatus());
            throw new IllegalStateException("Account is not ACTIVE: " + accountNumber);
        }

        account.setBalance(account.getBalance().add(amount).setScale(2, java.math.RoundingMode.HALF_UP));
        accountRepository.save(account);
        log.info("DEPOSIT_SUCCESS - accountNumber: {}, amount: {}, newBalance: {}", accountNumber, amount, account.getBalance());
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = String.format("%010d", (long) (random.nextDouble() * 9_000_000_000L) + 1_000_000_000L);
        } while (accountRepository.findByAccountNumber(number).isPresent());
        return number;
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .version(account.getVersion())
                .build();
    }
}
