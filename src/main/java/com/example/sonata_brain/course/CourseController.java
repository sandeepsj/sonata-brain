package com.example.sonata_brain.course;

import com.example.sonata_brain.model.Course;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    public record CreateCourse(String name) {
    }

    public record CourseResponse(String name, Course.Status status, LocalDateTime closedAt) {
        static CourseResponse of(Course c) {
            return new CourseResponse(c.getName(), c.getStatus(), c.getClosedAt());
        }
    }

    private final CourseService courses;

    public CourseController(CourseService courses) {
        this.courses = courses;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@RequestBody(required = false) CreateCourse request) {
        return CourseResponse.of(courses.create(request == null ? null : request.name()));
    }

    @GetMapping
    public List<CourseResponse> list(@RequestParam(required = false) Course.Status status) {
        return courses.list(status).stream().map(CourseResponse::of).toList();
    }

    @GetMapping("/{name}")
    public CourseResponse get(@PathVariable String name) {
        return CourseResponse.of(courses.get(name));
    }

    @PatchMapping("/{name}/close")
    public CourseResponse close(@PathVariable String name) {
        return CourseResponse.of(courses.close(name));
    }

    @PatchMapping("/{name}/reopen")
    public CourseResponse reopen(@PathVariable String name) {
        return CourseResponse.of(courses.reopen(name));
    }
}
