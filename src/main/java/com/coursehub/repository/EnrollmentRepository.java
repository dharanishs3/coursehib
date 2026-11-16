package com.coursehub.repository;

import com.coursehub.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByCourseId(Long courseId);

    List<Enrollment> findByUserIdOrderByEnrolledAtDescIdDesc(Long userId);

    List<Enrollment> findByCourseId(Long courseId);
}
