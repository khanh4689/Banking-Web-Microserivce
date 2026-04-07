package com.banking.transaction.controller;

import com.banking.transaction.dto.ApiResponse;
import com.banking.transaction.dto.PagedResponse;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /transactions/transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        String userId = jwt.getSubject();
        TransactionResponse response = transactionService.transfer(userId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /transactions/history?accountNumber=xxx&page=0&size=10
     * Returns paginated transaction history sorted by createdAt DESC.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getHistory(
            @NotBlank(message = "accountNumber is required") @RequestParam String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionHistory(accountNumber, page, size)));
    }
}
