package com.coursehub.controller;

import com.coursehub.dto.request.CourseRequest;
import com.coursehub.dto.request.CourseStatusRequest;
import com.coursehub.dto.response.CourseDetailsResponse;
import com.coursehub.dto.response.CourseListResponse;
import com.coursehub.dto.response.PagedResponse;
import com.coursehub.service.CourseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management, search and catalogue")
public class CourseController {

    private final CourseService courseService;

    @PostMapping(value = "/admin/courses", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseDetailsResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request));
    }

    @GetMapping("/courses/{courseId}")
    public ResponseEntity<CourseDetailsResponse> getCourseById(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseById(courseId));
    }

    @GetMapping("/courses")
    public ResponseEntity<PagedResponse<CourseListResponse>> searchCourses(
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String instructorName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${app.pagination.default-page-size}") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        return ResponseEntity.ok(courseService.searchCourses(
                courseName, category, instructorName, keyword, page, size, sortBy, sortDirection));
    }

    @PutMapping(value = "/admin/courses/{courseId}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseDetailsResponse> updateCourse(@PathVariable Long courseId,
                                                              @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, request));
    }

    @PatchMapping(value = "/admin/courses/{courseId}/status", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseDetailsResponse> changeStatus(@PathVariable Long courseId,
                                                              @Valid @RequestBody CourseStatusRequest request) {
        return ResponseEntity.ok(courseService.changeStatus(courseId, request));
    }

    @DeleteMapping("/admin/courses/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.noContent().build();
    }
}
