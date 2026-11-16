package com.coursehub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class VideoLinkRequest {

    @NotBlank(message = "Video title is required")
    @Size(max = 200, message = "Video title must be at most 200 characters")
    private String videoTitle;

    @NotBlank(message = "Video URL is required")
    @Size(max = 1000, message = "Video URL must be at most 1000 characters")
    private String videoUrl;

    @NotNull(message = "Duration in seconds is required")
    @Positive(message = "Duration must be a positive number of seconds")
    private Integer durationSeconds;
}
