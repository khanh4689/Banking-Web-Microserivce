package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.entity.Account;
import com.banking.account.entity.AccountStatus;
import com.banking.account.exception.AccountNotFoundException;
import com.banking.account.exception.InsufficientBalanceException;
import com.banking.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;

    @InjectMocks private AccountService accountService;

    private Account activeAccount;
    private final String ACCOUNT_NUMBER = "1234567890";
    private final String USER_ID = "user-001";

    @BeforeEach
    void setUp() {
        activeAccount = Account.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .accountNumber(ACCOUNT_NUMBER)
                .balance(new BigDecimal("500.00"))
                .currency("VND")
                .status(AccountStatus.ACTIVE)
                .version(0L)
                .build();
    }

    // ─── createAccount() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createAccount: success — new account created with zero balance")
    void createAccount_success() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.createAccount(USER_ID);

        assertNotNull(response);
        assertEquals(USER_ID, response.getUserId());
        assertEquals(new BigDecimal("0.00"), response.getBalance());
        assertEquals("VND", response.getCurrency());
        assertEquals(AccountStatus.ACTIVE, response.getStatus());

        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("createAccount: returns existing account when user already has one")
    void createAccount_returnsExistingWhenAlreadyExists() {
        when(accountRepository.existsByUserId(USER_ID)).thenReturn(true);
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeAccount));

        AccountResponse response = accountService.createAccount(USER_ID);

        assertEquals(ACCOUNT_NUMBER, response.getAccountNumber());
        verify(accountRepository, never()).save(any());
    }

    // ─── getAccountByUserId() ─────────────────────────────────────────────────

    @Test
    @DisplayName("getAccountByUserId: success")
    void getAccountByUserId_success() {
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(activeAccount));

        AccountResponse response = accountService.getAccountByUserId(USER_ID);

        assertEquals(USER_ID, response.getUserId());
        assertEquals(ACCOUNT_NUMBER, response.getAccountNumber());
    }

    @Test
    @DisplayName("getAccountByUserId: throws AccountNotFoundException when not found")
    void getAccountByUserId_throwsWhenNotFound() {
        when(accountRepository.findByUserId("unknown")).thenReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccountByUserId("unknown"));

        assertTrue(ex.getMessage().contains("unknown"));
    }

    // ─── getAccountByNumber() ─────────────────────────────────────────────────

    @Test
    @DisplayName("getAccountByNumber: success")
    void getAccountByNumber_success() {
        when(accountRepository.findByAccountNumber(ACCOUNT_NUMBER)).thenReturn(Optional.of(activeAccount));

        AccountResponse response = accountService.getAccountByNumber(ACCOUNT_NUMBER);

        assertEquals(ACCOUNT_NUMBER, response.getAccountNumber());
    }

    @Test
    @DisplayName("getAccountByNumber: throws AccountNotFoundException when not found")
    void getAccountByNumber_throwsWhenNotFound() {
        when(accountRepository.findByAccountNumber("9999999999")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccountByNumber("9999999999"));
    }

    // ─── debit() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("debit: success — balance reduced correctly")
    void debit_success() {
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(activeAccount));

        accountService.debit(ACCOUNT_NUMBER, new BigDecimal("200.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(new BigDecimal("300.00"), captor.getValue().getBalance());
    }

    @Test
    @DisplayName("debit: throws AccountNotFoundException when account not found")
    void debit_throwsWhenAccountNotFound() {
        when(accountRepository.findByAccountNumberWithLock("bad-acc")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.debit("bad-acc", new BigDecimal("100.00")));

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("debit: throws InsufficientBalanceException when balance too low")
    void debit_throwsWhenInsufficientBalance() {
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(activeAccount));

        InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class,
                () -> accountService.debit(ACCOUNT_NUMBER, new BigDecimal("999.00")));

        assertTrue(ex.getMessage().contains(ACCOUNT_NUMBER));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("debit: throws IllegalStateException when account is not ACTIVE")
    void debit_throwsWhenAccountNotActive() {
        activeAccount.setStatus(AccountStatus.SUSPENDED);
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(activeAccount));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> accountService.debit(ACCOUNT_NUMBER, new BigDecimal("100.00")));

        assertTrue(ex.getMessage().contains("not ACTIVE"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("debit: edge case — exact balance debit succeeds")
    void debit_exactBalanceSucceeds() {
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(activeAccount));

        accountService.debit(ACCOUNT_NUMBER, new BigDecimal("500.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(new BigDecimal("0.00"), captor.getValue().getBalance());
    }

    @Test
    @DisplayName("debit: throws IllegalArgumentException when amount is zero")
    void debit_throwsWhenAmountIsZero() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.debit(ACCOUNT_NUMBER, BigDecimal.ZERO));

        assertEquals("Debit amount must be greater than zero", ex.getMessage());
        verify(accountRepository, never()).findByAccountNumberWithLock(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("debit: throws IllegalArgumentException when amount is negative")
    void debit_throwsWhenAmountIsNegative() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.debit(ACCOUNT_NUMBER, new BigDecimal("-50.00")));

        assertEquals("Debit amount must be greater than zero", ex.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("debit: throws IllegalArgumentException when amount is null")
    void debit_throwsWhenAmountIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> accountService.debit(ACCOUNT_NUMBER, null));

        verify(accountRepository, never()).save(any());
    }

    // ─── credit() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("credit: success — balance increased correctly")
    void credit_success() {
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(activeAccount));

        accountService.credit(ACCOUNT_NUMBER, new BigDecimal("300.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertEquals(new BigDecimal("800.00"), captor.getValue().getBalance());
    }

    @Test
    @DisplayName("credit: throws AccountNotFoundException when account not found")
    void credit_throwsWhenAccountNotFound() {
        when(accountRepository.findByAccountNumberWithLock("bad-acc")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.credit("bad-acc", new BigDecimal("100.00")));

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("credit: throws IllegalStateException when account is CLOSED")
    void credit_throwsWhenAccountClosed() {
        activeAccount.setStatus(AccountStatus.CLOSED);
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(activeAccount));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> accountService.credit(ACCOUNT_NUMBER, new BigDecimal("100.00")));

        assertTrue(ex.getMessage().contains("not ACTIVE"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("credit: throws IllegalArgumentException when amount is zero")
    void credit_throwsWhenAmountIsZero() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.credit(ACCOUNT_NUMBER, BigDecimal.ZERO));

        assertEquals("Credit amount must be greater than zero", ex.getMessage());
        verify(accountRepository, never()).findByAccountNumberWithLock(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("credit: throws IllegalArgumentException when amount is negative")
    void credit_throwsWhenAmountIsNegative() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.credit(ACCOUNT_NUMBER, new BigDecimal("-100.00")));

        assertEquals("Credit amount must be greater than zero", ex.getMessage());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("credit: throws IllegalArgumentException when amount is null")
    void credit_throwsWhenAmountIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> accountService.credit(ACCOUNT_NUMBER, null));

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("credit: edge case — very large amount credited correctly")
    void credit_largeAmountSucceeds() {
        when(accountRepository.findByAccountNumberWithLock(ACCOUNT_NUMBER)).thenReturn(Optional.of(activeAccount));

        accountService.credit(ACCOUNT_NUMBER, new BigDecimal("99999999999999999.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertTrue(captor.getValue().getBalance().compareTo(BigDecimal.ZERO) > 0);
    }
}
