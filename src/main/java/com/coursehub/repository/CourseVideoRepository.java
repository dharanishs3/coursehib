package com.coursehub.repository;

import com.coursehub.entity.CourseVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseVideoRepository extends JpaRepository<CourseVideo, Long> {

    List<CourseVideo> findByCourseIdOrderBySequenceNumberAsc(Long courseId);

    long countByCourseId(Long courseId);

    @Query("SELECT COALESCE(MAX(v.sequenceNumber), 0) FROM CourseVideo v WHERE v.course.id = :courseId")
    int findMaxSequenceNumber(@Param("courseId") Long courseId);


    @Query("SELECT COALESCE(SUM(v.durationSeconds), 0L) FROM CourseVideo v WHERE v.course.id = :courseId")
    long sumDurationByCourseId(@Param("courseId") Long courseId);
}
