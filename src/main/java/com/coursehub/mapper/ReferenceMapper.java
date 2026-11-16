package com.coursehub.mapper;

import com.coursehub.dto.response.CourseReferenceResponse;
import com.coursehub.entity.CourseReference;

public final class ReferenceMapper {

    private ReferenceMapper() {
    }

    public static CourseReferenceResponse toResponse(CourseReference reference) {
        return new CourseReferenceResponse(
                reference.getId(),
                reference.getReferenceTitle(),
                reference.getReferenceUrl());
    }
}
