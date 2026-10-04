package com.example.sonata_brain.fee;

import com.example.sonata_brain.fee.FeeDtos.CloneRequest;
import com.example.sonata_brain.fee.FeeDtos.FeeStructureRequest;
import com.example.sonata_brain.fee.FeeDtos.FeeStructureResponse;
import com.example.sonata_brain.fee.FeeDtos.TimelineRequest;
import com.example.sonata_brain.fee.FeeDtos.TimelineResponse;
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

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public FeeStructureResponse create(@RequestBody(required = false) FeeStructureRequest request) {
            return structures.create(request);
        }

        @GetMapping
        public List<FeeStructureResponse> list(@RequestParam(required = false) String courseName) {
            return structures.list(courseName);
        }

        @GetMapping("/{id}")
        public FeeStructureResponse get(@PathVariable Long id) {
            return structures.get(id);
        }

        @PutMapping("/{id}")
        public FeeStructureResponse update(@PathVariable Long id, @RequestBody(required = false) FeeStructureRequest request) {
            return structures.update(id, request);
        }

        @PostMapping("/{id}/clone")
        @ResponseStatus(HttpStatus.CREATED)
        public FeeStructureResponse copy(@PathVariable Long id, @RequestBody(required = false) CloneRequest request) {
            return structures.copy(id, request == null ? null : request.name());
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

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public TimelineResponse create(@RequestBody(required = false) TimelineRequest request) {
            return timeline.create(request);
        }

        @GetMapping
        public List<TimelineResponse> list(@RequestParam(required = false) String courseName) {
            return timeline.list(courseName);
        }

        @PutMapping("/{id}")
        public TimelineResponse update(@PathVariable Long id, @RequestBody(required = false) TimelineRequest request) {
            return timeline.update(id, request);
        }

        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(@PathVariable Long id) {
            timeline.delete(id);
        }

        @GetMapping("/preview")
        public List<BillingPeriod> preview(@RequestParam String courseName,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
            return timeline.preview(courseName, from, to);
        }
    }
}
