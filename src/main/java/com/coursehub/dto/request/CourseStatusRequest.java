package com.coursehub.dto.request;

import com.coursehub.entity.enums.CourseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseStatusRequest {

    @NotNull(message = "Status is required (DRAFT, PUBLISHED or ARCHIVED)")
    private CourseStatus status;
}
