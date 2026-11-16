package com.coursehub.dto.response;

import com.coursehub.entity.enums.CourseStatus;
import com.coursehub.entity.enums.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of the course catalogue (table view). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseListResponse {

    private Long id;
    private String courseName;
    private String category;
    private String instructorName;
    private DifficultyLevel difficultyLevel;
    private String language;
    private Long durationSeconds;
    private CourseStatus status;
}
