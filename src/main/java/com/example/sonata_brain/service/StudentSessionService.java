package com.example.sonata_brain.service;

import com.example.sonata_brain.dues.DuesService;
import com.example.sonata_brain.model.StudentSession;
import com.example.sonata_brain.repository.StudentSessionRepository;
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
    private DuesService duesService;

    /**
     * Create a new student session and publish dues tracking event
     */
    public StudentSession createStudentSession(StudentSession studentSession) {
        logger.info("Creating new student session for student ID: {} in course: {}",
                   studentSession.getStudent().getAdmissionNo(), studentSession.getCourse().getName());

        StudentSession savedSession = studentSessionRepository.save(studentSession);

        recalculate(savedSession);
        logger.info("Created student session ID: {} and recalculated the student's dues", savedSession.getId());
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
            recalculate(savedSession);
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
        existingSession.setDateOfDisjoining(disjoinDate);
        StudentSession savedSession = studentSessionRepository.save(existingSession);

        recalculate(savedSession);
        logger.info("Set disjoin date for student session ID: {} and recalculated the student's dues", savedSession.getId());
        return savedSession;
    }

    /** REQ-030.AC8, REQ-031.AC4, REQ-074.AC1: before the response returns. */
    private void recalculate(StudentSession session) {
        studentSessionRepository.flush();
        duesService.recalculateStudent(session.getStudent().getAdmissionNo());
    }

    private boolean isDisjoinDateChanged(LocalDateTime previous, LocalDateTime current) {
        if (previous == null && current == null) return false;
        if (previous == null || current == null) return true;
        return !previous.equals(current);
    }
}
