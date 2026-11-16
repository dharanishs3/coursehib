package com.coursehub.entity;

import com.coursehub.entity.enums.VideoSourceType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "course_videos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_course_video_sequence",
                columnNames = {"course_id", "sequence_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String videoTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VideoSourceType videoSourceType;

    @Column(nullable = false, length = 1000)
    private String videoUrl;

    @Column(nullable = false)
    private Integer sequenceNumber;

    // Entered manually
    @Column(nullable = false)
    private Integer durationSeconds;

    @Builder.Default
    @OneToMany(mappedBy = "courseVideo")
    private List<LessonProgress> lessonProgressList = new ArrayList<>();
}
