package com.banking.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuditLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(securityContext -> securityContext.getAuthentication())
            .map(Authentication::getName)
            .defaultIfEmpty("anonymous")
            .doOnNext(username -> {
                String path = exchange.getRequest().getURI().getPath();
                String method = exchange.getRequest().getMethod().name();
                String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
                
                log.info("Audit: user='{}' method='{}' path='{}' correlationId='{}'", 
                        username, method, path, correlationId != null ? correlationId : "");
            })
            .then(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        // Run after security filters to get the security context
        return -1;
    }
}
