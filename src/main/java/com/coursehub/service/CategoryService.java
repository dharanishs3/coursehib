package com.coursehub.service;

import com.coursehub.dto.request.CategoryRequest;
import com.coursehub.dto.response.CategoryResponse;
import com.coursehub.entity.Category;
import com.coursehub.exception.ConflictException;
import com.coursehub.exception.ResourceNotFoundException;
import com.coursehub.mapper.CategoryMapper;
import com.coursehub.repository.CategoryRepository;
import com.coursehub.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.getCategoryName().trim();
        if (categoryRepository.existsByCategoryNameIgnoreCase(name)) {
            throw new ConflictException("Category name already exists");
        }
        Category category = Category.builder()
                .categoryName(name)
                .description(cleanDescription(request.getDescription()))
                .build();
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Category category = findCategory(categoryId);
        String name = request.getCategoryName().trim();
        if (categoryRepository.existsByCategoryNameIgnoreCaseAndIdNot(name, categoryId)) {
            throw new ConflictException("Category name already exists");
        }
        category.setCategoryName(name);
        category.setDescription(cleanDescription(request.getDescription()));
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll(Sort.by("categoryName")).stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = findCategory(categoryId);
        // A category that still has courses must never be deleted (and courses are never deleted with it).
        if (courseRepository.existsByCategoryId(categoryId)) {
            throw new ConflictException("Category is used by courses and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private String cleanDescription(String description) {
        return (description == null || description.isBlank()) ? null : description.trim();
    }
}
