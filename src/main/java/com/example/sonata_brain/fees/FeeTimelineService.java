package com.example.sonata_brain.fees;

import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.dues.DuesService;
import com.example.sonata_brain.fees.FeeDtos.EntryRequest;
import com.example.sonata_brain.fees.FeeDtos.EntryResponse;
import com.example.sonata_brain.fees.FeeDtos.PreviewTerm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The school-wide timeline (REQ-058). Every change is checked for term boundaries and recalculates all dues. */
@Service
@Transactional
public class FeeTimelineService {

    private final TimelineEntryRepository timeline;
    private final FeeStructureRepository structures;
    private final DuesService dues;

    public FeeTimelineService(TimelineEntryRepository timeline, FeeStructureRepository structures, DuesService dues) {
        this.timeline = timeline;
        this.structures = structures;
        this.dues = dues;
    }

    @Transactional(readOnly = true)
    public List<EntryResponse> list() {
        List<TimelineEntry> entries = timeline.findAllByOrderByEffectiveFromAsc();
        List<EntryResponse> result = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            TimelineEntry e = entries.get(i);
            LocalDate to = i + 1 < entries.size() ? entries.get(i + 1).getEffectiveFrom().minusDays(1) : null;
            result.add(new EntryResponse(e.getId(), e.getStructure().getId(), e.getStructure().getName(),
                    e.getStructure().getTermMonths(), e.getEffectiveFrom(), to));
        }
        return result;
    }

    public EntryResponse create(EntryRequest request) {
        FeeStructure structure = validStructure(request);
        List<TimelineEntry> entries = timeline.findAllByOrderByEffectiveFromAsc();
        rejectSameDate(entries, request.effectiveFrom(), null);
        List<Timeline.Entry> proposed = new ArrayList<>(entries.stream().map(TimelineEntry::toEntry).toList());
        proposed.add(new Timeline.Entry(request.effectiveFrom(), structure));
        check(proposed);
        TimelineEntry saved = timeline.saveAndFlush(new TimelineEntry(structure, request.effectiveFrom()));
        dues.recalculateAll();
        return response(saved.getId());
    }

    public EntryResponse update(Long id, EntryRequest request) {
        TimelineEntry entry = find(id);
        FeeStructure structure = validStructure(request);
        List<TimelineEntry> entries = timeline.findAllByOrderByEffectiveFromAsc();
        rejectSameDate(entries, request.effectiveFrom(), id);
        List<Timeline.Entry> proposed = new ArrayList<>(entries.stream()
                .map(e -> e.getId().equals(id) ? new Timeline.Entry(request.effectiveFrom(), structure) : e.toEntry())
                .toList());
        check(proposed);
        entry.setStructure(structure);
        entry.setEffectiveFrom(request.effectiveFrom());
        timeline.saveAndFlush(entry);
        dues.recalculateAll();
        return response(id);
    }

    public void delete(Long id) {
        TimelineEntry entry = find(id);
        List<Timeline.Entry> proposed = new ArrayList<>(timeline.findAllByOrderByEffectiveFromAsc().stream()
                .filter(e -> !e.getId().equals(id)).map(TimelineEntry::toEntry).toList());
        check(proposed);
        timeline.delete(entry);
        timeline.flush();
        dues.recalculateAll();
    }

    /** REQ-059.AC2: the terms overlapping [from, to]; writes nothing. */
    @Transactional(readOnly = true)
    public List<PreviewTerm> preview(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw ApiException.badRequest("'from' must be on or before 'to'");
        }
        return Timeline.terms(entries(), from, to).stream().map(PreviewTerm::of).toList();
    }

    /** The current timeline, sorted, for the dues calculation. */
    @Transactional(readOnly = true)
    public List<Timeline.Entry> entries() {
        return timeline.findAllByOrderByEffectiveFromAsc().stream().map(TimelineEntry::toEntry).toList();
    }

    private void check(List<Timeline.Entry> proposed) {
        List<Timeline.Entry> sorted = proposed.stream().sorted(Comparator.comparing(Timeline.Entry::from)).toList();
        Timeline.boundaryProblem(sorted).ifPresent(problem -> {
            throw ApiException.badRequest(problem);
        });
    }

    private static void rejectSameDate(List<TimelineEntry> entries, LocalDate date, Long except) {
        if (entries.stream().anyMatch(e -> e.getEffectiveFrom().equals(date) && !e.getId().equals(except))) {
            throw ApiException.conflict("The timeline already has an entry starting on " + date);
        }
    }

    private FeeStructure validStructure(EntryRequest request) {
        if (request == null || request.feeStructureId() == null) {
            throw ApiException.invalidField("feeStructureId", "feeStructureId is required");
        }
        if (request.effectiveFrom() == null) {
            throw ApiException.invalidField("effectiveFrom", "effectiveFrom is required");
        }
        return structures.findById(request.feeStructureId()).orElseThrow(() -> ApiException.invalidField(
                "feeStructureId", "Unknown fee structure " + request.feeStructureId()));
    }

    private TimelineEntry find(Long id) {
        return timeline.findById(id).orElseThrow(() -> ApiException.notFound("Timeline entry " + id + " not found"));
    }

    private EntryResponse response(Long id) {
        return list().stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow();
    }
}
