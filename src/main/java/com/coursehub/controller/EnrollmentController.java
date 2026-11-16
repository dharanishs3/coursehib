package com.coursehub.controller;

import com.coursehub.dto.request.VideoProgressRequest;
import com.coursehub.dto.response.EnrollmentDetailsResponse;
import com.coursehub.dto.response.EnrollmentProgressResponse;
import com.coursehub.dto.response.EnrollmentResponse;
import com.coursehub.service.EnrollmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Enroll in courses and track learning progress (LEARNER only)")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enroll")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable Long courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(courseId));
    }

    @GetMapping("/enrollments/my")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments() {
        return ResponseEntity.ok(enrollmentService.getMyEnrollments());
    }

    @GetMapping("/enrollments/{enrollmentId}/progress")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<EnrollmentProgressResponse> getProgress(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(enrollmentService.getProgress(enrollmentId));
    }

    @PutMapping(value = "/enrollments/{enrollmentId}/videos/{videoId}/progress",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<EnrollmentProgressResponse> updateVideoProgress(@PathVariable Long enrollmentId,
                                                                          @PathVariable Long videoId,
                                                                          @Valid @RequestBody VideoProgressRequest request) {
        return ResponseEntity.ok(enrollmentService.updateVideoProgress(enrollmentId, videoId, request));
    }

    @GetMapping("/enrollments/{enrollmentId}")
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<EnrollmentDetailsResponse> getEnrollmentDetails(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentDetails(enrollmentId));
    }
}
