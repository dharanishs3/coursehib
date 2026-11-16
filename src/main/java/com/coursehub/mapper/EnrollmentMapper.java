package com.coursehub.mapper;

import com.coursehub.dto.response.CourseMaterialResponse;
import com.coursehub.dto.response.CourseReferenceResponse;
import com.coursehub.dto.response.EnrollmentDetailsResponse;
import com.coursehub.dto.response.EnrollmentProgressResponse;
import com.coursehub.dto.response.EnrollmentResponse;
import com.coursehub.dto.response.LearningVideoResponse;
import com.coursehub.dto.response.ReviewResponse;
import com.coursehub.entity.Course;
import com.coursehub.entity.Enrollment;

import java.math.BigDecimal;
import java.util.List;

public final class EnrollmentMapper {

    private EnrollmentMapper() {
    }

    public static EnrollmentResponse toResponse(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        return new EnrollmentResponse(
                enrollment.getId(),
                course.getId(),
                course.getCourseName(),
                course.getCategory().getCategoryName(),
                course.getInstructorName(),
                course.getDifficultyLevel(),
                course.getLanguage(),
                course.getDurationSeconds(),
                course.getStatus(),
                enrollment.getStatus(),
                enrollment.getCompletionPercentage(),
                enrollment.getEnrolledAt(),
                enrollment.getCompletedAt());
    }

    public static EnrollmentDetailsResponse toDetailsResponse(Enrollment enrollment,
                                                              BigDecimal averageRating, Long totalRatings,
                                                              List<LearningVideoResponse> videos,
                                                              List<CourseMaterialResponse> materials,
                                                              List<CourseReferenceResponse> references,
                                                              List<ReviewResponse> latestReviews,
                                                              ReviewResponse myReview) {
        Course course = enrollment.getCourse();
        return new EnrollmentDetailsResponse(
                enrollment.getId(),
                enrollment.getEnrolledAt(),
                enrollment.getStatus(),
                enrollment.getCompletionPercentage(),
                enrollment.getCompletedAt(),
                course.getId(),
                course.getCourseName(),
                course.getDescription(),
                course.getCategory().getCategoryName(),
                course.getInstructorName(),
                course.getDifficultyLevel(),
                course.getLanguage(),
                course.getDurationSeconds(),
                course.getStatus(),
                averageRating,
                totalRatings,
                videos,
                materials,
                references,
                latestReviews,
                myReview);
    }

    public static EnrollmentProgressResponse toProgressResponse(Enrollment enrollment, int completedVideos,
                                                                int totalVideos) {
        return new EnrollmentProgressResponse(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getStatus(),
                enrollment.getCompletionPercentage(),
                completedVideos,
                totalVideos,
                enrollment.getCompletedAt());
    }
}
