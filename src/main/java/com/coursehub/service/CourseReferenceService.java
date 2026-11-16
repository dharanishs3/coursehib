package com.coursehub.service;

import com.coursehub.dto.request.ReferenceRequest;
import com.coursehub.dto.response.CourseReferenceResponse;
import com.coursehub.entity.Course;
import com.coursehub.entity.CourseReference;
import com.coursehub.exception.ResourceNotFoundException;
import com.coursehub.helper.CourseAccessHelper;
import com.coursehub.mapper.ReferenceMapper;
import com.coursehub.repository.CourseReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** External reference links of a course. Only ADMIN changes them, enrolled learners and ADMIN read them. */
@Service
@RequiredArgsConstructor
public class CourseReferenceService {

    private final CourseReferenceRepository courseReferenceRepository;
    private final CourseAccessHelper courseAccessHelper;

    @Transactional(readOnly = true)
    public List<CourseReferenceResponse> getReferences(Long courseId) {
        courseAccessHelper.getCourseForContentAccess(courseId);
        return courseReferenceRepository.findByCourseIdOrderByIdAsc(courseId).stream()
                .map(ReferenceMapper::toResponse)
                .toList();
    }

    @Transactional
    public CourseReferenceResponse addReference(Long courseId, ReferenceRequest request) {
        Course course = courseAccessHelper.getEditableCourse(courseId); // 409 for archived courses
        CourseReference reference = CourseReference.builder()
                .course(course)
                .referenceTitle(request.getReferenceTitle().trim())
                .referenceUrl(request.getReferenceUrl().trim())
                .build();
        return ReferenceMapper.toResponse(courseReferenceRepository.save(reference));
    }

    @Transactional
    public CourseReferenceResponse updateReference(Long referenceId, ReferenceRequest request) {
        CourseReference reference = findReference(referenceId);
        courseAccessHelper.getEditableCourse(reference.getCourse().getId());

        reference.setReferenceTitle(request.getReferenceTitle().trim());
        reference.setReferenceUrl(request.getReferenceUrl().trim());
        return ReferenceMapper.toResponse(courseReferenceRepository.save(reference));
    }

    @Transactional
    public void deleteReference(Long referenceId) {
        CourseReference reference = findReference(referenceId);
        courseAccessHelper.getEditableCourse(reference.getCourse().getId());
        courseReferenceRepository.delete(reference);
    }

    private CourseReference findReference(Long referenceId) {
        return courseReferenceRepository.findById(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Reference not found"));
    }
}
