package com.example.beprojectweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "stem_event_lessons", uniqueConstraints = @UniqueConstraint(
        name = "uk_stem_event_grade", columnNames = "grade"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StemEventLesson {
    @Id
    @Column(length = 100)
    String id;

    @Column(nullable = false)
    Integer grade;

    @Column(nullable = false, length = 500)
    String title;

    @Column(length = 2000)
    String note;

    @Column(name = "drive_image_url", length = 2048)
    String driveImageUrl;
}
