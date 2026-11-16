package com.coursehub.repository;

import com.coursehub.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByCategoryNameIgnoreCase(String categoryName);

    // Used on update: another category (different id) must not already have this name.
    boolean existsByCategoryNameIgnoreCaseAndIdNot(String categoryName, Long id);
}
