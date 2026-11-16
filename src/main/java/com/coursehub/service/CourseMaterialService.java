package com.coursehub.service;

import com.coursehub.dto.response.CourseMaterialResponse;
import com.coursehub.entity.Course;
import com.coursehub.entity.CourseMaterial;
import com.coursehub.exception.BadRequestException;
import com.coursehub.exception.ResourceNotFoundException;
import com.coursehub.helper.CourseAccessHelper;
import com.coursehub.mapper.MaterialMapper;
import com.coursehub.repository.CourseMaterialRepository;
import com.coursehub.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Course material files (PDF, DOCX, PPTX, XLSX, TXT). Only ADMIN changes them, enrolled learners and ADMIN read them. */
@Service
@RequiredArgsConstructor
public class CourseMaterialService {

    private final CourseMaterialRepository courseMaterialRepository;
    private final CourseAccessHelper courseAccessHelper;
    private final FileStorageUtil fileStorageUtil;

    @Value("${app.storage.max-material-size-mb}")
    private long maxMaterialSizeMb;

    @Transactional(readOnly = true)
    public List<CourseMaterialResponse> getMaterials(Long courseId) {
        courseAccessHelper.getCourseForContentAccess(courseId);
        return courseMaterialRepository.findByCourseIdOrderByIdAsc(courseId).stream()
                .map(MaterialMapper::toResponse)
                .toList();
    }

    @Transactional
    public CourseMaterialResponse uploadMaterial(Long courseId, String materialTitle, MultipartFile file) {
        if (materialTitle == null || materialTitle.isBlank()) {
            throw new BadRequestException("Material title is required");
        }
        if (materialTitle.trim().length() > 200) {
            throw new BadRequestException("Material title must be at most 200 characters");
        }
        if (file != null && file.getSize() > maxMaterialSizeMb * 1024 * 1024) {
            throw new BadRequestException("File is too large. Maximum size is " + maxMaterialSizeMb + " MB");
        }

        // The original name is kept only for display and download. Any folder part is removed.
        String originalName = (file == null) ? null
                : StringUtils.getFilename(StringUtils.cleanPath(String.valueOf(file.getOriginalFilename())));
        if (originalName != null && (originalName.isBlank() || originalName.startsWith("."))) {
            throw new BadRequestException("Invalid file name");
        }
        if (originalName != null && originalName.length() > 255) {
            throw new BadRequestException("File name must be at most 255 characters");
        }

        Course course = courseAccessHelper.getEditableCourse(courseId);
        String storedName = fileStorageUtil.saveMaterial(file); // also rejects empty files and unsupported types
        CourseMaterial material = CourseMaterial.builder()
                .course(course)
                .materialTitle(materialTitle.trim())
                .fileName(originalName)
                .fileUrl(storedName)
                .contentType(MediaTypeFactory.getMediaType(originalName)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM).toString())
                .fileSize(file.getSize())
                .build();
        courseMaterialRepository.save(material);
        return MaterialMapper.toResponse(material);
    }

    // Metadata of one material (used for the download file name and content type).
    @Transactional(readOnly = true)
    public CourseMaterialResponse getMaterial(Long materialId) {
        return MaterialMapper.toResponse(findMaterialWithAccess(materialId));
    }

    // The access check runs on every download, so knowing the URL is not enough.
    @Transactional(readOnly = true)
    public Resource getMaterialFile(Long materialId) {
        return fileStorageUtil.loadMaterial(findMaterialWithAccess(materialId).getFileUrl());
    }

    @Transactional
    public void deleteMaterial(Long materialId) {
        CourseMaterial material = courseMaterialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
        courseAccessHelper.getEditableCourse(material.getCourse().getId()); // 409 for archived courses

        courseMaterialRepository.delete(material);
        courseMaterialRepository.flush();
        fileStorageUtil.deleteMaterial(material.getFileUrl());
    }

    private CourseMaterial findMaterialWithAccess(Long materialId) {
        CourseMaterial material = courseMaterialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
        courseAccessHelper.checkContentAccess(material.getCourse());
        return material;
    }
}
