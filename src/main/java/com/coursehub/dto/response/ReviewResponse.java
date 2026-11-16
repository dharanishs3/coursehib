package com.coursehub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long reviewId;
    private Long courseId;
    private String reviewerName;
    private Integer rating;
    private String reviewComment;
    private LocalDateTime submittedAt;
}
