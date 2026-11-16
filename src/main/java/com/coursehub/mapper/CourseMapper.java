package com.coursehub.mapper;

import com.coursehub.dto.response.CourseDetailsResponse;
import com.coursehub.dto.response.CourseListResponse;
import com.coursehub.entity.Course;

import java.math.BigDecimal;

public final class CourseMapper {

    private CourseMapper() {
    }

    public static CourseListResponse toListResponse(Course course) {
        return new CourseListResponse(
                course.getId(),
                course.getCourseName(),
                course.getCategory().getCategoryName(),
                course.getInstructorName(),
                course.getDifficultyLevel(),
                course.getLanguage(),
                course.getDurationSeconds(),
                course.getStatus());
    }

    // The counts and rating are calculated by the service and passed in.
    public static CourseDetailsResponse toDetailsResponse(Course course, BigDecimal averageRating, Long totalRatings,
                                                          int totalVideos, int totalMaterials, int totalReferences) {
        return new CourseDetailsResponse(
                course.getId(),
                course.getCourseName(),
                course.getDescription(),
                course.getCategory().getCategoryName(),
                course.getCategory().getId(),
                course.getInstructorName(),
                course.getDifficultyLevel(),
                course.getLanguage(),
                course.getDurationSeconds(),
                averageRating,
                totalRatings,
                totalVideos,
                totalMaterials,
                totalReferences,
                course.getStatus(),
                course.getCreatedAt(),
                course.getUpdatedAt());
    }
}
