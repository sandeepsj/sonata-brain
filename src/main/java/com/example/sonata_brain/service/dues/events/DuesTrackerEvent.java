package com.example.sonata_brain.service.dues.events;

import com.example.sonata_brain.model.StudentSession;
import org.springframework.context.ApplicationEvent;

public abstract class DuesTrackerEvent extends ApplicationEvent {

    public DuesTrackerEvent(Object source) {
        super(source);
    }

    public abstract String getEventType();
}
