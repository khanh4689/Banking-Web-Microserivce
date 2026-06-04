package com.banking.transaction.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountClient {

    private final RestTemplate restTemplate;

    @Value("${account-service.url}")
    private String accountServiceUrl;

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccountByUserId(String userId) {
        String url = accountServiceUrl + "/internal/accounts/user/" + userId;
        log.debug("Calling account-service: GET {}", url);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Account not found for userId={}", userId);
            throw new IllegalArgumentException("No account found for user: " + userId);
        } catch (ResourceAccessException e) {
            log.error("account-service unavailable when fetching account for userId={}", userId, e);
            throw new IllegalStateException("Account service is currently unavailable. Please try again later.");
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccountByNumber(String accountNumber) {
        String url = accountServiceUrl + "/internal/accounts/" + accountNumber;
        log.debug("Calling account-service: GET {}", url);
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Account not found: accountNumber={}", accountNumber);
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        } catch (ResourceAccessException e) {
            log.error("account-service unavailable when fetching accountNumber={}", accountNumber, e);
            throw new IllegalStateException("Account service is currently unavailable. Please try again later.");
        }
    }

    public void debit(String accountNumber, BigDecimal amount) {
        String url = accountServiceUrl + "/internal/accounts/" + accountNumber + "/debit";
        log.debug("Calling account-service: POST {} amount={}", url, amount);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        Map<String, Object> body = Map.of("amount", amount);
        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Debit failed for accountNumber={}: {}", accountNumber, e.getResponseBodyAsString());
            throw new IllegalStateException("Debit failed for " + accountNumber + ": " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("account-service unavailable during debit for accountNumber={}", accountNumber, e);
            throw new IllegalStateException("Account service is currently unavailable. Please try again later.");
        }
    }

    public void credit(String accountNumber, BigDecimal amount) {
        String url = accountServiceUrl + "/internal/accounts/" + accountNumber + "/credit";
        log.debug("Calling account-service: POST {} amount={}", url, amount);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        Map<String, Object> body = Map.of("amount", amount);
        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Credit failed for accountNumber={}: {}", accountNumber, e.getResponseBodyAsString());
            throw new IllegalStateException("Credit failed for " + accountNumber + ": " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("account-service unavailable during credit for accountNumber={}", accountNumber, e);
            throw new IllegalStateException("Account service is currently unavailable. Please try again later.");
        }
    }
}
