package com.cloud.backend.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("User registered: id={}, username={}, email={}",
                event.getUserId(), event.getUsername(), event.getEmail());
    }
}
