package com.example.sonata_brain.repository;

import com.example.sonata_brain.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    Optional<Course> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Course> findAllByOrderByNameAsc();

    List<Course> findByStatusOrderByNameAsc(Course.Status status);
}
