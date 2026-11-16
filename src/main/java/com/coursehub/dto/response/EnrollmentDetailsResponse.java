package com.coursehub.dto.response;

import com.coursehub.entity.enums.CourseStatus;
import com.coursehub.entity.enums.DifficultyLevel;
import com.coursehub.entity.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the learning page needs in one response: enrollment, course, videos with progress,
 * materials, references and reviews. Lists are never null. No file paths are included.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDetailsResponse {

    // Enrollment
    private Long enrollmentId;
    private LocalDateTime enrolledAt;
    private EnrollmentStatus enrollmentStatus;
    private BigDecimal completionPercentage;
    private LocalDateTime completedAt;

    // Course
    private Long courseId;
    private String courseName;
    private String description;
    private String category;
    private String instructorName;
    private DifficultyLevel difficultyLevel;
    private String language;
    private Long durationSeconds;
    private CourseStatus courseStatus;
    private BigDecimal averageRating;
    private Long totalRatings;

    // Content
    private List<LearningVideoResponse> videos;
    private List<CourseMaterialResponse> materials;
    private List<CourseReferenceResponse> references;

    // Reviews
    private List<ReviewResponse> latestReviews;
    private ReviewResponse myReview;
}
