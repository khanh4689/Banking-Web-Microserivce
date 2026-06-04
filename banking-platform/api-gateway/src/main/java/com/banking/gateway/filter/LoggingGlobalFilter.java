package com.banking.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        
        // Put in MDC for synchronous logging currently in scope
        MDC.put("traceId", traceId);
        final String finalTraceId = traceId;
        
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .build();
                
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        log.info("GATEWAY_REQUEST - method: {}, uri: {}", request.getMethod(), request.getURI());
        
        return chain.filter(modifiedExchange).doOnSubscribe(subscription -> {
            Route route = modifiedExchange.getAttribute(GATEWAY_ROUTE_ATTR);
            if (route != null) {
                try (MDC.MDCCloseable c = MDC.putCloseable("traceId", finalTraceId)) {
                    log.info("ROUTING - service: {}, path: {}", route.getId(), request.getURI().getPath());
                    log.info("DOWNSTREAM_CALL - targetService: {}", route.getUri());
                }
            }
        }).doFinally(signalType -> {
            int status = modifiedExchange.getResponse().getStatusCode() != null ? 
                    modifiedExchange.getResponse().getStatusCode().value() : 200;
            
            try (MDC.MDCCloseable c = MDC.putCloseable("traceId", finalTraceId)) {
                log.info("GATEWAY_RESPONSE - status: {}", status);
            }
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
