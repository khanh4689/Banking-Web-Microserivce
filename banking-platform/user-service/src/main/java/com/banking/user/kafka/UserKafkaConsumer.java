package com.banking.user.kafka;

import com.banking.common.event.UserCreatedEvent;
import com.banking.user.exception.DuplicateUserException;
import com.banking.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserKafkaConsumer {

    private final UserProfileService userProfileService;

    @KafkaListener(topics = "user-events", groupId = "user-service-group")
    public void consume(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent for authId={}, email={}", event.getId(), event.getEmail());
        try {
            userProfileService.createProfile(
                    UUID.randomUUID(),
                    event.getId(),
                    event.getEmail(),
                    event.getUsername()
            );
        } catch (DuplicateUserException e) {
            // Idempotent — skip silently if already processed (e.g. Kafka retry)
            log.warn("Skipping duplicate UserCreatedEvent for authId={}", event.getId());
        }
    }
}
