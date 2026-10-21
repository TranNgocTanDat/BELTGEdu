package com.example.beprojectweb.entity;

import com.example.beprojectweb.enums.DigitalProgram;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "digital_lessons", uniqueConstraints = @UniqueConstraint(
        name = "uk_digital_lesson_position", columnNames = {"program", "grade", "lesson_order"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DigitalLesson {
    @Id
    @Column(length = 100)
    String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    DigitalProgram program;

    @Column(nullable = false)
    Integer grade;

    @Column(name = "lesson_order", nullable = false)
    Integer lessonOrder;

    @Column(nullable = false, length = 500)
    String title;

    @Column(length = 2000)
    String note;

    @Column(name = "drive_image_url", length = 2048)
    String driveImageUrl;
}
