package com.example.sonata_brain.fee;

import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.course.CourseService;
import com.example.sonata_brain.dues.DuesEvents.CourseDuesInvalidated;
import com.example.sonata_brain.fee.FeeDtos.TimelineRequest;
import com.example.sonata_brain.fee.FeeDtos.TimelineResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** The per-course fee timeline (REQ-053) and the billing periods it produces (REQ-054, REQ-055). */
@Service
@Transactional
public class FeeTimelineService {

    private final FeeTimelineRepository timeline;
    private final FeeStructureService structures;
    private final CourseService courses;
    private final ApplicationEventPublisher events;

    public FeeTimelineService(FeeTimelineRepository timeline, FeeStructureService structures, CourseService courses,
                              ApplicationEventPublisher events) {
        this.timeline = timeline;
        this.structures = structures;
        this.courses = courses;
        this.events = events;
    }

    public TimelineResponse create(TimelineRequest request) {
        requireBody(request);
        String course = courses.requireActive(request.courseName()).getName();
        FeeStructure structure = checkedStructure(request, course);
        checkDates(request);
        checkNoOverlap(course, request.effectiveFrom(), request.effectiveTo(), null);
        FeeTimelineEntry entry = timeline.save(
                new FeeTimelineEntry(course, structure, request.effectiveFrom(), request.effectiveTo()));
        events.publishEvent(new CourseDuesInvalidated(course));
        return TimelineResponse.of(entry);
    }

    public TimelineResponse update(Long id, TimelineRequest request) {
        FeeTimelineEntry entry = find(id);
        requireBody(request);
        if (request.courseName() != null && !request.courseName().equals(entry.getCourseName())) {
            throw ApiException.invalidField("courseName", "A timeline entry can't move to another course");
        }
        FeeStructure structure = checkedStructure(request, entry.getCourseName());
        checkDates(request);
        checkNoOverlap(entry.getCourseName(), request.effectiveFrom(), request.effectiveTo(), id);
        entry.update(structure, request.effectiveFrom(), request.effectiveTo());
        events.publishEvent(new CourseDuesInvalidated(entry.getCourseName()));
        return TimelineResponse.of(entry);
    }

    public void delete(Long id) {
        FeeTimelineEntry entry = find(id);
        timeline.delete(entry);
        events.publishEvent(new CourseDuesInvalidated(entry.getCourseName()));
    }

    @Transactional(readOnly = true)
    public List<TimelineResponse> list(String courseName) {
        List<FeeTimelineEntry> entries = courseName == null
                ? timeline.findAll().stream()
                    .sorted((a, b) -> a.getCourseName().equals(b.getCourseName())
                            ? a.getEffectiveFrom().compareTo(b.getEffectiveFrom())
                            : a.getCourseName().compareTo(b.getCourseName()))
                    .toList()
                : timeline.findByCourseNameOrderByEffectiveFromAsc(courseName);
        return entries.stream().map(TimelineResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public List<BillingPeriod> preview(String courseName, LocalDate from, LocalDate to) {
        courses.get(courseName);
        if (to.isBefore(from)) {
            throw ApiException.invalidField("to", "'to' must not be before 'from'");
        }
        return BillingPeriodCalculator.periodsBetween(slots(courseName), from, to);
    }

    /** The course's timeline in calculator form. */
    @Transactional(readOnly = true)
    public List<BillingPeriodCalculator.Slot> slots(String courseName) {
        return timeline.findByCourseNameOrderByEffectiveFromAsc(courseName).stream()
                .map(FeeTimelineEntry::slot)
                .toList();
    }

    private FeeTimelineEntry find(Long id) {
        return timeline.findById(id).orElseThrow(() -> ApiException.notFound("Timeline entry " + id + " not found"));
    }

    private static void requireBody(TimelineRequest request) {
        if (request == null) {
            throw ApiException.badRequest("A request body is required");
        }
    }

    private FeeStructure checkedStructure(TimelineRequest request, String course) {
        if (request.feeStructureId() == null) {
            throw ApiException.invalidField("feeStructureId", "A fee structure is required");
        }
        FeeStructure structure = structures.find(request.feeStructureId());
        if (!structure.getCourseName().equals(course)) {
            throw ApiException.invalidField("feeStructureId",
                    "Fee structure " + structure.getId() + " belongs to course '" + structure.getCourseName() + "'");
        }
        return structure;
    }

    private static void checkDates(TimelineRequest request) {
        if (request.effectiveFrom() == null) {
            throw ApiException.invalidField("effectiveFrom", "effectiveFrom is required");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw ApiException.invalidField("effectiveTo", "effectiveTo must not be before effectiveFrom");
        }
    }

    private void checkNoOverlap(String course, LocalDate from, LocalDate to, Long ignoreId) {
        for (FeeTimelineEntry other : timeline.findByCourseNameOrderByEffectiveFromAsc(course)) {
            if (!other.getId().equals(ignoreId) && other.overlaps(from, to)) {
                throw ApiException.conflict("Overlaps timeline entry " + other.getId() + " ("
                        + other.getEffectiveFrom() + " to "
                        + (other.getEffectiveTo() == null ? "open" : other.getEffectiveTo()) + ")");
            }
        }
    }
}
