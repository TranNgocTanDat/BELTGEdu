package com.example.beprojectweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "stem_lessons", uniqueConstraints = @UniqueConstraint(
        name = "uk_stem_lesson_order", columnNames = {"course_id", "lesson_order"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StemLesson {
    @Id
    @Column(length = 100)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    StemCourse course;

    @Column(name = "lesson_order", nullable = false)
    Integer lessonOrder;

    @Column(nullable = false, length = 500)
    String title;

    @Column(length = 2000)
    String note;

    @Column(name = "drive_image_url", length = 2048)
    String driveImageUrl;

    @Column(name = "drive_image_url_aig", length = 2048)
    String driveImageUrlAig;

    @Column(name = "drive_image_url_etech", length = 2048)
    String driveImageUrlEtech;
}
