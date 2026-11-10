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
@Table(name = "stem_event_grade_lessons", uniqueConstraints = @UniqueConstraint(
        name = "uk_stem_event_grade_lesson", columnNames = {"event_id", "grade"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StemEventGradeLesson {
    @Id
    @Column(length = 120)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    StemEventTemplate event;

    @Column(nullable = false)
    Integer grade;

    @Column(nullable = false, length = 500)
    String title;

    @Column(length = 2000)
    String note;

    @Column(name = "drive_image_url", length = 2048)
    String driveImageUrl;
}
