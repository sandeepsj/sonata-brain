package com.example.sonata_brain.fees;

import com.example.sonata_brain.fees.FeeDtos.EntryRequest;
import com.example.sonata_brain.fees.FeeDtos.EntryResponse;
import com.example.sonata_brain.fees.FeeDtos.PreviewTerm;
import com.example.sonata_brain.fees.FeeDtos.StructureRequest;
import com.example.sonata_brain.fees.FeeDtos.StructureResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
        public StructureResponse update(@PathVariable Long id, @RequestBody StructureRequest request) {
            return structures.update(id, request);
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

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public EntryResponse create(@RequestBody EntryRequest request) {
            return timeline.create(request);
        }

        @PutMapping("/{id}")
        public EntryResponse update(@PathVariable Long id, @RequestBody EntryRequest request) {
            return timeline.update(id, request);
        }

        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(@PathVariable Long id) {
            timeline.delete(id);
        }

        @GetMapping("/preview")
        public List<PreviewTerm> preview(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
            return timeline.preview(from, to);
        }
    }
}
