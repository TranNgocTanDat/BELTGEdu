package com.example.beprojectweb.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "stem_plan_lesson_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stem_plan_lesson_order_item", columnNames = {"plan_item_id", "lesson_id"}),
                @UniqueConstraint(name = "uk_stem_plan_lesson_order_position", columnNames = {"plan_item_id", "lesson_position"})
        },
        indexes = @Index(name = "ix_stem_plan_lesson_order_item", columnList = "plan_item_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StemPlanLessonOrder {
    @Id @GeneratedValue(strategy = GenerationType.UUID) UUID id;
    @Column(name = "plan_item_id", nullable = false) UUID planItemId;
    @Column(name = "lesson_id", nullable = false, length = 100) String lessonId;
    @Column(name = "lesson_position", nullable = false) Integer position;
}