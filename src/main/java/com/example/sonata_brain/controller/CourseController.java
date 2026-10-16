package com.example.sonata_brain.controller;

import com.example.sonata_brain.common.error.ApiException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import com.example.sonata_brain.model.Course;
import com.example.sonata_brain.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseRepository courseRepository;

    @Autowired
    public CourseController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /** REQ-041.AC1: all courses, or only ACTIVE or CLOSED ones. */
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses(@RequestParam(required = false) Course.Status status) {
        List<Course> all = courseRepository.findAll(Sort.by("name"));
        return ResponseEntity.ok(status == null ? all : all.stream().filter(c -> c.getStatus() == status).toList());
    }

    /** REQ-041.AC2. */
    @GetMapping("/{name}")
    public Course getCourse(@PathVariable String name) {
        return find(name);
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        course.markNew();
        return ResponseEntity.ok(courseRepository.save(course));
    }

    /** REQ-042.AC1: closing keeps sessions, payments and dues as they are (REQ-042.AC3). */
    @PatchMapping("/{name}/close")
    @Transactional
    public Course close(@PathVariable String name) {
        Course course = find(name);
        if (course.isClosed()) {
            throw ApiException.conflict("Course '" + name + "' is already closed");
        }
        course.close();
        return courseRepository.save(course);
    }

    /** REQ-042.AC2. */
    @PatchMapping("/{name}/reopen")
    @Transactional
    public Course reopen(@PathVariable String name) {
        Course course = find(name);
        if (!course.isClosed()) {
            throw ApiException.conflict("Course '" + name + "' is not closed");
        }
        course.reopen();
        return courseRepository.save(course);
    }

    private Course find(String name) {
        return courseRepository.findById(name)
                .orElseThrow(() -> ApiException.notFound("Course '" + name + "' not found"));
    }
}
