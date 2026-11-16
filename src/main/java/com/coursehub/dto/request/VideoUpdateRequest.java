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
public class VideoUpdateRequest {

    @NotBlank(message = "Video title is required")
    @Size(max = 200, message = "Video title must be at most 200 characters")
    private String videoTitle;

    @NotNull(message = "Sequence number is required")
    @Positive(message = "Sequence number must be 1 or more")
    private Integer sequenceNumber;

    @NotNull(message = "Duration in seconds is required")
    @Positive(message = "Duration must be a positive number of seconds")
    private Integer durationSeconds;
}
