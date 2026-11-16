package com.coursehub.dto.response;

import com.coursehub.entity.enums.VideoSourceType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseVideoResponse {

    private Long videoId;
    private String videoTitle;
    private VideoSourceType videoSourceType;
    private String playbackUrl;
    private Integer sequenceNumber;
    private Integer durationSeconds;
}
