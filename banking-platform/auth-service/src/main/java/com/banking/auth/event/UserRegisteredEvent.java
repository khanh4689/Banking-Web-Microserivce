package com.banking.auth.event;

/**
 * In-process Spring event published after a user is persisted.
 * Contains everything the email listener needs — no DB lookup required.
 */
public record UserRegisteredEvent(
        String userId,
        String email,
        String username,
        String verificationToken
) {}
