package com.coursehub.repository;

import com.coursehub.entity.CourseReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseReferenceRepository extends JpaRepository<CourseReference, Long> {

    List<CourseReference> findByCourseIdOrderByIdAsc(Long courseId);

    long countByCourseId(Long courseId);

}
