package com.banking.auth.dto;

public class ResendVerificationEmailRequest {
    private String email;

    public ResendVerificationEmailRequest() {
    }

    public ResendVerificationEmailRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
