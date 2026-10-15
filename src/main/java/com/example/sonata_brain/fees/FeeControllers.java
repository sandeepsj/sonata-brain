package com.example.sonata_brain.fees;

import com.example.sonata_brain.fees.FeeDtos.EntryChange;
import com.example.sonata_brain.fees.FeeDtos.EntryRequest;
import com.example.sonata_brain.fees.FeeDtos.EntryResponse;
import com.example.sonata_brain.fees.FeeDtos.PreviewTerm;
import com.example.sonata_brain.fees.FeeDtos.StructureChange;
import com.example.sonata_brain.fees.FeeDtos.StructureRequest;
import com.example.sonata_brain.fees.FeeDtos.StructureResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** HTTP endpoints for fee structures and the timeline. Rules live in the services. */
public final class FeeControllers {

    private FeeControllers() {
    }

    @RestController
    @RequestMapping("/api/fee-structures")
    public static class FeeStructureController {
        private final FeeStructureService structures;

        public FeeStructureController(FeeStructureService structures) {
            this.structures = structures;
        }

        @GetMapping
        public List<StructureResponse> list() {
            return structures.list();
        }

        @GetMapping("/{id}")
        public StructureResponse get(@PathVariable Long id) {
            return structures.get(id);
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public StructureResponse create(@RequestBody StructureRequest request) {
            return structures.create(request);
        }

        @PutMapping("/{id}")
        public StructureChange update(@PathVariable Long id, @RequestBody StructureRequest request,
                                      @RequestParam(defaultValue = "false") boolean dryRun) {
            return structures.update(id, request, dryRun);
        }

        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(@PathVariable Long id) {
            structures.delete(id);
        }
    }

    @RestController
    @RequestMapping("/api/fee-timeline")
    public static class FeeTimelineController {
        private final FeeTimelineService timeline;

        public FeeTimelineController(FeeTimelineService timeline) {
            this.timeline = timeline;
        }

        @GetMapping
        public List<EntryResponse> list() {
            return timeline.list();
        }

        /** 201 when added; 200 for a dry run, which saves nothing (REQ-058.AC11). */
        @PostMapping
        public ResponseEntity<EntryChange> create(@RequestBody EntryRequest request,
                                                  @RequestParam(defaultValue = "false") boolean dryRun) {
            return ResponseEntity.status(dryRun ? HttpStatus.OK : HttpStatus.CREATED).body(timeline.create(request, dryRun));
        }

        @PutMapping("/{id}")
        public EntryChange update(@PathVariable Long id, @RequestBody EntryRequest request,
                                  @RequestParam(defaultValue = "false") boolean dryRun) {
            return timeline.update(id, request, dryRun);
        }

        @DeleteMapping("/{id}")
        public EntryChange delete(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean dryRun) {
            return timeline.delete(id, dryRun);
        }

        @GetMapping("/preview")
        public List<PreviewTerm> preview(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
            return timeline.preview(from, to);
        }
    }
}
