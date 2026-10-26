package com.example.beprojectweb.entity.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "report_goals")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 8)
    String period;

    @Column(nullable = false, length = 500)
    String title;

    @Column(nullable = false, length = 2000)
    String output;

    @Column(nullable = false, length = 160)
    String owner;

    @Column(nullable = false)
    Integer progress;

    @Column(nullable = false, length = 30)
    String status;
}
