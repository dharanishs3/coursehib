package com.coursehub.mapper;

import com.coursehub.dto.response.CourseVideoResponse;
import com.coursehub.dto.response.LearningVideoResponse;
import com.coursehub.entity.CourseVideo;
import com.coursehub.entity.LessonProgress;
import com.coursehub.entity.enums.VideoSourceType;

public final class VideoMapper {

    private VideoMapper() {
    }

    public static CourseVideoResponse toResponse(CourseVideo video) {
        return new CourseVideoResponse(
                video.getId(),
                video.getVideoTitle(),
                video.getVideoSourceType(),
                playbackUrl(video),
                video.getSequenceNumber(),
                video.getDurationSeconds());
    }

    // progress is null when the learner has no progress record for this video yet (then completed = false).
    public static LearningVideoResponse toLearningResponse(CourseVideo video, LessonProgress progress) {
        boolean completed = progress != null && Boolean.TRUE.equals(progress.getCompleted());
        return new LearningVideoResponse(
                video.getId(),
                video.getVideoTitle(),
                video.getVideoSourceType(),
                playbackUrl(video),
                video.getSequenceNumber(),
                video.getDurationSeconds(),
                completed,
                completed ? progress.getCompletedAt() : null);
    }

    // Uploaded videos are played through the protected stream endpoint, so the stored file name is never exposed.
    public static String playbackUrl(CourseVideo video) {
        if (video.getVideoSourceType() == VideoSourceType.UPLOAD) {
            return "/api/videos/" + video.getId() + "/stream";
        }
        return video.getVideoUrl();
    }
}
