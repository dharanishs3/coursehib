package com.coursehub.service;

import com.coursehub.dto.request.ReviewRequest;
import com.coursehub.dto.response.PagedResponse;
import com.coursehub.dto.response.ReviewResponse;
import com.coursehub.entity.Course;
import com.coursehub.entity.CourseReview;
import com.coursehub.exception.BadRequestException;
import com.coursehub.exception.ConflictException;
import com.coursehub.exception.ForbiddenException;
import com.coursehub.exception.ResourceNotFoundException;
import com.coursehub.helper.CourseAccessHelper;
import com.coursehub.mapper.ReviewMapper;
import com.coursehub.repository.CourseRepository;
import com.coursehub.repository.CourseReviewRepository;
import com.coursehub.repository.EnrollmentRepository;
import com.coursehub.repository.UserRepository;
import com.coursehub.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Course ratings and feedback. Only enrolled learners write reviews, and only their own review can be changed. */
@Service
@RequiredArgsConstructor
public class ReviewService {


    @Value("${app.pagination.max-page-size}")
    private int maxPageSize;

    private final CourseReviewRepository courseReviewRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseAccessHelper courseAccessHelper;
    private final CurrentUserService currentUserService;

    @Transactional
    public ReviewResponse submitReview(Long courseId, ReviewRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        Long userId = currentUserService.getCurrentUserId();
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ForbiddenException("Only enrolled learners can review this course");
        }
        if (courseReviewRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ConflictException("You have already reviewed this course");
        }

        CourseReview review = CourseReview.builder()
                .user(userRepository.getReferenceById(userId))
                .course(course)
                .rating(request.getRating())
                .reviewComment(cleanComment(request.getReviewComment()))
                .build(); // submittedAt is set automatically when the review is saved
        return ReviewMapper.toResponse(courseReviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        CourseReview review = courseReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getUser().getId().equals(currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("You can update only your own review");
        }

        // Only the rating and comment change. submittedAt stays the original date.
        review.setRating(request.getRating());
        review.setReviewComment(cleanComment(request.getReviewComment()));
        return ReviewMapper.toResponse(courseReviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getReviews(Long courseId, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > maxPageSize) {
            throw new BadRequestException("page must be between 0 and 100000 and size must be between 1 and " + maxPageSize);
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        courseAccessHelper.checkCourseVisible(course);

        // Latest first (the id breaks ties so pages stay in a stable order).
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "submittedAt").and(Sort.by(Sort.Direction.DESC, "reviewId")));
        return PagedResponse.from(courseReviewRepository.findByCourseId(courseId, pageable)
                .map(ReviewMapper::toResponse));
    }

    // The comment is optional: a blank comment is stored as null.
    private String cleanComment(String comment) {
        return (comment == null || comment.isBlank()) ? null : comment.trim();
    }
}
