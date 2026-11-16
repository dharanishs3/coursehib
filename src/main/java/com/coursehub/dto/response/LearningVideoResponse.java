package com.coursehub.dto.response;

import com.coursehub.entity.enums.VideoSourceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A video on the learning page: the video data plus the learner's own progress. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LearningVideoResponse {

    private Long videoId;
    private String videoTitle;
    private VideoSourceType videoSourceType;
    private String playbackUrl;
    private Integer sequenceNumber;
    private Integer durationSeconds;
    private Boolean completed;
    private LocalDateTime completedAt;
}
