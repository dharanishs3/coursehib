package com.coursehub.dto.response;

import com.coursehub.entity.enums.CourseStatus;
import com.coursehub.entity.enums.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailsResponse {

    private Long id;
    private String courseName;
    private String description;
    private String category;
    private Long categoryId;
    private String instructorName;
    private DifficultyLevel difficultyLevel;
    private String language;
    private Long durationSeconds;
    private BigDecimal averageRating;
    private Long totalRatings;
    private Integer totalVideos;
    private Integer totalMaterials;
    private Integer totalReferences;
    private CourseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
