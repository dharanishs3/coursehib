package com.coursehub.helper;

import com.coursehub.entity.Enrollment;
import com.coursehub.entity.enums.EnrollmentStatus;
import com.coursehub.repository.CourseVideoRepository;
import com.coursehub.repository.EnrollmentRepository;
import com.coursehub.repository.LessonProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Calculates enrollment progress from the real lesson progress records (never by adding or subtracting).
 * completionPercentage = completed videos / total videos * 100 (count based, not duration based).
 * Zero videos: 0.00 and ENROLLED, never COMPLETED.
 * Must be called inside a transaction.
 */
@Component
@RequiredArgsConstructor
public class EnrollmentProgressHelper {

    private final CourseVideoRepository courseVideoRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final EnrollmentRepository enrollmentRepository;

    public void recalculate(Enrollment enrollment) {
        long totalVideos = courseVideoRepository.countByCourseId(enrollment.getCourse().getId());
        long completedVideos = lessonProgressRepository.countByEnrollmentIdAndCompletedTrue(enrollment.getId());

        if (totalVideos == 0 || completedVideos == 0) {
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setCompletedAt(null);
            enrollment.setCompletionPercentage(BigDecimal.ZERO.setScale(2));
        } else if (completedVideos >= totalVideos) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletionPercentage(BigDecimal.valueOf(100).setScale(2));
            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(LocalDateTime.now());
            }
        } else {
            enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
            enrollment.setCompletedAt(null);
            enrollment.setCompletionPercentage(BigDecimal.valueOf(completedVideos * 100)
                    .divide(BigDecimal.valueOf(totalVideos), 2, RoundingMode.HALF_UP));
        }
        enrollmentRepository.save(enrollment);
    }

    // Used when a video is added or deleted: every enrollment of the course may change.
    public void recalculateForCourse(Long courseId) {
        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        enrollments.forEach(this::recalculate);
    }
}
