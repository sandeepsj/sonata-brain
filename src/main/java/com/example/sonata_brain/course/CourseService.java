package com.example.sonata_brain.course;

import com.example.sonata_brain.common.error.ApiException;
import com.example.sonata_brain.model.Course;
import com.example.sonata_brain.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courses;

    public CourseService(CourseRepository courses) {
        this.courses = courses;
    }

    public Course create(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw ApiException.invalidField("name", "Course name is required");
        }
        String name = rawName.trim();
        if (courses.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("A course named '" + name + "' already exists");
        }
        return courses.save(new Course(name));
    }

    @Transactional(readOnly = true)
    public List<Course> list(Course.Status status) {
        return status == null ? courses.findAllByOrderByNameAsc() : courses.findByStatusOrderByNameAsc(status);
    }

    @Transactional(readOnly = true)
    public Course get(String name) {
        return courses.findById(name).orElseThrow(() -> ApiException.notFound("Course '" + name + "' not found"));
    }

    public Course close(String name) {
        Course course = get(name);
        if (course.isClosed()) {
            throw ApiException.conflict("Course '" + name + "' is already closed");
        }
        course.close(LocalDateTime.now());
        return course;
    }

    public Course reopen(String name) {
        Course course = get(name);
        if (!course.isClosed()) {
            throw ApiException.conflict("Course '" + name + "' is already active");
        }
        course.reopen();
        return course;
    }

    /** A closed course takes no new sessions, timeline entries or course-fee payments (REQ-042.AC4). */
    @Transactional(readOnly = true)
    public Course requireActive(String name) {
        Course course = get(name);
        if (course.isClosed()) {
            throw ApiException.conflict("Course '" + name + "' is closed");
        }
        return course;
    }
}
