package com.example.sonata_brain.fee;

import com.example.sonata_brain.common.error.ApiError.FieldProblem;
import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.course.CourseService;
import com.example.sonata_brain.dues.DuesEvents.CourseDuesInvalidated;
import com.example.sonata_brain.fee.FeeDtos.FeeStructureRequest;
import com.example.sonata_brain.fee.FeeDtos.FeeStructureResponse;
import com.example.sonata_brain.fee.FeeDtos.TermAmount;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Fee structure templates (REQ-050 to REQ-052). */
@Service
@Transactional
public class FeeStructureService {

    private static final Set<Integer> ALLOWED_TERMS_PER_YEAR = Set.of(1, 2, 3, 4, 6, 12);

    private final FeeStructureRepository structures;
    private final FeeTimelineRepository timeline;
    private final CourseService courses;
    private final ApplicationEventPublisher events;

    public FeeStructureService(FeeStructureRepository structures, FeeTimelineRepository timeline,
                               CourseService courses, ApplicationEventPublisher events) {
        this.structures = structures;
        this.timeline = timeline;
        this.courses = courses;
        this.events = events;
    }

    public FeeStructureResponse create(FeeStructureRequest request) {
        Map<Integer, BigDecimal> amounts = validate(request);
        String course = courses.get(request.courseName()).getName();
        String name = request.name().trim();
        if (structures.existsByCourseNameAndName(course, name)) {
            throw ApiException.conflict("Course '" + course + "' already has a fee structure named '" + name + "'");
        }
        FeeStructure structure = new FeeStructure(name, course);
        structure.setTerms(request.termsPerYear(), amounts);
        return FeeStructureResponse.of(structures.save(structure), false);
    }

    public FeeStructureResponse update(Long id, FeeStructureRequest request) {
        FeeStructure structure = find(id);
        if (request != null && request.courseName() != null && !request.courseName().equals(structure.getCourseName())) {
            throw ApiException.invalidField("courseName", "The course of a fee structure can't be changed");
        }
        FeeStructureRequest withCourse = new FeeStructureRequest(request == null ? null : request.name(),
                structure.getCourseName(), request == null ? null : request.termsPerYear(),
                request == null ? null : request.terms());
        Map<Integer, BigDecimal> amounts = validate(withCourse);
        String name = withCourse.name().trim();
        if (structures.existsByCourseNameAndNameAndIdNot(structure.getCourseName(), name, id)) {
            throw ApiException.conflict("Course '" + structure.getCourseName()
                    + "' already has a fee structure named '" + name + "'");
        }
        structure.setName(name);
        structure.setTerms(withCourse.termsPerYear(), amounts);
        boolean used = timeline.existsByFeeStructure_Id(id);
        if (used) {
            events.publishEvent(new CourseDuesInvalidated(structure.getCourseName()));
        }
        return FeeStructureResponse.of(structure, used);
    }

    public FeeStructureResponse copy(Long id, String newName) {
        FeeStructure source = find(id);
        if (newName == null || newName.isBlank()) {
            throw ApiException.invalidField("name", "A name for the copy is required");
        }
        String name = newName.trim();
        if (structures.existsByCourseNameAndName(source.getCourseName(), name)) {
            throw ApiException.conflict("Course '" + source.getCourseName()
                    + "' already has a fee structure named '" + name + "'");
        }
        FeeStructure copy = new FeeStructure(name, source.getCourseName());
        copy.setTerms(source.getTermsPerYear(), source.amountsByTerm());
        return FeeStructureResponse.of(structures.save(copy), false);
    }

    public void delete(Long id) {
        FeeStructure structure = find(id);
        if (timeline.existsByFeeStructure_Id(id)) {
            throw ApiException.conflict("Fee structure " + id + " is used in the timeline and can't be deleted");
        }
        structures.delete(structure);
    }

    @Transactional(readOnly = true)
    public List<FeeStructureResponse> list(String courseName) {
        List<FeeStructure> found = courseName == null
                ? structures.findAllByOrderByCourseNameAscNameAsc()
                : structures.findByCourseNameOrderByNameAsc(courseName);
        return found.stream().map(s -> FeeStructureResponse.of(s, timeline.existsByFeeStructure_Id(s.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public FeeStructureResponse get(Long id) {
        return FeeStructureResponse.of(find(id), timeline.existsByFeeStructure_Id(id));
    }

    FeeStructure find(Long id) {
        return structures.findById(id).orElseThrow(() -> ApiException.notFound("Fee structure " + id + " not found"));
    }

    /** Collects every problem so one 400 lists them all (REQ-005.AC2). Returns amounts by term when valid. */
    private static Map<Integer, BigDecimal> validate(FeeStructureRequest r) {
        List<FieldProblem> problems = new ArrayList<>();
        if (r == null) {
            throw ApiException.badRequest("A request body is required");
        }
        if (r.name() == null || r.name().isBlank()) {
            problems.add(new FieldProblem("name", "Name is required"));
        }
        if (r.courseName() == null || r.courseName().isBlank()) {
            problems.add(new FieldProblem("courseName", "Course is required"));
        }
        Integer n = r.termsPerYear();
        if (n == null || !ALLOWED_TERMS_PER_YEAR.contains(n)) {
            problems.add(new FieldProblem("termsPerYear", "Terms per year must be 1, 2, 3, 4, 6 or 12"));
        }
        Map<Integer, BigDecimal> amounts = new TreeMap<>();
        String termsMessage = null;
        List<TermAmount> terms = r.terms() == null ? List.of() : r.terms();
        Set<Integer> duplicates = new TreeSet<>();
        for (int i = 0; i < terms.size(); i++) {
            TermAmount t = terms.get(i);
            if (t == null || t.termNumber() == null) {
                problems.add(new FieldProblem("terms[" + i + "].termNumber", "Term number is required"));
                continue;
            }
            if (t.amount() == null) {
                problems.add(new FieldProblem("terms[" + i + "].amount", "Amount is required"));
            } else if (t.amount().signum() < 0) {
                problems.add(new FieldProblem("terms[" + i + "].amount", "Amount must not be negative"));
            }
            if (amounts.containsKey(t.termNumber())) {
                duplicates.add(t.termNumber());
            }
            amounts.put(t.termNumber(), t.amount());
        }
        if (n != null && ALLOWED_TERMS_PER_YEAR.contains(n)) {
            Set<Integer> expected = IntStream.rangeClosed(1, n).boxed().collect(Collectors.toCollection(TreeSet::new));
            Set<Integer> missing = new TreeSet<>(expected);
            missing.removeAll(amounts.keySet());
            Set<Integer> extra = new TreeSet<>(amounts.keySet());
            extra.removeAll(expected);
            List<String> parts = new ArrayList<>();
            if (!missing.isEmpty()) {
                parts.add("missing terms: " + join(missing));
            }
            if (!duplicates.isEmpty()) {
                parts.add("duplicate terms: " + join(duplicates));
            }
            if (!extra.isEmpty()) {
                parts.add("terms out of range: " + join(extra));
            }
            if (!parts.isEmpty()) {
                termsMessage = "Terms must be numbered 1 to " + n + " exactly once (" + String.join("; ", parts) + ")";
                problems.add(new FieldProblem("terms", termsMessage));
            }
        }
        if (!problems.isEmpty()) {
            String message = termsMessage != null && problems.size() == 1 ? termsMessage : "Validation failed";
            if (termsMessage != null && problems.size() > 1) {
                message = "Validation failed: " + termsMessage;
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, message, problems);
        }
        return amounts;
    }

    private static String join(Set<Integer> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }
}
