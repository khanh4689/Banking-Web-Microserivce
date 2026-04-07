package com.banking.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_user_id", columnList = "user_id"),
        @Index(name = "idx_accounts_account_number", columnList = "account_number", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(columnDefinition = "UUID")
    private UUID id;

    // Intentionally NOT a FK to user-service — microservice boundary
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "account_number", nullable = false, unique = true, length = 12)
    private String accountNumber;

    // BigDecimal is mandatory for monetary values — never use double/float
    // scale=2 enforces cent-level precision (e.g. 0.00), precision=19 supports large balances
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    // ISO 4217 currency code — required for multi-currency support and correct rounding rules
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Optimistic locking — prevents lost updates when concurrent transactions modify balance
    // JPA will throw OptimisticLockException instead of silently overwriting data
    @Version
    @Column(nullable = false)
    private Long version;
}
