package com.coursehub.mapper;

import com.coursehub.dto.response.CourseMaterialResponse;
import com.coursehub.entity.CourseMaterial;

public final class MaterialMapper {

    private MaterialMapper() {
    }

    public static CourseMaterialResponse toResponse(CourseMaterial material) {
        return new CourseMaterialResponse(
                material.getId(),
                material.getMaterialTitle(),
                material.getFileName(),
                material.getContentType(),
                material.getFileSize(),
                "/api/materials/" + material.getId() + "/download");
    }
}
