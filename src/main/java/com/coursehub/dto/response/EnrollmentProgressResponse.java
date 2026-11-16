package com.coursehub.dto.response;

import com.coursehub.entity.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Short progress summary of an enrollment (count based: completed videos / total videos). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentProgressResponse {

    private Long enrollmentId;
    private Long courseId;
    private EnrollmentStatus enrollmentStatus;
    private BigDecimal completionPercentage;
    private Integer completedVideos;
    private Integer totalVideos;
    private LocalDateTime completedAt;
}
