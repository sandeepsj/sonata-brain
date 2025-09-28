package com.example.sonata_brain.service.dues.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DuesTrackerEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public DuesTrackerEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishEvent(DuesTrackerEvent event) {
        eventPublisher.publishEvent(event);
    }
}
