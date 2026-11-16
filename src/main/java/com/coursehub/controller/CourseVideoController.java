package com.coursehub.controller;

import com.coursehub.dto.request.VideoLinkRequest;
import com.coursehub.dto.request.VideoUpdateRequest;
import com.coursehub.dto.response.CourseVideoResponse;
import com.coursehub.service.CourseVideoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@RequiredArgsConstructor
@Tag(name = "Course Videos", description = "Upload, link, order and stream course videos")
public class CourseVideoController {

    private final CourseVideoService courseVideoService;

    @GetMapping("/courses/{courseId}/videos")
    public ResponseEntity<List<CourseVideoResponse>> getVideos(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseVideoService.getVideos(courseId));
    }

    @PostMapping(value = "/admin/courses/{courseId}/videos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseVideoResponse> uploadVideo(@PathVariable Long courseId,
                                                           @RequestParam String videoTitle,
                                                           @RequestParam Integer durationSeconds,
                                                           @RequestParam MultipartFile videoFile) {
        CourseVideoResponse response = courseVideoService.uploadVideo(courseId, videoTitle, durationSeconds, videoFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/admin/courses/{courseId}/videos/link",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseVideoResponse> addVideoLink(@PathVariable Long courseId,
                                                            @Valid @RequestBody VideoLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseVideoService.addVideoLink(courseId, request));
    }

    @PutMapping(value = "/admin/courses/{courseId}/videos/{videoId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseVideoResponse> updateVideo(@PathVariable Long courseId,
                                                           @PathVariable Long videoId,
                                                           @Valid @RequestBody VideoUpdateRequest request) {
        return ResponseEntity.ok(courseVideoService.updateVideo(courseId, videoId, request));
    }

    @DeleteMapping("/admin/videos/{videoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long videoId) {
        courseVideoService.deleteVideo(videoId);
        return ResponseEntity.noContent().build();
    }

    // Streams an uploaded video. Spring answers with 206 Partial Content automatically when the
    // request has a Range header, which is what the video player uses for seeking.
    @GetMapping(value = "/videos/{videoId}/stream", produces = MediaType.ALL_VALUE)
    public ResponseEntity<Resource> streamVideo(@PathVariable Long videoId) {
        Resource video = courseVideoService.getVideoFile(videoId);
        MediaType mediaType = MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(video);
    }
}
