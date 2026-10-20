package com.example.beprojectweb.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.*;

@Entity @Table(name="registrations", uniqueConstraints=@UniqueConstraint(name="uk_registration_school_year", columnNames={"school_id","academic_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @FieldDefaults(level=AccessLevel.PRIVATE)
public class Registration {
 @Id @GeneratedValue(strategy=GenerationType.UUID) UUID id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="school_id", nullable=false) School school;
 @Column(name="academic_year", nullable=false, length=20) String academicYear;
 @Enumerated(EnumType.STRING) @Column(name="stem_program", length=16) com.example.beprojectweb.enums.StemProgram stemProgram;
 @Enumerated(EnumType.STRING) @Column(name="digital_program", length=16) com.example.beprojectweb.enums.DigitalProgram digitalProgram;
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="registration_stem_grades", joinColumns=@JoinColumn(name="registration_id")) @Column(name="grade", nullable=false) Set<Integer> stemGrades=new LinkedHashSet<>();
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="registration_digital_grades", joinColumns=@JoinColumn(name="registration_id")) @Column(name="grade", nullable=false) Set<Integer> digitalGrades=new LinkedHashSet<>();
 @Column(name="term_1_start") LocalDate term1Start; @Column(name="term_2_start") LocalDate term2Start; @Column(name="term_3_start") LocalDate term3Start; @Column(name="term_4_start") LocalDate term4Start; @Column(name="digital_start") LocalDate digitalStart;
 @Enumerated(EnumType.STRING) @Column(name="stem_study_day", length=12) DayOfWeek stemStudyDay;
 @Enumerated(EnumType.STRING) @Column(name="digital_study_day", length=12) DayOfWeek digitalStudyDay;
}
