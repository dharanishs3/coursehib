package com.coursehub.mapper;

import com.coursehub.dto.response.CategoryResponse;
import com.coursehub.entity.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getCategoryName(), category.getDescription());
    }
}
