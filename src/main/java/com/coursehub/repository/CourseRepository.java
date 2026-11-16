package com.coursehub.repository;

import com.coursehub.entity.Course;
import com.coursehub.entity.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // Used to block category deletion while courses still use it.
    boolean existsByCategoryId(Long categoryId);
    @Query("""
            SELECT c FROM Course c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:courseName IS NULL OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :courseName, '%')))
              AND (:categoryName IS NULL OR LOWER(c.category.categoryName) LIKE LOWER(CONCAT('%', :categoryName, '%')))
              AND (:instructorName IS NULL OR LOWER(c.instructorName) LIKE LOWER(CONCAT('%', :instructorName, '%')))
              AND (:keyword IS NULL
                   OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.instructorName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.category.categoryName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Course> search(@Param("status") CourseStatus status,
                        @Param("courseName") String courseName,
                        @Param("categoryName") String categoryName,
                        @Param("instructorName") String instructorName,
                        @Param("keyword") String keyword,
                        Pageable pageable);
}
