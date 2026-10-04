package com.example.sonata_brain.service;

import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.dues.DuesEvents.StudentDuesInvalidated;
import com.example.sonata_brain.model.StudentSession;
import com.example.sonata_brain.repository.StudentSessionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Saves sessions and asks for the student's dues to be recomputed afterwards (REQ-030.AC8, REQ-031.AC4). */
@Service
@Transactional
public class StudentSessionService {

    private final StudentSessionRepository sessions;
    private final ApplicationEventPublisher events;

    public StudentSessionService(StudentSessionRepository sessions, ApplicationEventPublisher events) {
        this.sessions = sessions;
        this.events = events;
    }

    public StudentSession createStudentSession(StudentSession session) {
        StudentSession saved = sessions.save(session);
        events.publishEvent(new StudentDuesInvalidated(saved.getStudent().getAdmissionNo()));
        return saved;
    }

    public StudentSession disjoinStudent(Long sessionId, LocalDateTime disjoinDate) {
        StudentSession session = sessions.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session " + sessionId + " not found"));
        session.setDateOfDisjoining(disjoinDate);
        events.publishEvent(new StudentDuesInvalidated(session.getStudent().getAdmissionNo()));
        return session;
    }
}
