package com.example.beprojectweb.entity;

import com.example.beprojectweb.enums.StemProgram;
import com.example.beprojectweb.enums.StemSubject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "stem_courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StemCourse {
    @Id
    @Column(length = 80)
    String id;

    @Column(nullable = false, length = 40)
    String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    StemProgram program;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    StemSubject subject;

    Integer grade;
    Integer module;

    @Column(nullable = false)
    Integer part;
}
