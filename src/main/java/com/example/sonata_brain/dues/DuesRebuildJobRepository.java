package com.example.sonata_brain.dues;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DuesRebuildJobRepository extends JpaRepository<DuesRebuildJob, Long> {
}
