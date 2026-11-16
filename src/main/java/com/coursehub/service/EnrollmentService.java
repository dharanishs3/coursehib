package com.coursehub.service;

import com.coursehub.dto.request.VideoProgressRequest;
import com.coursehub.dto.response.CourseMaterialResponse;
import com.coursehub.dto.response.CourseReferenceResponse;
import com.coursehub.dto.response.EnrollmentDetailsResponse;
import com.coursehub.dto.response.EnrollmentProgressResponse;
import com.coursehub.dto.response.EnrollmentResponse;
import com.coursehub.dto.response.LearningVideoResponse;
import com.coursehub.dto.response.ReviewResponse;
import com.coursehub.entity.Course;
import com.coursehub.entity.CourseVideo;
import com.coursehub.entity.Enrollment;
import com.coursehub.entity.LessonProgress;
import com.coursehub.entity.enums.CourseStatus;
import com.coursehub.exception.ConflictException;
import com.coursehub.exception.ForbiddenException;
import com.coursehub.exception.ResourceNotFoundException;
import com.coursehub.helper.EnrollmentProgressHelper;
import com.coursehub.helper.RatingHelper;
import com.coursehub.mapper.EnrollmentMapper;
import com.coursehub.mapper.MaterialMapper;
import com.coursehub.mapper.ReferenceMapper;
import com.coursehub.mapper.ReviewMapper;
import com.coursehub.mapper.VideoMapper;
import com.coursehub.repository.CourseMaterialRepository;
import com.coursehub.repository.CourseReferenceRepository;
import com.coursehub.repository.CourseRepository;
import com.coursehub.repository.CourseReviewRepository;
import com.coursehub.repository.CourseVideoRepository;
import com.coursehub.repository.EnrollmentRepository;
import com.coursehub.repository.LessonProgressRepository;
import com.coursehub.repository.UserRepository;
import com.coursehub.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enrollment and learning progress of the logged-in learner.
 * The learner always comes from the JWT (CurrentUserService), and only the owner of an enrollment may use it.
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final CourseReferenceRepository courseReferenceRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final EnrollmentProgressHelper enrollmentProgressHelper;
    private final RatingHelper ratingHelper;
    private final CurrentUserService currentUserService;

    @Transactional
    public EnrollmentResponse enroll(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new ConflictException("Archived courses do not accept new enrollments");
        }
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ConflictException("Only published courses accept enrollments");
        }

        Long userId = currentUserService.getCurrentUserId();
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ConflictException("You are already enrolled in this course");
        }

        Enrollment enrollment = Enrollment.builder()
                .user(userRepository.getReferenceById(userId))
                .course(course)
                .build(); // status ENROLLED and completion 0.00 by default
        return EnrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments() {
        return enrollmentRepository.findByUserIdOrderByEnrolledAtDescIdDesc(currentUserService.getCurrentUserId())
                .stream()
                .map(EnrollmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnrollmentProgressResponse getProgress(Long enrollmentId) {
        return buildProgress(getOwnedEnrollment(enrollmentId));
    }

    @Transactional
    public EnrollmentProgressResponse updateVideoProgress(Long enrollmentId, Long videoId,
                                                          VideoProgressRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        checkOwner(enrollment);

        CourseVideo video = courseVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        if (!video.getCourse().getId().equals(enrollment.getCourse().getId())) {
            throw new ResourceNotFoundException("Video not found in this course");
        }

        // One progress record per enrollment and video: update it, or create it the first time.
        LessonProgress progress = lessonProgressRepository.findByEnrollmentIdAndCourseVideoId(enrollmentId, videoId)
                .orElseGet(() -> LessonProgress.builder().enrollment(enrollment).courseVideo(video).build());

        boolean completed = request.getCompleted();
        if (completed && !Boolean.TRUE.equals(progress.getCompleted())) {
            progress.setCompletedAt(LocalDateTime.now());
        }
        if (!completed) {
            progress.setCompletedAt(null);
        }
        progress.setCompleted(completed);
        lessonProgressRepository.saveAndFlush(progress);

        enrollmentProgressHelper.recalculate(enrollment);
        return buildProgress(enrollment);
    }

    @Transactional(readOnly = true)
    public EnrollmentDetailsResponse getEnrollmentDetails(Long enrollmentId) {
        Enrollment enrollment = getOwnedEnrollment(enrollmentId);
        Long courseId = enrollment.getCourse().getId();

        // The learner's progress records by video id, so each video can show completed / not completed.
        Map<Long, LessonProgress> progressByVideo = lessonProgressRepository.findByEnrollmentId(enrollmentId)
                .stream()
                .collect(Collectors.toMap(progress -> progress.getCourseVideo().getId(), Function.identity()));

        List<LearningVideoResponse> videos = courseVideoRepository.findByCourseIdOrderBySequenceNumberAsc(courseId)
                .stream()
                .map(video -> VideoMapper.toLearningResponse(video, progressByVideo.get(video.getId())))
                .toList();
        List<CourseMaterialResponse> materials = courseMaterialRepository.findByCourseIdOrderByIdAsc(courseId)
                .stream()
                .map(MaterialMapper::toResponse)
                .toList();
        List<CourseReferenceResponse> references = courseReferenceRepository.findByCourseIdOrderByIdAsc(courseId)
                .stream()
                .map(ReferenceMapper::toResponse)
                .toList();
        List<ReviewResponse> latestReviews = courseReviewRepository
                .findTop5ByCourseIdOrderBySubmittedAtDescReviewIdDesc(courseId)
                .stream()
                .map(ReviewMapper::toResponse)
                .toList();
        ReviewResponse myReview = courseReviewRepository
                .findByUserIdAndCourseId(currentUserService.getCurrentUserId(), courseId)
                .map(ReviewMapper::toResponse)
                .orElse(null);

        BigDecimal averageRating = ratingHelper.getAverageRating(courseId);
        Long totalRatings = ratingHelper.getTotalRatings(courseId);
        return EnrollmentMapper.toDetailsResponse(enrollment, averageRating, totalRatings, videos, materials,
                references, latestReviews, myReview);
    }

    private Enrollment getOwnedEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        checkOwner(enrollment);
        return enrollment;
    }


    private void checkOwner(Enrollment enrollment) {
        if (!enrollment.getUser().getId().equals(currentUserService.getCurrentUserId())) {
            throw new ForbiddenException("This enrollment does not belong to you");
        }
    }

    private EnrollmentProgressResponse buildProgress(Enrollment enrollment) {
        int completedVideos = (int) lessonProgressRepository.countByEnrollmentIdAndCompletedTrue(enrollment.getId());
        int totalVideos = (int) courseVideoRepository.countByCourseId(enrollment.getCourse().getId());
        return EnrollmentMapper.toProgressResponse(enrollment, completedVideos, totalVideos);
    }
}
