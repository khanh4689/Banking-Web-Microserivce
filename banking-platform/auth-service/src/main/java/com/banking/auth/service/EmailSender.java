package com.banking.auth.service;

public interface EmailSender {
    void send(String to, String subject, String body);
}
