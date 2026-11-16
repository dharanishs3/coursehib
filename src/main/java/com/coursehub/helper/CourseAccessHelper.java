package com.coursehub.helper;

import com.coursehub.entity.Course;
import com.coursehub.entity.enums.CourseStatus;
import com.coursehub.entity.enums.UserRole;
import com.coursehub.exception.ConflictException;
import com.coursehub.exception.ForbiddenException;
import com.coursehub.exception.ResourceNotFoundException;
import com.coursehub.repository.CourseRepository;
import com.coursehub.repository.EnrollmentRepository;
import com.coursehub.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Shared checks used by the video, material and reference services.
 * Must be called inside a transaction.
 */
@Component
@RequiredArgsConstructor
public class CourseAccessHelper {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUserService;

    /** For viewing course details and reviews: ADMIN sees all. A LEARNER sees PUBLISHED courses, and ARCHIVED ones only if enrolled. */
    public void checkCourseVisible(Course course) {
        if (currentUserService.getCurrentRole() == UserRole.ADMIN || course.getStatus() == CourseStatus.PUBLISHED) {
            return;
        }
        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseId(
                currentUserService.getCurrentUserId(), course.getId());
        if (course.getStatus() == CourseStatus.ARCHIVED && enrolled) {
            return;
        }
        throw new ResourceNotFoundException("Course not found");
    }

    /** For ADMIN changes: the course must exist and must not be ARCHIVED. */
    public Course getEditableCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new ConflictException("Archived courses cannot be modified");
        }
        return course;
    }

    /** For viewing course content: ADMIN, or a learner enrolled in the course (archived courses stay open for them). */
    public Course getCourseForContentAccess(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        checkContentAccess(course);
        return course;
    }

    public void checkContentAccess(Course course) {
        if (currentUserService.getCurrentRole() == UserRole.ADMIN) {
            return;
        }
        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseId(
                currentUserService.getCurrentUserId(), course.getId());
        if (!enrolled) {
            throw new ForbiddenException("You must be enrolled in this course to access its content");
        }
    }
}
