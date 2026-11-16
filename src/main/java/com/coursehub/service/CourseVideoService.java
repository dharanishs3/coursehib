package com.coursehub.service;

import com.coursehub.dto.request.VideoLinkRequest;
import com.coursehub.dto.request.VideoUpdateRequest;
import com.coursehub.dto.response.CourseVideoResponse;
import com.coursehub.entity.Course;
import com.coursehub.entity.CourseVideo;
import com.coursehub.entity.enums.VideoSourceType;
import com.coursehub.exception.BadRequestException;
import com.coursehub.exception.ResourceNotFoundException;
import com.coursehub.helper.CourseAccessHelper;
import com.coursehub.helper.EnrollmentProgressHelper;
import com.coursehub.mapper.VideoMapper;
import com.coursehub.repository.CourseRepository;
import com.coursehub.repository.CourseVideoRepository;
import com.coursehub.repository.LessonProgressRepository;
import com.coursehub.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CourseVideoService {

    private final CourseRepository courseRepository;
    private final CourseVideoRepository courseVideoRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseAccessHelper courseAccessHelper;
    private final EnrollmentProgressHelper enrollmentProgressHelper;
    private final FileStorageUtil fileStorageUtil;

    @Transactional(readOnly = true)
    public List<CourseVideoResponse> getVideos(Long courseId) {
        courseAccessHelper.getCourseForContentAccess(courseId);
        return courseVideoRepository.findByCourseIdOrderBySequenceNumberAsc(courseId).stream()
                .map(VideoMapper::toResponse)
                .toList();
    }

    @Transactional
    public CourseVideoResponse addVideoLink(Long courseId, VideoLinkRequest request) {
        Course course = courseAccessHelper.getEditableCourse(courseId);
        CourseVideo video = createVideo(course, request.getVideoTitle().trim(), VideoSourceType.EXTERNAL_LINK,
                request.getVideoUrl().trim(), request.getDurationSeconds());
        courseVideoRepository.save(video);
        afterVideoChange(course);
        return VideoMapper.toResponse(video);
    }

    @Transactional
    public CourseVideoResponse uploadVideo(Long courseId, String videoTitle, Integer durationSeconds,
                                           MultipartFile videoFile) {
        if (videoTitle == null || videoTitle.isBlank()) {
            throw new BadRequestException("Video title is required");
        }
        if (videoTitle.trim().length() > 200) {
            throw new BadRequestException("Video title must be at most 200 characters");
        }
        if (durationSeconds == null || durationSeconds < 1) {
            throw new BadRequestException("Duration must be a positive number of seconds");
        }

        Course course = courseAccessHelper.getEditableCourse(courseId);
        String storedName = fileStorageUtil.saveVideo(videoFile);
        CourseVideo video = createVideo(course, videoTitle.trim(), VideoSourceType.UPLOAD, storedName, durationSeconds);
        courseVideoRepository.save(video);
        afterVideoChange(course);
        return VideoMapper.toResponse(video);
    }

    @Transactional
    public CourseVideoResponse updateVideo(Long courseId, Long videoId, VideoUpdateRequest request) {
        Course course = courseAccessHelper.getEditableCourse(courseId);

        List<CourseVideo> videos = courseVideoRepository.findByCourseIdOrderBySequenceNumberAsc(courseId);
        CourseVideo video = videos.stream()
                .filter(v -> v.getId().equals(videoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Video not found in this course"));

        int newSequence = request.getSequenceNumber();
        if (newSequence > videos.size()) {
            throw new BadRequestException("Sequence number must be between 1 and " + videos.size());
        }

        video.setVideoTitle(request.getVideoTitle().trim());
        video.setDurationSeconds(request.getDurationSeconds());

        if (newSequence != video.getSequenceNumber()) {
            videos.remove(video);
            videos.add(newSequence - 1, video); // move the video to its new place, the others shift automatically
            renumber(videos);
        }

        afterVideoChange(course);
        return VideoMapper.toResponse(video);
    }

    @Transactional
    public void deleteVideo(Long videoId) {
        CourseVideo video = courseVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        Course course = courseAccessHelper.getEditableCourse(video.getCourse().getId());

        lessonProgressRepository.deleteByCourseVideoId(videoId);
        courseVideoRepository.delete(video);
        courseVideoRepository.flush();

        // Close the gap left by the deleted video: numbers go back to 1..N.
        renumber(courseVideoRepository.findByCourseIdOrderBySequenceNumberAsc(course.getId()));
        afterVideoChange(course);

        if (video.getVideoSourceType() == VideoSourceType.UPLOAD) {
            fileStorageUtil.deleteVideo(video.getVideoUrl());
        }
    }

    // Returns the uploaded video file for streaming. Only ADMIN or an enrolled learner may watch it.
    @Transactional(readOnly = true)
    public Resource getVideoFile(Long videoId) {
        CourseVideo video = courseVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        if (video.getVideoSourceType() != VideoSourceType.UPLOAD) {
            throw new BadRequestException("This video is an external link and is not streamed by this server");
        }
        courseAccessHelper.checkContentAccess(video.getCourse());
        return fileStorageUtil.loadVideo(video.getVideoUrl());
    }

    // New videos go to the end: sequence = highest existing sequence + 1.
    private CourseVideo createVideo(Course course, String title, VideoSourceType sourceType, String url,
                                    Integer durationSeconds) {
        return CourseVideo.builder()
                .course(course)
                .videoTitle(title)
                .videoSourceType(sourceType)
                .videoUrl(url)
                .sequenceNumber(courseVideoRepository.findMaxSequenceNumber(course.getId()) + 1)
                .durationSeconds(durationSeconds)
                .build();
    }

    // Sets the numbers to 1..N in list order. The numbers are first made negative, because two videos
    // of one course may never hold the same number at the same moment (unique constraint).
    private void renumber(List<CourseVideo> videos) {
        for (int i = 0; i < videos.size(); i++) {
            videos.get(i).setSequenceNumber(-(i + 1));
        }
        courseVideoRepository.saveAllAndFlush(videos);
        for (int i = 0; i < videos.size(); i++) {
            videos.get(i).setSequenceNumber(i + 1);
        }
        courseVideoRepository.saveAllAndFlush(videos);
    }

    // After any video change: course duration = sum of video durations, and enrollment progress is recalculated.
    private void afterVideoChange(Course course) {
        course.setDurationSeconds(courseVideoRepository.sumDurationByCourseId(course.getId()));
        courseRepository.save(course);
        enrollmentProgressHelper.recalculateForCourse(course.getId());
    }
}
