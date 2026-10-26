package com.example.beprojectweb.entity.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "report_issues")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 2000)
    String title;

    @Column(nullable = false, length = 40)
    String level;

    @Column(nullable = false, length = 2000)
    String proposal;

    @Column(length = 16)
    String kind;

    @Column(length = 160)
    String owner;

    @Column(name = "due_date")
    LocalDate dueDate;

    @Column(length = 8)
    String period;

    @Column(length = 24)
    String status;

    @Column(length = 2000)
    String impact;
}
