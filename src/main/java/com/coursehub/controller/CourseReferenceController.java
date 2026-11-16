package com.coursehub.controller;

import com.coursehub.dto.request.ReferenceRequest;
import com.coursehub.dto.response.CourseReferenceResponse;
import com.coursehub.service.CourseReferenceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class CourseReferenceController {

    private final CourseReferenceService courseReferenceService;

    @GetMapping("/courses/{courseId}/references")
    public ResponseEntity<List<CourseReferenceResponse>> getReferences(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseReferenceService.getReferences(courseId));
    }

    @PostMapping(value = "/admin/courses/{courseId}/references",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseReferenceResponse> addReference(@PathVariable Long courseId,
                                                                @Valid @RequestBody ReferenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseReferenceService.addReference(courseId, request));
    }

    @PutMapping(value = "/admin/references/{referenceId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseReferenceResponse> updateReference(@PathVariable Long referenceId,
                                                                   @Valid @RequestBody ReferenceRequest request) {
        return ResponseEntity.ok(courseReferenceService.updateReference(referenceId, request));
    }

    @DeleteMapping("/admin/references/{referenceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReference(@PathVariable Long referenceId) {
        courseReferenceService.deleteReference(referenceId);
        return ResponseEntity.noContent().build();
    }
}
