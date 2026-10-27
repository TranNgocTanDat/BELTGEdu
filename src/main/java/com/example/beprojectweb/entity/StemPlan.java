package com.example.beprojectweb.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Entity
@Table(name = "stem_plans", uniqueConstraints = @UniqueConstraint(
        name = "uk_stem_plan_term", columnNames = {"registration_id", "grade", "term"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StemPlan {
    @Id @GeneratedValue(strategy = GenerationType.UUID) UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "registration_id", nullable = false) Registration registration;
    @Column(nullable = false) Integer grade;
    @Column(nullable = false) Integer term;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_id", nullable = false) StemCourse course;
    @Builder.Default @Column(name = "effective_from_week", nullable = false, columnDefinition = "integer default 1") Integer effectiveFromWeek = 1;
    @Column(name = "change_note", columnDefinition = "TEXT") String changeNote;
}
