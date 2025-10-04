package com.example.sonata_brain.repository;

import com.example.sonata_brain.model.FeeDuesTracker;
import com.example.sonata_brain.model.FeeStructure;
import com.example.sonata_brain.model.StudentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeDuesTrackerRepository extends JpaRepository<FeeDuesTracker, Long> {

    // Find dues by student session
    List<FeeDuesTracker> findByStudentSessionOrderByFeeStructure_YearAscFeeStructure_TermAsc(StudentSession studentSession);

    // Find dues by student ID
    @Query("SELECT fdt FROM FeeDuesTracker fdt WHERE fdt.studentSession.student.admissionNo = :studentId ORDER BY fdt.feeStructure.year ASC, fdt.feeStructure.term ASC")
    List<FeeDuesTracker> findByStudentIdOrderByTermYearAscTermNumberAsc(@Param("studentId") Long studentId);

    // Find dues by year
    @Query("SELECT fdt FROM FeeDuesTracker fdt WHERE fdt.feeStructure.year = :termYear ORDER BY fdt.feeStructure.term ASC, fdt.studentSession.id ASC")
    List<FeeDuesTracker> findByTermYearOrderByTermNumberAscStudentSessionAsc(@Param("termYear") Integer termYear);

    // Find dues by term (year and term number)
    @Query("SELECT fdt FROM FeeDuesTracker fdt WHERE fdt.feeStructure.year = :termYear AND fdt.feeStructure.term = :termNumber")
    List<FeeDuesTracker> findByTermYearAndTermNumber(@Param("termYear") Integer termYear, @Param("termNumber") Integer termNumber);

    // Find dues by course name
    @Query("SELECT fdt FROM FeeDuesTracker fdt WHERE fdt.feeStructure.course.name = :courseName ORDER BY fdt.feeStructure.year ASC, fdt.feeStructure.term ASC")
    List<FeeDuesTracker> findByCourseNameOrderByTermYearAscTermNumberAsc(@Param("courseName") String courseName);

    // Find dues by status
    @Query("SELECT fdt FROM FeeDuesTracker fdt WHERE fdt.status = :status ORDER BY fdt.feeStructure.year ASC, fdt.feeStructure.term ASC")
    List<FeeDuesTracker> findByStatusOrderByTermYearAscTermNumberAsc(@Param("status") FeeDuesTracker.DuesStatus status);

    // Find dues by student and status
    @Query("SELECT fdt FROM FeeDuesTracker fdt WHERE fdt.studentSession.student.admissionNo = :studentId AND fdt.status = :status ORDER BY fdt.feeStructure.year ASC, fdt.feeStructure.term ASC")
    List<FeeDuesTracker> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") FeeDuesTracker.DuesStatus status);

    // Find specific due by session and fee structure
    Optional<FeeDuesTracker> findByStudentSessionAndFeeStructure(StudentSession studentSession, FeeStructure feeStructure);

    // Find dues for a session after a specific term
    @Query("SELECT fdt FROM FeeDuesTracker fdt WHERE fdt.studentSession = :session AND " +
           "(fdt.feeStructure.year > :year OR (fdt.feeStructure.year = :year AND fdt.feeStructure.term > :termNumber))")
    List<FeeDuesTracker> findByStudentSessionAfterTerm(@Param("session") StudentSession session,
                                                       @Param("year") Integer year,
                                                       @Param("termNumber") Integer termNumber);

    // Find active dues (not cancelled)
    @Query("SELECT fdt FROM FeeDuesTracker fdt WHERE fdt.status != 'CANCELLED' ORDER BY fdt.feeStructure.year ASC, fdt.feeStructure.term ASC")
    List<FeeDuesTracker> findActiveDues();

    // Count dues by status for a student
    @Query("SELECT COUNT(fdt) FROM FeeDuesTracker fdt WHERE fdt.studentSession.student.admissionNo = :studentId AND fdt.status = :status")
    Long countByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") FeeDuesTracker.DuesStatus status);

    // Get dues summary for a student
    @Query("SELECT fdt.status, COUNT(fdt), SUM(fdt.dueAmount) FROM FeeDuesTracker fdt " +
           "WHERE fdt.studentSession.student.admissionNo = :studentId GROUP BY fdt.status")
    List<Object[]> getDuesSummaryByStudentId(@Param("studentId") Long studentId);

    // Find dues that match a payment (by student, term, and course)
    @Query("SELECT fdt FROM FeeDuesTracker fdt WHERE fdt.studentSession.student.admissionNo = :studentId " +
           "AND fdt.feeStructure.course.name = :courseName " +
           "AND fdt.feeStructure.year = :termYear AND fdt.feeStructure.term = :termNumber " +
           "AND fdt.status IN ('DUE', 'PARTIALLY_PAID') ORDER BY fdt.createdAt ASC")
    List<FeeDuesTracker> findMatchingDuesForPayment(@Param("studentId") Long studentId,
                                                    @Param("courseName") String courseName,
                                                    @Param("termYear") Integer termYear,
                                                    @Param("termNumber") Integer termNumber);

    // Find dues by student ID and fee structure ID (for payment deactivation)
    @Query("SELECT fdt FROM FeeDuesTracker fdt WHERE fdt.studentSession.student.admissionNo = :studentId " +
           "AND fdt.feeStructure.id = :feeStructureId")
    List<FeeDuesTracker> findByStudentIdAndFeeStructureId(@Param("studentId") Long studentId,
                                                          @Param("feeStructureId") Long feeStructureId);

    // Find all active student sessions that should have dues for a new fee structure
    @Query("SELECT DISTINCT fdt.studentSession FROM FeeDuesTracker fdt WHERE fdt.feeStructure.year = :year")
    List<StudentSession> findActiveSessionsForYear(@Param("year") Integer year);

    // Check if dues already exist for a session and fee structure combination
    boolean existsByStudentSessionAndFeeStructure(StudentSession studentSession, FeeStructure feeStructure);
}
