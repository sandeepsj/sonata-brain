package com.example.sonata_brain.service.dues.events;

import com.example.sonata_brain.model.StudentSession;
import java.time.LocalDateTime;

public class StudentSessionUpdatedEvent extends DuesTrackerEvent {

    private final StudentSession studentSession;
    private final LocalDateTime previousDisjoinDate;
    private final LocalDateTime newDisjoinDate;

    public StudentSessionUpdatedEvent(Object source, StudentSession studentSession,
                                    LocalDateTime previousDisjoinDate, LocalDateTime newDisjoinDate) {
        super(source);
        this.studentSession = studentSession;
        this.previousDisjoinDate = previousDisjoinDate;
        this.newDisjoinDate = newDisjoinDate;
    }

    public StudentSession getStudentSession() {
        return studentSession;
    }

    public LocalDateTime getPreviousDisjoinDate() {
        return previousDisjoinDate;
    }

    public LocalDateTime getNewDisjoinDate() {
        return newDisjoinDate;
    }

    public boolean isDisjoinDateChanged() {
        if (previousDisjoinDate == null && newDisjoinDate == null) return false;
        if (previousDisjoinDate == null || newDisjoinDate == null) return true;
        return !previousDisjoinDate.equals(newDisjoinDate);
    }

    @Override
    public String getEventType() {
        return "STUDENT_SESSION_UPDATED";
    }
}
