package com.banking.account.kafka;

import com.banking.account.service.AccountService;
import com.banking.common.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedEventConsumer {

    private final AccountService accountService;

    @KafkaListener(topics = "user-events", groupId = "account-service-group")
    public void onUserCreated(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent for userId={}", event.getId());
        try {
            accountService.createAccount(event.getId());
        } catch (Exception e) {
            log.error("Failed to create account for userId={}: {}", event.getId(), e.getMessage());
            // Let Kafka retry — do not swallow silently
            throw e;
        }
    }
}
