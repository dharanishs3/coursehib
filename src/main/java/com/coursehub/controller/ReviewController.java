package com.coursehub.controller;

import com.coursehub.dto.request.ReviewRequest;
import com.coursehub.dto.response.PagedResponse;
import com.coursehub.dto.response.ReviewResponse;
import com.coursehub.service.ReviewService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Course ratings and feedback")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/courses/{courseId}/reviews")
    public ResponseEntity<PagedResponse<ReviewResponse>> getReviews(@PathVariable Long courseId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "${app.pagination.default-page-size}") int size) {
        return ResponseEntity.ok(reviewService.getReviews(courseId, page, size));
    }

    @PostMapping(value = "/courses/{courseId}/reviews",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<ReviewResponse> submitReview(@PathVariable Long courseId,
                                                       @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.submitReview(courseId, request));
    }

    @PutMapping(value = "/reviews/{reviewId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('LEARNER')")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long reviewId,
                                                       @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request));
    }
}
