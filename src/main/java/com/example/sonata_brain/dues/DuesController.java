package com.example.sonata_brain.dues;

import com.example.sonata_brain.dues.DuesDtos.DueResponse;
import com.example.sonata_brain.dues.DuesDtos.Job;
import com.example.sonata_brain.dues.DuesDtos.Summary;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Reading dues and starting recalculations (REQ-074.AC8, REQ-076). Dues are never written through the API. */
@RestController
@RequestMapping("/api/dues")
public class DuesController {

    private final DuesService dues;
    private final DuesRebuildJobs jobs;

    public DuesController(DuesService dues, DuesRebuildJobs jobs) {
        this.dues = dues;
        this.jobs = jobs;
    }

    @GetMapping
    public List<DueResponse> list(@RequestParam(required = false) Long studentId,
                                  @RequestParam(required = false) String courseName,
                                  @RequestParam(required = false) DueStatus status,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dues.list(studentId, courseName, status, from, to);
    }

    @GetMapping("/summary")
    public Summary summary(@RequestParam Long studentId) {
        return dues.summary(studentId);
    }

    @PostMapping("/rebuild")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> rebuild(@RequestParam DuesRebuildJobs.Scope scope, @RequestParam(required = false) Long id) {
        return Map.of("jobId", jobs.start(scope, id).jobId());
    }

    @GetMapping("/rebuild/{jobId}")
    public Job job(@PathVariable String jobId) {
        return jobs.get(jobId);
    }
}
