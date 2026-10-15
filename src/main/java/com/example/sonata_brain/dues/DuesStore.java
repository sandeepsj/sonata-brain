package com.example.sonata_brain.dues;

import com.example.sonata_brain.dues.DuesDtos.Changes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads the facts dues are calculated from and writes {@code fee_due}, with plain SQL so a recalculation of everyone is
 * a handful of round trips instead of several per student (the database may be far away). Every method runs in the
 * caller's transaction.
 */
@Component
class DuesStore {

    private static final int BATCH = 500;

    /** A stored due, with the columns the comparison needs. */
    record Stored(long id, long studentId, String course, LocalDate periodStart, LocalDate periodEnd, long structureId,
                  BigDecimal amount, BigDecimal paid, BigDecimal discount, BigDecimal balance, DueStatus status) {
    }

    /** A due to write: {@code id} is null for a new one. */
    record Row(Long id, long studentId, String course, LocalDate periodStart, LocalDate periodEnd, long structureId,
               BigDecimal amount, BigDecimal paid, BigDecimal discount, BigDecimal balance, DueStatus status) {
    }

    private static final String SESSIONS = """
            SELECT student_id, course_name, date_of_joining::date AS joined, date_of_disjoining::date AS left_on
            FROM student_sessions WHERE course_name IS NOT NULL""";
    private static final String PAYMENTS = """
            SELECT id, student_id, course_name, date, amount_paid, coalesce(discount_amount, 0) AS discount
            FROM payments WHERE payment_type = 'REGULAR' AND is_active AND course_name IS NOT NULL""";
    private static final String DUES = """
            SELECT id, student_id, course_name, period_start, period_end, fee_structure_id, amount, paid_amount,
                   discount_amount, balance, status
            FROM fee_due""";

    private final JdbcTemplate jdbc;

    DuesStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<Long> studentIds() {
        return jdbc.queryForList("SELECT admission_no FROM student ORDER BY admission_no", Long.class);
    }

    Map<Long, List<DuesCalculator.Session>> sessions(Long studentId) {
        List<Object[]> rows = studentId == null
                ? jdbc.query(SESSIONS, (rs, i) -> session(rs))
                : jdbc.query(SESSIONS + " AND student_id = ?", (rs, i) -> session(rs), studentId);
        return rows.stream().collect(Collectors.groupingBy(r -> (Long) r[0],
                Collectors.mapping(r -> (DuesCalculator.Session) r[1], Collectors.toList())));
    }

    Map<Long, List<DuesCalculator.Payment>> payments(Long studentId) {
        List<Object[]> rows = studentId == null
                ? jdbc.query(PAYMENTS, (rs, i) -> payment(rs))
                : jdbc.query(PAYMENTS + " AND student_id = ?", (rs, i) -> payment(rs), studentId);
        return rows.stream().collect(Collectors.groupingBy(r -> (Long) r[0],
                Collectors.mapping(r -> (DuesCalculator.Payment) r[1], Collectors.toList())));
    }

    Map<Long, List<Stored>> dues(Long studentId) {
        List<Stored> rows = studentId == null
                ? jdbc.query(DUES, (rs, i) -> stored(rs))
                : jdbc.query(DUES + " WHERE student_id = ?", (rs, i) -> stored(rs), studentId);
        return rows.stream().collect(Collectors.groupingBy(Stored::studentId));
    }

    /** Applies inserts, updates and deletes in batches. */
    Changes write(List<Row> inserts, List<Row> updates, List<Long> deletes) {
        for (List<Row> chunk : chunks(inserts)) {
            jdbc.batchUpdate("""
                    INSERT INTO fee_due (student_id, course_name, period_start, period_end, fee_structure_id, amount,
                                         paid_amount, discount_amount, balance, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", chunk, chunk.size(), (ps, r) -> {
                ps.setLong(1, r.studentId());
                ps.setString(2, r.course());
                ps.setDate(3, Date.valueOf(r.periodStart()));
                ps.setDate(4, Date.valueOf(r.periodEnd()));
                ps.setLong(5, r.structureId());
                ps.setBigDecimal(6, r.amount());
                ps.setBigDecimal(7, r.paid());
                ps.setBigDecimal(8, r.discount());
                ps.setBigDecimal(9, r.balance());
                ps.setString(10, r.status().name());
            });
        }
        for (List<Row> chunk : chunks(updates)) {
            jdbc.batchUpdate("""
                    UPDATE fee_due SET period_end = ?, fee_structure_id = ?, amount = ?, paid_amount = ?,
                                       discount_amount = ?, balance = ?, status = ?
                    WHERE id = ?""", chunk, chunk.size(), (ps, r) -> {
                ps.setDate(1, Date.valueOf(r.periodEnd()));
                ps.setLong(2, r.structureId());
                ps.setBigDecimal(3, r.amount());
                ps.setBigDecimal(4, r.paid());
                ps.setBigDecimal(5, r.discount());
                ps.setBigDecimal(6, r.balance());
                ps.setString(7, r.status().name());
                ps.setLong(8, r.id());
            });
        }
        for (List<Long> chunk : chunks(deletes)) {
            jdbc.batchUpdate("DELETE FROM fee_due WHERE id = ?", chunk, chunk.size(), (ps, id) -> ps.setLong(1, id));
        }
        return new Changes(inserts.size(), updates.size(), deletes.size());
    }

    private static <T> List<List<T>> chunks(List<T> all) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i += BATCH) {
            out.add(all.subList(i, Math.min(all.size(), i + BATCH)));
        }
        return out;
    }

    private static Object[] session(ResultSet rs) throws SQLException {
        Date left = rs.getDate("left_on");
        return new Object[] {rs.getLong("student_id"), new DuesCalculator.Session(rs.getString("course_name"),
                rs.getDate("joined").toLocalDate(), left == null ? null : left.toLocalDate())};
    }

    private static Object[] payment(ResultSet rs) throws SQLException {
        return new Object[] {rs.getLong("student_id"), new DuesCalculator.Payment(rs.getLong("id"),
                rs.getString("course_name"), rs.getTimestamp("date").toLocalDateTime(), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("discount"))};
    }

    private static Stored stored(ResultSet rs) throws SQLException {
        return new Stored(rs.getLong("id"), rs.getLong("student_id"), rs.getString("course_name"),
                rs.getDate("period_start").toLocalDate(), rs.getDate("period_end").toLocalDate(),
                rs.getLong("fee_structure_id"), rs.getBigDecimal("amount"), rs.getBigDecimal("paid_amount"),
                rs.getBigDecimal("discount_amount"), rs.getBigDecimal("balance"),
                DueStatus.valueOf(rs.getString("status")));
    }
}
