package com.example.sonata_brain.service.dues.events;

import com.example.sonata_brain.model.StudentSession;

public class StudentSessionCreatedEvent extends DuesTrackerEvent {

    private final StudentSession studentSession;

    public StudentSessionCreatedEvent(Object source, StudentSession studentSession) {
        super(source);
        this.studentSession = studentSession;
    }

    public StudentSession getStudentSession() {
        return studentSession;
    }

    @Override
    public String getEventType() {
        return "STUDENT_SESSION_CREATED";
    }
}
