package com.coursehub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_progress_enrollment_video",
                columnNames = {"enrollment_id", "course_video_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long progressId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_video_id", nullable = false)
    private CourseVideo courseVideo;

    @Builder.Default
    @Column(nullable = false)
    private Boolean completed = false;

    private LocalDateTime completedAt;
}
