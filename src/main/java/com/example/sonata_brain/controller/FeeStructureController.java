package com.example.sonata_brain.controller;

import com.example.sonata_brain.dto.FeeStructureCreateRequest;
import com.example.sonata_brain.model.Course;
import com.example.sonata_brain.model.FeeStructure;
import com.example.sonata_brain.repository.CourseRepository;
import com.example.sonata_brain.repository.FeeStructureRepository;
import com.example.sonata_brain.exception.FeeStructureValidationException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/fee-structures")
public class FeeStructureController {

    private final FeeStructureRepository feeStructureRepository;
    private final CourseRepository courseRepository;

    @Autowired
    public FeeStructureController(FeeStructureRepository feeStructureRepository,
                                CourseRepository courseRepository) {
        this.feeStructureRepository = feeStructureRepository;
        this.courseRepository = courseRepository;
    }

    @PostMapping
    public ResponseEntity<List<FeeStructure>> createFeeStructure(@Valid @RequestBody FeeStructureCreateRequest request) {
        // Validate course exists
        Course course = courseRepository.findById(request.getCourseId())
            .orElseThrow(() -> new FeeStructureValidationException(
                String.format("Course with ID '%s' not found. Please ensure the course exists before creating a fee structure.", 
                    request.getCourseId())
            ));

        // Check if fee structure already exists for this course and year
        if (feeStructureRepository.existsByCourseAndYear(course, request.getYear())) {
            throw new FeeStructureValidationException(
                String.format("Fee structure already exists for course '%s' and year %d. Multiple fee structures for the same course and year are not allowed.", 
                    course.getName(), request.getYear())
            );
        }

        // Validate that all terms are provided
        Set<Integer> providedTerms = new HashSet<>();
        request.getTermFees().forEach(termFee -> providedTerms.add(termFee.getTerm()));
        
        // Check if all terms from 1 to numberOfTerms are present
        Set<Integer> requiredTerms = new HashSet<>();
        IntStream.rangeClosed(1, request.getNumberOfTerms()).forEach(requiredTerms::add);
        
        if (!providedTerms.equals(requiredTerms)) {
            throw new FeeStructureValidationException(
                String.format("Invalid term configuration. All terms from 1 to %d must be provided. Missing terms: %s", 
                    request.getNumberOfTerms(),
                    requiredTerms.stream()
                        .filter(term -> !providedTerms.contains(term))
                        .map(String::valueOf)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("")
                )
            );
        }

        // Validate term numbers are sequential
        request.getTermFees().forEach(termFee -> {
            if (termFee.getTerm() < 1 || termFee.getTerm() > request.getNumberOfTerms()) {
                throw new FeeStructureValidationException(
                    String.format("Invalid term number %d. Term numbers must be between 1 and %d", 
                        termFee.getTerm(), request.getNumberOfTerms())
                );
            }
        });

        // Create fee structures for each term
        List<FeeStructure> createdStructures = request.getTermFees().stream()
            .map(termFee -> {
                FeeStructure feeStructure = new FeeStructure();
                feeStructure.setCourse(course);
                feeStructure.setYear(request.getYear());
                feeStructure.setTerm(termFee.getTerm());
                feeStructure.setAmount(termFee.getAmount());
                return feeStructureRepository.save(feeStructure);
            })
            .toList();

        return ResponseEntity.ok(createdStructures);
    }

    @GetMapping("/by-year/{year}")
    public ResponseEntity<List<FeeStructure>> getFeeStructuresByYear(@PathVariable Integer year) {
        List<FeeStructure> feeStructures = feeStructureRepository.findByYear(year);
        if (feeStructures.isEmpty()) {
            throw new FeeStructureValidationException(
                String.format("No fee structures found for year %d", year)
            );
        }
        return ResponseEntity.ok(feeStructures);
    }

    @GetMapping("/by-course/{courseId}")
    public ResponseEntity<List<FeeStructure>> getFeeStructuresByCourse(@PathVariable String courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new FeeStructureValidationException(
                String.format("Course with ID '%s' not found", courseId)
            ));
        
        List<FeeStructure> feeStructures = feeStructureRepository.findByCourse(course);
        if (feeStructures.isEmpty()) {
            throw new FeeStructureValidationException(
                String.format("No fee structures found for course '%s'", course.getName())
            );
        }
        return ResponseEntity.ok(feeStructures);
    }

    @GetMapping("/by-course/{courseId}/year/{year}")
    public ResponseEntity<List<FeeStructure>> getFeeStructuresByCourseAndYear(
            @PathVariable String courseId,
            @PathVariable Integer year) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new FeeStructureValidationException(
                String.format("Course with ID '%s' not found", courseId)
            ));
        
        List<FeeStructure> feeStructures = feeStructureRepository.findByCourseAndYear(course, year);
        if (feeStructures.isEmpty()) {
            throw new FeeStructureValidationException(
                String.format("No fee structures found for course '%s' and year %d", 
                    course.getName(), year)
            );
        }
        return ResponseEntity.ok(feeStructureRepository.findByCourseAndYear(course, year));
    }

    @GetMapping("/by-course/{courseId}/year/{year}/term/{term}")
    public ResponseEntity<FeeStructure> getFeeStructureByCourseYearAndTerm(
            @PathVariable String courseId,
            @PathVariable Integer year,
            @PathVariable Integer term) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new FeeStructureValidationException(
                String.format("Course with ID '%s' not found", courseId)
            ));

        return ResponseEntity.ok(feeStructureRepository.findByCourseAndYearAndTerm(course, year, term)
            .orElseThrow(() -> new FeeStructureValidationException(
                String.format("Fee structure not found for course '%s', year %d, term %d", 
                    course.getName(), year, term)
            )));
    }
}
