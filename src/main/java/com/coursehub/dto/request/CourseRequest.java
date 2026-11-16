package com.coursehub.dto.request;

import com.coursehub.entity.enums.DifficultyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseRequest {

    @NotBlank(message = "Course name is required")
    @Size(max = 200, message = "Course name must be at most 200 characters")
    private String courseName;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    @NotNull(message = "Category id is required")
    private Long categoryId;

    @NotBlank(message = "Instructor name is required")
    @Size(max = 150, message = "Instructor name must be at most 150 characters")
    private String instructorName;

    @NotNull(message = "Difficulty level is required (BEGINNER, INTERMEDIATE or ADVANCED)")
    private DifficultyLevel difficultyLevel;

    @NotBlank(message = "Language is required")
    @Size(max = 50, message = "Language must be at most 50 characters")
    private String language;
}
