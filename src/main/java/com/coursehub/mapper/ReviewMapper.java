package com.coursehub.mapper;

import com.coursehub.dto.response.ReviewResponse;
import com.coursehub.entity.CourseReview;

public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewResponse toResponse(CourseReview review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getCourse().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getReviewComment(),
                review.getSubmittedAt());
    }
}
