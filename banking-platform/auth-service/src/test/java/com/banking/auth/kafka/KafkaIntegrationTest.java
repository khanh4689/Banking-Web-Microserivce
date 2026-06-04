package com.banking.auth.kafka;

import com.banking.common.event.UserCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void testUserCreatedFlow() {
        UserCreatedEvent event = new UserCreatedEvent(
                UUID.randomUUID().toString(),
                "test@gmail.com",
                "0123456789"
        );

        kafkaTemplate.send("user-created", event);
        System.out.println("Message sent successfully!");
    }
}
