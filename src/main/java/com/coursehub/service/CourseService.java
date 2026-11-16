package com.coursehub.service;

import com.coursehub.dto.request.CourseRequest;
import com.coursehub.dto.request.CourseStatusRequest;
import com.coursehub.dto.response.CourseDetailsResponse;
import com.coursehub.dto.response.CourseListResponse;
import com.coursehub.dto.response.PagedResponse;
import com.coursehub.entity.Category;
import com.coursehub.entity.Course;
import com.coursehub.entity.CourseMaterial;
import com.coursehub.entity.CourseVideo;
import com.coursehub.entity.enums.CourseStatus;
import com.coursehub.entity.enums.UserRole;
import com.coursehub.entity.enums.VideoSourceType;
import com.coursehub.exception.BadRequestException;
import com.coursehub.exception.ConflictException;
import com.coursehub.exception.ResourceNotFoundException;
import com.coursehub.helper.CourseAccessHelper;
import com.coursehub.helper.RatingHelper;
import com.coursehub.mapper.CourseMapper;
import com.coursehub.repository.CategoryRepository;
import com.coursehub.repository.CourseMaterialRepository;
import com.coursehub.repository.CourseReferenceRepository;
import com.coursehub.repository.CourseRepository;
import com.coursehub.repository.CourseVideoRepository;
import com.coursehub.repository.EnrollmentRepository;
import com.coursehub.security.CurrentUserService;
import com.coursehub.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseService {


    // Only these fields can be used for sorting (never arbitrary entity fields).
    private static final Set<String> SORT_FIELDS =
            Set.of("courseName", "instructorName", "durationSeconds", "createdAt");

    @Value("${app.pagination.max-page-size}")
    private int maxPageSize;

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final CourseReferenceRepository courseReferenceRepository;
    private final RatingHelper ratingHelper;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageUtil fileStorageUtil;
    private final CourseAccessHelper courseAccessHelper;

    @Transactional
    public CourseDetailsResponse createCourse(CourseRequest request) {
        Course course = Course.builder()
                .courseName(request.getCourseName().trim())
                .description(request.getDescription().trim())
                .category(findCategory(request.getCategoryId()))
                .instructorName(request.getInstructorName().trim())
                .difficultyLevel(request.getDifficultyLevel())
                .language(request.getLanguage().trim())
                .build(); // status defaults to DRAFT and duration to 0
        return toDetails(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public CourseDetailsResponse getCourseById(Long courseId) {
        Course course = findCourse(courseId);
        courseAccessHelper.checkCourseVisible(course);
        return toDetails(course);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CourseListResponse> searchCourses(String courseName, String category, String instructorName,
                                                           String keyword, int page, int size,
                                                           String sortBy, String sortDirection) {
        // Check the paging inputs, then build the Pageable. Only whitelisted sort fields are allowed.
        if (page < 0 || page > 100000 || size < 1 || size > maxPageSize) {
            throw new BadRequestException("page must be between 0 and 100000 and size must be between 1 and " + maxPageSize);
        }
        if (!SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("sortBy must be one of " + SORT_FIELDS);
        }
        if (!sortDirection.equalsIgnoreCase("asc") && !sortDirection.equalsIgnoreCase("desc")) {
            throw new BadRequestException("sortDirection must be asc or desc");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

        // ADMIN sees every status, a LEARNER sees only PUBLISHED courses.
        CourseStatus status = currentUserService.getCurrentRole() == UserRole.ADMIN ? null : CourseStatus.PUBLISHED;

        Page<CourseListResponse> result = courseRepository
                .search(status, clean(courseName), clean(category), clean(instructorName), clean(keyword), pageable)
                .map(CourseMapper::toListResponse);
        return PagedResponse.from(result);
    }

    @Transactional
    public CourseDetailsResponse updateCourse(Long courseId, CourseRequest request) {
        Course course = findCourse(courseId);
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new ConflictException("Archived courses cannot be modified");
        }
        course.setCourseName(request.getCourseName().trim());
        course.setDescription(request.getDescription().trim());
        course.setCategory(findCategory(request.getCategoryId()));
        course.setInstructorName(request.getInstructorName().trim());
        course.setDifficultyLevel(request.getDifficultyLevel());
        course.setLanguage(request.getLanguage().trim());
        return toDetails(courseRepository.save(course));
    }

    /**
     * Allowed changes: DRAFT -> PUBLISHED, DRAFT -> ARCHIVED, PUBLISHED -> ARCHIVED,
     * PUBLISHED -> DRAFT (only while nobody is enrolled). ARCHIVED is permanent.
     */
    @Transactional
    public CourseDetailsResponse changeStatus(Long courseId, CourseStatusRequest request) {
        Course course = findCourse(courseId);
        CourseStatus current = course.getStatus();
        CourseStatus target = request.getStatus();

        if (current == CourseStatus.ARCHIVED) {
            throw new ConflictException("Archived courses are permanent and their status cannot be changed");
        }
        if (current == target) {
            throw new ConflictException("Course is already " + current);
        }
        if (current == CourseStatus.PUBLISHED && target == CourseStatus.DRAFT
                && enrollmentRepository.existsByCourseId(courseId)) {
            throw new ConflictException("A course with enrollments cannot go back to DRAFT. Archive it instead");
        }

        course.setStatus(target);
        return toDetails(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = findCourse(courseId);
        if (enrollmentRepository.existsByCourseId(courseId)) {
            throw new ConflictException("This course has enrollments and cannot be deleted. Archive the course instead");
        }
        // Uploaded video and material files are deleted from disk after the database rows are gone.
        List<String> videoFiles = courseVideoRepository.findByCourseIdOrderBySequenceNumberAsc(courseId).stream()
                .filter(video -> video.getVideoSourceType() == VideoSourceType.UPLOAD)
                .map(CourseVideo::getVideoUrl)
                .toList();
        List<String> materialFiles = courseMaterialRepository.findByCourseIdOrderByIdAsc(courseId).stream()
                .map(CourseMaterial::getFileUrl)
                .toList();
        courseRepository.delete(course);
        courseRepository.flush();
        videoFiles.forEach(fileStorageUtil::deleteVideo);
        materialFiles.forEach(fileStorageUtil::deleteMaterial);
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private CourseDetailsResponse toDetails(Course course) {
        Long courseId = course.getId();
        return CourseMapper.toDetailsResponse(
                course,
                ratingHelper.getAverageRating(courseId),
                ratingHelper.getTotalRatings(courseId),
                (int) courseVideoRepository.countByCourseId(courseId),
                (int) courseMaterialRepository.countByCourseId(courseId),
                (int) courseReferenceRepository.countByCourseId(courseId));
    }

    // Blank filter values are treated as "no filter".
    private String clean(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
