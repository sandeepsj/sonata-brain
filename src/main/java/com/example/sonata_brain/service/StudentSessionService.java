package com.example.sonata_brain.service;

import com.example.sonata_brain.model.StudentSession;
import com.example.sonata_brain.repository.StudentSessionRepository;
import com.example.sonata_brain.service.dues.events.DuesTrackerEventPublisher;
import com.example.sonata_brain.service.dues.events.StudentSessionCreatedEvent;
import com.example.sonata_brain.service.dues.events.StudentSessionUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class StudentSessionService {

    private static final Logger logger = LoggerFactory.getLogger(StudentSessionService.class);

    @Autowired
    private StudentSessionRepository studentSessionRepository;

    @Autowired
    private DuesTrackerEventPublisher eventPublisher;

    /**
     * Create a new student session and publish dues tracking event
     */
    public StudentSession createStudentSession(StudentSession studentSession) {
        logger.info("Creating new student session for student ID: {} in course: {}",
                   studentSession.getStudent().getAdmissionNo(), studentSession.getCourse().getName());

        StudentSession savedSession = studentSessionRepository.save(studentSession);

        // Publish event for dues tracking
        eventPublisher.publishEvent(new StudentSessionCreatedEvent(this, savedSession));

        logger.info("Created student session ID: {} and published dues tracking event", savedSession.getId());
        return savedSession;
    }

    /**
     * Update student session and publish event if disjoin date changed
     */
    public StudentSession updateStudentSession(Long sessionId, StudentSession updatedSession) {
        logger.info("Updating student session ID: {}", sessionId);

        Optional<StudentSession> existingSessionOpt = studentSessionRepository.findById(sessionId);
        if (existingSessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Student session not found with ID: " + sessionId);
        }

        StudentSession existingSession = existingSessionOpt.get();
        LocalDateTime previousDisjoinDate = existingSession.getDateOfDisjoining();

        // Update fields
        if (updatedSession.getDateOfDisjoining() != null) {
            existingSession.setDateOfDisjoining(updatedSession.getDateOfDisjoining());
        }

        StudentSession savedSession = studentSessionRepository.save(existingSession);

        // Publish event if disjoin date changed
        LocalDateTime newDisjoinDate = savedSession.getDateOfDisjoining();
        if (isDisjoinDateChanged(previousDisjoinDate, newDisjoinDate)) {
            eventPublisher.publishEvent(new StudentSessionUpdatedEvent(
                this, savedSession, previousDisjoinDate, newDisjoinDate));
            logger.info("Published dues tracking event for session disjoin update");
        }

        logger.info("Updated student session ID: {}", savedSession.getId());
        return savedSession;
    }

    /**
     * Get student session by ID
     */
    @Transactional(readOnly = true)
    public Optional<StudentSession> findById(Long sessionId) {
        return studentSessionRepository.findById(sessionId);
    }

    /**
     * Set disjoin date for a student session
     */
    public StudentSession disjoinStudent(Long sessionId, LocalDateTime disjoinDate) {
        logger.info("Setting disjoin date for student session ID: {} to {}", sessionId, disjoinDate);

        Optional<StudentSession> existingSessionOpt = studentSessionRepository.findById(sessionId);
        if (existingSessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Student session not found with ID: " + sessionId);
        }

        StudentSession existingSession = existingSessionOpt.get();
        LocalDateTime previousDisjoinDate = existingSession.getDateOfDisjoining();

        existingSession.setDateOfDisjoining(disjoinDate);
        StudentSession savedSession = studentSessionRepository.save(existingSession);

        // Publish event for dues tracking
        eventPublisher.publishEvent(new StudentSessionUpdatedEvent(
            this, savedSession, previousDisjoinDate, disjoinDate));

        logger.info("Set disjoin date for student session ID: {} and published dues tracking event",
                   savedSession.getId());
        return savedSession;
    }

    private boolean isDisjoinDateChanged(LocalDateTime previous, LocalDateTime current) {
        if (previous == null && current == null) return false;
        if (previous == null || current == null) return true;
        return !previous.equals(current);
    }
}
