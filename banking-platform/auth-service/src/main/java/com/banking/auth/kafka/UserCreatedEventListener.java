package com.banking.auth.kafka;

import com.banking.common.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedEventListener {

    private final AuthKafkaProducer authKafkaProducer;

    /**
     * Fires AFTER the transaction commits successfully.
     * This guarantees the user is persisted in auth_db before we publish to Kafka.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        log.info("Transaction committed — publishing UserCreatedEvent to Kafka for userId={}", event.getId());
        authKafkaProducer.sendUserCreatedEvent(event);
    }
}
