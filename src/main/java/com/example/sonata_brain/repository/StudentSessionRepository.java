package com.example.sonata_brain.repository;

import com.example.sonata_brain.model.Course;
import com.example.sonata_brain.model.Student;
import com.example.sonata_brain.model.StudentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSessionRepository extends JpaRepository<StudentSession, Long> {

    @Query("SELECT ss FROM StudentSession ss WHERE ss.student = :student AND ss.course = :course " +
           "AND (ss.dateOfDisjoining IS NULL OR ss.dateOfDisjoining > CURRENT_TIMESTAMP)")
    Optional<StudentSession> findActiveSession(@Param("student") Student student, @Param("course") Course course);

    @Query("SELECT ss FROM StudentSession ss WHERE ss.student.admissionNo = :studentId " +
           "AND (ss.dateOfDisjoining IS NULL OR ss.dateOfDisjoining > CURRENT_TIMESTAMP)")
    List<StudentSession> findActiveSessionsByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT ss FROM StudentSession ss WHERE ss.course.name = :courseName " +
           "AND (ss.dateOfDisjoining IS NULL OR ss.dateOfDisjoining > CURRENT_TIMESTAMP)")
    List<StudentSession> findActiveSessionsByCourseName(@Param("courseName") String courseName);

    @Query("SELECT ss FROM StudentSession ss WHERE ss.student = :student AND ss.course = :course " +
           "AND ((ss.dateOfDisjoining IS NULL) OR " +
           "(:startDate < COALESCE(ss.dateOfDisjoining, CURRENT_TIMESTAMP) AND " +
           ":endDate > ss.dateOfJoining))")
    List<StudentSession> findOverlappingSessions(@Param("student") Student student,
                                                @Param("course") Course course,
                                                @Param("startDate") java.time.LocalDateTime startDate,
                                                @Param("endDate") java.time.LocalDateTime endDate);
}
