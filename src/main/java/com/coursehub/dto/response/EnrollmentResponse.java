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

/** One enrollment of the logged-in learner, with the main data of its course. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private Long enrollmentId;
    private Long courseId;
    private String courseName;
    private String category;
    private String instructorName;
    private DifficultyLevel difficultyLevel;
    private String language;
    private Long durationSeconds;
    private CourseStatus courseStatus;
    private EnrollmentStatus enrollmentStatus;
    private BigDecimal completionPercentage;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}
