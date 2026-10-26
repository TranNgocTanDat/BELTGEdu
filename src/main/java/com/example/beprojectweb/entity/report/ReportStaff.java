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
@Table(name = "report_staff")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportStaff {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 160)
    String name;

    @Column(nullable = false, length = 120)
    String role;

    @Column(nullable = false, length = 2000)
    String scope;

    @Column(name = "expected_output", nullable = false, length = 2000)
    String output;

    @Column(nullable = false, length = 30)
    String status;
}
