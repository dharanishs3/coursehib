package com.coursehub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VideoProgressRequest {

    @NotNull(message = "Completed is required (true or false)")
    private Boolean completed;
}
