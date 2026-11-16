package com.coursehub.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "course_references")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String referenceTitle;

    @Column(nullable = false, length = 1000)
    private String referenceUrl;
}
