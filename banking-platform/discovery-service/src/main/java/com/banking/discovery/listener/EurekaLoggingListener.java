package com.banking.discovery.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EurekaLoggingListener {
    private static final Logger log = LoggerFactory.getLogger(EurekaLoggingListener.class);

    @EventListener
    public void onInstanceRegistered(EurekaInstanceRegisteredEvent event) {
        log.info("SERVICE_REGISTERED - serviceName: {}, instanceId: {}", 
                event.getInstanceInfo().getAppName(), 
                event.getInstanceInfo().getInstanceId());
    }

    @EventListener
    public void onInstanceCanceled(EurekaInstanceCanceledEvent event) {
        log.warn("SERVICE_DOWN - serviceName: {}, instanceId: {}", 
                event.getAppName(), 
                event.getServerId());
    }
}
