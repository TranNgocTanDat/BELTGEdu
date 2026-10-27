package com.example.beprojectweb.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Entity
@Table(name = "stem_plan_versions", indexes = {
        @Index(name = "ix_stem_plan_version_lookup", columnList = "registration_id,grade,term,effective_from_week")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StemPlanVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "registration_id", nullable = false) Registration registration;
    @Column(nullable = false) Integer grade;
    @Column(nullable = false) Integer term;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_id", nullable = false) StemCourse course;
    @Column(name = "effective_from_week", nullable = false) Integer effectiveFromWeek;
    @Column(name = "change_note", columnDefinition = "TEXT") String changeNote;
    @Builder.Default @Column(nullable = false, columnDefinition = "boolean default true") boolean active = true;
}
