package com.example.sonata_brain.fees;

import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.dues.DuesService;
import com.example.sonata_brain.fees.FeeDtos.AmountRequest;
import com.example.sonata_brain.fees.FeeDtos.StructureRequest;
import com.example.sonata_brain.fees.FeeDtos.StructureResponse;
import com.example.sonata_brain.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class FeeStructureService {

    private final FeeStructureRepository structures;
    private final TimelineEntryRepository timeline;
    private final CourseRepository courses;
    private final DuesService dues;

    public FeeStructureService(FeeStructureRepository structures, TimelineEntryRepository timeline,
                               CourseRepository courses, DuesService dues) {
        this.structures = structures;
        this.timeline = timeline;
        this.courses = courses;
        this.dues = dues;
    }

    @Transactional(readOnly = true)
    public List<StructureResponse> list() {
        return structures.findAllByOrderByNameAsc().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public StructureResponse get(Long id) {
        return response(find(id));
    }

    public StructureResponse create(StructureRequest request) {
        Valid valid = validate(request);
        if (structures.existsByNameIgnoreCase(valid.name())) {
            throw ApiException.conflict("A fee structure named '" + valid.name() + "' already exists");
        }
        return response(structures.save(new FeeStructure(valid.name(), valid.termMonths(), valid.amounts())));
    }

    /** REQ-057.AC6/AC7: allowed while on the timeline, unless a later entry would fall off a term boundary. */
    public StructureResponse update(Long id, StructureRequest request) {
        FeeStructure structure = find(id);
        Valid valid = validate(request);
        if (structures.existsByNameIgnoreCaseAndIdNot(valid.name(), id)) {
            throw ApiException.conflict("A fee structure named '" + valid.name() + "' already exists");
        }
        List<TimelineEntry> entries = timeline.findAllByOrderByEffectiveFromAsc();
        boolean used = entries.stream().anyMatch(e -> e.getStructure().getId().equals(id));
        if (used && valid.termMonths() != structure.getTermMonths()) {
            FeeStructure edited = new FeeStructure(valid.name(), valid.termMonths(), valid.amounts());
            List<Timeline.Entry> proposed = entries.stream()
                    .map(e -> new Timeline.Entry(e.getEffectiveFrom(),
                            e.getStructure().getId().equals(id) ? edited : e.getStructure()))
                    .toList();
            Timeline.boundaryProblem(proposed).ifPresent(problem -> {
                throw ApiException.conflict("Changing the term length would break the timeline. " + problem);
            });
        }
        structure.replace(valid.name(), valid.termMonths(), valid.amounts());
        structures.saveAndFlush(structure);
        if (used) {
            dues.recalculateAll();
        }
        return response(structure);
    }

    public void delete(Long id) {
        FeeStructure structure = find(id);
        if (timeline.existsByStructure_Id(id)) {
            throw ApiException.conflict("'" + structure.getName() + "' is on the fee timeline; remove it from there first");
        }
        structures.delete(structure);
    }

    private FeeStructure find(Long id) {
        return structures.findById(id).orElseThrow(() -> ApiException.notFound("Fee structure " + id + " not found"));
    }

    private StructureResponse response(FeeStructure s) {
        return StructureResponse.of(s, timeline.existsByStructure_Id(s.getId()));
    }

    private record Valid(String name, int termMonths, List<FeeAmount> amounts) {
    }

    private Valid validate(StructureRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw ApiException.invalidField("name", "Name is required");
        }
        if (request.termMonths() == null || request.termMonths() < 1 || request.termMonths() > 12) {
            throw ApiException.invalidField("termMonths", "termMonths must be a whole number from 1 to 12");
        }
        List<FeeAmount> amounts = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<AmountRequest> raw = request.amounts() == null ? List.of() : request.amounts();
        for (int i = 0; i < raw.size(); i++) {
            AmountRequest a = raw.get(i);
            String field = "amounts[" + i + "]";
            if (a == null || a.courseName() == null || !courses.existsById(a.courseName())) {
                throw ApiException.invalidField(field + ".courseName",
                        "Unknown course '" + (a == null ? null : a.courseName()) + "'");
            }
            if (!seen.add(a.courseName())) {
                throw ApiException.invalidField(field + ".courseName",
                        "Course '" + a.courseName() + "' is listed more than once");
            }
            if (a.amount() == null || a.amount().compareTo(BigDecimal.ZERO) < 0) {
                throw ApiException.invalidField(field + ".amount", "Amount must be 0 or more");
            }
            amounts.add(new FeeAmount(a.courseName(), a.amount()));
        }
        return new Valid(request.name().trim(), request.termMonths(), amounts);
    }
}
