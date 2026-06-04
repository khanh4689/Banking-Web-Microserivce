package com.banking.auth.event;

import com.banking.auth.service.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredEventListener {

    private final EmailSender emailSender;

    /**
     * Fires ONLY after the transaction commits successfully.
     * Runs on a separate thread (@Async) so it never blocks the HTTP response.
     *
     * Why AFTER_COMMIT:
     *  - If the transaction rolls back (e.g. DB error), the email is never sent.
     *  - Guarantees the user row actually exists before we tell them to verify.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Sending verification email to userId={}", event.userId());
        try {
            String verifyLink = "http://localhost:8082/auth/verify-email?token=" + event.verificationToken();
            String body = "Hi " + event.username() + ",\n\n"
                    + "Click the link below to verify your email:\n" + verifyLink
                    + "\n\nThis link expires in 24 hours.";

            emailSender.send(event.email(), "Verify your account", body);
            log.info("Verification email sent to {}", event.email());
        } catch (Exception e) {
            // Email failure must NOT affect the registration result — user is already saved.
            // In production: push to a retry queue or dead-letter store.
            log.error("Failed to send verification email to {}: {}", event.email(), e.getMessage());
        }
    }
}
