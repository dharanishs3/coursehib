package com.coursehub.repository;

import com.coursehub.entity.CourseReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    Optional<CourseReview> findByUserIdAndCourseId(Long userId, Long courseId);

    long countByCourseId(Long courseId);

    // One page of the reviews of a course.
    Page<CourseReview> findByCourseId(Long courseId, Pageable pageable);

    List<CourseReview> findTop5ByCourseIdOrderBySubmittedAtDescReviewIdDesc(Long courseId);

    @Query("SELECT AVG(r.rating) FROM CourseReview r WHERE r.course.id = :courseId")
    Double findAverageRatingByCourseId(@Param("courseId") Long courseId);
}
