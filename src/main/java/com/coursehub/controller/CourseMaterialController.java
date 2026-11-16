package com.coursehub.controller;

import com.coursehub.dto.response.CourseMaterialResponse;
import com.coursehub.service.CourseMaterialService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@RequiredArgsConstructor
public class CourseMaterialController {

    private final CourseMaterialService courseMaterialService;

    @GetMapping("/courses/{courseId}/materials")
    public ResponseEntity<List<CourseMaterialResponse>> getMaterials(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseMaterialService.getMaterials(courseId));
    }

    @PostMapping(value = "/admin/courses/{courseId}/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseMaterialResponse> uploadMaterial(@PathVariable Long courseId,
                                                                 @RequestParam String materialTitle,
                                                                 @RequestParam MultipartFile file) {
        CourseMaterialResponse response = courseMaterialService.uploadMaterial(courseId, materialTitle, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Returns the file as binary. The client sends the Bearer token with fetch and saves the returned Blob.
    @GetMapping(value = "/materials/{materialId}/download", produces = MediaType.ALL_VALUE)
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long materialId) {
        CourseMaterialResponse material = courseMaterialService.getMaterial(materialId);
        Resource file = courseMaterialService.getMaterialFile(materialId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(material.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(material.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file);
    }

    @DeleteMapping("/admin/materials/{materialId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long materialId) {
        courseMaterialService.deleteMaterial(materialId);
        return ResponseEntity.noContent().build();
    }
}
