package com.banking.transaction.service;

import com.banking.transaction.client.AccountClient;
import com.banking.transaction.dto.PagedResponse;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.entity.TransactionStatus;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    /**
     * Transfer money between two accounts.
     *
     * Flow:
     *  1. Idempotency check — return existing result if key already processed
     *  2. Resolve fromAccount via account-service (userId → accountNumber)
     *  3. Validate both accounts (exist, ACTIVE, sufficient balance)
     *  4. Persist PENDING transaction
     *  5. Debit source → Credit destination (pessimistic lock on account-service side)
     *  6. Mark SUCCESS
     *  7. On any error → mark FAILED, re-throw so HTTP layer returns 4xx/5xx
     *
     * @param userId         extracted from JWT — never from client body
     * @param request        toAccountNumber + amount
     * @param idempotencyKey optional header; prevents duplicate transfers on retry
     */
    @Transactional
    public TransactionResponse transfer(String userId, TransferRequest request, String idempotencyKey) {
        // Set correlationId in MDC so every log line in this transfer carries the same traceId
        String correlationId = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            log.info("TRANSFER_START - userId={}, toAccount={}, amount={}, correlationId={}",
                    userId, request.toAccountNumber(), request.amount(), correlationId);

            // ── 1. Idempotency ────────────────────────────────────────────────────
            if (idempotencyKey != null) {
                var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    log.info("TRANSFER_IDEMPOTENT - key={}, existing txId={}", idempotencyKey, existing.get().getId());
                    return toResponse(existing.get());
                }
            }

            // ── 2. Resolve source account ─────────────────────────────────────────
            Map<String, Object> fromAccountData = accountClient.getAccountByUserId(userId);
            String fromAccountNumber = (String) fromAccountData.get("accountNumber");

            // ── 3. Validate ───────────────────────────────────────────────────────
            if (fromAccountNumber.equals(request.toAccountNumber())) {
                throw new IllegalArgumentException("Cannot transfer to the same account");
            }

            validateAccount(fromAccountData, "Source");

            Map<String, Object> toAccountData = accountClient.getAccountByNumber(request.toAccountNumber());
            validateAccount(toAccountData, "Destination");

            BigDecimal balance = new BigDecimal(fromAccountData.get("balance").toString());
            BigDecimal amount  = request.amount().setScale(2, java.math.RoundingMode.HALF_UP);

            if (balance.compareTo(amount) < 0) {
                log.warn("TRANSFER_INSUFFICIENT_BALANCE - fromAccount={}, balance={}, requested={}",
                        fromAccountNumber, balance, amount);
                throw new IllegalArgumentException("Insufficient balance. Available: " + balance);
            }

            // ── 4. Persist PENDING ────────────────────────────────────────────────
            Transaction tx = Transaction.builder()
                    .id(UUID.randomUUID())
                    .idempotencyKey(idempotencyKey)
                    .fromAccount(fromAccountNumber)
                    .toAccount(request.toAccountNumber())
                    .amount(amount)
                    .currency((String) fromAccountData.getOrDefault("currency", "VND"))
                    .status(TransactionStatus.PENDING)
                    .build();

            transactionRepository.save(tx);
            log.info("TRANSFER_PENDING - txId={}, from={}, to={}, amount={}",
                    tx.getId(), fromAccountNumber, request.toAccountNumber(), amount);

            // ── 5. Execute debit + credit ─────────────────────────────────────────
            try {
                accountClient.debit(fromAccountNumber, amount);
                accountClient.credit(request.toAccountNumber(), amount);
            } catch (Exception e) {
                tx.setStatus(TransactionStatus.FAILED);
                tx.setFailureReason(e.getMessage());
                transactionRepository.save(tx);
                log.error("TRANSFER_FAILED - txId={}, reason={}", tx.getId(), e.getMessage(), e);
                throw new IllegalStateException("Transfer failed: " + e.getMessage(), e);
            }

            // ── 6. Mark SUCCESS ───────────────────────────────────────────────────
            tx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(tx);
            log.info("TRANSFER_SUCCESS - txId={}, from={}, to={}, amount={}",
                    tx.getId(), fromAccountNumber, request.toAccountNumber(), amount);

            return toResponse(tx);
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Returns paginated transaction history for an account (sender or receiver).
     * Always sorted by createdAt DESC — newest first.
     */
    @Transactional(readOnly = true)
    public PagedResponse<TransactionResponse> getTransactionHistory(String accountNumber, int page, int size) {
        log.info("Fetching transaction history for accountId={}, page={}, size={}", accountNumber, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> result = transactionRepository.findByFromAccountOrToAccount(
                accountNumber, accountNumber, pageable);
        
        log.info("Found {} transactions for accountId={}", result.getTotalElements(), accountNumber);

        return PagedResponse.<TransactionResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private void validateAccount(Map<String, Object> data, String label) {
        String status = (String) data.get("status");
        if (!"ACTIVE".equals(status)) {
            log.warn("Account validation failed: {} account {} is not ACTIVE", label, data.get("accountNumber"));
            throw new IllegalArgumentException(label + " account is not ACTIVE. Current status: " + status);
        }
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .fromAccount(tx.getFromAccount())
                .toAccount(tx.getToAccount())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .status(tx.getStatus())
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }
}
